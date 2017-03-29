package net.eric.tpc.bank;

import org.apache.mina.core.service.IoHandlerAdapter;

import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.Panticipantor;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.service.ServiceFactory;

public class BankServiceFactory {
    private static Panticipantor panticipantor = null;

    public static PeerTransactionManager<TransferBill> getTransactionManager() {
        if (panticipantor != null) {
            return panticipantor;
        }
        synchronized (BankServiceFactory.class) {
            if (panticipantor == null) {
                panticipantor = new Panticipantor<TransferBill>();
                panticipantor.setDtLogger(ServiceFactory.getDtLogger());
            }
        }
        return panticipantor;
    }

    public static IoHandlerAdapter newIoHandlerAdapter() {
        BankServerHandler handler = new BankServerHandler();
        handler.setTransManager(BankServiceFactory.getTransactionManager());
        return handler;
    }
}
