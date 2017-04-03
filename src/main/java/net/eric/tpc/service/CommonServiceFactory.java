package net.eric.tpc.service;

import net.eric.tpc.biz.Validator;
import net.eric.tpc.common.UniFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.KeyStoreDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.KeyGenerator;

import static net.eric.tpc.common.UniFactory.getObject;

public class CommonServiceFactory extends UniFactory {
    public static void register() {
        UniFactory.register(CommonServiceFactory.class, 9);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T createObject(Class<T> clz, String classifier) {
        if (DtLogger.class.equals(clz)) {
            DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
            dtLogger.setDtLoggerDao(getObject(DtRecordDao.class));
            return (T) dtLogger;
        }
        if (KeyGenerator.class.equals(clz)) {
            return (T) KeyGenerators.getInstance();
        }
        if (Validator.class.equals(clz) && TransferBill.class.getCanonicalName().equals(classifier)) {
            return (T) new BillBasicValidator();
        }
        return null;
    }

    @Override
    public void init(Object param) {
        KeyPersisterDbImpl keyPersister = new KeyPersisterDbImpl();
        keyPersister.setKeyStoreDao( getObject(KeyStoreDao.class));
        KeyGenerators.init(keyPersister);
    }
}
