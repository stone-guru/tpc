package net.eric.tpc.regulator;

import java.math.BigDecimal;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.BizActionListener;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.service.BillSaveStrategy;

public class RegulatorBizStrategy implements PeerBizStrategy<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(RegulatorBizStrategy.class);

    private BillSaveStrategy billSaver;
    private Validator<TransferBill> billValidator;
    
    @Override
    public Future<ActionStatus> checkAndPrepare(long xid, TransferBill bill) {
        logger.info("checkAndPrepare trans " + xid);
        ActionStatus status = billValidator.check(bill);
        if (status.isOK()) {
            status = this.ruleCheck(bill);
            if (status.isOK()) {
                return this.billSaver.prepareCommit(xid, bill);
            }
        }
        return Futures.immediateFuture(status);
    }

    @Override
    public Future<Void> commit(long xid, BizActionListener commitListener) {
        logger.info("commit trans " + xid);
        return this.billSaver.commit(xid, commitListener);
    }

    @Override
    public Future<Void> abort(long xid, BizActionListener abortListener) {
        logger.info("abort trans " + xid);
        return this.billSaver.abort(xid, abortListener);
    }

    private ActionStatus ruleCheck(TransferBill bill) {
        final BigDecimal amountLimit = BigDecimal.valueOf(2000L);
        if (bill.getAmount().compareTo(amountLimit) > 0) {
            logger.info(bill.getTransSN() + " great than limit, refuse it");
            return ActionStatus.create(BizCode.BEYOND_TRANS_AMOUNT_LIMIT, amountLimit.toString());
        }
        return ActionStatus.OK;
    }

    public void setBillSaver(BillSaveStrategy billSaver) {
        this.billSaver = billSaver;
    }

    public void setBillValidator(Validator<TransferBill> billValidator) {
        this.billValidator = billValidator;
    }
    
    
}
