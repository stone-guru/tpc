package net.eric.tpc.bankserver.net;

import org.apache.mina.core.service.IoHandlerAdapter;

import net.eric.tpc.bankserver.service.PeerTransactionManagerImpl;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.PeerTransactionManager;

public class BankServiceFactory {
    private static PeerTransactionManager<TransferMessage> transManager = null;

    public static PeerTransactionManager<TransferMessage> getTransactionManager() {
        if (transManager != null) {
            return transManager;
        }
        synchronized (BankServiceFactory.class) {
            if (transManager == null) {
                transManager = new PeerTransactionManagerImpl();
            }
        }
        return transManager;
    }

    public static IoHandlerAdapter newIoHandlerAdapter() {
        BankServerHandler handler = new BankServerHandler();
        handler.setTransManager(BankServiceFactory.getTransactionManager());
        return handler;
    }
}
