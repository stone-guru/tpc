package net.eric.tpc.service;

import net.eric.tpc.biz.AccountRepository;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.AccountDao;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;

public class ServiceFactory {
    public static DtLogger<TransferBill> getDtLogger() {
        DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
        DtRecordDao dao = PersisterFactory.getMapper(DtRecordDao.class);
        dtLogger.setDtLoggerDao(dao);
        return dtLogger;
    }

    public static AccountRepository getAccountRepository() {
        AccountRepositoryImpl acctRepo = new AccountRepositoryImpl();
        AccountDao accountDao = PersisterFactory.getMapper(AccountDao.class);
        TransferBillDao BillDao = PersisterFactory.getMapper(TransferBillDao.class);
        acctRepo.setAccountDao(accountDao);
        acctRepo.setTransferBillDao(BillDao);
        return acctRepo;

    }
}
