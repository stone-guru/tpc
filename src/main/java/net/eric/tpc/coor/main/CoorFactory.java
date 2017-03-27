package net.eric.tpc.coor.main;

import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.coor.stub.AbcBizStrategy;
import net.eric.tpc.coor.stub.CoorDtLoggerDbImpl;
import net.eric.tpc.coor.stub.CoorTransManagerImpl;
import net.eric.tpc.coor.stub.MinaCommunicator;
import net.eric.tpc.proto.CoorTransManager;

public class CoorFactory {
    public static CoorTransManager<TransferMessage> getCoorTransManager(){
        CoorTransManagerImpl m = new CoorTransManagerImpl();
        m.setDtLogger(new CoorDtLoggerDbImpl());
        m.setBizStrategy(new AbcBizStrategy());
        m.setCommunicator(new MinaCommunicator(new ObjectSerializationCodecFactory()));
        return m;
    }
}
