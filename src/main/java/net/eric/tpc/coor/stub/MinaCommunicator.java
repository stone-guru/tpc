package net.eric.tpc.coor.stub;

import static net.eric.tpc.common.Pair.asPair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.eric.tpc.biz.BizErrorCode;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.Pair;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.PeerResult2;
import net.eric.tpc.proto.CoorCommuResult;
import net.eric.tpc.proto.CoorCommunicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.Node;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.VoteResult;

public class MinaCommunicator implements CoorCommunicator<TransferMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MinaCommunicator.class);

    public static final Function<Object, MinaChannel> OBJECT_AS_CHANNEL = new Function<Object, MinaChannel>() {
        public MinaChannel apply(Object obj) {
            return (MinaChannel) obj;
        }
    };

    public static final Function<Object, String> OBJECT_AS_STRING = new Function<Object, String>() {
        public String apply(Object obj) {
            return (String) obj;
        }
    };

    protected Map<Node, MinaChannel> channelMap = Collections.emptyMap();
    protected ExecutorService commuTaskpool = Executors.newCachedThreadPool();
    private ExecutorService sequenceTaskPool = Executors.newSingleThreadExecutor();
    protected AtomicReference<CommunicationRound> roundRef = new AtomicReference<CommunicationRound>(null);
    private ProtocolCodecFactory codecFactory;

    public MinaCommunicator(ProtocolCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    @Override
    public boolean connectPanticipants(List<Node> nodes) {
        Optional<Map<Node, MinaChannel>> opt = this.connectOrAbort(nodes);
        if (!opt.isPresent()) {
            System.out.println("Not all node connected");
            return false;
        }
        this.channelMap = opt.get();
        return true;
    }

    @Override
    public Future<CoorCommuResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, TransferMessage>> tasks) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Pair<Node, TransferMessage> p : tasks) {
            final DataPacket packet = new DataPacket(DataPacket.BEGIN_TRANS, transStartRec, p.snd());
            requests.add(asPair(p.fst(), (Object) packet));
        }
        return this.sendRequest(requests, MinaCommunicator.BEGIN_TRANS_ASSEMBLER);
    }

    private static final PeerResult2.Assembler BEGIN_TRANS_ASSEMBLER = new PeerResult2.OneItemAssembler() {
        @Override
        public PeerResult2 start(Node node, Object message) {
            final DataPacket packet = (DataPacket) message;
            if (DataPacket.BEGIN_TRANS_ANSWER.equals(packet.getCode())) {
                if (DataPacket.YES.equals(packet.getParam1())) {
                    return PeerResult2.approve(node, true);
                } else if (DataPacket.NO.equals(packet.getParam1())) {
                    return PeerResult2.refuse(node, BizErrorCode.REFUSE_TRANS, packet.getParam2().toString());
                }
            }
            return PeerResult2.fail(node, BizErrorCode.PEER_PRTC_ERROR, packet.toString());
        }
    };

    public Future<CoorCommuResult> gatherVote(String xid, List<Node> nodes) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Node node : nodes) {
            final DataPacket packet = new DataPacket(DataPacket.VOTE_REQ, xid, "");
            requests.add(asPair(node, (Object) packet));
        }
        return this.sendRequest(requests, MinaCommunicator.VOTE_REQ_ASSEMBLER);
    }

    private static final PeerResult2.Assembler VOTE_REQ_ASSEMBLER = new PeerResult2.OneItemAssembler() {
        @Override
        public PeerResult2 start(Node node, Object message) {
            final DataPacket packet = (DataPacket) message;
            if (DataPacket.VOTE_ANSWER.equals(packet.getCode())) {
                if (DataPacket.YES.equals(packet.getParam1())) {
                    return PeerResult2.approve(node, true);
                } else if (DataPacket.NO.equals(packet.getParam1())) {
                    return PeerResult2.refuse(node, BizErrorCode.REFUSE_COMMIT, packet.getParam2().toString());
                }
            }
            return PeerResult2.fail(node, BizErrorCode.PEER_PRTC_ERROR, packet.toString());
        }
    };
    
    public void notifyDecision(String xid, Decision decision, List<Node> nodes) {
    }

    public void shutdown() {
        Future<Void> t = this.closeChannels(this.channelMap.values());
        try {
            t.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.commuTaskpool.shutdown();
        this.sequenceTaskPool.shutdown();
    }

    private Future<Void> closeChannels(final Iterable<MinaChannel> channels) {
        Callable<Void> closeAction = new Callable<Void>() {
            private final CountDownLatch closeActionLatch = new CountDownLatch(Iterables.size(channels));

            @Override
            public Void call() throws Exception {
                if (logger.isDebugEnabled()) {
                    logger.debug("begin close connections");
                }
                for (final MinaChannel channel : channels) {
                    Runnable closeTask = new Runnable() {
                        public void run() {
                            try {
                                channel.close();
                            } catch (Exception e) {
                                logger.error("close channel error", e);
                            } finally {
                                closeActionLatch.countDown();
                            }
                        }
                    };
                    commuTaskpool.submit(closeTask);
                }
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("wait closeAction latch");
                    }
                    closeActionLatch.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("Wait close connections", e);
                }
                return null;
            }
        };
        return this.sequenceTaskPool.submit(closeAction);
    }

    public Future<CoorCommuResult> sendRequest(List<Pair<Node, Object>> requests) {
        return this.sendRequest(requests, PeerResult2.ONE_ITEM_ASSEMBLER);
    }

    public Future<CoorCommuResult> sendRequest(List<Pair<Node, Object>> requests, PeerResult2.Assembler assembler) {
        return this.communicate(requests, RoundType.DOUBLE_SIDE, assembler);
    }

    private Future<CoorCommuResult> sendMessage(List<Pair<Node, Object>> messages) {
        return this.communicate(messages, RoundType.SINGLE_SIDE, PeerResult2.ONE_ITEM_ASSEMBLER);
    }

    private Future<CoorCommuResult> communicate(final List<Pair<Node, Object>> messages, final RoundType roundType,
            PeerResult2.Assembler assembler) {
        for (final Pair<Node, Object> p : messages) {
            if (!this.channelMap.containsKey(p.fst())) {
                throw new IllegalStateException("Given node not connected first " + p.fst());
            }
        }
        final CommunicationRound round = this.newRound(messages.size(), roundType, assembler);

        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                roundRef.set(round);
                for (final Pair<Node, Object> p : messages) {
                    final MinaChannel channel = channelMap.get(p.fst());
                    Runnable task = new Runnable() {
                        public void run() {
                            System.out.println("before call channel send");
                            final boolean sendOnly = roundType == RoundType.SINGLE_SIDE;
                            channel.sendMessage(p.snd(), sendOnly);
                            System.out.println("after call channel send");
                        }
                    };
                    commuTaskpool.submit(task);
                }
            }
        };
        this.sequenceTaskPool.submit(startAction);
        return round.getResult();
    }

    private Future<CoorCommuResult> connectAll(final List<Node> nodes) {
        final CommunicationRound round = this.newRound(nodes.size(), RoundType.SINGLE_SIDE,
                PeerResult2.ONE_ITEM_ASSEMBLER);
        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                roundRef.set(round);
                for (final Node node : nodes) {
                    Runnable task = new Runnable() {
                        public void run() {
                            MinaChannel channel = new MinaChannel(node, MinaCommunicator.this.codecFactory,
                                    MinaCommunicator.this.roundRef);
                            try {
                                channel.connect();
                                round.regMessage(node, channel);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("connec " + node + " finish reg");
                                }
                            } catch (Exception e) {
                                round.regFailure(node, "EXCEPTION", e.getMessage());
                            }
                        }
                    };
                    commuTaskpool.submit(task);
                }
            }
        };

        this.sequenceTaskPool.submit(startAction);
        return round.getResult();
    }

    private CommunicationRound newRound(int peerCount, RoundType roundType, PeerResult2.Assembler assembler) {
        CommunicationRound round = new CommunicationRound(this.commuTaskpool, this.sequenceTaskPool, peerCount,
                roundType, assembler);
        return round;
    }

    public void closeConnections() {
        closeChannels(this.channelMap.values());
    }

    private Optional<Map<Node, MinaChannel>> connectOrAbort(List<Node> nodes) {
        Future<CoorCommuResult> future = connectAll(nodes);
        CoorCommuResult result;
        try {
            result = future.get();
            // this.displayConnectResult(result);
            System.out.println("Got async connect Future");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Optional.absent();
        } catch (ExecutionException e) {
            e.printStackTrace();
            return Optional.absent();
        }
        if (result.isAllOK()) {
            Map<Node, MinaChannel> map = new HashMap<Node, MinaChannel>();
            for (PeerResult2 r : result.getResults()) {
                map.put(r.peer(), (MinaChannel) r.result());
            }
            return Optional.of(map);
        }

        closeChannels(result.okResultAs(MinaCommunicator.OBJECT_AS_CHANNEL));
        return Optional.absent();
    }
}
