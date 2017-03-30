package net.eric.tpc.service;

import static net.eric.tpc.common.Pair.asPair;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.bank.AccountRepositoryImpl;
import net.eric.tpc.bank.BankServiceFactory;
import net.eric.tpc.biz.AccountRepository;
import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.common.Node;
import net.eric.tpc.entity.Account;
import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.AccountType;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.proto.BizActionListener;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.proto.TransStartRec;

public class AccountRepositoryTest {
    static Node boc = new Node("server.boc.org", 10021);
    static Node bbc = new Node("server.boc.org", 10022);

    private static TransferBill genTransferMessage(String SnPrefix, int n) {
        TransferBill msg = new TransferBill();
        msg.setTransSN(SnPrefix + n);
        msg.setLaunchTime(new Date());
        msg.setAccount(new AccountIdentity("mike", "BOC"));
        msg.setOppositeAccount(new AccountIdentity("rose", "ABC"));
        msg.setReceivingBankCode("BOC");
        msg.setAmount(BigDecimal.valueOf(500));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        return msg;
    }

    public static void prepareCommit(String xid, PeerBizStrategy<TransferBill> bizStrategy) {
        TransferBill bill = genTransferMessage(xid, 1);
        ActionStatus result = ActionStatus.force(bizStrategy.checkAndPrepare(xid, bill));
        System.out.println(result);
    }

    public static void initAccounts(AccountRepository acctRepo, String bankCode) {
        List<String> names = Arrays.asList("mike", "tom", "rose", "kate", "BOC", "CCB");
        Date now = new Date();
        BigDecimal balance = BigDecimal.valueOf(1000L);

        for (String name : names) {
            if (name.equalsIgnoreCase(bankCode)) {
                continue;
            }

            final boolean isBankAccount = "BOC".equalsIgnoreCase(name) || "CCB".equalsIgnoreCase(name);
            Account account = new Account();

            account.setType(isBankAccount ? AccountType.BANK : AccountType.PERSONAL);
            account.setAcctName(name);
            account.setAcctNumber(name);
            account.setBalance(isBankAccount ? BigDecimal.ZERO : balance);
            account.setBankCode(bankCode);
            account.setOpeningTime(now);
            account.setLastModiTime(now);
            account.setOverdraftLimit(name.equals("mike") ? BigDecimal.ZERO : BigDecimal.valueOf(2000));
            acctRepo.createAccount(account);
            System.out.println("Insert account " + account.getAcctName());
        }
    }

    private static TransStartRec genTransRec(String prefix) {
        String xid = KeyGenerator.nextKey(prefix);
        TransStartRec st = new TransStartRec(xid, new Node("localhost", 9001), ImmutableList.of(boc, bbc));
        return st;
    }

    private static BizActionListener bizActionListener = new BizActionListener() {
        @Override
        public void onSuccess(String xid) {
            System.out.println("success on " + xid);
        }

        @Override
        public void onFailure(String xid) {
            System.out.println("fail on " + xid);
        }
    };

    public static void main(String[] args) {
        final String url = "jdbc:h2:tcp://localhost:9100/bank";
        KeyGenerator.init();
        PersisterFactory.initialize(url);
        AccountRepositoryImpl acctRepo = (AccountRepositoryImpl) BankServiceFactory.getAccountRepository();

        // TestUtil.clearTable("ACCOUNT", "org.h2.Driver", url, "sa", "");
        // TestUtil.clearTable("TRANSFER_BILL", "org.h2.Driver", url, "sa", "");
        // initAccounts(acctRepo, "BOC");
         //prepareCommit("XID4", acctRepo);
        // acctRepo.resetLockedKey(ImmutableList.of(asPair("mike", "XID2"),
        // asPair("rose", "XID2")));
        // acctRepo.commit("XID2", bizActionListener);
        acctRepo.resetLockedKey(ImmutableList.of(asPair("mike", "XID4"), asPair("rose", "XID4")));
        acctRepo.abort("XID4", bizActionListener);
    }
}
