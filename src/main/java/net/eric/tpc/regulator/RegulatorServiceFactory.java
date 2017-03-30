package net.eric.tpc.regulator;

import org.apache.mina.core.service.IoHandlerAdapter;

import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.persist.TransferBillDao;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Panticipantor;
import net.eric.tpc.proto.PeerBizStrategy;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.service.DtLoggerDbImpl;

public class RegulatorServiceFactory {
    private static Panticipantor<TransferBill> panticipantor = null;

    public static PeerTransactionManager<TransferBill> getTransactionManager() {
        if (panticipantor != null) {
            return panticipantor;
        }
        synchronized (RegulatorServiceFactory.class) {
            if (panticipantor == null) {
                panticipantor = new Panticipantor<TransferBill>();
                panticipantor.setDtLogger(getDtLogger());
                panticipantor.setBizStrategy(getPeerBizStrategy());
            }
        }
        return panticipantor;
    }

    public static IoHandlerAdapter newIoHandlerAdapter() {
        PeerIoHandler handler = new PeerIoHandler();
        handler.setTransManager(RegulatorServiceFactory.getTransactionManager());
        return handler;
    }

    public static DtLogger<TransferBill> getDtLogger() {
        DtLoggerDbImpl dtLogger = new DtLoggerDbImpl();
        DtRecordDao dao = PersisterFactory.getMapper(DtRecordDao.class);
        dtLogger.setDtLoggerDao(dao);
        return dtLogger;
    }

    public static PeerBizStrategy<TransferBill> getPeerBizStrategy() {
        RegulatorBizStrategy strategy = new RegulatorBizStrategy();
        TransferBillDao billDao = PersisterFactory.getMapper(TransferBillDao.class);
        strategy.setTransferBillDao(billDao);
        return strategy;
    }
}
