package net.eric.tpc.proto;

import com.google.common.base.Optional;

public interface DtLogger<B> {
    String recordBeginTrans(TransStartRec transNodes, B b);

    void recordStart2PC(String xid);
    
    void recordVoteResult(String xid, Vote vote);
    
    void recordDecision(String xid, Decision decsion);
    
    Optional<Decision> getDecisionFor(String xid);
}
