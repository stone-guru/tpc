package net.eric.tpc.coor.main;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorCommunicatorFactory;
import net.eric.tpc.coor.stub.CoorDtLoggerDbImpl;
import net.eric.tpc.coor.stub.CoorTransManagerImpl;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.CoorTransManager;

public class CoorFactory {
    private static CommunicatorFactory<TransferMessage> communicatorFactory = new CoorCommunicatorFactory();
    public static CoorTransManager<TransferMessage> getCoorTransManager(){
        CoorTransManagerImpl m = new CoorTransManagerImpl();
        m.setDtLogger(new CoorDtLoggerDbImpl());
        m.setBizStrategy(new AbcBizStrategy());
        m.setCommunicatorFactory(communicatorFactory);
        return m;
    }
    
    public static void shutDown(){
        communicatorFactory.close();
    }
}
