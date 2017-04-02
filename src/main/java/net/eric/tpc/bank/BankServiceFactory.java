package net.eric.tpc.bank;

import net.eric.tpc.common.UniFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.AccountDao;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;

public class BankServiceFactory extends UniFactory {
            
    public static void register() {
        UniFactory.register(BankServiceFactory.class, 10);
    }

    
    private PeerCommunicatorFactory peerCommunicatorFactory = new PeerCommunicatorFactory();
            
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T createObject(Class<T> clz, String classifier) {
        if (PeerIoHandler.class.equals(clz) &&  isBankServer(classifier)) {
            PeerIoHandler handler = new PeerIoHandler();
            handler.setTransManager(UniFactory.getObject(Panticipantor.class, classifier));
            handler.setCommuTaskPool(peerCommunicatorFactory.getCommuTaskPool());
            return (T) handler;
        }

        if (Panticipantor.class.equals(clz) && isBankServer(classifier)) {
            Panticipantor<TransferBill> panticipantor;
            panticipantor = new Panticipantor<TransferBill>();
            panticipantor.setDtLogger(UniFactory.getObject(DtLogger.class));
            panticipantor.setBizStrategy(UniFactory.getObject(AccountRepositoryImpl.class));
            panticipantor.setDecisionQuerier(peerCommunicatorFactory.getDecisionQuerier()); 
            return (T) panticipantor;
        }
        if (AccountRepositoryImpl.class.equals(clz)) {
            AccountRepositoryImpl acctRepo = new AccountRepositoryImpl();
            AccountDao accountDao = UniFactory.getObject(AccountDao.class);
            TransferBillDao BillDao = UniFactory.getObject(TransferBillDao.class);
            acctRepo.setAccountDao(accountDao);
            acctRepo.setTransferBillDao(BillDao);
            return (T) acctRepo;
        }

        return null;
    }
    
    private boolean isBankServer(String s){
        return s!= null && "BANK".equalsIgnoreCase(s);
    }
    
    @Override
    public void close(){
        this.peerCommunicatorFactory.close();
    }
}
