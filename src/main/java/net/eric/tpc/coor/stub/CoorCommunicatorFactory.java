package net.eric.tpc.coor.stub;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.Either;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.CoorCommunicator;
import net.eric.tpc.proto.Node;

public class CoorCommunicatorFactory implements CommunicatorFactory<TransferMessage> {
    private ExecutorService commuTaskPool = Executors.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();
    private ProtocolCodecFactory codecFactory = new ObjectSerializationCodecFactory();
    
    @Override
    public Either<ActionResult, CoorCommunicator<TransferMessage>> getCommunicator(List<Node> peers) {
        CoorCommunicator<TransferMessage> communicator = //
                new MinaCommunicator(this.commuTaskPool, this.sequenceTaskPool, this.codecFactory); 
        ActionResult result = communicator.connectPanticipants(peers);
        if(result.isOK()){
            return Either.right(communicator);
        }
        return Either.left(result);
    }

    @Override
    public void releaseCommunicator(CoorCommunicator<TransferMessage> c) {
        c.close();
    }

    @Override
    public void close() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        this.commuTaskPool.shutdown();
        this.sequenceTaskPool.shutdown();
    }
    
}
