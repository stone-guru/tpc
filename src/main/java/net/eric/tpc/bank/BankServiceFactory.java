package net.eric.tpc.bank;

import org.apache.mina.core.service.IoHandlerAdapter;

import net.eric.tpc.biz.AccountRepository;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.AccountDao;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.service.DtLoggerDbImpl;

public class BankServiceFactory {
    private static Panticipantor<TransferBill>  panticipantor = null;
    private static AccountRepositoryImpl accountRepository = null;
    
    public static PeerTransactionManager<TransferBill> getTransactionManager() {
        if (panticipantor != null) {
            return panticipantor;
        }
        synchronized (BankServiceFactory.class) {
            if (panticipantor == null) {
                panticipantor = new Panticipantor<TransferBill>();
                panticipantor.setDtLogger(getDtLogger());
                panticipantor.setBizStrategy(getPeerBizStrategy());
            }
        }
        return panticipantor;
    }


    public static AccountRepositoryImpl getAccountRepositoryImpl() {
        if (accountRepository != null) {
            return accountRepository;
        }
        synchronized (BankServiceFactory.class) {
            if (accountRepository == null) {
                AccountRepositoryImpl acctRepo = new AccountRepositoryImpl();
                AccountDao accountDao = PersisterFactory.getMapper(AccountDao.class);
                TransferBillDao BillDao = PersisterFactory.getMapper(TransferBillDao.class);
                acctRepo.setAccountDao(accountDao);
                acctRepo.setTransferBillDao(BillDao);
                accountRepository = acctRepo;
            }
        }
        return accountRepository;
    }
    
    public static DtLogger<TransferBill> getDtLogger() {
        DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
        DtRecordDao dao = PersisterFactory.getMapper(DtRecordDao.class);
        dtLogger.setDtLoggerDao(dao);
        return dtLogger;
    }
    
    public static PeerBizStrategy<TransferBill> getPeerBizStrategy(){
        return getAccountRepositoryImpl();
    }
    
    public static AccountRepository getAccountRepository() {
        return getAccountRepositoryImpl();
    }
    
    public static IoHandlerAdapter newIoHandlerAdapter() {
        PeerIoHandler handler = new PeerIoHandler();
        handler.setTransManager(BankServiceFactory.getTransactionManager());
        return handler;
    }
}
