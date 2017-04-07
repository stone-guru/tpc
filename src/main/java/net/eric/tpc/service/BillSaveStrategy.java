package net.eric.tpc.service;

import static net.eric.tpc.base.Pair.asPair;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.BizActionListener;
import net.eric.tpc.proto.Types.Decision;

public class BillSaveStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BillSaveStrategy.class);
    
    private ExecutorService threadPool;
    private Validator<TransferBill> billValidator;
    private TransferBillDao transferBillDao;

    public Future<ActionStatus> prepareCommit(long xid, TransferBill bill) {
        Callable<ActionStatus> task = new Callable<ActionStatus>() {
            @Override
            public ActionStatus call() throws Exception {
                try {
                    BillSaveStrategy.this.transferBillDao.insert(bill);
                    BillSaveStrategy.this.transferBillDao.updateLock(asPair(bill.getTransSN(), xid));
                    return ActionStatus.OK;
                } catch (Exception e) {
                    logger.error("AbcBizStrategy.prepareCommit", e);
                    return ActionStatus.innerError(e.getMessage());
                }
            }
        };
        return this.threadPool.submit(task);
    }

    public Future<Void> commit(long xid, BizActionListener commitListener) {
        return this.doDecision(xid, Decision.COMMIT, commitListener);
    }

    public Future<Void> abort(long xid, BizActionListener abortListener) {
        return this.doDecision(xid, Decision.ABORT, abortListener);
    }

    private void innerCommit(long xid) {
        this.transferBillDao.clearLockByXid(xid);
    }

    private void innerAbort(long xid) {
        this.transferBillDao.deleteByXid(xid);
    }

    private Future<Void> doDecision(long xid, Decision decision, final BizActionListener listener) {
        Callable<Void> commitTask = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                boolean success = false;
                try {
                    if (decision == Decision.COMMIT) {
                        BillSaveStrategy.this.innerCommit(xid);
                    } else if (decision == Decision.ABORT) {
                        BillSaveStrategy.this.innerAbort(xid);
                    } else {
                        throw new UnImplementedException("Decision Enum got more");
                    }
                    success = true;
                } catch (Exception e) {
                    logger.error("doDecison", e);
                }
                BillSaveStrategy.this.callBizActionListener(xid, decision, success, listener);
                return null;
            }
        };
        return this.threadPool.submit(commitTask);
    }

    private void callBizActionListener(long xid, Decision decision, boolean success, BizActionListener listener) {
        if (listener == null) {
            return;
        }
        try {
            if (success) {
                listener.onSuccess(xid);
            } else {
                listener.onFailure(xid);
            }
        } catch (Exception e) {
            logger.error("executeListener when " + xid + " " + decision + " and exec result is " + success, e);
        }
    }

    
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    public Validator<TransferBill> getBillValidator() {
        return billValidator;
    }

    public void setBillValidator(Validator<TransferBill> billValidator) {
        this.billValidator = billValidator;
    }

    public TransferBillDao getTransferBillDao() {
        return transferBillDao;
    }

    public void setTransferBillDao(TransferBillDao transferBillDao) {
        this.transferBillDao = transferBillDao;
    }
}
