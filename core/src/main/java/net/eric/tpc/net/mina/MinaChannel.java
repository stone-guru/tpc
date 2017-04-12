package net.eric.tpc.net.mina;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.PeerChannel;
import net.eric.tpc.proto.Types.ErrorCode;

public class MinaChannel<T> implements PeerChannel<T> {
    private static final Logger logger = LoggerFactory.getLogger(MinaChannel.class);

    private ExecutorService commuTaskPool;
    private IoSession session;
    private InetSocketAddress peer;
    private AtomicReference<CommunicationRound<?>> roundRef = new AtomicReference<CommunicationRound<?>>(null);

    private static AttributeKey channelKey = new AttributeKey(MinaChannel.class, "MINA_CHANNEL");

    public static void putChannelToSession(MinaChannel<?> channel, IoSession session) {
        session.setAttribute(channelKey, channel);
    }

    @SuppressWarnings("unchecked")
    public static <A> MinaChannel<A> getRoundFromSession(IoSession session) {
        return (MinaChannel<A>) session.getAttribute(channelKey);
    }

    public MinaChannel(InetSocketAddress peer, IoSession session, ExecutorService commuTaskPool) {
        this.commuTaskPool = commuTaskPool;
        this.session = session;
        this.peer = peer;
        
        putChannelToSession(this, session);
    }

    @Override
    public <R> Future<Boolean> request(T message, CommunicationRound<R> round ) {
        if (!roundRef.compareAndSet(null, round)) {
            throw new IllegalStateException("another round is waiting");
        }

        Callable<Boolean> action = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final WriteFuture writeFuture = session.write(message);
                writeFuture.awaitUninterruptibly();
                if (!writeFuture.isWritten()) {
                    logger.error("MinaChannel request", writeFuture.getException());
                    String errMessage = (writeFuture.getException() != null) ? writeFuture.getException().getMessage()
                            : "";
                    round.regResult(MinaChannel.this.peer, Maybe.fail(ErrorCode.SEND_ERROR, errMessage));
                    return false;
                }
                return true;
            }
        };

        return commuTaskPool.submit(action);
    }

    @Override
    public Future<Boolean> notify(T message, CommunicationRound<Boolean> round) {

        Callable<Boolean> action = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final WriteFuture writeFuture = session.write(message);
                writeFuture.awaitUninterruptibly();
                if (!writeFuture.isWritten()) {
                    String errMessage = (writeFuture.getException() != null) ? writeFuture.getException().getMessage()
                            : "";
                    round.regResult(MinaChannel.this.peer, Maybe.fail(ErrorCode.SEND_ERROR, errMessage));
                    return false;
                } else {
                    round.regResult(MinaChannel.this.peer, Maybe.success(true));
                }
                return true;
            }
        };

        return commuTaskPool.submit(action);
    }

    
    public CommunicationRound<T> getRound() {
        @SuppressWarnings("unchecked")
        final CommunicationRound<T> round = (CommunicationRound<T>) this.roundRef.get(); 
        return round;
    }

    @Override
    public void finishRound() {
        roundRef.set(null);
    }

    public IoSession getSession() {
        return this.session;
    }

    @Override
    public InetSocketAddress getPeer() {
        return this.peer;
    }

    public static class SocketHandler<T> extends IoHandlerAdapter {
        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            MinaChannel<T> channel = MinaChannel.getRoundFromSession(session);

            if (channel == null || channel.getRound() == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("not bind with round, abandom message," + message.toString());
                }
                return;
            }

            CommunicationRound<T> round = channel.getRound();
            if (!round.isWithinRound()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("not in round, abandom message," + message.toString());
                }
            }

            round.regMessage(channel.peer, message);
        }
    }

}
