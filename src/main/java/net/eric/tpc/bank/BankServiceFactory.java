package net.eric.tpc.bank;

import com.google.common.base.Optional;

import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.AccountDao;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;

public class BankServiceFactory extends UniFactory {
    private PeerCommunicatorFactory peerCommunicatorFactory = new PeerCommunicatorFactory();

    
    public BankServiceFactory(){
        NightWatch.regCloseAction(new Runnable() {
            @Override
            public void run() {
                BankServiceFactory.this.peerCommunicatorFactory.close();
            };
        });
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected <T> Optional<Pair<T, Boolean>> createObject(Class<T> clz, Object classifier ) {
        if (PeerIoHandler.class.equals(clz) && isBankServer(classifier)) {
            PeerIoHandler handler = new PeerIoHandler();
            handler.setTransManager(getObject(Panticipantor.class, classifier));
            handler.setTaskPool(peerCommunicatorFactory.getCommuTaskPool());
            return Pair.of((T) handler, true);
        }

        if (Panticipantor.class.equals(clz) && isBankServer(classifier)) {
            Panticipantor<TransferBill> panticipantor;
            panticipantor = new Panticipantor<TransferBill>();
            panticipantor.setDtLogger(getObject(DtLogger.class));
            panticipantor.setBizStrategy(getObject(AccountRepositoryImpl.class));
            panticipantor.setDecisionQuerier(peerCommunicatorFactory.getDecisionQuerier());
            return Pair.of((T) panticipantor, true);
        }
        if (AccountRepositoryImpl.class.equals(clz)) {
            AccountRepositoryImpl acctRepo = new AccountRepositoryImpl();
            AccountDao accountDao = getObject(AccountDao.class);
            TransferBillDao BillDao = getObject(TransferBillDao.class);
            acctRepo.setAccountDao(accountDao);
            acctRepo.setTransferBillDao(BillDao);
            acctRepo.setBillValidator(getObject(Validator.class, TransferBill.class));
            return Pair.of((T) acctRepo, true);
        }

        return Optional.absent();
    }

    private boolean isBankServer(Object s) {
        return s != null && "BANK".equals(s);
    }
 
}
