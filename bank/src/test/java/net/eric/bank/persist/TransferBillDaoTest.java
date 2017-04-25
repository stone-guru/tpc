package net.eric.bank.persist;

import static net.eric.tpc.base.Pair.asPair;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Date;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.TransferBill;
import net.eric.tpc.base.NightWatch;

@Test(singleThreaded=true)
public class TransferBillDaoTest {
    private static final String BILL_SN = "982872393";

    private TransferBillDao transferBillDao;

    @BeforeSuite
    public static void init() {
        final String dbUrl = "jdbc:h2:/tmp/account_test";
        //FIXME UniFactory.registerMaybe(null);//FIXME new PersisterFactory(dbUrl));
    }

    @AfterSuite
    public static void close() {
        NightWatch.executeCloseActions();
    }

    @BeforeMethod
    public void beforeTest() {
        BankDbInit initor = null;//FIXME UniFactory.getObject(BankDbInit.class);
        initor.dropTransferBillTable();
        initor.createTransferBillTable();

        this.transferBillDao = null;//FIXME UniFactory.getObject(TransferBillDao.class);
        this.transferBillDao.insert(newBill());
    }

    @Test
    public void testUpdateLock1() {
        this.transferBillDao.updateLock(asPair(BILL_SN, 10086L));
    }

    @Test
    public void testUpdateLock2() {
        this.transferBillDao.updateLock(asPair(BILL_SN, null));
    }

    @Test
    public void testSelectByXid() {
        this.transferBillDao.updateLock(asPair(BILL_SN, 10086L));
        TransferBill bill = this.transferBillDao.selectByXid(10086L);
        assertNotNull(bill);
        assertEquals(BILL_SN, bill.getTransSN());
    }

   @Test
    public void testDeleteByXid() {
        this.transferBillDao.updateLock(asPair(BILL_SN, 10086L));

        this.transferBillDao.deleteByXid(10086L);
        
        TransferBill bill = this.transferBillDao.selectByXid(10086L);
        assertNull(bill);
    }

    @Test
    public void testClearLockByXid() {
        this.transferBillDao.updateLock(asPair(BILL_SN, 10086L));

        this.transferBillDao.deleteByXid(10086L);
        
        this.transferBillDao.clearLockByXid(10086L);
    }
    
    private TransferBill newBill() {
        TransferBill bill = new TransferBill();
        bill.setTransSN(BILL_SN);
        bill.setLaunchTime(new Date());
        bill.setPayer(new AccountIdentity("mike", "BOC"));
        bill.setReceiver(new AccountIdentity("jack", "ABC"));
        bill.setReceivingBankCode("BOC");
        bill.setAmount(BigDecimal.valueOf(200));
        bill.setSummary("for cigrate");
        bill.setVoucherNumber("BIK09283-33843");

        return bill;
    }
}
