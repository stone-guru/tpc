package net.eric.tpc.coor.stub;

import java.util.List;

import net.eric.tpc.proto.CoorDtLogger;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.DtRecord;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.Vote;

public class CoorDtLoggerDbImpl implements CoorDtLogger {

    @Override
    public String generateXid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String recordBeginTrans(TransStartRec transNodes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void recordVote(String xid, Vote vote) {
        // TODO Auto-generated method stub

    }

    @Override
    public void recordDecision(String xid, Decision decsion) {
        // TODO Auto-generated method stub

    }

    @Override
    public void markFinished(String xid) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<DtRecord> getUnfinishedTrans() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Decision getDecisionFor(String xid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void recordStart2PC(String xid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void abandomTrans(String xid) {
        // TODO Auto-generated method stub

    }

}
