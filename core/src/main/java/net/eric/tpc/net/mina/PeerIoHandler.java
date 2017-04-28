package net.eric.tpc.net.mina;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.TransSession;
import net.eric.tpc.net.RequestHandler.ProcessResult;
import net.eric.tpc.net.binary.Message;

public class PeerIoHandler extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PeerIoHandler.class);

    private static final RequestHandler UnknownCommandHandler = new RequestHandler() {
        @Override
        public short getCorrespondingCode() {
            throw new ShouldNotHappenException("UnknownCommandHandler has no correspondingCode");
        }

        @Override
        public ProcessResult process(TransSession session, Message request) {
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    };

    private Map<Short, RequestHandler> requestHandlerMap;;
    private ExecutorService taskPool;
    private TransSession transSession = new TransSession();

    public PeerIoHandler(List<RequestHandler> handlers) {
        this.initRequestHandlerMap(handlers);
    }

    private void initRequestHandlerMap(List<RequestHandler> handlers) {
        requestHandlerMap = new HashMap<Short, RequestHandler>();
        for (RequestHandler h : handlers) {
            requestHandlerMap.put(h.getCorrespondingCode(), h);
        }
    }

    protected RequestHandler getRequestHandler(short code) {
        RequestHandler handler = requestHandlerMap.get(code);
        if (handler == null) {
            return UnknownCommandHandler;
        }
        return handler;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received: " + message.toString());
        }

        Message request = (Message) message;

        RequestHandler handler = this.getRequestHandler(request.getCommandCode());

        ProcessResult result = null;
        try {
            result = handler.process(transSession, request);
        } catch (Exception e) {
            logger.error("Process Message Error, can not generate response", e);
            return;
        }

        if (result.getResponse().isPresent())
            this.replyMessage(session, result.getResponse().get(), result.isCloseAfterSend());
        else if (result.isCloseAfterSend())
            session.closeOnFlush();
    }

    private void replyMessage(final IoSession session, final Message packet, boolean closeAfterSend) {

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
            if (this.taskPool == null) {
                throw new IllegalStateException("commuTaskPool not set");
            }
            this.taskPool.submit(closeAction);
        }
    }

    public void setTaskPool(ExecutorService taskPool) {
        this.taskPool = taskPool;
    }
}
