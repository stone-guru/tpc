package net.eric.tpc.persist;

import java.io.Reader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import net.eric.tpc.entity.Account;
import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.AccountType;
import net.eric.tpc.entity.TransferBill;

public class H2DbTest {
    private static SqlSession session;

    public static void main(String[] args) throws Exception {
        Reader reader = Resources.getResourceAsReader("net/eric/tpc/bankserver/persist/mapper/bankserver-config.xml");
        Properties properties = new Properties();
        properties.put("url", "jdbc:h2:tcp://localhost:9100/bank");

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        session = sqlSessionFactory.openSession();

        // accountDao.createAccountTable();

        // accountDao.modifyBalance(asPair("mike", BigDecimal.valueOf(-100)));

        //DatabaseInit databaseInit = session.getMapper(DatabaseInit.class);
        //databaseInit.createAccountTable();
        // databaseInit.createTransferMessageTable();

        AccountDao accountDao = session.getMapper(AccountDao.class);
        //initAccounts(accountDao, "BOC");
        insertTransferMessage();
        session.commit();
        session.close();
    }

    public static void insertTransferMessage() {
        TransferBillDao dmDao = session.getMapper(TransferBillDao.class);
        TransferBill msg = new TransferBill();

//        msg.setVersion("1.1");
//        msg.setMessageNumber("2193239888");
//        msg.setLaunchTime(new Date());
//        msg.setSender("BOC Server");
//        msg.setReceiver("KOT Server");
//        msg.setMessageType(MessageType.NORMAL);

        msg.setTransSN("982872393");

        msg.setAccount(new AccountIdentity("mike", "BOC"));
        msg.setOppositeAccount(new AccountIdentity("jack", "ABC"));
        msg.setReceivingBankCode("BOC");
        msg.setAmount(BigDecimal.valueOf(200));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

    }

    public static void initAccounts(AccountDao accountDao, String bankCode) {
        List<String> names = Arrays.asList("mike", "tom", "rose", "kate", "BOC", "CCB");
        Date now = new Date();
        BigDecimal balance = BigDecimal.valueOf(1000L);

        for (String name : names) {
            if (name.equalsIgnoreCase(bankCode)) {
                continue;
            }
            
            final boolean isBankAccount ="BOC".equalsIgnoreCase(name) || "CCB".equalsIgnoreCase(name);
            Account account = new Account();
                        
            account.setType(isBankAccount ? AccountType.BANK : AccountType.PERSONAL);
            account.setAcctName(name);
            account.setAcctNumber(name);
            account.setBalance(isBankAccount? BigDecimal.ZERO : balance);
            account.setBankCode(bankCode);
            account.setOpeningTime(now);
            account.setLastModiTime(now);

            accountDao.insert(account);
            System.out.println("Insert account " + account.getAcctName());
        }
    }
}
