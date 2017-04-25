package net.eric.bank.coor;

import static net.eric.tpc.base.Pair.asPair;

import java.net.InetSocketAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.eric.bank.biz.BizCode;
import net.eric.bank.biz.Validator;
import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.service.BillSaveStrategy;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.common.Configuration;
import net.eric.tpc.proto.CoorBizStrategy;

public class AbcBizStrategy implements CoorBizStrategy<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(AbcBizStrategy.class);

    private Configuration config = new Configuration();
    private BillSaveStrategy billSaver;
    private Validator<TransferBill> billValidator ;
    
    @Override
    public Maybe<TaskPartition<TransferBill>> splitTask(long xid, TransferBill bill) {
        logger.info("splitTask for " + xid);

        ActionStatus checkResult = basicCheck(bill);
        if (!checkResult.isOK()) {
            return Maybe.fail(checkResult);
        }

        List<Pair<InetSocketAddress, TransferBill>> tasks;
        if (this.isSameBankTrans(bill)) {
            tasks = ImmutableList.of(this.assignToSameBank(bill), this.assignToRegulator(bill));
            // tasks = ImmutableList.of(this.assignToSameBank(bill));
        } else {
            tasks = ImmutableList.of(this.asignToBankAtPayment(bill), this.asignToBankAtReceive(bill),
                    this.assignToRegulator(bill));
            // tasks = ImmutableList.of(this.asignToBankAtPayment(bill),
            // this.asignToBankAtReceive(bill));
        }
        final TransferBill myBill = this.assignToMySelf(bill);
        return Maybe.success(new TaskPartition<TransferBill>(myBill, tasks));
    }

    public ActionStatus basicCheck(TransferBill bill) {
        ActionStatus checkResult = billValidator.check(bill);
        if (!checkResult.isOK()) {
            return checkResult;
        }

        if (!config.isBankNodeExists(bill.getPayer().getBankCode())) {
            return ActionStatus.create(BizCode.NO_BANK_NODE, bill.getPayer().getBankCode());
        }

        if (!config.isBankNodeExists(bill.getReceiver().getBankCode())) {
            return ActionStatus.create(BizCode.NO_BANK_NODE, bill.getReceiver().getBankCode());
        }

        return ActionStatus.OK;
    }

    private boolean isSameBankTrans(TransferBill bill) {
        return bill.getPayer().getBankCode().equals(bill.getReceiver().getBankCode());
    }

    /**
     * 收款与付款账户都在同一银行
     */
    private Pair<InetSocketAddress, TransferBill> assignToSameBank(TransferBill bill) {
        InetSocketAddress node = config.getNode(bill.getPayer().getBankCode()).get();
        TransferBill hisBill = bill.copy();
        return asPair(node, hisBill);
    }

    /**
     * 给付款账户所在行的转账指令
     */
    private Pair<InetSocketAddress, TransferBill> asignToBankAtPayment(TransferBill bill) {
        final String bankCode = bill.getPayer().getBankCode();
        InetSocketAddress node = config.getNode(bankCode).get();

        TransferBill hisBill = bill.copy();
        // 收款账户替换为本行在对方行所开账户
        String myAccount = config.getAbcAccountOn(bankCode).get();
        hisBill.setReceiver(new AccountIdentity(myAccount, bankCode));
        return asPair(node, hisBill);
    }

    /**
     * 给收款账户所在行的转账指令
     */
    private Pair<InetSocketAddress, TransferBill> asignToBankAtReceive(TransferBill bill) {
        final String bankCode = bill.getReceiver().getBankCode();
        InetSocketAddress node = config.getNode(bankCode).get();

        TransferBill hisBill = bill.copy();
        // 付款账户替换为本行在对方行所开账户
        String myAccount = config.getAbcAccountOn(bankCode).get();
        hisBill.setPayer(new AccountIdentity(myAccount, bankCode));
        return asPair(node, hisBill);
    }

    /**
     * 对监管机构，原单转发
     */
    private Pair<InetSocketAddress, TransferBill> assignToRegulator(TransferBill bill) {
        InetSocketAddress node = config.getRegulatoryNode().get();
        return asPair(node, bill.copy());
    }

    /**
     * 简化一下，自己就原单照存
     * <p>
     * FIXME 按银行的做法，可能会很复杂
     * </p>
     */
    private TransferBill assignToMySelf(TransferBill bill) {
        return bill.copy();
    }

    @Override
    public ActionStatus prepareCommit(long xid, TransferBill bill ) {
        logger.info("prepareCommit" + xid);
        return this.billSaver.checkAndPrepare(xid, bill);
    }

    @Override
    public boolean commit(long xid){
        logger.info("commit" + xid);
        return this.billSaver.commit(xid);
    }

    @Override
    public boolean abort(long xid){
        logger.info("abort" + xid);
        return this.billSaver.abort(xid);
    }

    
    public void setBillSaver(BillSaveStrategy billSaver) {
        this.billSaver = billSaver;
    }

    public void setBillValidator(Validator<TransferBill> billValidator) {
        this.billValidator = billValidator;
    }
}
