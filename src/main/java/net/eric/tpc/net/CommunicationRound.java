package net.eric.tpc.net;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.proto.CoorCommuResult;
import net.eric.tpc.proto.Node;

public class CommunicationRound {
    private static final Logger logger = LoggerFactory.getLogger(CommunicationRound.class);

    public static enum RoundType {
        DOUBLE_SIDE, SINGLE_SIDE
    };

    private int wantedCount;

    private RoundType roundType = RoundType.DOUBLE_SIDE;

    private PeerResult2.Assembler assembler;
    private ConcurrentMap<Node, PeerResult2> resultMap = new ConcurrentHashMap<Node, PeerResult2>();

    private CountDownHolder latch;
    private ExecutorService commuTaskPool;
    private ExecutorService sequenceTaskPool;

    public CommunicationRound(ExecutorService pool, ExecutorService sequenceTaskPool, int n, RoundType roundType,
            PeerResult2.Assembler assembler) {
        this.commuTaskPool = pool;
        this.wantedCount = n;
        this.roundType = roundType;
        this.assembler = assembler;
        this.sequenceTaskPool = sequenceTaskPool;
        this.latch = new CountDownHolder(n);
    }

    public CommunicationRound(ExecutorService pool, ExecutorService sequenceTaskPool, int n, RoundType roundType) {
        this(pool, sequenceTaskPool, n, roundType, PeerResult2.ONE_ITEM_ASSEMBLER);
    }

    public RoundType roundType() {
        return this.roundType;
    }

    public int wantedCount() {
        return this.wantedCount;
    }

    synchronized public Future<CoorCommuResult> getResult() {
        if (!this.isWithinRound()) {
            return Futures.immediateFuture(this.generateResult());
        }
        Callable<CoorCommuResult> task = new Callable<CoorCommuResult>() {
            public CoorCommuResult call() throws Exception {
                CommunicationRound.this.latch.await();
                return CommunicationRound.this.generateResult();
            }
        };
        return commuTaskPool.submit(task);
    }

    public void clearLatch() {
        this.latch.clear();
    }

    private CoorCommuResult generateResult() {
        final CoorCommuResult result = new CoorCommuResult(this.wantedCount, this.resultMap.values());
        if (logger.isDebugEnabled()) {
            List<PeerResult2> okResults = result.okResults();
            logger.debug(" CommuResult want " + result.wantedCount() + ", got ok result " + okResults.size());
            for (PeerResult2 pr : result.getResults()) {
                logger.debug(pr.toString());
            }
        }
        return result;
    }

    public boolean isWithinRound() {
        return !this.latch.isAllDone();
    }

    synchronized public void regFailure(Node node, String errorCode, String errorReason) {
        if (!this.isWithinRound() || this.isPeerResult2Done(node)) {
            return;
        }
        try {
            this.resultMap.put(node, PeerResult2.fail(node, errorCode, errorReason));
        } finally {
            this.latch.countDown(node);
        }
    }

    public void regMessage(Node node, Object message) {
        regMessage(node, Optional.of(message));
    }

    public void finishReceiving(Node node) {
        regMessage(node, Optional.absent());
    }

    private Optional<PeerResult2> tryAddMeesageforNewNode(Node node, Optional<Object> r) {
        PeerResult2 result = null;
        synchronized (this.resultMap) {
            if (!this.resultMap.containsKey(node)) {
                if (r.isPresent()) {
                    result = this.assembler.start(node, r.get());
                } else {
                    final Optional<PeerResult2> none = Optional.absent();
                    result = this.assembler.finish(node, none);
                }
                this.resultMap.put(node, result);
                return Optional.of(result);
            }
        }
        return Optional.absent();
    }

    private void regMessage(Node node, Optional<Object> r) {
        if (!this.isWithinRound() || this.isPeerResult2Done(node)) {
            return;
        }
        Optional<PeerResult2> opt = Optional.absent();
        try {
            // 是否是节点的第一次消息
            if (!this.resultMap.containsKey(node)) {
                opt = tryAddMeesageforNewNode(node, r);
            }
            // 为已有结果的节点增加消息
            if (!opt.isPresent()) {
                PeerResult2 result;
                if (r.isPresent()) { // 是有消息需要登记
                    final PeerResult2 tmp = PeerResult2.pending(node, r.get());
                    result = this.resultMap.merge(node, tmp, CommunicationRound.this.FOLD_FUNCTION);
                } else { // r is empty 表示本轮通讯结束的意思
                    final PeerResult2 tmp = PeerResult2.approve(node, new Object());
                    result = this.resultMap.merge(node, tmp, CommunicationRound.this.END_FUNCTION);
                }
                opt = Optional.of(result);
            }
        } finally {
            if (opt.isPresent() && opt.get().isDone()) {
                this.latch.countDown(node);
                if (logger.isDebugEnabled()) {
                    logger.debug("regMessage countDown 1 by " + node.toString());
                }
            }
        }
    }

    private boolean isPeerResult2Done(Node node) {
        if (this.resultMap.containsKey(node)) {
            PeerResult2 r = this.resultMap.get(node);
            return r.isDone();
        }
        return false;
    }

    private static class CountDownHolder {
        private CountDownLatch countDown;
        private Set<Node> callerSet;

        public CountDownHolder(int n) {
            countDown = new CountDownLatch(n);
            callerSet = new HashSet<Node>();
        }

        synchronized public void countDown(Node node) {
            if (!this.callerSet.contains(node)) {
                this.callerSet.add(node);
                this.countDown.countDown();
            }
        }

        public void await() throws InterruptedException {
            this.countDown.await();
        }

        public boolean isAllDone() {
            return this.countDown.getCount() <= 0;
        }

        public void clear() {
            while (this.countDown.getCount() > 0) {
                this.countDown.countDown();
            }
        }
    }

    public final BiFunction<PeerResult2, PeerResult2, PeerResult2> FOLD_FUNCTION = new BiFunction<PeerResult2, PeerResult2, PeerResult2>() {
        public PeerResult2 apply(PeerResult2 t, PeerResult2 u) {
            return CommunicationRound.this.assembler.fold(t.peer(), t, u.result());
        }
    };

    public final BiFunction<PeerResult2, PeerResult2, PeerResult2> END_FUNCTION = new BiFunction<PeerResult2, PeerResult2, PeerResult2>() {
        public PeerResult2 apply(PeerResult2 t, PeerResult2 u) {
            return CommunicationRound.this.assembler.finish(u.peer(), Optional.of(t));
        }
    };
}
