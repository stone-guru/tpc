package net.eric.tpc.regulator;

import net.eric.tpc.common.UniFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;
import net.eric.tpc.proto.PeerTransactionManager;

public class RegulatorServiceFactory extends UniFactory {
    public static void register()    {
        UniFactory.register(RegulatorServiceFactory.class, 10);
    }
    
    private static final String MY_CLASSIFER = "REGULATOR";

    @Override
    protected <T> T createObject(Class<T> clz, String classifier) {
       if(PeerIoHandler.class.equals(clz) && this.isRegulator(classifier)){
           PeerIoHandler handler = new PeerIoHandler();
           handler.setTransManager(UniFactory.getObject(PeerTransactionManager.class, MY_CLASSIFER));
           return (T)handler;
       }
       
       if(RegulatorBizStrategy.class.equals(clz)){
           RegulatorBizStrategy strategy = new RegulatorBizStrategy();
           strategy.setTransferBillDao(UniFactory.getObject(TransferBillDao.class));
           return (T)strategy;
       }
       
       if(PeerTransactionManager.class.equals(clz) && this.isRegulator(classifier)){
           Panticipantor<TransferBill> panticipantor = new Panticipantor<TransferBill>();
           panticipantor.setBizStrategy(UniFactory.getObject(RegulatorBizStrategy.class));
           panticipantor.setDtLogger(UniFactory.getObject(DtLogger.class));
           return (T)panticipantor;
       }
        return null;
    }
    
    private boolean isRegulator(String classifier){
        return MY_CLASSIFER.equalsIgnoreCase(classifier);
    }
}
