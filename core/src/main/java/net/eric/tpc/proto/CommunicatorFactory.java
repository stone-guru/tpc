package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.List;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;

public interface CommunicatorFactory<B>{
    Either<ActionStatus, Communicator<B>> getCommunicator(List<InetSocketAddress> peers);
    
    void releaseCommunicator(Communicator<B> c);
    
    void close();
}
