package net.eric.tpc.proto;

import com.google.common.base.Optional;

import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public interface DtLogger<B> {
    void recordBeginTrans(TransStartRec startRec, B b, boolean isInitiator);

    void recordVote(String xid, Vote vote);

    void recordDecision(String xid, Decision decsion);

    void markTransFinished(String xid);
    
    Optional<Decision> getDecisionFor(String xid);
}
