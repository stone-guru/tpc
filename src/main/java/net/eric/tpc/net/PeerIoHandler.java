package net.eric.tpc.net;

import static net.eric.tpc.base.Pair.asPair;

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
import com.google.common.collect.ImmutableList;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.RequestHandler.ProcessResult;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.PeerTransactionState;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

public class PeerIoHandler extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PeerIoHandler.class);

    private static final RequestHandler UnknownCommandHandler = new RequestHandler() {
        @Override
        public String getCorrespondingCode() {
            throw new ShouldNotHappenException("UnknownCommandHandler has no correspondingCode");
        }

        @Override
        public ProcessResult process(IoSession session, DataPacket request) {
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    };

    private Map<String, RequestHandler> requestHandlerMap = initRequestHandlers();
    private PeerTransactionManager<TransferBill> transManager;
    private ExecutorService commuTaskPool;

    private Map<String, RequestHandler> initRequestHandlers() {
        List<RequestHandler> handlers = ImmutableList.of(//
                new BeginTransRequestHandler(), //
                new VoteRequestHandler(), //
                new TransDecisionHandler(), //
                new PeerDecisonQueryHandler());

        Map<String, RequestHandler> handlerMap = new HashMap<String, RequestHandler>();
        for (RequestHandler h : handlers) {
            handlerMap.put(h.getCorrespondingCode(), h);
        }
        return handlerMap;
    }

    private RequestHandler getRequestHandler(String code) {
        RequestHandler handler = requestHandlerMap.get(code);
        if (handler == null) {
            return UnknownCommandHandler;
        }
        return handler;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        if (logger.isInfoEnabled()) {
            final String remote = session.getRemoteAddress().toString();
            logger.info(remote + "  connected");
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        if (logger.isInfoEnabled()) {
            final String remote = session.getRemoteAddress().toString();
            logger.info(remote + "  closed");
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        DataPacket request = (DataPacket) message;

        RequestHandler handler = this.getRequestHandler(request.getCode());

        final ProcessResult result = handler.process(session, request);
        if (result.getResponse().isPresent()) {
            this.sendMessage(session, result.getResponse().get(), result.closeAfterSend());
        } else if (result.closeAfterSend()) {
            session.closeOnFlush();
        }
    }

    private void sendMessage(final IoSession session, final DataPacket packet, boolean closeAfterSend) {

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
            if (this.commuTaskPool == null) {
                throw new IllegalStateException("commuTaskPool not set");
            }
            this.commuTaskPool.submit(closeAction);
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        if (!IdleStatus.READER_IDLE.equals(status)) {
            return;
        }

        // 通过一个handler来取session中的state，保证逻辑的负责者唯一
        Optional<PeerTransactionState<TransferBill>> state = TransactionRequestHandler.getTransactionState(session);

        if (!state.isPresent()) {
            // if (logger.isDebugEnabled()) {
            // logger.debug("No transaction state on this session, do nothing in
            // sessionIdle");
            // }
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

    class BeginTransRequestHandler extends TransactionRequestHandler<TransferBill> {
        @Override
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

        @Override
        public ProcessResult process(IoSession session, DataPacket request) {
            Optional<PeerTransactionState<TransferBill>> state = TransactionRequestHandler
                    .getTransactionState(session);
            if (!state.isPresent()) {
                state = Optional.of(new PeerTransactionState<TransferBill>());
                TransactionRequestHandler.putTransactionState(session, state.get());
            }
            return this.process(request, state.get());
        }

        @Override
        public ProcessResult process(DataPacket request, PeerTransactionState<TransferBill> state) {
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
            return new ProcessResult(response, !r.isOK());
        }

        @Override
        public boolean requireTransState() {
            return false;
        }
    }

    class VoteRequestHandler extends TransactionRequestHandler<TransferBill> {
        @Override
        public String getCorrespondingCode() {
            return DataPacket.VOTE_REQ;
        }

        @Override
        public ProcessResult process(DataPacket request, PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();

            ActionStatus r = PeerIoHandler.this.transManager.processVoteReq(xid, state);
            DataPacket response = null;
            if (r.isOK()) {
                response = new DataPacket(DataPacket.VOTE_ANSWER, xid, DataPacket.YES);
            } else {
                response = new DataPacket(DataPacket.VOTE_ANSWER, xid, DataPacket.NO, r);
            }
            return new ProcessResult(response, !r.isOK());
        }
    }

    class TransDecisionHandler extends TransactionRequestHandler<TransferBill> {
        @Override
        public String getCorrespondingCode() {
            return DataPacket.TRANS_DECISION;
        }

        @Override
        public ProcessResult process(DataPacket request, PeerTransactionState<TransferBill> state) {
            String xid = (String) request.getParam1();
            String decision = (String) request.getParam2();

            if (DataPacket.YES.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.COMMIT, state);
            } else if (DataPacket.NO.equals(decision)) {
                PeerIoHandler.this.transManager.processTransDecision(xid, Decision.ABORT, state);
            } else {
                logger.error(DataPacket.PEER_PRTC_ERROR, "Unknown commit decision " + decision);
            }
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    }

    class PeerDecisonQueryHandler extends DecisionQueryHandler {
        @Override
        protected Optional<Decision> getDecisionFor(String xid) {
            return PeerIoHandler.this.transManager.queryDecision(xid);
        }
    }

}
