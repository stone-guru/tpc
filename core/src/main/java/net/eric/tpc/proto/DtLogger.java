package net.eric.tpc.proto;

import com.google.common.base.Optional;

import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public interface DtLogger {
    void recordBeginTrans(TransStartRec startRec, Object b, boolean isInitiator);

    void recordVote(long xid, Vote vote);

    void recordDecision(long xid, Decision decsion);

    void markTransFinished(long xid);
    
    Optional<Decision> getDecisionFor(long xid);
}
