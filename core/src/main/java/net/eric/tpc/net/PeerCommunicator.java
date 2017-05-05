package net.eric.tpc.net;

import static net.eric.tpc.base.Pair.asPair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.MessageAssemblers.YesOrNoAssembler;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.DecisionQuerier;
import net.eric.tpc.proto.FutureTk;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;

public class PeerCommunicator implements DecisionQuerier, TaskPoolProvider {
    private static final Logger logger = LoggerFactory.getLogger(PeerCommunicator.class);

    @Inject
    private ChannelFactory channelFactory;
    private ExecutorService commuTaskPool = FutureTk.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();

    @Override
    public Optional<Decision> queryDecision(long xid, List<InetSocketAddress> peers) {
        Maybe<List<PeerChannel<Message>>> channels = channelFactory.getChannel(Message.class, peers, RoundType.WAIT_ONE);
        if (!channels.isRight())
            return Optional.absent();
        BasicCommunicator<Message> communicator = new BasicCommunicator<Message>(this, channels.getRight());

        List<Pair<InetSocketAddress, Message>> requests = Lists.transform(peers, //
                node -> asPair(node, new Message(xid, (short) 1, CommandCodes.DECISION_QUERY)));

        Future<RoundResult<Boolean>> future = communicator.communicate(requests, RoundType.WAIT_ALL, //
                new YesOrNoAssembler(xid, ErrorCode.WRONG_ANSWER));

        try {
            RoundResult<Boolean> result = future.get();
            if (result.okResultCount() > 0) {
                return result.okResults().get(0) ? Optional.of(Decision.COMMIT) : Optional.of(Decision.ABORT);
            }
            return Optional.absent();
        } catch (Exception e) {
            logger.error("Future RoundResult. get", e);
            return Optional.absent();
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

    @Override
    public void close() throws IOException {
        this.sequenceTaskPool.submit(() -> {
            PeerCommunicator.this.commuTaskPool.shutdown();
            PeerCommunicator.this.sequenceTaskPool.shutdown();
        });
    }

}
