package net.eric.tpc.net;

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

import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.net.RequestHandler.ProcessResult;

public abstract class AbstractIoHandler extends IoHandlerAdapter{
    private static final Logger logger = LoggerFactory.getLogger(AbstractIoHandler.class);
    
    private static final RequestHandler UnknownCommandHandler = new RequestHandler() {
        @Override
        public String getCorrespondingCode() {
            throw new ShouldNotHappenException("UnknownCommandHandler has no correspondingCode");
        }

        @Override
        public ProcessResult process(TransSession session, DataPacket request) {
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    };

    private Map<String, RequestHandler> requestHandlerMap = initRequestHandlerMap();
    private ExecutorService taskPool;
    private TransSession transSession = new TransSession();
    
    private Map<String, RequestHandler> initRequestHandlerMap() {
        List<RequestHandler>  handlers = this.requestHandlers();
        Map<String, RequestHandler> handlerMap = new HashMap<String, RequestHandler>();
        for (RequestHandler h : handlers) {
            handlerMap.put(h.getCorrespondingCode(), h);
        }
        return handlerMap;
    }

    protected abstract List<RequestHandler> requestHandlers();
    
    protected RequestHandler getRequestHandler(String code) {
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

        DataPacket request = (DataPacket) message;

        RequestHandler handler = this.getRequestHandler(request.getCode());
        System.out.println("Got handler " + handler);
        
        final ProcessResult result = handler.process(transSession, request);
        
        
        if (result.getResponse().isPresent()) {
            this.replyMessage(session, result.getResponse().get(), result.closeAfterSend());
        } else if (result.closeAfterSend()) {
            session.closeOnFlush();
        }
    }

    private void replyMessage(final IoSession session, final DataPacket packet, boolean closeAfterSend) {

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
