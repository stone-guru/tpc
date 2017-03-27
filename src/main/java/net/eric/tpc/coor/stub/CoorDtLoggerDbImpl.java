package net.eric.tpc.coor.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.UnImplementedException;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.Vote;

public class CoorDtLoggerDbImpl implements DtLogger<TransferMessage> {
    private static final Logger logger = LoggerFactory.getLogger(CoorDtLoggerDbImpl.class);

    @Override
    public String recordBeginTrans(TransStartRec transStartRec, TransferMessage bill) {
        logger.info("CoorDtLoggerDbImpl.recordBeginTrans", transStartRec.toString());
        return null;
    }

    @Override
    public void recordDecision(String xid, Decision decision) {
        logger.info("CoorDtLoggerDbImpl.recordDecision", xid + ", " + decision);
    }

    @Override
    public Optional<Decision>  getDecisionFor(String xid) {
        throw new UnImplementedException();
    }

    @Override
    public void recordStart2PC(String xid) {
        logger.info("CoorDtLoggerDbImpl.recordStart2PC", xid);
    }

    @Override
    public void recordVoteResult(String xid, Vote vote) {
        logger.info("CoorDtLoggerDbImpl.recordVoteResult", xid + ", " +vote);
    }

}
