package net.eric.bank.bod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import net.eric.tpc.net.MinaChannel;
import net.eric.tpc.proto.DecisionQuerier;

public class PeerCommunicatorFactory {
    {
        if (!MinaChannel.isSharedConnectorInitialized()) {
            MinaChannel.initSharedConnector(new ObjectSerializationCodecFactory());
        }
    }

    private ExecutorService commuTaskPool = Executors.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();

    public DecisionQuerier getDecisionQuerier() {
        PeerCommunicator communicator = new PeerCommunicator(this.commuTaskPool, this.sequenceTaskPool);
        return communicator;
    }
    
    

    public ExecutorService getCommuTaskPool() {
        return commuTaskPool;
    }


    public void close() {
        this.sequenceTaskPool.submit(new Runnable() {
            @Override
            public void run() {
                PeerCommunicatorFactory.this.sequenceTaskPool.shutdown();
                PeerCommunicatorFactory.this.commuTaskPool.shutdown();
                MinaChannel.disposeConnector();
            }
        });
    }
}
