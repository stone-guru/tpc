package net.eric.tpc.net.mina;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.ChannelFactory;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.PeerChannel;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.net.binary.MessageCodecFactory;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Types.ErrorCode;

public class MinaChannelFactory implements ChannelFactory {
    private static final Logger logger = LoggerFactory.getLogger(MinaChannelFactory.class);

    private SocketConnector sharedConnector;
    private ExecutorService commuTaskPool = Executors.newCachedThreadPool();

    public MinaChannelFactory(){
        SocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(3000);
        DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
        filterChain.addLast("codec", new ProtocolCodecFilter(new MessageCodecFactory()));
        connector.setHandler(new MinaChannel.SocketHandler<Message>());
    }
    
    
    @Override
    public <T> Maybe<List<PeerChannel<T>>> getChannel(Class<T> messageTypClass, List<InetSocketAddress> peers,
            RoundType roundType) {
        Future<RoundResult<PeerChannel<T>>> f = connectPeers(peers);
        RoundResult<PeerChannel<T>> r = null;
        try{
            r = f.get();
        }catch(Exception e){
            return Maybe.fail(ActionStatus.innerError(e.getMessage()));
        }
        // 一个都没连上 或 在需要全连时不是都连上了
        if (r.okResultCount() == 0 || (!r.isAllOK() && roundType == RoundType.WAIT_ALL)) {
            List<PeerChannel<T>> channels = r.okResults();
            this.releaseChannel(channels);
            return Maybe.fail(r.getAnError());
        }

        return Maybe.success(r.okResults());
    }

    @Override
    public <T> void releaseChannel(List<PeerChannel<T>> channels) {
        channels.forEach(c -> {
            MinaChannel<T> channel = (MinaChannel<T>)c;
            channel.getSession().closeNow();
        });
    }

    private <T> Future<RoundResult<PeerChannel<T>>> connectPeers(List<InetSocketAddress> peers) {
        final CommunicationRound<PeerChannel<T>> round = new CommunicationRound<PeerChannel<T>>(peers,RoundType.WAIT_ALL);
        peers.forEach(node -> {
            Runnable task = new Runnable() {
                public void run() {
                    ConnectFuture f;
                    IoSession session = null;
                    try {
                        f = sharedConnector.connect(node);
                        f.awaitUninterruptibly();
                        session = f.getSession();
                    } catch (Exception e) {
                        round.regResult(node, Maybe.fail(ErrorCode.PEER_NOT_CONNECTED, e.getMessage()));
                        return;
                    }
                    if (session == null) {
                        round.regResult(node, Maybe.fail(ErrorCode.PEER_NOT_CONNECTED, getExceptionMessage(f)));
                    } else {
                        round.regResult(node, Maybe.success(new MinaChannel<T>(session, commuTaskPool)));
                    }
                }
            };
            commuTaskPool.submit(task);
        });

       return round.getResult(commuTaskPool);
    }

    private String getExceptionMessage(ConnectFuture future) {
        if (future != null && future.getException() != null) {
            return future.getException().getMessage();
        }
        return "";
    }

}
