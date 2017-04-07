package net.eric.tpc.net;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.proto.RoundResult;

public class CommunicationRound {
    private static final Logger logger = LoggerFactory.getLogger(CommunicationRound.class);

    public static enum RoundType {
        DOUBLE_SIDE, SINGLE_SIDE
    };

    public static enum WaitType {
        WAIT_ALL, WAIT_ONE
    }
    
    private int wantedCount;

    private RoundType roundType = RoundType.DOUBLE_SIDE;
    private WaitType waitType = WaitType.WAIT_ALL;
    
    private PeerResult.Assembler assembler;
    private ConcurrentMap<InetSocketAddress, PeerResult> resultMap = new ConcurrentHashMap<InetSocketAddress, PeerResult>();

    private CountDownHolder latch;
    private ExecutorService commuTaskPool;

    public CommunicationRound(ExecutorService pool, ExecutorService sequenceTaskPool, int n, RoundType roundType, WaitType waitType,
            PeerResult.Assembler assembler) {
        this.commuTaskPool = pool;
        this.wantedCount = n;
        this.roundType = roundType;
        this.waitType = waitType;
        this.assembler = assembler;
        
        this.latch = new CountDownHolder(n);
    }

    public CommunicationRound(ExecutorService pool, ExecutorService sequenceTaskPool, int n, RoundType roundType) {
        this(pool, sequenceTaskPool, n, roundType, WaitType.WAIT_ALL, PeerResult.ONE_ITEM_ASSEMBLER);
    }

    public RoundType roundType() {
        return this.roundType;
    }

    public int wantedCount() {
        return this.wantedCount;
    }

    synchronized public Future<RoundResult> getResult() {
        if (!this.isWithinRound()) {
            return Futures.immediateFuture(this.generateResult());
        }
        Callable<RoundResult> task = new Callable<RoundResult>() {
            public RoundResult call() throws Exception {
                CommunicationRound.this.latch.await();
                return CommunicationRound.this.generateResult();
            }
        };
        return commuTaskPool.submit(task);
    }

    public void clearLatch() {
        this.latch.clear();
    }

    private RoundResult generateResult() {
        final RoundResult result = new RoundResult(this.wantedCount, this.resultMap.values());
        if (logger.isInfoEnabled()){
            List<PeerResult> okResults = result.okResults();
            logger.info(" CommuResult want " + result.wantedCount() + ", got ok result " + okResults.size());
            for (PeerResult pr : result.getResults()) {
                logger.info(pr.toString());
            }
        }
        return result;
    }

    public boolean isWithinRound() {
        return !this.latch.isAllDone();
    }

    synchronized public void regFailure(InetSocketAddress node, short errorCode, String errorReason) {
        if (!this.isWithinRound() || this.isPeerResult2Done(node)) {
            return;
        }
        try {
            this.resultMap.put(node, PeerResult.fail(node, errorCode, errorReason));
        } finally {
            this.latch.countDown(node);
        }
    }

    public void regMessage(InetSocketAddress node, Object message) {
        regMessage(node, Optional.of(message));
    }

    public void finishReceiving(InetSocketAddress node) {
        regMessage(node, Optional.absent());
    }

    private Optional<PeerResult> tryAddMeesageforNewNode(InetSocketAddress node, Optional<Object> r) {
        PeerResult result = null;
        synchronized (this.resultMap) {
            if (!this.resultMap.containsKey(node)) {
                if (r.isPresent()) {
                    result = this.assembler.start(node, r.get());
                } else {
                    final Optional<PeerResult> none = Optional.absent();
                    result = this.assembler.finish(node, none);
                }
                this.resultMap.put(node, result);
                return Optional.of(result);
            }
        }
        return Optional.absent();
    }

    private void regMessage(InetSocketAddress node, Optional<Object> r) {
        if (!this.isWithinRound() || this.isPeerResult2Done(node)) {
            return;
        }
        Optional<PeerResult> opt = Optional.absent();
        try {
            // 是否是节点的第一次消息
            if (!this.resultMap.containsKey(node)) {
                opt = tryAddMeesageforNewNode(node, r);
            }
            // 为已有结果的节点增加消息
            if (!opt.isPresent()) {
                PeerResult result;
                if (r.isPresent()) { // 是有消息需要登记
                    final PeerResult tmp = PeerResult.pending(node, r.get());
                    result = this.resultMap.merge(node, tmp, CommunicationRound.this.FOLD_FUNCTION);
                } else { // r is empty 表示本轮通讯结束的意思
                    final PeerResult tmp = PeerResult.approve(node, new Object());
                    result = this.resultMap.merge(node, tmp, CommunicationRound.this.END_FUNCTION);
                }
                opt = Optional.of(result);
            }
        } finally {
            if(!opt.isPresent()){
                return; //消息未记录
            }
            if (opt.get().isDone()) {
                this.latch.countDown(node);
                if (logger.isDebugEnabled()) {
                    logger.debug("regMessage countDown 1 by " + node.toString());
                }
            }
            //成功获取了一条请求，本轮只需要一条记录，清除阻塞标记
            if(this.waitType == WaitType.WAIT_ONE && opt.get().isRight()){
                this.latch.clear();
            }
        }
    }

    private boolean isPeerResult2Done(InetSocketAddress node) {
        if (this.resultMap.containsKey(node)) {
            PeerResult r = this.resultMap.get(node);
            return r.isDone();
        }
        return false;
    }

    private static class CountDownHolder {
        private CountDownLatch countDown;
        private Set<InetSocketAddress> callerSet;

        public CountDownHolder(int n) {
            countDown = new CountDownLatch(n);
            callerSet = new HashSet<InetSocketAddress>();
        }

        synchronized public void countDown(InetSocketAddress node) {
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

    public final BiFunction<PeerResult, PeerResult, PeerResult> FOLD_FUNCTION = new BiFunction<PeerResult, PeerResult, PeerResult>() {
        public PeerResult apply(PeerResult t, PeerResult u) {
            return CommunicationRound.this.assembler.fold(t.peer(), t, u.result());
        }
    };

    public final BiFunction<PeerResult, PeerResult, PeerResult> END_FUNCTION = new BiFunction<PeerResult, PeerResult, PeerResult>() {
        public PeerResult apply(PeerResult t, PeerResult u) {
            return CommunicationRound.this.assembler.finish(u.peer(), Optional.of(t));
        }
    };
}
