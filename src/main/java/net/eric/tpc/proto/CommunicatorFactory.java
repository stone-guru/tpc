package net.eric.tpc.proto;

import java.util.List;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;
import net.eric.tpc.base.Node;

public interface CommunicatorFactory<B>{
    Either<ActionStatus, Communicator<B>> getCommunicator(List<Node> peers);
    
    void releaseCommunicator(Communicator<B> c);
    
    void close();
}
