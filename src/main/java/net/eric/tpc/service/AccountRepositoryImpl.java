package net.eric.tpc.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.Futures;

import net.eric.tpc.biz.AccountRepository;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.Pair;
import net.eric.tpc.common.ShouldNotHappenException;
import net.eric.tpc.entity.Account;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.AccountDao;
import net.eric.tpc.persist.TransferBillDao;

public class AccountRepositoryImpl implements AccountRepository {
    private AccountLocker accountLocker = new AccountLocker(3, 500);
    private ExecutorService pool = Executors.newCachedThreadPool();
    private AccountDao accountDao;
    private TransferBillDao transferBillDao;
    
    @Override
    public Future<ActionStatus> checkAndPrepare(String xid, TransferBill bill) {
        if (xid == null) {
            throw new NullPointerException("xid is null");
        }
        if (bill == null) {
            throw new NullPointerException("bill is null");
        }
        ActionStatus status = bill.fieldCheck();
        if (status.isOK()) {
            Callable<ActionStatus> prepareTask = new Callable<ActionStatus>(){
                @Override
                public ActionStatus call() throws Exception {
                    return AccountRepositoryImpl.this.innerPrepareCommint(xid, bill);
                }
            };
            return pool.submit(prepareTask);
        }
        return Futures.immediateFuture(status);
    }

    private ActionStatus innerPrepareCommint(String xid, TransferBill bill) {
        String accountNumber = bill.getAccount().getNumber();
        String oppAccountNumber = bill.getOppositeAccount().getNumber();
        boolean locked = false;
        boolean success = false;
        try {
            // 加锁机制的实现目前不care账户是否正真存在
            locked = accountLocker.repeatTryLock(accountNumber, oppAccountNumber, xid);
            if (!locked) {
                return ActionStatus.create(BizCode.ACCOUNT_LOCKED, accountNumber);
            }
            ActionStatus accountCheckStatus = this.accountBalanceCheck(//
                    accountNumber, oppAccountNumber, bill.getAmount());
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
            if (!success && locked) {
                this.accountLocker.releaseLock(accountNumber, oppAccountNumber, xid);
            }
        }
    }

    private ActionStatus accountBalanceCheck(String accountNumber, String oppAccountNumber, BigDecimal amount) {
        Account account = this.accountDao.selectByAcctNumber(accountNumber);
        if (account == null) {
            return ActionStatus.create(BizCode.ACCOUNT_NOT_EXISTS, accountNumber);
        }
        Account oppositeAccount = this.accountDao.selectByAcctNumber(oppAccountNumber);
        if (oppositeAccount == null) {
            return ActionStatus.create(BizCode.ACCOUNT_NOT_EXISTS, oppAccountNumber);
        }

        final int cmpZero = account.getOverdraftLimit().compareTo(BigDecimal.ZERO);
        if (cmpZero == 0) {// 不可透支账户
            if (account.getBalance().compareTo(amount) < 0) {
                // 按银行业务规范, 不可外送账户余额,只回送转账单金额
                return ActionStatus.create(BizCode.INSUFFICIENT_BALANCE, amount.toString());
            }
            return ActionStatus.OK;
        } else if (cmpZero > 0) { // 可透支账户
            if (account.getBalance().add(account.getOverdraftLimit()).compareTo(amount) < 0) {
                return ActionStatus.create(BizCode.BEYOND_OVERDRAFT_LIMIT, amount.toString());
            }
            return ActionStatus.OK;
        } else {
            throw new ShouldNotHappenException("Overdraft limit less than zero");
        }
    }
    

    @Override
    public Future<Void> abort(String xid) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Future<Void> commit(String xid) {
        return null;
    }

    @Override
    public void createAccount(Account account) {
        this.accountDao.insert(account);
    }

    @Override
    public Account getAccount(String acctId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Account> getAllAccount() {
        // TODO Auto-generated method stub
        return null;
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



}
