package net.eric.tpc.net;

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

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.PeerTransactionState;

public class PeerIoHandler extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PeerIoHandler.class);

    private static final String TRANS_SESSION_ITEM = "transactionSession";

    private Map<String, RequestHandler<TransferBill>> requestHandlerMap = initRequestHandlers();

    private RequestHandler<TransferBill> unknownCommandHandler = new UnknowCommandRequestHandler<TransferBill>();

    private PeerTransactionManager<TransferBill> transManager;

    private Map<String, RequestHandler<TransferBill>> initRequestHandlers() {
        List<RequestHandler<TransferBill>> handlers = new ArrayList<RequestHandler<TransferBill>>(10);
        handlers.add(new HeartBeatRequestHandler<TransferBill>());
        handlers.add(new BeginTransRequestHandler());
        handlers.add(new VoteRequestHandler());
        handlers.add(new TransDecisionHandler());

        Map<String, RequestHandler<TransferBill>> handlerMap = new HashMap<String, RequestHandler<TransferBill>>();
        for (RequestHandler<TransferBill> h : handlers) {
            handlerMap.put(h.getCorrespondingCode(), h);
        }
        return handlerMap;
    }

    private RequestHandler<TransferBill> getRequestHandler(String code) {
        RequestHandler<TransferBill> handler = requestHandlerMap.get(code);
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
        session.setAttribute(PeerIoHandler.TRANS_SESSION_ITEM, new PeerTransactionState<TransferBill>());
    }

    private Optional<PeerTransactionState<TransferBill>> getTransStateFromSession(IoSession session) {
        @SuppressWarnings("unchecked")
        PeerTransactionState<TransferBill> state = (PeerTransactionState<TransferBill>) session.getAttribute(PeerIoHandler.TRANS_SESSION_ITEM);
        return Optional.fromNullable(state);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received: " + message.toString());
        }

        final DataPacket request = (DataPacket) message;
        RequestHandler<TransferBill> handler = this.getRequestHandler(request.getCode());
        Optional<PeerTransactionState<TransferBill>> state = getTransStateFromSession(session);

        if (!state.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction state, do nothing");
            }
            return;
        }

        final Optional<DataPacket> opt = handler.process(request, state.get());

        if (opt.isPresent()) {
            DataPacket response = opt.get(); 
            response.setRound(request.getRound());
            WriteFuture wf = session.write(response);

            if (logger.isDebugEnabled()) {
                wf.addListener(new IoFutureListener<IoFuture>() {
                    public void operationComplete(IoFuture future) {
                        logger.debug("response written : " + response);
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
        //System.out.println("READER IDLE " + session.getIdleCount(status));

        Optional<PeerTransactionState<TransferBill>> state = getTransStateFromSession(session);

        if (!state.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction state, do nothing in sessionIdle");
            }
            return;
        }

        PeerIoHandler.this.transManager.processTimeout(state.get());
    }

    public void setTransManager(PeerTransactionManager<TransferBill> transManager) {
        this.transManager = transManager;
    }

    class BeginTransRequestHandler implements RequestHandler<TransferBill> {
        public String getCorrespondingCode() {
            return DataPacket.BEGIN_TRANS;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState<TransferBill> state) {
            TransStartRec transNodes = (TransStartRec) request.getParam1();
            TransferBill transMsg = (TransferBill) request.getParam2();

            ActionStatus r = PeerIoHandler.this.transManager.beginTrans(transNodes, transMsg, state);

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

    class VoteRequestHandler implements RequestHandler<TransferBill> {
        public String getCorrespondingCode() {
            return DataPacket.VOTE_REQ;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();

            ActionStatus r = PeerIoHandler.this.transManager.processVoteReq(xid, state);
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

    class TransDecisionHandler implements RequestHandler<TransferBill> {
        public String getCorrespondingCode() {
            return DataPacket.TRANS_DECISION;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();
            String decision = (String) request.getParam2();

            if (DataPacket.YES.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.COMMIT, state);
            } else if (DataPacket.NO.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.ABORT, state);
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

    class DecisionQueryHandler implements RequestHandler<TransferBill> {
        public String getCorrespondingCode() {
            return DataPacket.DECISION_QUERY;
        }

        public Optional<DataPacket> process(DataPacket request, PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();

            Optional<Decision> decision = PeerIoHandler.this.transManager.queryDecision(xid);
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
            return false;
        }
    }
}
