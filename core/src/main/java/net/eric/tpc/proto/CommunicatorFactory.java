package net.eric.tpc.proto;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.List;

import net.eric.tpc.base.Maybe;

public interface CommunicatorFactory extends Closeable{
    Maybe<Communicator> getCommunicator(List<InetSocketAddress> peers);
    
    void releaseCommunicator(Communicator c);
    
}
