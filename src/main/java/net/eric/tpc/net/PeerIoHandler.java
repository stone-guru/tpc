package net.eric.tpc.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

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
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.PeerTransactionState;
import net.eric.tpc.proto.TransStartRec;

import static net.eric.tpc.base.Pair.asPair;

public class PeerIoHandler extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PeerIoHandler.class);

    private static final String TRANS_SESSION_ITEM = "transactionSession";

    private Map<String, RequestHandler<TransferBill>> requestHandlerMap = initRequestHandlers();
    private RequestHandler<TransferBill> unknownCommandHandler = new UnknowCommandRequestHandler<TransferBill>();
    private PeerTransactionManager<TransferBill> transManager;
    private ExecutorService commuTaskPool;

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

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received: " + message.toString());
        }

        DataPacket request = (DataPacket) message;

        Optional<PeerTransactionState<TransferBill>> opt = getTransStateFromSession(session);
        RequestHandler<TransferBill> handler = this.getRequestHandler(request.getCode());

        if (handler.requireTransState() && !opt.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction state, do nothing");
            }
            return;
        }

        final Pair<Optional<DataPacket>, Boolean> result = handler.process(request, opt.orNull());
        final boolean closeAfterSend = result.snd();
        if (result.fst().isPresent()) {
            this.sendMessage(session, result.fst().get(), closeAfterSend);
        } else if (closeAfterSend) {
            session.closeOnFlush();
        }
    }

    private void sendMessage(IoSession session, DataPacket packet, boolean closeAfterSend) {
        
        final WriteFuture wf = session.write(packet);

        if (logger.isDebugEnabled()) {
            wf.addListener(new IoFutureListener<IoFuture>() {
                public void operationComplete(IoFuture future) {
                    logger.debug("response written : " + packet);
                }
            });
        }
        if (closeAfterSend) {
            Runnable closeAction = new Runnable() {
                @Override
                public void run() {
                    wf.awaitUninterruptibly();
                    session.closeOnFlush();
                }
            };
            if(this.commuTaskPool == null){
                System.out.println("commuTaskPool is null");
            }
            this.commuTaskPool.submit(closeAction);
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        if (!IdleStatus.READER_IDLE.equals(status)) {
            return;
        }

        Optional<PeerTransactionState<TransferBill>> state = getTransStateFromSession(session);
        if (!state.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No transaction state on this session, do nothing in sessionIdle");
            }
            return;
        }

        PeerIoHandler.this.transManager.processTimeout(state.get());
    }

    public void setTransManager(PeerTransactionManager<TransferBill> transManager) {
        this.transManager = transManager;
    }

    public void setCommuTaskPool(ExecutorService commuTaskPool) {
        this.commuTaskPool = commuTaskPool;
    }

    private Optional<PeerTransactionState<TransferBill>> getTransStateFromSession(IoSession session) {
        @SuppressWarnings("unchecked")
        PeerTransactionState<TransferBill> state = (PeerTransactionState<TransferBill>) session
                .getAttribute(PeerIoHandler.TRANS_SESSION_ITEM);
        return Optional.fromNullable(state);
    }

    class BeginTransRequestHandler implements RequestHandler<TransferBill> {
        public String getCorrespondingCode() {
            return DataPacket.BEGIN_TRANS;
        }

        private Maybe<Pair<TransStartRec, TransferBill>> peekBizEntities(DataPacket request) {
            Maybe<TransStartRec> startRec = Maybe.safeCast(request.getParam2(), TransStartRec.class, //
                    DataPacket.BAD_DATA_PACKET, "param2 should be a TransStartRec");
            if (!startRec.isRight()) {
                return Maybe.fail(startRec.getLeft());
            }
            Maybe<TransferBill> bill = Maybe.safeCast(request.getParam3(), TransferBill.class, //
                    DataPacket.BAD_DATA_PACKET, "param3 should be a TransStartRec");
            if (!bill.isRight()) {
                return Maybe.fail(bill.getLeft());
            }
            return Maybe.success(asPair(startRec.getRight(), bill.getRight()));
        }

        public Pair<Optional<DataPacket>, Boolean> process(DataPacket request,
                PeerTransactionState<TransferBill> state) {
            DataPacket response = null;
            ActionStatus r = null;

            Maybe<Pair<TransStartRec, TransferBill>> bizEntities = this.peekBizEntities(request);
            if (!bizEntities.isRight()) {
                r = bizEntities.getLeft();
            } else {
                TransStartRec startRec = bizEntities.getRight().fst();
                TransferBill bill = bizEntities.getRight().snd();
                r = PeerIoHandler.this.transManager.beginTrans(startRec, bill, state);
            }
            if (r.isOK()) {
                response = new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, request.getParam1(), DataPacket.YES);
            } else {
                response = new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, request.getParam1(), DataPacket.NO, r);
            }
            return asPair(Optional.of(response), !r.isOK());
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

        public Pair<Optional<DataPacket>, Boolean> process(DataPacket request,
                PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();

            ActionStatus r = PeerIoHandler.this.transManager.processVoteReq(xid, state);
            DataPacket response = null;
            if (r.isOK()) {
                response = new DataPacket(DataPacket.VOTE_ANSWER, xid, DataPacket.YES);
            } else {
                response = new DataPacket(DataPacket.VOTE_ANSWER, xid, DataPacket.NO, r);
            }
            return asPair(Optional.of(response), !r.isOK());
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

        public Pair<Optional<DataPacket>, Boolean> process(DataPacket request,
                PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();
            String decision = (String) request.getParam2();

            if (DataPacket.YES.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.COMMIT, state);
            } else if (DataPacket.NO.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.ABORT, state);
            } else {
                logger.error(DataPacket.PEER_PRTC_ERROR, "Unknown commit decision " + decision);
            }
            return asPair(Optional.absent(), true);
        }

        @Override
        public boolean requireTransState() {
            return true;
        }
    }

    // class DecisionQueryHandler implements RequestHandler<TransferBill> {
    // public String getCorrespondingCode() {
    // return DataPacket.DECISION_QUERY;
    // }
    //
    // public Pair<Optional<DataPacket>, Boolean> process(DataPacket request,
    // PeerTransactionState<TransferBill> state) {
    // String xid = (String) request.getParam1();
    //
    // Optional<Decision> decision =
    // PeerIoHandler.this.transManager.queryDecision(xid);
    // String param2 = null;
    // if (decision.isPresent()) {
    // switch (decision.get()) {
    // case ABORT:
    // param2 = DataPacket.NO;
    // break;
    // case COMMIT:
    // param2 = DataPacket.YES;
    // break;
    // default:
    // throw new UnImplementedException();
    // }
    // } else {
    // param2 = DataPacket.UNKNOWN;
    // }
    //
    // return asPair(Optional.of(new DataPacket(DataPacket.DECISION_ANSWER, xid,
    // param2)), true);
    // }
    //
    // @Override
    // public boolean requireTransState() {
    // return false;
    // }
    // }
}
