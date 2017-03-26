package net.eric.tpc.bankserver.net;

import org.apache.mina.core.service.IoHandlerAdapter;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.AbstractTransManager;
import net.eric.tpc.proto.TransactionManager;

public class BankServiceFactory {
    private static TransactionManager<TransferMessage> transManager = null;
    
    
    public static TransactionManager<TransferMessage> getTransactionManager(){
        if(transManager != null){
            return transManager;
        }
        transManager = new AbstractTransManager<TransferMessage>();
        return transManager;
    }
    
    public static IoHandlerAdapter newIoHandlerAdapter(){
        BankServerHandler handler = new BankServerHandler();
        handler.setTransManager(BankServiceFactory.getTransactionManager());
        return handler;
    }
}
