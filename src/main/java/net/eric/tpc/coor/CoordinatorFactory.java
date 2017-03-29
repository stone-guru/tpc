package net.eric.tpc.coor;

import net.eric.tpc.common.Node;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorCommunicatorFactory;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.Coordinator;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.service.DtLoggerDbImpl;
import net.eric.tpc.service.ServiceFactory;

public class CoordinatorFactory {
    private static CommunicatorFactory<TransferBill> communicatorFactory = new CoorCommunicatorFactory();

    private static TransactionManager<TransferBill> coordinator;

    public static TransactionManager<TransferBill> getCoorTransManager() {

        if (coordinator == null) {
            synchronized (CoordinatorFactory.class) {
                if (coordinator == null) {
                    Coordinator<TransferBill> coor = new Coordinator<TransferBill>();
                    coor.setDtLogger(ServiceFactory.getDtLogger());
                    coor.setBizStrategy(new AbcBizStrategy());
                    coor.setCommunicatorFactory(communicatorFactory);
                    coor.setSelf(new Node("localhost", 10088));
                    coordinator = coor;
                }
            }
        }

        return coordinator;
    }

    public static void shutDown() {
        communicatorFactory.close();
    }
}
