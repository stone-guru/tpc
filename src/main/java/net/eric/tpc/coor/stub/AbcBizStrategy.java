package net.eric.tpc.coor.stub;

import static net.eric.tpc.common.Pair.asPair;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.biz.BizCode;
import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.Configuration;
import net.eric.tpc.common.Maybe;
import net.eric.tpc.common.Node;
import net.eric.tpc.common.Pair;
import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.BizActionListener;
import net.eric.tpc.proto.CoorBizStrategy;
import net.eric.tpc.service.BillSaveStrategy;

public class AbcBizStrategy implements CoorBizStrategy<TransferBill> {
    private static final Logger logger = LoggerFactory.getLogger(AbcBizStrategy.class);

    private Configuration config = new Configuration();
    private TransferBillDao transferBillDao;
    private ExecutorService pool = Executors.newFixedThreadPool(2);
    private BillSaveStrategy billSaver = new BillSaveStrategy(this.pool);
    
    @Override
    public Maybe<TaskPartition<TransferBill>> splitTask(String xid, TransferBill bill) {
        logger.info("splitTask for " + xid);
        
        ActionStatus checkResult = basicCheck(bill);
        if (!checkResult.isOK()) {
            return Maybe.fail(checkResult);
        }

        List<Pair<Node, TransferBill>> tasks;
        if (this.isSameBankTrans(bill)) {
            tasks = ImmutableList.of(this.assignToSameBank(bill), this.assignToRegulator(bill));
        } else {
            tasks = ImmutableList.of(this.asignToBankAtPayment(bill), this.asignToBankAtReceive(bill),
                    this.assignToRegulator(bill));
        }
        final TransferBill myBill = this.assignToMySelf(bill);
        return Maybe.success(new TaskPartition<TransferBill>(myBill, tasks));
    }

    public ActionStatus basicCheck(TransferBill bill) {
        ActionStatus checkResult = bill.fieldCheck();
        if (!checkResult.isOK()) {
            return checkResult;
        }
        if (!config.isBankNodeExists(bill.getAccount().getBankCode())) {
            return ActionStatus.create(BizCode.NO_BANK_NODE, bill.getAccount().getBankCode());
        }

        if (!config.isBankNodeExists(bill.getOppositeAccount().getBankCode())) {
            return ActionStatus.create(BizCode.NO_BANK_NODE, bill.getOppositeAccount().getBankCode());
        }

        return ActionStatus.OK;
    }

    private boolean isSameBankTrans(TransferBill bill) {
        return bill.getAccount().getBankCode().equals(bill.getOppositeAccount().getBankCode());
    }

    /**
     * 收款与付款账户都在同一银行
     */
    private Pair<Node, TransferBill> assignToSameBank(TransferBill bill) {
        Node node = config.getNode(bill.getAccount().getBankCode()).get();
        TransferBill hisBill = bill.copy();
        return asPair(node, hisBill);
    }

    /**
     * 给付款账户所在行的转账指令
     */
    private Pair<Node, TransferBill> asignToBankAtPayment(TransferBill bill) {
        final String bankCode = bill.getAccount().getBankCode();
        Node node = config.getNode(bankCode).get();

        TransferBill hisBill = bill.copy();
        // 收款账户替换为本行在对方行所开账户
        String myAccount = config.getAbcAccountOn(bankCode).get();
        hisBill.setOppositeAccount(new AccountIdentity(myAccount, bankCode));
        return asPair(node, hisBill);
    }

    /**
     * 给收款账户所在行的转账指令
     */
    private Pair<Node, TransferBill> asignToBankAtReceive(TransferBill bill) {
        final String bankCode = bill.getOppositeAccount().getBankCode();
        Node node = config.getNode(bankCode).get();

        TransferBill hisBill = bill.copy();
        // 付款账户替换为本行在对方行所开账户
        String myAccount = config.getAbcAccountOn(bankCode).get();
        hisBill.setAccount(new AccountIdentity(myAccount, bankCode));
        return asPair(node, hisBill);
    }

    /**
     * 对监管机构，原单转发
     */
    private Pair<Node, TransferBill> assignToRegulator(TransferBill bill) {
        Node node = config.getRegulatoryNode().get();
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
    public Future<ActionStatus> prepareCommit(String xid, TransferBill bill) {
        logger.info("prepareCommit" + xid);
        return this.billSaver.prepareCommit(xid, bill);
    }

    @Override
    public Future<Void> commit(String xid, BizActionListener commitListener) {
        logger.info("commit" + xid);
        return this.billSaver.commit(xid, commitListener);
    }

    @Override
    public Future<Void> abort(String xid, BizActionListener abortListener) {
        logger.info("abort" + xid);
        return this.billSaver.abort(xid, abortListener);
    }

    public TransferBillDao getTransferBillDao() {
        return transferBillDao;
    }

    public void setTransferBillDao(TransferBillDao transferBillDao) {
        this.transferBillDao = transferBillDao;
        this.billSaver.setTransferBillDao(transferBillDao);
    }
}
