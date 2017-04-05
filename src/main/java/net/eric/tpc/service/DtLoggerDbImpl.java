package net.eric.tpc.service;

import static net.eric.tpc.base.Pair.asPair;

import java.util.Date;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.eric.tpc.base.Node;
import net.eric.tpc.entity.DtRecord;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;
import net.eric.tpc.util.Util;

public class DtLoggerDbImpl implements DtLogger<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(DtLoggerDbImpl.class);
    private DtRecordDao dtLoggerDao;
    
    @Override
    public void recordBeginTrans(TransStartRec transStartRec, TransferBill bill, boolean isInitiator) {
        logger.info("CoorDtLoggerDbImpl.recordBeginTrans", transStartRec.toString());
        DtRecord record = new DtRecord();
        record.setXid(transStartRec.getXid());
        record.setCoordinator(transStartRec.getCoordinator().toString());
        
        
        Iterator<Node> it = transStartRec.getParticipants().iterator();
        StringBuilder builder = new StringBuilder(it.next().toString());
        while(it.hasNext()){
            builder.append(",").append(it.next().toString());
        }
        record.setPariticipants(builder.toString());
        record.setStartTime(new Date());
        record.setStart2PC(isInitiator);
        record.setBizMessage(Util.ObjectToBytes(bill));
        
        this.dtLoggerDao.insert(record);
    }

    
    
    @Override
    public void recordDecision(String xid, Decision decision) {
        logger.info("CoorDtLoggerDbImpl.recordDecison", xid + ", " +decision);
        
        DtRecord record = new DtRecord();
        record.setXid(xid);
        record.setDecision(decision);
        record.setFinishTime(new Date());
        
        this.dtLoggerDao.updateDecision(record);
    }

    @Override
    public Optional<Decision>  getDecisionFor(String xid) {
        Decision decision = this.dtLoggerDao.selectDecision(xid);
        return Optional.fromNullable(decision);
    }

    @Override
    public void recordVote(String xid, Vote vote) {
        logger.info("CoorDtLoggerDbImpl.recordVote", xid + ", " +vote);
        
        DtRecord record = new DtRecord();
        record.setXid(xid);
        record.setVote(vote);
        record.setVoteTime(new Date());
        
        this.dtLoggerDao.updateVote(record);
    }

    public DtRecordDao getDtLoggerDao() {
        return dtLoggerDao;
    }

    public void setDtLoggerDao(DtRecordDao dtLoggerDao) {
        this.dtLoggerDao = dtLoggerDao;
    }

    @Override
    public void markTransFinished(String xid) {
       logger.info("mark transaction finishd " + xid);
       this.dtLoggerDao.updateFinishTime(asPair(xid, new Date()));
    }
}
