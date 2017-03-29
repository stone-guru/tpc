package net.eric.tpc.proto;

import com.google.common.base.Optional;

public interface DtLogger<B> {
    void recordBeginTrans(TransStartRec startRec, B b, boolean isInitiator);

    void recordVote(String xid, Vote vote);

    void recordDecision(String xid, Decision decsion);

    void markTransFinished(String xid);
    
    Optional<Decision> getDecisionFor(String xid);
}
