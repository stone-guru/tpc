package net.eric.tpc.coor.stub;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.RoundType;

public class MinaChannel {
    private static final Logger logger = LoggerFactory.getLogger(MinaChannel.class);

    private static SocketConnector sharedConnector = null;

    synchronized public static void initSharedConnector(ProtocolCodecFactory codecFactory) {
        if (MinaChannel.sharedConnector != null) {
            throw new IllegalStateException("sharedConnector has been initialized");
        }

        SocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(3000);
        DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
        filterChain.addLast("codec", new ProtocolCodecFilter(codecFactory));
        connector.setHandler(new SocketHandler());
        MinaChannel.sharedConnector = connector;
    }

    public static SocketConnector getSharedConnector() {
        if (MinaChannel.sharedConnector == null) {
            throw new IllegalStateException("sharedConnector not initialized, call InitSharedConnector first");
        }
        return MinaChannel.sharedConnector;
    }

    public static void disposeConnector() {
        if (sharedConnector != null) {
            if (!sharedConnector.isDisposed()) {
                sharedConnector.dispose();
            }
        }
    }

    private IoSession session;
    private Node node;
    private AtomicReference<CommunicationRound> roundRef;

    public MinaChannel(Node node, AtomicReference<CommunicationRound> roundRef) {
        this.node = node;
        this.roundRef = roundRef;
    }

    public boolean connect() throws Exception {
        SocketConnector connector = MinaChannel.getSharedConnector();
        ConnectFuture future = connector.connect(new InetSocketAddress(node.getAddress(), node.getPort()));
        future.awaitUninterruptibly();
        if (!future.isConnected()) {
            return false;
        }
        this.session = future.getSession();
        session.setAttribute("ROUND_REF", this.roundRef);
        session.setAttribute("PEER", this.node);
        return true;
    }

    public IoSession session() {
        return this.session;
    }

    public Node node() {
        return this.node;
    }

    public void sendMessage(Object request) {
        this.sendMessage(request, false);
    }

    public void sendMessage(Object request, boolean sendOnly) {
        final WriteFuture writeFuture = session.write(request);
        writeFuture.awaitUninterruptibly();
        if (!writeFuture.isWritten()) {
            if (writeFuture.getException() != null) {
                CommunicationRound round = roundRef.get();
                round.regFailure(this.node, "ERROR_SEND", writeFuture.getException().getMessage());
            }
        } else if (sendOnly) {
            CommunicationRound round = roundRef.get();
            round.regMessage(node, true);
        }
    }

    public void close() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("MinaChannel.close entry");
        }
        CloseFuture future = session.getCloseFuture();
        future.awaitUninterruptibly(2000);
    }

    private static class SocketHandler extends IoHandlerAdapter {
        private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            Optional<Pair<CommunicationRound, Node>> opt = this.currentRound(session);
            if (opt.isPresent()) {
                CommunicationRound round = opt.get().fst();
                Node node = opt.get().snd();
                round.finishReceiving(node);
            }
            super.sessionClosed(session);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            super.sessionIdle(session, status);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            super.exceptionCaught(session, cause);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if (logger.isDebugEnabled()) {
                logger.debug("Got message from " + session.getServiceAddress().toString() + ", " + message);
            }
            Optional<Pair<CommunicationRound, Node>> opt = this.currentRound(session);
            if (opt.isPresent()) {
                CommunicationRound round = opt.get().fst();
                Node node = opt.get().snd();
                round.regMessage(node, message);
            }
        }

        private Optional<Pair<CommunicationRound, Node>> currentRound(IoSession session) {
            @SuppressWarnings("unchecked")
            AtomicReference<CommunicationRound> roundRef = (AtomicReference<CommunicationRound>) session
                    .getAttribute("ROUND_REF");
            if (roundRef.get() == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No communication round object found, abandom result");
                }
                return Optional.absent();
            }

            CommunicationRound round = roundRef.get();
            if (!round.isWithinRound()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not in communication round, abandom result");
                }
                return Optional.absent();
            }
            if (round.roundType() != RoundType.DOUBLE_SIDE) {
                if (logger.isDebugEnabled()) {
                    logger.debug("This round does not require answer, abandom result");
                }
                return Optional.absent();
            }

            Node node = (Node) session.getAttribute("PEER");

            if (node == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No PEER node set");
                }
                return Optional.absent();
            }
            return Optional.of(Pair.asPair(round, node));
        }
    }
}