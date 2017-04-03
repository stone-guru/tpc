package net.eric.tpc.coor;

import net.eric.tpc.biz.Validator;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.common.UniFactory;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorCommunicatorFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.Coordinator;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.KeyGenerator;
import net.eric.tpc.proto.TransactionManager;

public class CoordinatorFactory extends UniFactory {

    public static void register() {
        UniFactory.register(CoordinatorFactory.class, 10);
    }

    private static CommunicatorFactory<TransferBill> communicatorFactory = new CoorCommunicatorFactory();
    private ServerConfig config;

    @Override
    protected void close() {
        if (communicatorFactory != null) {
            communicatorFactory.close();
        }
    }

    @Override
    protected void init(Object param) {
        this.config = (ServerConfig) param;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T createObject(Class<T> clz, String classifer) {
        if (clz.equals(TransactionManager.class)) {
            AbcBizStrategy bizStrategy = new AbcBizStrategy();
            bizStrategy.setTransferBillDao(getObject(TransferBillDao.class));
            bizStrategy.setBillBasicValidator(getObject(Validator.class, TransferBill.class.getCanonicalName()));
            
            Coordinator<TransferBill> coor = new Coordinator<TransferBill>();
            coor.setBizStrategy(bizStrategy);
            coor.setDtLogger(getObject(DtLogger.class));
            coor.setCommunicatorFactory(communicatorFactory);
            coor.setSelf(config.getWorkingNode());
            coor.setKeyGenerator(getObject(KeyGenerator.class));
            
            return (T) coor;
        }
        if (clz.equals(CoorIoHandler.class)) {
            TransactionManager<TransferBill> manager = getObject(TransactionManager.class);
            CoorIoHandler handler = new CoorIoHandler();
            handler.setTransactionManager(manager);
            return (T) handler;
        }
        return null;
    }
}
