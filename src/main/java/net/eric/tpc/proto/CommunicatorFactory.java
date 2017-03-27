package net.eric.tpc.proto;

import java.util.List;

import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.Either;

public interface CommunicatorFactory<B>{
    Either<ActionResult, CoorCommunicator<B>> getCommunicator(List<Node> peers);
    
    void releaseCommunicator(CoorCommunicator<B> c);
    
    void close();
}
