package net.eric.bank.persist;

import net.eric.tpc.persist.CoreDbInit;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.KeyStoreDao;
import net.eric.tpc.persist.MyBatisModule;

public class BankPersistModule extends MyBatisModule {

    public static String MY_BATIS_CONFIG_FILE = "net/eric/bank/persist/mapper/bank-config.xml";

    public BankPersistModule(String jdbcUrl) {
        super(MyBatisModule.createSession(MY_BATIS_CONFIG_FILE, jdbcUrl),
                new Class<?>[] { CoreDbInit.class, DtRecordDao.class, KeyStoreDao.class, //
                        AccountDao.class, TransferBillDao.class, BankDbInit.class });
    }
}
