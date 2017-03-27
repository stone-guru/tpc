package net.eric.tpc.coor.stub;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.AbstractCoorTransManager;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.CoorBizStrategy;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Node;

public class CoorTransManagerImpl extends AbstractCoorTransManager<TransferMessage> {

    @Override
    protected DtLogger<TransferMessage> dtLogger() {
        return this.dtLogger;
    }

    @Override
    protected CoorBizStrategy<TransferMessage> bizStrategy() {
        return this.bizStrategy;
    }

    @Override
    protected CommunicatorFactory<TransferMessage> communicatorFactory() {
        return this.communicatorFactory;
    }

    @Override
    protected Node mySelf() {
        return this.myself;
    }
    
    public void setDtLogger(DtLogger<TransferMessage> dtLogger) {
        this.dtLogger = dtLogger;
    }

    public void setBizStrategy(CoorBizStrategy<TransferMessage> bizStrategy) {
        this.bizStrategy = bizStrategy;
    }

    public void setCommunicatorFactory(CommunicatorFactory<TransferMessage> communicator) {
        this.communicatorFactory = communicator;
    }

    public void setMyself(Node myself) {
        this.myself = myself;
    }

    private DtLogger<TransferMessage> dtLogger;
    private CoorBizStrategy<TransferMessage> bizStrategy;
    private CommunicatorFactory<TransferMessage> communicatorFactory;
    private Node myself = new Node("localhost", 7878);
}
