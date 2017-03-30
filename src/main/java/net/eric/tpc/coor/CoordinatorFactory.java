package net.eric.tpc.coor;

import org.apache.mina.core.service.IoHandler;

import net.eric.tpc.common.Node;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorCommunicatorFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.Coordinator;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.service.DtLoggerDbImpl;

public class CoordinatorFactory {
    private static CommunicatorFactory<TransferBill> communicatorFactory = new CoorCommunicatorFactory();

    private static TransactionManager<TransferBill> coordinator;

    public static TransactionManager<TransferBill> getCoorTransManager() {

        if (coordinator == null) {
            synchronized (CoordinatorFactory.class) {
                if (coordinator == null) {
                    Coordinator<TransferBill> coor = new Coordinator<TransferBill>();
                    coor.setDtLogger(getDtLogger());
                    coor.setBizStrategy(new AbcBizStrategy());
                    coor.setCommunicatorFactory(communicatorFactory);
                    coor.setSelf(new Node("localhost", 10088));
                    coordinator = coor;
                }
            }
        }

        return coordinator;
    }

    public static DtLogger<TransferBill> getDtLogger() {
        DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
        DtRecordDao dao = PersisterFactory.getMapper(DtRecordDao.class);
        dtLogger.setDtLoggerDao(dao);
        return dtLogger;
    }
    
    public static IoHandler getIoHandler(){
       return new CoorIoHandler();
    }
    
    public static void shutDown() {
        communicatorFactory.close();
    }
}
