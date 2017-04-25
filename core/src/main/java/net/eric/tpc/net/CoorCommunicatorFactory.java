package net.eric.tpc.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.net.mina.MinaChannelFactory;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.FutureTk;

public class CoorCommunicatorFactory implements CommunicatorFactory, TaskPoolProvider {
    private static final Logger logger = LoggerFactory.getLogger(CoorCommunicatorFactory.class);

    private ChannelFactory channelFactory;
    private ExecutorService commuTaskPool = FutureTk.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();

    public CoorCommunicatorFactory() {
        this(Collections.emptyList());
    }

    @Inject
    public CoorCommunicatorFactory(@Named("Extra Object Codecs") List<ObjectCodec> objectCodecs) {
        channelFactory = new MinaChannelFactory(objectCodecs);
        logger.debug("CoorCommunicatorFactory created " + this.toString());
    }

    @Override
    public Maybe<Communicator> getCommunicator(List<InetSocketAddress> peers) {
        Maybe<List<PeerChannel<Message>>> maybe = channelFactory.getChannel(Message.class, peers, RoundType.WAIT_ALL);
        if (!maybe.isRight()) {
            return maybe.castLeft();
        }
        return Maybe.success(new CoorCommunicator(this, maybe.getRight()));
    }

    @Override
    public void releaseCommunicator(Communicator communicator) {
        BasicCommunicator<?> c = (BasicCommunicator<?>) communicator;
        channelFactory.releaseChannel(c.getChannels());
    }

    @Override
    public void close() throws IOException {
        this.sequenceTaskPool.submit(() -> {
            CoorCommunicatorFactory.this.commuTaskPool.shutdown();
            CoorCommunicatorFactory.this.sequenceTaskPool.shutdown();
        });

        try {
            this.channelFactory.close();
        } catch (IOException e) {
            logger.error("close channelFactory", e);
        }
    }

    @Override
    public ExecutorService getSequenceTaskPool() {
        return this.sequenceTaskPool;
    }

    @Override
    public ExecutorService getCommuTaskPool() {
        return this.commuTaskPool;
    }
}
