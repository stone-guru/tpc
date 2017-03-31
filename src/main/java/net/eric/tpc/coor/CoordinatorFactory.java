package net.eric.tpc.coor;

import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.common.UniFactory;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorCommunicatorFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.Coordinator;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.TransactionManager;

public class CoordinatorFactory extends UniFactory {

    public static void register()   {
        UniFactory.register(CoordinatorFactory.class, 10);
    }
    
    private static CommunicatorFactory<TransferBill> communicatorFactory = new CoorCommunicatorFactory();
    private ServerConfig config;
    
    @Override
    protected void close(){
        if (communicatorFactory != null) {
            communicatorFactory.close();
        }
    }

    
    @Override
    protected void init(Object param){
        this.config = (ServerConfig)param;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T createObject(Class<T> clz, String classifer) {
        if (clz.equals(TransactionManager.class)) {
            Coordinator<TransferBill> coor = new Coordinator<TransferBill>();
            coor.setDtLogger(UniFactory.getObject(DtLogger.class));
            AbcBizStrategy bizStrategy = new AbcBizStrategy();
            bizStrategy.setTransferBillDao(UniFactory.getObject(TransferBillDao.class));
            coor.setBizStrategy(bizStrategy);
            coor.setCommunicatorFactory(communicatorFactory);
            coor.setSelf(config.getWorkingNode());
            return (T) coor;
        }
        if(clz.equals(CoorIoHandler.class)){
            TransactionManager<TransferBill> manager = UniFactory.getObject(TransactionManager.class);
            CoorIoHandler handler = new CoorIoHandler();
            handler.setTransactionManager(manager);
            return (T)handler;
        }
        return null;
    }
}
