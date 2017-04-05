package net.eric.tpc.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Optional;

import net.eric.tpc.base.Pair;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.KeyStoreDao;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.KeyGenerator;

public class CommonServiceFactory extends UniFactory {

    private ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public CommonServiceFactory() {
        KeyPersisterDbImpl keyPersister = new KeyPersisterDbImpl();
        keyPersister.setKeyStoreDao(getObject(KeyStoreDao.class));
        KeyGenerators.init(keyPersister);

        NightWatch.regCloseAction(new Runnable() {
            @Override
            public void run() {
                CommonServiceFactory.this.threadPool.shutdownNow();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> Optional<Pair<T, Boolean>> createObject(Class<T> clz, Object classifier) {
        if (DtLogger.class.equals(clz)) {
            DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
            dtLogger.setDtLoggerDao(getObject(DtRecordDao.class));
            return Pair.of((T) dtLogger, true);
        }
        if (KeyGenerator.class.equals(clz)) {
            return Pair.of((T) KeyGenerators.getInstance(), true);
        }
        if (Validator.class.equals(clz) && TransferBill.class.getCanonicalName().equals(classifier)) {
            return Pair.of((T) new BillBasicValidator(), true);
        }
        if (BillSaveStrategy.class.equals(clz)) {
            BillSaveStrategy bsStrategy = new BillSaveStrategy();
            bsStrategy.setBillValidator(getObject(Validator.class, TransferBill.class.getCanonicalName()));
            bsStrategy.setThreadPool(threadPool);
            bsStrategy.setTransferBillDao(getObject(TransferBillDao.class));
            return Pair.of((T) bsStrategy, true);
        }
        return Optional.absent();
    }

}
