package net.eric.tpc.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Either;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.coor.stub.CoorCommunicator;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.CommunicationRound.WaitType;
import net.eric.tpc.proto.RoundResult;

public class MinaCommunicator {

    private static final Logger logger = LoggerFactory.getLogger(CoorCommunicator.class);
    protected Map<Node, MinaChannel> channelMap = Collections.emptyMap();
    protected ExecutorService commuTaskPool;
    protected ExecutorService sequenceTaskPool;
    protected AtomicReference<CommunicationRound> roundRef = new AtomicReference<CommunicationRound>(null);

    private List<Function<List<Pair<Node, Object>>, List<Pair<Node, Object>>>> beforeSendHooks = Lists.newArrayList();

    public static final Function<Object, String> OBJECT_AS_STRING = new Function<Object, String>() {
        public String apply(Object obj) {
            return (String) obj;
        }
    };

    public static final Function<Object, MinaChannel> OBJECT_AS_CHANNEL = new Function<Object, MinaChannel>() {
        public MinaChannel apply(Object obj) {
            return (MinaChannel) obj;
        }
    };

    public MinaCommunicator(ExecutorService commuTaskPool, ExecutorService sequenceTaskPool) {
        this.commuTaskPool = commuTaskPool;
        this.sequenceTaskPool = sequenceTaskPool;
    }

    /**
     * 在进行实际发送前，留给子类的回调方法，可以进行一些个特定的处理。
     * </p>
     * 如果加入了多个hook，这些hook将按加入先后顺序依次调用，前一个的输出作为下一个的输入，最后结果用来进行实际发送。
     * </p>
     * 
     * @param hook Function 输入为将要发送的请求，输出的结果就实际用来发送
     */
    public void addBeforeSendHook(Function<List<Pair<Node, Object>>, List<Pair<Node, Object>>> hook) {
        Preconditions.checkNotNull(hook);
        this.beforeSendHooks.add(hook);
    }

    public ActionStatus connectPeers(List<Node> nodes, WaitType waitType) {
        Either<ActionStatus, Map<Node, MinaChannel>> either = this.connectOrAbort(nodes, waitType);
        if (either.isRight()) {
            this.channelMap = either.getRight();
            return ActionStatus.OK;
        }
        return either.getLeft();
    }

    public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("start to close connections");
        }
        @SuppressWarnings("unused")
        Future<Void> canNotCare = this.closeChannels(this.channelMap.values());
    }

    public Future<Void> closeChannels(final Iterable<MinaChannel> channels) {
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

    public Future<RoundResult> sendRequest(List<Pair<Node, Object>> requests, PeerResult.Assembler assembler) {
        return this.communicate(requests, RoundType.DOUBLE_SIDE, WaitType.WAIT_ALL, assembler);
    }

    public Future<RoundResult> sendMessage(List<Pair<Node, Object>> messages) {
        return this.communicate(messages, RoundType.SINGLE_SIDE, WaitType.WAIT_ALL, PeerResult.ONE_ITEM_ASSEMBLER);
    }

    public Future<RoundResult> communicate(List<Pair<Node, Object>> messages, final RoundType roundType,
            final WaitType waitType, PeerResult.Assembler assembler) {
        if (waitType == WaitType.WAIT_ALL) {
            for (final Pair<Node, Object> p : messages) {
                // 需要的节点是有没链接成功的
                if (!this.channelMap.containsKey(p.fst())) {
                    throw new IllegalStateException("Given node not connected first " + p.fst());
                }
            }
        }
        final CommunicationRound round = this.newRound(messages.size(), roundType, waitType, assembler);
        final List<Pair<Node, Object>> request = callBeforeSendHooks(messages);

        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                roundRef.set(round);
                for (final Pair<Node, Object> p : request) {
                    final MinaChannel channel = channelMap.get(p.fst());
                    if (channel == null) {
                        final Node node = p.fst();
                        round.regFailure(node, "NOT_CONNECTED", node.toString());
                    } else {
                        Runnable task = new Runnable() {
                            public void run() {
                                final boolean sendOnly = roundType == RoundType.SINGLE_SIDE;
                                channel.sendMessage(p.snd(), sendOnly);
                            }
                        };
                        commuTaskPool.submit(task);
                    }
                }
            }
        };

        this.sequenceTaskPool.submit(startAction);
        return round.getResult();
    }

    public void closeConnections() {
        closeChannels(this.channelMap.values());
    }

    private List<Pair<Node, Object>> callBeforeSendHooks(List<Pair<Node, Object>> messages) {
        List<Pair<Node, Object>> callResult = messages;
        for (Function<List<Pair<Node, Object>>, List<Pair<Node, Object>>> f : this.beforeSendHooks) {
            callResult = f.apply(callResult);
            if (callResult == null) {
                throw new NullPointerException("result of beforeSendHook");
            }
        }
        return callResult;
    }

    private CommunicationRound newRound(int peerCount, RoundType roundType, WaitType waitType,
            PeerResult.Assembler assembler) {
        CommunicationRound round = new CommunicationRound(this.commuTaskPool, this.sequenceTaskPool, peerCount,
                roundType, waitType, assembler);
        return round;
    }

    private Maybe<Map<Node, MinaChannel>> connectOrAbort(List<Node> nodes, WaitType waitType) {
        Future<RoundResult> future = connectThese(nodes);
        ActionStatus ar = RoundResult.force(future);
        if (!future.isDone()) {
            // Can do nothing
            return Maybe.fail(ar);
        }

        RoundResult result = null;
        try {
            result = future.get();
        } catch (Exception e) {
            throw new ShouldNotHappenException();
        }

        // 一个都没连上
        if (result.okResultCount() == 0) {
            return Maybe.fail(result.getAnError());
        }
        // 需要全都链接的不是都连上了
        if (!result.isAllOK() && (waitType == WaitType.WAIT_ALL)) {
            closeChannels(result.okResultAs(MinaCommunicator.OBJECT_AS_CHANNEL));
            return Maybe.fail(result.getAnError());
        }

        Map<Node, MinaChannel> map = new HashMap<Node, MinaChannel>();
        for (PeerResult r : result.getResults()) {
            if (r.isRight()) {
                map.put(r.peer(), (MinaChannel) r.result());
            }
        }
        return Maybe.success(map);
    }

    private Future<RoundResult> connectThese(final List<Node> nodes) {
        final CommunicationRound round = this.newRound(nodes.size(), RoundType.SINGLE_SIDE, WaitType.WAIT_ALL,
                PeerResult.ONE_ITEM_ASSEMBLER);
        Runnable startAction = new Runnable() {
            @Override
            public void run() {
                roundRef.set(round);
                for (final Node node : nodes) {
                    Runnable task = new Runnable() {
                        public void run() {
                            MinaChannel channel = MinaChannel.newInstance(node, MinaCommunicator.this.roundRef);
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

}