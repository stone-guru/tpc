package net.eric.bank.regulator;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.bank.biz.BizCode;
import net.eric.bank.biz.Validator;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.service.BillSaveStrategy;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.proto.PeerBizStrategy;

public class RegulatorBizStrategy implements PeerBizStrategy<TransferBill>{
    private static final Logger logger = LoggerFactory.getLogger(RegulatorBizStrategy.class);

    private BillSaveStrategy billSaver;
    private Validator<TransferBill> billValidator;
    
    @Override
    public ActionStatus checkAndPrepare(long xid, TransferBill bill) {
        logger.info("checkAndPrepare trans " + xid);
        ActionStatus status = billValidator.check(bill);
        if (status.isOK()) {
            status = this.ruleCheck(bill);
            if (status.isOK()) {
                return this.billSaver.checkAndPrepare(xid, bill);
            }
        }
        return status;
    }

    @Override
    public boolean commit(long xid){
        logger.info("commit trans " + xid);
        return this.billSaver.commit(xid);
    }

    @Override
    public boolean abort(long xid){
        logger.info("abort trans " + xid);
        return this.billSaver.abort(xid);
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
