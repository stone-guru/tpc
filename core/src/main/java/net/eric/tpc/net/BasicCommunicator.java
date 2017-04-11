package net.eric.tpc.net;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Types.ErrorCode;

public class BasicCommunicator<M> {

    private static final Logger logger = LoggerFactory.getLogger(BasicCommunicator.class);
    
    protected Map<InetSocketAddress, PeerChannel<M>> channelMap = Collections.emptyMap();
    protected AtomicReference<CommunicationRound<?>> roundRef = new AtomicReference<CommunicationRound<?>>(null);
    private TaskPoolProvider poolProvider;
    
    public BasicCommunicator(TaskPoolProvider poolProvider, List<PeerChannel<M>> channels) {
        this.poolProvider = poolProvider;

        this.channelMap = new HashMap<InetSocketAddress, PeerChannel<M>>();
        channels.forEach(channel -> this.channelMap.put(channel.getPeer(), channel));
    }

    public Future<RoundResult<Boolean>> notify(List<Pair<InetSocketAddress, M>> messages, final RoundType roundType) {
        final CommunicationRound<Boolean> round = new CommunicationRound<Boolean>(Pair.projectFirst(messages), //
                roundType);

        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                for (final Pair<InetSocketAddress, M> p : messages) {
                    final PeerChannel<M> channel = channelMap.get(p.fst());
                    if (channel == null) {
                        final InetSocketAddress node = p.fst();
                        round.regResult(node, Maybe.fail(ErrorCode.PEER_NOT_CONNECTED, node.toString()));
                    } else {
                        @SuppressWarnings("unused")
                        Future<Boolean> notCareHere = channel.notify(p.snd(), round);
                    }
                }
            }
        };
        this.poolProvider.getSequenceTaskPool().submit(startAction);
        return round.getResult(poolProvider.getCommuTaskPool());
    }

    public <R> Future<RoundResult<R>> communicate(List<Pair<InetSocketAddress, M>> messages, final RoundType roundType,
            Function<Object, Maybe<R>> assembler) {
        final CommunicationRound<R> round = new CommunicationRound<R>(Pair.projectFirst(messages), roundType, assembler);

        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                roundRef.set(round);
                for (final Pair<InetSocketAddress, M> p : messages) {
                    final PeerChannel<M> channel = channelMap.get(p.fst());
                    if (channel == null) {
                        final InetSocketAddress node = p.fst();
                        round.regResult(node, Maybe.fail(ErrorCode.PEER_NOT_CONNECTED, node.toString()));
                    } else {
                        @SuppressWarnings("unused")
                        Future<Boolean> f = channel.request(p.snd(), round );
                    }
                    ;
                }
            }
        };

        this.poolProvider.getSequenceTaskPool().submit(startAction);
        return round.getResult(poolProvider.getCommuTaskPool());
    }

}