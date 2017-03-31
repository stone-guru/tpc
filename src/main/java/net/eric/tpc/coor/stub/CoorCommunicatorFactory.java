package net.eric.tpc.coor.stub;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;
import net.eric.tpc.base.Node;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.Communicator;

public class CoorCommunicatorFactory implements CommunicatorFactory<TransferBill> {

    private ExecutorService commuTaskPool = Executors.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();

    {
        MinaChannel.initSharedConnector(new ObjectSerializationCodecFactory());
    }

    @Override
    public Either<ActionStatus, Communicator<TransferBill>> getCommunicator(List<Node> peers) {
        Communicator<TransferBill> communicator = new MinaCommunicator(this.commuTaskPool,
                this.sequenceTaskPool);
        ActionStatus result = communicator.connectPanticipants(peers);
        if (result.isOK()) {
            return Either.right(communicator);
        }
        return Either.left(result);
    }

    @Override
    public void releaseCommunicator(Communicator<TransferBill> communicator) {
        communicator.close();
    }

    @Override
    public void close() {
        this.sequenceTaskPool.submit(new Runnable() {
            @Override
            public void run() {
                CoorCommunicatorFactory.this.sequenceTaskPool.shutdown();
                CoorCommunicatorFactory.this.commuTaskPool.shutdown();
                MinaChannel.disposeConnector();
            }
        });
    }
}
