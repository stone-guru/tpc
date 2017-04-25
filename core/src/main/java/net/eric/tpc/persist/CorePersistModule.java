package net.eric.tpc.persist;

public class CorePersistModule extends MyBatisModule {
    public static String MY_BATIS_CONFIG_FILE = "net/eric/tpc/persist/mapper/tpc-core-config.xml";

    public CorePersistModule(String jdbcUrl) {
        super(MyBatisModule.createSession(MY_BATIS_CONFIG_FILE, jdbcUrl), //
                new Class<?>[] { CoreDbInit.class, DtRecordDao.class, KeyStoreDao.class });
    }
}
