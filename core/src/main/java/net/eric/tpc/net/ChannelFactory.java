package net.eric.tpc.net;

import java.net.InetSocketAddress;
import java.util.List;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.CommunicationRound.RoundType;

public interface ChannelFactory {
    <T> Maybe<List<PeerChannel<T>>> getChannel(Class<T> messageTypClass, List<InetSocketAddress> peers, RoundType type);

    <T> void releaseChannel(List<PeerChannel<T>> channel);
}
