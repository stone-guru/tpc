package net.eric.tpc.net.mina;

import com.google.common.collect.ImmutableMap;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.net.RequestHandler;
import net.eric.tpc.net.RequestHandler.ProcessResult;
import net.eric.tpc.net.TransSession;
import net.eric.tpc.net.binary.Message;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;


public class PeerIoHandler extends IoHandlerAdapter implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(PeerIoHandler.class);

    private static final RequestHandler UnknownCommandHandler = new RequestHandler() {
        @Override
        public short[] getCorrespondingCodes() {
            throw new ShouldNotHappenException("UnknownCommandHandler has no correspondingCode");
        }

        @Override
        public ProcessResult process(TransSession session, Message request) {
            return ProcessResult.NO_RESPONSE_AND_CLOSE;
        }
    };

    private Map<Short, RequestHandler> requestHandlerMap;

    private ExecutorService taskPool = Executors.newFixedThreadPool(3);
    private TransSession transSession = new TransSession();
    private BiConsumer<IoSession, Object> unMessageHandler;

    public PeerIoHandler(List<RequestHandler> handlers) {
        this(handlers, null);
    }

    public PeerIoHandler(List<RequestHandler> handlers, BiConsumer<IoSession, Object> unMessageHandler) {
        this.initRequestHandlerMap(handlers);
        this.unMessageHandler = unMessageHandler;
        NightWatch.regCloseable("PeerIoHandler", this);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.error("Exception ", cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received: " + message.toString());
        }

        if (message instanceof Message) {
            Message request = (Message) message;
            RequestHandler handler = this.getRequestHandler(request.getCode());
            ProcessResult result;
            try {
                result = handler.process(transSession, request);
            } catch (Exception e) {
                logger.error("Process Message Error, can not generate response", e);
                result = ProcessResult.errorAndClose(request.getXid(), request.getRound());
            }

            if (result.getResponse().isPresent())
                this.replyMessage(session, result.getResponse().get(), result.isCloseAfterSend());
            else if (result.isCloseAfterSend())
                session.closeOnFlush();
        } else {
            this.processNotMessage(session, message);
        }
    }

    @Override
    public void close() throws IOException {
        this.taskPool.shutdown();
    }

    protected void processNotMessage(IoSession session, Object message) throws Exception {
        if (this.unMessageHandler != null) {
            this.unMessageHandler.accept(session, message);
        } else {
            throw new UnImplementedException();
        }
    }

    private void replyMessage(final IoSession session, final Message packet, boolean closeAfterSend) {

        final WriteFuture wf = session.write(packet);

        if (logger.isDebugEnabled()) {
            wf.addListener(future -> logger.debug("response written : " + packet));
        }

        if (closeAfterSend) {
            this.taskPool.submit(() -> {
                wf.awaitUninterruptibly();
                session.closeOnFlush();
            });
        }
    }

    private void initRequestHandlerMap(List<RequestHandler> handlers) {
        ImmutableMap.Builder<Short, RequestHandler> builder = new ImmutableMap.Builder<>();
        for (RequestHandler h : handlers) {
            for (short code : h.getCorrespondingCodes()) {
                builder.put(code, h);
            }
        }
        this.requestHandlerMap = builder.build();
    }

    protected RequestHandler getRequestHandler(short code) {
        RequestHandler handler = requestHandlerMap.get(code);
        if (handler == null) {
            return UnknownCommandHandler;
        }
        return handler;
    }

}
