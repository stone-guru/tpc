package net.eric.tpc.bankserver.net;

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

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.HeartBeatRequestHandler;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.UnknowCommandRequestHandler;
import net.eric.tpc.proto.TransStartRec;
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

    @SuppressWarnings("rawtypes")
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received: " + message.toString());
        }

        final DataPacket request = (DataPacket) message;
        RequestHandler handler = this.getRequestHandler(request.getCode());
        PeerTransactionState state = (PeerTransactionState) session.getAttribute(BankServerHandler.TRANS_SESSION_ITEM);
        final DataPacket response = handler.process(request, state);

        if (response != null) {
            WriteFuture wf = session.write(response);

            if (logger.isDebugEnabled()) {
                wf.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        logger.debug("response written : " + response);
                    }
                });
            }
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        System.out.println("IDLE " + session.getIdleCount(status));
    }

    public void setTransManager(PeerTransactionManager transManager) {
        this.transManager = transManager;
    }

    class BeginTransRequestHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.BEGIN_TRANS;
        }

        public DataPacket process(DataPacket request, PeerTransactionState state) {
            TransStartRec transNodes = (TransStartRec) request.getParam1();
            TransferMessage transMsg = (TransferMessage) request.getParam2();

            ActionResult r = BankServerHandler.this.transManager.beginTrans(transNodes, transMsg, state);

            if (r.isOK()) {
                return new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, DataPacket.YES);
            } else {
                return new DataPacket(DataPacket.BEGIN_TRANS_ANSWER, DataPacket.NO, r.toString());
            }
        }
    }

    class VoteRequestHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.VOTE_REQ;
        }

        public DataPacket process(DataPacket request, PeerTransactionState state) {
            String xid = (String) request.getParam1();

            ActionResult r = BankServerHandler.this.transManager.processVoteReq(xid, state);
            if (r.isOK()) {
                return new DataPacket(DataPacket.VOTE_ANSWER, DataPacket.YES);
            } else {
                return new DataPacket(DataPacket.VOTE_ANSWER, DataPacket.NO, r.toString());
            }
        }
    }

    class TransDecisionHandler implements RequestHandler {
        public String getCorrespondingCode() {
            return DataPacket.TRANS_DECISION;
        }

        public DataPacket process(DataPacket request, PeerTransactionState state) {
            String xid = (String) request.getParam1();
            String decision = (String) request.getParam2();

            if (DataPacket.YES.equals(decision)) {
                BankServerHandler.this.transManager.processTransDecision(xid, true, state);
            } else if (DataPacket.NO.equals(decision)){
                BankServerHandler.this.transManager.processTransDecision(xid, false, state);
            }else{
                logger.error(BizErrorCode.PEER_PRTC_ERROR, "Unknown commit decision " + decision);
            }
            return null;
        }
    }
}
