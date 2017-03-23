package net.eric.tpc.bankserver.net;

import org.apache.mina.core.service.IoHandlerAdapter;

import net.eric.tpc.proto.AbstractTransManager;
import net.eric.tpc.proto.TransactionManager;

public class BankServiceFactory {
	@SuppressWarnings("rawtypes")
	private static TransactionManager transManager = null;
	
	
	public static TransactionManager getTransactionManager(){
		if(transManager != null){
			return transManager;
		}
		transManager = new AbstractTransManager();
		return transManager;
	}
	
	public static IoHandlerAdapter newIoHandlerAdapter(){
		BankServerHandler handler = new BankServerHandler();
		handler.setTransManager(BankServiceFactory.getTransactionManager());
		return handler;
	}
}
