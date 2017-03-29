package net.eric.tpc.proto;

import java.util.List;

import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.Either;
import net.eric.tpc.common.Node;

public interface CommunicatorFactory<B>{
    Either<ActionStatus, Communicator<B>> getCommunicator(List<Node> peers);
    
    void releaseCommunicator(Communicator<B> c);
    
    void close();
}
