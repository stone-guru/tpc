package net.eric.tpc.proto;

import java.util.List;

public interface CoorDtLogger {
    String generateXid();
    String recordBeginTrans(TransStartRec transNodes);
    void recordVote(String xid, Vote vote);
    void recordDecision(String xid, Decision decsion);
    void markFinished(String xid);
    List<DtRecord> getUnfinishedTrans();
    Decision getDecisionFor(String xid);
    void recordStart2PC(String xid);
    void abandomTrans(String xid);
}
