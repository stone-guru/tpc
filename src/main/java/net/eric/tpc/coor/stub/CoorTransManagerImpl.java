package net.eric.tpc.coor.stub;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.proto.AbstractCoorTransManager;
import net.eric.tpc.proto.CoorBizStrategy;
import net.eric.tpc.proto.CoorCommunicator;
import net.eric.tpc.proto.CoorDtLogger;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.Vote;

public class CoorTransManagerImpl extends AbstractCoorTransManager<TransferMessage> {

    @Override
    protected CoorDtLogger dtLogger() {
        return this.dtLogger;
    }

    @Override
    protected CoorBizStrategy<TransferMessage> bizStrategy() {
        return this.bizStrategy;
    }

    @Override
    protected CoorCommunicator<TransferMessage> communicator() {
        return this.communicator;
    }

    @Override
    protected Node mySelf() {
        return this.myself;
    }

    @Override
    protected Vote selfVote(String xid, TransferMessage biz) {
        return null;
    }
    
    public void setDtLogger(CoorDtLogger dtLogger) {
        this.dtLogger = dtLogger;
    }

    public void setBizStrategy(CoorBizStrategy<TransferMessage> bizStrategy) {
        this.bizStrategy = bizStrategy;
    }

    public void setCommunicator(CoorCommunicator<TransferMessage> communicator) {
        this.communicator = communicator;
    }

    public void setMyself(Node myself) {
        this.myself = myself;
    }



    private CoorDtLogger dtLogger;
    private CoorBizStrategy<TransferMessage> bizStrategy;
    private CoorCommunicator<TransferMessage> communicator;
    private Node myself = new Node("localhost", 7878);
}
