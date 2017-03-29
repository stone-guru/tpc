package net.eric.tpc.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.eric.tpc.biz.BizCode;
import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.UnImplementedException;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.HeartBeatRequestHandler;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.UnknowCommandRequestHandler;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.PeerTransactionState;

public class BankServerHandler extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BankServerHandler.class);

    private static final String TRANS_SESSION_ITEM = "transactionSession";

    private Map<String, RequestHandler> requestHandlerMap = initRequestHandlers();

    private RequestHandler unknownCommandHandler = new UnknowCommandRequestHandler();

    private PeerTransactionManager transManager;

    private Map<String, RequestHandler> initRequestHandlers() {
        List<RequestHandler> handlers = new ArrayList<RequestHandler>(10);
        handlers.add(new HeartBeatRequestHandler());
        handlers.add(new BeginTransRequestHandler());
        handlers.add(new VoteRequestHandler());
        handlers.add(new TransDecisionHandler());

        Map<String, RequestHandler> handlerMap = new HashMap<String, RequestHandler>();
        for (RequestHandler h : handlers) {
            handlerMap.put(h.getCorrespondingCode(), h);
        }
        return handlerMap;
    }

    private RequestHandler getRequestHandler(String code) {
        RequestHandler handler = requestHandlerMap.get(code);
        if (handler == null) {
            return this.unknownCommandHandler;
        }
        return handler;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
        session.setAttribute(BankServerHandler.TRANS_SESSION_ITEM, new PeerTransactionState());
    }

    private Optional<PeerTransactionState> getTransStateFromSession(IoSession session) {
        PeerTransactionState state = (PeerTransactionState) session.getAttribute(BankServerHandler.TRANS_SESSION_ITEM);
        return Optional.fromNullable(state);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received: " + message.toString());
        }

        final DataPacket request = (DataPacket) message;
        RequestHandler handler = this.getRequestHandler(request.getCode());
        Optional<PeerTransactionState> state = getTransStateFromSession(session);

        if (!state.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction state, do nothing");
            }
            return;
        }

        final Optional<DataPacket> response = handler.process(request, state.get());

        if (response.isPresent()) {
            WriteFuture wf = session.write(response.get());

            if (logger.isDebugEnabled()) {
                wf.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        logger.debug("response written : " + response.get());
                    }
                });
            }
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        if (!IdleStatus.READER_IDLE.equals(status)) {
            return;
        }
        System.out.println("READER IDLE " + session.getIdleCount(status));

        @SuppressWarnings("rawtypes")
        Optional<PeerTransactionState> state = getTransStateFromSession(session);

        if (!state.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction state, do nothing in sessionIdle");
            }
            return;
        }

        BankServerHandler.this.transManager.processTimeout(state.get());
    }

    public void setTransManager(PeerTransactionManager transManager) {
        this.transManager = transManager;
    }

    class BeginTransRequestHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.BEGIN_TRANS;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState state) {
            TransStartRec transNodes = (TransStartRec) request.getParam1();
            TransferBill transMsg = (TransferBill) request.getParam2();

            ActionStatus r = BankServerHandler.this.transManager.beginTrans(transNodes, transMsg, state);

            if (r.isOK()) {
                return Optional.of(new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, DataPacket.YES));
            } else {
                return Optional.of(new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, DataPacket.NO, r.toString()));
            }
        }

        @Override
        public boolean requireTransState() {
            return true;
        }
    }

    class VoteRequestHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.VOTE_REQ;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState state) {
            String xid = (String) request.getParam1();

            ActionStatus r = BankServerHandler.this.transManager.processVoteReq(xid, state);
            if (r.isOK()) {
                return Optional.of(new DataPacket(DataPacket.VOTE_ANSWER, DataPacket.YES));
            } else {
                return Optional.of(new DataPacket(DataPacket.VOTE_ANSWER, DataPacket.NO, r.toString()));
            }
        }

        @Override
        public boolean requireTransState() {
            return true;
        }
    }

    class TransDecisionHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.TRANS_DECISION;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState state) {
            String xid = (String) request.getParam1();
            String decision = (String) request.getParam2();

            if (DataPacket.YES.equals(decision)) {
                BankServerHandler.this.transManager.processTransDecision(xid, Decision.COMMIT, state);
            } else if (DataPacket.NO.equals(decision)) {
                BankServerHandler.this.transManager.processTransDecision(xid, Decision.ABORT, state);
            } else {
                logger.error(BizCode.PEER_PRTC_ERROR, "Unknown commit decision " + decision);
            }
            return Optional.absent();
        }

        @Override
        public boolean requireTransState() {
            return true;
        }
    }

    class DecisionQueryHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.DECISION_QUERY;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState state) {
            String xid = (String) request.getParam1();

            Optional<Decision> decision = BankServerHandler.this.transManager.queryDecision(xid);
            String param2 = null;
            if (decision.isPresent()) {
                switch (decision.get()) {
                case ABORT:
                    param2 = DataPacket.NO;
                    break;
                case COMMIT:
                    param2 = DataPacket.YES;
                    break;
                default:
                    throw new UnImplementedException();
                }
            } else {
                param2 = DataPacket.UNKNOWN;
            }

            return Optional.of(new DataPacket(DataPacket.DECISION_ANSWER, xid, param2));
        }

        @Override
        public boolean requireTransState() {
            return true;
        }
    }
}
