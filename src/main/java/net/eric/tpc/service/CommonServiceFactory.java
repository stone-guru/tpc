package net.eric.tpc.service;

import net.eric.tpc.common.KeyGenerator.KeyPersister;
import net.eric.tpc.common.UniFactory;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.KeyStoreDao;
import net.eric.tpc.proto.DtLogger;

public class CommonServiceFactory extends UniFactory {
    public static void register() {
        UniFactory.register(CommonServiceFactory.class, 9);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T createObject(Class<T> clz, String classifier) {
        if (DtLogger.class.equals(clz)) {
            DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
            dtLogger.setDtLoggerDao(UniFactory.getObject(DtRecordDao.class));
            return (T) dtLogger;
        }

        if (KeyPersister.class.equals(clz)) {
            KeyPersisterDbImpl keyPersister = new KeyPersisterDbImpl();
            keyPersister.setKeyStoreDao(UniFactory.getObject(KeyStoreDao.class));
            return (T) keyPersister;
        }
        return null;
    }
}
