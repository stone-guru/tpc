package net.eric.tpc.net;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

public interface PeerChannel<T> {
    InetSocketAddress getPeer();

    <R> Future<Boolean> request(T message, CommunicationRound<R> round);

    Future<Boolean> notify(T message, CommunicationRound<Boolean> round);

    void finishRound();
}
