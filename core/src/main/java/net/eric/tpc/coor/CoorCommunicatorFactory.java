package net.eric.tpc.coor;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.MinaChannel;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.CommunicatorFactory;

public class CoorCommunicatorFactory implements CommunicatorFactory {

    private ExecutorService commuTaskPool = Executors.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();

    {
        MinaChannel.initSharedConnector(new ObjectSerializationCodecFactory());
    }

    @Override
    public Maybe<Communicator> getCommunicator(List<InetSocketAddress> peers) {
        Communicator communicator = new CoorCommunicator(this.commuTaskPool,
                this.sequenceTaskPool);
        ActionStatus result = communicator.connectPanticipants(peers);
        return Maybe.might(result, communicator);
    }

    @Override
    public void releaseCommunicator(Communicator communicator) {
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
