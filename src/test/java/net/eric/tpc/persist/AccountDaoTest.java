package net.eric.tpc.persist;

import static net.eric.tpc.base.Pair.asPair;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.entity.Account;
import net.eric.tpc.entity.AccountType;

public class AccountDaoTest {
    private AccountDao accountDao;
    

    @BeforeSuite
    public static void init(){
        final String dbUrl = "jdbc:h2:/tmp/account_test";
        UniFactory.registerMaybe(new PersisterFactory(dbUrl));
    }
    
    @AfterSuite
    public static void close(){
        NightWatch.executeCloseActions();
    }
    
    @BeforeTest
    public void beforeTest(){
        DatabaseInit initor = UniFactory.getObject(DatabaseInit.class);
        initor.dropAccountTable();
        initor.createAccountTable();
        
        this.accountDao = UniFactory.getObject(AccountDao.class);
        accountDao.insert(ryan());
        
    }
    
    
    @Test
    public void testModifyBalance() {
        accountDao.modifyBalance(asPair("ryan", BigDecimal.TEN));
    }

    @Test
    public void testUpdateLock() {
        accountDao.updateLock(asPair("ryan", null));
        accountDao.updateLock(asPair("ryan", 10086L));
    }

    @Test
    public void testSelectByAcctNumber() {
        final String accountNumber = "ryan";
       Account acct = accountDao.selectByAcctNumber(accountNumber);
       assertNotNull(acct);
       assertEquals(accountNumber, acct.getAcctNumber());
    }

    @Test
    public void testSelectAll() {
       List<Account> accounts = accountDao.selectAll();
       assertEquals(1, accounts.size());
    }

    
    private Account ryan(){
        Account ryan = new Account();
        ryan.setType(AccountType.PERSONAL);
        ryan.setAcctName("Ryan.White");
        ryan.setAcctNumber("ryan");
        ryan.setBalance(BigDecimal.valueOf(1000L));
        ryan.setOverdraftLimit(BigDecimal.valueOf(4000L));
        ryan.setBankCode("BOC");
        ryan.setOpeningTime(new Date());
        ryan.setLastModiTime(new Date());
        
        return ryan;
    }
}
