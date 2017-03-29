package net.eric.tpc.regulator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.BizActionListener;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.service.BillSaveStrategy;

public class RegulatorBizStrategy implements PeerBizStrategy<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(RegulatorBizStrategy.class);

    private ExecutorService pool = Executors.newFixedThreadPool(2);
    private BillSaveStrategy billSaver = new BillSaveStrategy(this.pool);

    @Override
    public Future<ActionStatus> checkAndPrepare(String xid, TransferBill bill) {
        logger.info("checkAndPrepare trans " + xid);
        return this.billSaver.prepareCommit(xid, bill);
    }

    @Override
    public Future<Void> commit(String xid, BizActionListener commitListener) {
        logger.info("commit trans " + xid);
        return  this.billSaver.commit(xid, commitListener);
    }

    @Override
    public Future<Void> abort(String xid, BizActionListener abortListener) {
        logger.info("abort trans " + xid);
        return this.billSaver.abort(xid, abortListener);
    }

    public void setTransferBillDao(TransferBillDao transferBillDao) {
        this.billSaver.setTransferBillDao(transferBillDao);
    }
}
