package net.eric.tpc.net;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.List;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.CommunicationRound.RoundType;

public interface ChannelFactory extends Closeable {
    <T> Maybe<List<PeerChannel<T>>> getChannel(Class<T> messageTypClass, List<InetSocketAddress> peers, RoundType type);

    <T> void releaseChannel(List<PeerChannel<T>> channel);

}
