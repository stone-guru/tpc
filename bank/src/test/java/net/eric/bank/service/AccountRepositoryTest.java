package net.eric.bank.service;

import static net.eric.tpc.base.Pair.asPair;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.eric.bank.biz.AccountRepository;
import net.eric.bank.bod.AccountRepositoryImpl;
import net.eric.bank.entity.Account;
import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.AccountType;
import net.eric.bank.entity.TransferBill;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.proto.BizActionListener;
import net.eric.tpc.proto.PeerBizStrategy;

public class AccountRepositoryTest {
    static InetSocketAddress boc = InetSocketAddress.createUnresolved("server.boc.org", 10021);
    static InetSocketAddress bbc = InetSocketAddress.createUnresolved("server.boc.org", 10022);

    private static TransferBill genTransferMessage(String SnPrefix, long n) {
        TransferBill msg = new TransferBill();
        msg.setTransSN(SnPrefix + n);
        msg.setLaunchTime(new Date());
        msg.setPayer(new AccountIdentity("mike", "BOC"));
        msg.setReceiver(new AccountIdentity("rose", "ABC"));
        msg.setReceivingBankCode("BOC");
        msg.setAmount(BigDecimal.valueOf(500));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        return msg;
    }

    public static void prepareCommit(long xid, PeerBizStrategy<TransferBill> bizStrategy) {
        TransferBill bill = genTransferMessage("SN", xid);
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

//    private static TransStartRec genTransRec(String prefix) {
//        String xid = KeyGenerator.nextKey(prefix);
//        TransStartRec st = new TransStartRec(xid, new Node("localhost", 9001), ImmutableList.of(boc, bbc));
//        return st;
//    }

    private static BizActionListener bizActionListener = new BizActionListener() {
        @Override
        public void onSuccess(long xid) {
            System.out.println("success on " + xid);
        }

        @Override
        public void onFailure(long xid) {
            System.out.println("fail on " + xid);
        }
    };

    public static void main(String[] args) {
        //final String url = "jdbc:h2:tcp://localhost:9100/bank";

        UniFactory.register(new PersisterFactory("jdbc:h2:tcp://localhost:9100/data_abc") );
       
        AccountRepositoryImpl acctRepo = (AccountRepositoryImpl) UniFactory.getObject(AccountRepositoryImpl.class);

        // TestUtil.clearTable("ACCOUNT", "org.h2.Driver", url, "sa", "");
        // TestUtil.clearTable("TRANSFER_BILL", "org.h2.Driver", url, "sa", "");
        // initAccounts(acctRepo, "BOC");
         //prepareCommit("XID4", acctRepo);
        // acctRepo.resetLockedKey(ImmutableList.of(asPair("mike", "XID2"),
        // asPair("rose", "XID2")));
        // acctRepo.commit("XID2", bizActionListener);
        acctRepo.resetLockedKey(ImmutableList.of(asPair("mike", 10086L), asPair("rose", 10086L)));
        acctRepo.abort(10086L, bizActionListener);
    }
}
