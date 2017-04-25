package net.eric.bank.bod;

import static net.eric.tpc.base.Pair.asPair;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import net.eric.bank.biz.AccountRepository;
import net.eric.bank.biz.BizCode;
import net.eric.bank.biz.Validator;
import net.eric.bank.entity.Account;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.persist.AccountDao;
import net.eric.bank.persist.TransferBillDao;
import net.eric.bank.util.Util;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.proto.Types.Decision;

public class AccountRepositoryImpl implements PeerBizStrategy, AccountRepository{
    private static final Logger logger = LoggerFactory.getLogger(AccountRepositoryImpl.class);

    private AccountLocker accountLocker = new AccountLocker(3, 500);
    private AccountDao accountDao;
    private TransferBillDao transferBillDao;
    private Validator<TransferBill> billValidator;

    @Override
    public ActionStatus checkAndPrepare(long xid, Object b) {
        Preconditions.checkNotNull(b, "bill");
        Preconditions.checkArgument(b instanceof TransferBill);

        final TransferBill bill = (TransferBill) b;
        ActionStatus status = billValidator.check(bill);
        if (status.isOK())
            return AccountRepositoryImpl.this.innerPrepareCommint(xid, bill);
        return status;
    }

    private ActionStatus innerPrepareCommint(long xid, TransferBill bill) {
        String accountNumber = bill.getPayer().getNumber();
        String oppAccountNumber = bill.getReceiver().getNumber();
        boolean locked = false;
        boolean success = false;
        try {
            // 加锁机制的实现目前不care账户是否正真存在，先锁再操作
            locked = accountLocker.repeatTryLock(accountNumber, oppAccountNumber, xid);
            if (!locked) {
                return ActionStatus.create(BizCode.ACCOUNT_LOCKED, accountNumber);
            }

            ActionStatus accountCheckStatus = this.businessCheck(bill);
            if (!accountCheckStatus.isOK()) {
                return accountCheckStatus;
            }
            try {
                this.transferBillDao.insert(bill);
                this.transferBillDao.updateLock(Pair.asPair(bill.getTransSN(), xid));
            } catch (Exception e) {
                return ActionStatus.create(BizCode.INNER_EXCEPTION, e.getMessage());
            }
            success = true;
            return ActionStatus.OK;
        } catch (Exception e) {
            return ActionStatus.innerError(e.getMessage());
        } finally {
            if (locked && !success) {
                this.accountLocker.releaseLock(accountNumber, oppAccountNumber, xid);
            }
            if (!success) {
                this.displayAllAccount();// 本次事务没有以后了，显示下账户信息
            }
        }
    }

    private ActionStatus businessCheck(TransferBill bill) {
        String accountNumber = bill.getPayer().getNumber();
        String oppAccountNumber = bill.getReceiver().getNumber();

        Account account = this.accountDao.selectByAcctNumber(accountNumber);
        if (account == null) {
            return ActionStatus.create(BizCode.ACCOUNT_NOT_EXISTS, accountNumber);
        }

        Account oppositeAccount = this.accountDao.selectByAcctNumber(oppAccountNumber);
        if (oppositeAccount == null) {
            return ActionStatus.create(BizCode.ACCOUNT_NOT_EXISTS, oppAccountNumber);
        }

        // 确认所申请转账账户是在我方开户账户
        if (!bill.getPayer().getBankCode().equalsIgnoreCase(account.getBankCode())) {
            return ActionStatus.create(BizCode.NOT_MY_ACCOUNT, bill.getPayer().toString());
        }

        if (!bill.getReceiver().getBankCode().equalsIgnoreCase(oppositeAccount.getBankCode())) {
            return ActionStatus.create(BizCode.NOT_MY_ACCOUNT, bill.getReceiver().toString());
        }

        // 同一个账户，转着玩吗 :-)
        if (accountNumber.equals(oppAccountNumber)) {
            return ActionStatus.create(BizCode.SAME_ACCOUNT, "Two accounts are same " + accountNumber);
        }

        return this.balanceCheck(account, bill.getAmount());
    }

    /**
     * 对转出方进行余额检查
     */
    private ActionStatus balanceCheck(Account account, BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ActionStatus.create(BizCode.AMOUNT_LE_ZERO, amount.toPlainString() + " <= 0");
        }

        final int cmpZero = account.getOverdraftLimit().compareTo(BigDecimal.ZERO);
        if (cmpZero == 0) {// 不可透支账户
            if (account.getBalance().compareTo(amount) < 0) {
                // 按银行业务规范, 不可外送账户余额,只回送转账单金额
                return ActionStatus.create(BizCode.INSUFFICIENT_BALANCE, amount.toString());
            }
            return ActionStatus.OK;
        }
        if (cmpZero > 0) { // 可透支账户
            if (account.getBalance().add(account.getOverdraftLimit()).compareTo(amount) < 0) {
                return ActionStatus.create(BizCode.BEYOND_OVERDRAFT_LIMIT, amount.toString());
            }
            return ActionStatus.OK;
        }
        // 可透支金额居然为负
        return ActionStatus.create(BizCode.INNER_EXCEPTION, "account data is not correct");
    }

    @Override
    public boolean commit(long xid) {
        return doDecision(xid, Decision.COMMIT);
    }

    @Override
    public boolean abort(long xid) {
        return doDecision(xid, Decision.ABORT);
    }

    private boolean doDecision(long xid, Decision decision) {
        Maybe<Pair<String, String>> maybe = this.accountLocker.getLockedKeyByXid(xid);
        if (!maybe.isRight()) {
            logger.error("when " + decision + ", " + maybe.getLeft().toString());
            return false;
        }

        boolean success = false;
        try {
            if (decision == Decision.COMMIT) {
                AccountRepositoryImpl.this.innerCommit(xid);
            } else if (decision == Decision.ABORT) {
                AccountRepositoryImpl.this.innerAbort(xid);
            } else {
                throw new UnImplementedException("Decision Enum got more");
            }
            success = true;
        } catch (Exception e) {
            logger.error("AccountRepositoryImpl.innerCommit", e);
        }

        // 显示本次事务处理后的账户情况
        AccountRepositoryImpl.this.displayAllAccount();
        return success;
    }

    private void innerCommit(long xid) {
        TransferBill bill = this.transferBillDao.selectByXid(xid);
        Preconditions.checkNotNull(bill, "bill for xid " + xid + " not exists unexpectedly.");

        final String accountNumber = bill.getPayer().getNumber();
        final String oppositeAccountNumber = bill.getReceiver().getNumber();
        BigDecimal amount = bill.getAmount();

        this.accountDao.modifyBalance(asPair(accountNumber, amount.negate()));
        this.accountDao.modifyBalance(asPair(oppositeAccountNumber, amount));
        this.transferBillDao.updateLock(asPair(bill.getTransSN(), (Long)null));

        // 若上述过程出现异常，解锁操作不会发生，保护账户不再执行其它操作，也利于其它诊断
        this.accountLocker.releaseByXid(xid);
    }

    private void innerAbort(long xid) {
        TransferBill bill = this.transferBillDao.selectByXid(xid);
        if (bill == null) {// 在检查阶段就没有保存，此处即可为空
            return;
        }
        Preconditions.checkNotNull(bill, "bill for xid " + xid + " not exists unexpectedly.");

        this.transferBillDao.deleteByXid(xid);

        // 若上述过程出现异常，解锁操作不会发生，保护账户不再执行其它操作，也利于其它诊断
        this.accountLocker.releaseByXid(xid);
    }

    private void displayAllAccount() {
        List<Account> accounts = this.accountDao.selectAll();
        Util.displayAccounts(accounts);
    }

    @Override
    public void createAccount(Account account) {
        this.accountDao.insert(account);
    }

    @Override
    public Account getAccount(String acctNumber) {
        return this.accountDao.selectByAcctNumber(acctNumber);
    }

    @Override
    public List<Account> getAllAccount() {
        return this.accountDao.selectAll();
    }

    public void resetLockedKey(List<Pair<String, Long>> keys) {
        this.accountLocker.setLockedKey(keys);
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
        this.accountLocker.setAccountDao(accountDao);
    }

    public TransferBillDao getTransferMessageDao() {
        return transferBillDao;
    }

    public void setTransferBillDao(TransferBillDao transferBillDao) {
        this.transferBillDao = transferBillDao;
    }

    public void setBillValidator(Validator<TransferBill> billValidator) {
        this.billValidator = billValidator;
    }
}
