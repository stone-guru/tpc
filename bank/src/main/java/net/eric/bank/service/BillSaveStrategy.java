package net.eric.bank.service;

import static net.eric.tpc.base.Pair.asPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.bank.biz.Validator;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.persist.TransferBillDao;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.proto.Types.Decision;

public class BillSaveStrategy implements PeerBizStrategy<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(BillSaveStrategy.class);

    private Validator<TransferBill> billValidator;
    private TransferBillDao transferBillDao;

    @Override
    public ActionStatus checkAndPrepare(long xid, TransferBill bill) {
        try {
            BillSaveStrategy.this.transferBillDao.insert(bill);
            BillSaveStrategy.this.transferBillDao.updateLock(asPair(bill.getTransSN(), xid));
            return ActionStatus.OK;
        } catch (Exception e) {
            logger.error("AbcBizStrategy.prepareCommit", e);
            return ActionStatus.innerError(e.getMessage());
        }
    }

    @Override
    public boolean commit(long xid) {
        return this.doDecision(xid, Decision.COMMIT);
    }

    @Override
    public boolean abort(long xid) {
        return this.doDecision(xid, Decision.ABORT);
    }

    private void innerCommit(long xid) {
        this.transferBillDao.clearLockByXid(xid);
    }

    private void innerAbort(long xid) {
        this.transferBillDao.deleteByXid(xid);
    }

    private boolean doDecision(long xid, Decision decision) {
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
        return success;
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
