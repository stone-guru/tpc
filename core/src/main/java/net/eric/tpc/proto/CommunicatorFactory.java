package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.List;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;
import net.eric.tpc.base.Maybe;

public interface CommunicatorFactory{
    Maybe<Communicator> getCommunicator(List<InetSocketAddress> peers);
    
    void releaseCommunicator(Communicator c);
    
    void close();
}
