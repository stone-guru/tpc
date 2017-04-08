package net.eric.tpc.coor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Optional;

import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.biz.Validator;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorCommunicatorFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.Coordinator;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.KeyGenerator;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.service.BillSaveStrategy;

public class CoordinatorFactory extends UniFactory {

    private CommunicatorFactory<TransferBill> communicatorFactory;
    private ServerConfig config;
    private ExecutorService ioHandlerTaskPool = Executors.newFixedThreadPool(3);
    
    public CoordinatorFactory(ServerConfig config) {
        this.config = config;
        this.communicatorFactory = new CoorCommunicatorFactory();

        NightWatch.regCloseAction(new Runnable() {
            @Override
            public void run() {
                CoordinatorFactory.this.ioHandlerTaskPool.shutdown();
                if (CoordinatorFactory.this.communicatorFactory != null) {
                    CoordinatorFactory.this.communicatorFactory.close();
                }
            };
        });
    }

    @SuppressWarnings("unchecked")

    @Override
    protected <T> Optional<Pair<T, Boolean>> createObject(Class<T> clz, Object classifier) {
        if (clz.equals(TransactionManager.class)) {
            AbcBizStrategy bizStrategy = new AbcBizStrategy();
            bizStrategy.setBillSaver(getObject(BillSaveStrategy.class));
            bizStrategy.setBillValidator(getObject(Validator.class, TransferBill.class));

            Coordinator<TransferBill> coor = new Coordinator<TransferBill>();
            coor.setBizStrategy(bizStrategy);
            coor.setDtLogger(getObject(DtLogger.class));
            coor.setCommunicatorFactory(communicatorFactory);
            coor.setSelf(config.getWorkingNode());
            coor.setKeyGenerator(getObject(KeyGenerator.class));

            return Pair.of((T) coor, true);
        }
        if (clz.equals(CoorIoHandler.class)) {
            TransactionManager<TransferBill> manager = getObject(TransactionManager.class);
            CoorIoHandler handler = new CoorIoHandler();
            handler.setTransactionManager(manager);
            handler.setDtLogger(getObject(DtLogger.class));
            handler.setTaskPool(ioHandlerTaskPool);
            return Pair.of((T) handler, true);
        }
        return Optional.absent();
    }

}
