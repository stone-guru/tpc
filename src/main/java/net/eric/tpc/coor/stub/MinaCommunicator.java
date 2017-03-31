package net.eric.tpc.coor.stub;

import static net.eric.tpc.base.Pair.asPair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.biz.BizCode;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.net.CommunicationRound;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.DataPacket;
import net.eric.tpc.net.PeerResult;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.TransStartRec;

public class MinaCommunicator implements Communicator<TransferBill> {

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
    protected ExecutorService commuTaskPool;
    private ExecutorService sequenceTaskPool;
    protected AtomicReference<CommunicationRound> roundRef = new AtomicReference<CommunicationRound>(null);

    public MinaCommunicator(ProtocolCodecFactory codecFactory) {
        this(Executors.newCachedThreadPool(), Executors.newSingleThreadExecutor());
    }

    public MinaCommunicator(ExecutorService commuTaskPool, ExecutorService sequenceTaskPool) {
        this.commuTaskPool = commuTaskPool;
        this.sequenceTaskPool = sequenceTaskPool;
    }

    @Override
    public ActionStatus connectPanticipants(List<Node> nodes) {
        Either<ActionStatus, Map<Node, MinaChannel>> either = this.connectOrAbort(nodes);
        if (either.isRight()) {
            this.channelMap = either.getRight();
            return ActionStatus.OK;
        }
        return either.getLeft();
    }

    @Override
    public Future<RoundResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, TransferBill>> tasks) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Pair<Node, TransferBill> p : tasks) {
            final DataPacket packet = new DataPacket(DataPacket.BEGIN_TRANS, transStartRec, p.snd());
            requests.add(asPair(p.fst(), (Object) packet));
        }
        return this.sendRequest(requests, MinaCommunicator.BEGIN_TRANS_ASSEMBLER);
    }

    private static final PeerResult.Assembler BEGIN_TRANS_ASSEMBLER = new PeerResult.OneItemAssembler() {
        @Override
        public PeerResult start(Node node, Object message) {
            final DataPacket packet = (DataPacket) message;
            if (DataPacket.BEGIN_TRANS_ANSWER.equals(packet.getCode())) {
                if (DataPacket.YES.equals(packet.getParam1())) {
                    return PeerResult.approve(node, true);
                } else if (DataPacket.NO.equals(packet.getParam1())) {
                    return PeerResult.refuse(node, BizCode.REFUSE_TRANS, packet.getParam2().toString());
                }
            }
            return PeerResult.fail(node, BizCode.PEER_PRTC_ERROR, packet.toString());
        }
    };

    public Future<RoundResult> gatherVote(String xid, List<Node> nodes) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();
        for (Node node : nodes) {
            final DataPacket packet = new DataPacket(DataPacket.VOTE_REQ, xid, "");
            requests.add(asPair(node, (Object) packet));
        }
        return this.sendRequest(requests, MinaCommunicator.VOTE_REQ_ASSEMBLER);
    }

    private static final PeerResult.Assembler VOTE_REQ_ASSEMBLER = new PeerResult.OneItemAssembler() {
        @Override
        public PeerResult start(Node node, Object message) {
            final DataPacket packet = (DataPacket) message;
            if (DataPacket.VOTE_ANSWER.equals(packet.getCode())) {
                if (DataPacket.YES.equals(packet.getParam1())) {
                    return PeerResult.approve(node, true);
                } else if (DataPacket.NO.equals(packet.getParam1())) {
                    return PeerResult.refuse(node, BizCode.REFUSE_COMMIT, packet.getParam2().toString());
                }
            }
            return PeerResult.fail(node, BizCode.PEER_PRTC_ERROR, packet.toString());
        }
    };

    public void notifyDecision(String xid, Decision decision, List<Node> nodes) {
        List<Pair<Node, Object>> requests = Lists.newArrayList();

        for (Node node : nodes) {
            String code = null;
            if (decision == Decision.COMMIT) {
                code = DataPacket.YES;
            } else if (decision == Decision.ABORT) {
                code = DataPacket.NO;
            } else {
                throw new UnImplementedException();
            }

            final DataPacket packet = new DataPacket(DataPacket.TRANS_DECISION, xid, code);
            requests.add(asPair(node, (Object) packet));
        }

        @SuppressWarnings("unused")
        Future<RoundResult> dontCare = this.sendMessage(requests);
    }

    public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("start to close connections");
        }
        @SuppressWarnings("unused")
        Future<Void> canNotCare = this.closeChannels(this.channelMap.values());
    }

    private Future<Void> closeChannels(final Iterable<MinaChannel> channels) {
        Callable<Void> closeAction = new Callable<Void>() {
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
                            }
                        }
                    };
                    commuTaskPool.submit(closeTask);
                    if (logger.isDebugEnabled()) {
                        logger.debug("closeTask submited");
                    }
                }
                return null;
            }
        };
        return this.sequenceTaskPool.submit(closeAction);
    }

    public Future<RoundResult> sendRequest(List<Pair<Node, Object>> requests) {
        return this.sendRequest(requests, PeerResult.ONE_ITEM_ASSEMBLER);
    }

    public Future<RoundResult> sendRequest(List<Pair<Node, Object>> requests, PeerResult.Assembler assembler) {
        return this.communicate(requests, RoundType.DOUBLE_SIDE, assembler);
    }

    private Future<RoundResult> sendMessage(List<Pair<Node, Object>> messages) {
        return this.communicate(messages, RoundType.SINGLE_SIDE, PeerResult.ONE_ITEM_ASSEMBLER);
    }

    private Future<RoundResult> communicate(final List<Pair<Node, Object>> messages, final RoundType roundType,
            PeerResult.Assembler assembler) {
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
                            final boolean sendOnly = roundType == RoundType.SINGLE_SIDE;
                            channel.sendMessage(p.snd(), sendOnly);
                        }
                    };
                    commuTaskPool.submit(task);
                }
            }
        };
        this.sequenceTaskPool.submit(startAction);
        return round.getResult();
    }

    private Future<RoundResult> connectAll(final List<Node> nodes) {
        final CommunicationRound round = this.newRound(nodes.size(), RoundType.SINGLE_SIDE,
                PeerResult.ONE_ITEM_ASSEMBLER);
        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                roundRef.set(round);
                for (final Node node : nodes) {
                    Runnable task = new Runnable() {
                        public void run() {
                            MinaChannel channel = new MinaChannel(node, MinaCommunicator.this.roundRef);
                            boolean connected = false;
                            try {
                                connected = channel.connect();
                            } catch (Exception e) {
                                logger.error("connect " + node.toString(), e);
                            }
                            if (connected) {
                                round.regMessage(node, channel);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("connec " + node + " finish reg");
                                }
                            } else {
                                round.regFailure(node, "NOT_CONNECTED", node.toString());
                            }
                        }
                    };
                    commuTaskPool.submit(task);
                }
            }
        };

        this.sequenceTaskPool.submit(startAction);
        return round.getResult();
    }

    private CommunicationRound newRound(int peerCount, RoundType roundType, PeerResult.Assembler assembler) {
        CommunicationRound round = new CommunicationRound(this.commuTaskPool, this.sequenceTaskPool, peerCount,
                roundType, assembler);
        return round;
    }

    public void closeConnections() {
        closeChannels(this.channelMap.values());
    }

    private Either<ActionStatus, Map<Node, MinaChannel>> connectOrAbort(List<Node> nodes) {
        Future<RoundResult> future = connectAll(nodes);
        ActionStatus ar = RoundResult.force(future);
        if (!future.isDone()) {
            // Can do nothing
            return Either.left(ar);
        }

        RoundResult result = null;
        try {
            result = future.get();
        } catch (Exception e) {
            throw new ShouldNotHappenException();
        }

        if (!result.isAllOK()) {
            closeChannels(result.okResultAs(MinaCommunicator.OBJECT_AS_CHANNEL));
            return Either.left(ar);
        }

        Map<Node, MinaChannel> map = new HashMap<Node, MinaChannel>();
        for (PeerResult r : result.getResults()) {
            map.put(r.peer(), (MinaChannel) r.result());
        }
        return Either.right(map);
    }

}
