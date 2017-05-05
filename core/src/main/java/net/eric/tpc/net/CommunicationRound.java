package net.eric.tpc.net;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.ShouldNotHappenException;
import net.eric.tpc.proto.RoundResult;

public class CommunicationRound<R> implements RoundResult<R> {
    private static final Logger logger = LoggerFactory.getLogger(CommunicationRound.class);

    public static enum RoundType {
        WAIT_ALL, WAIT_ONE
    }

    private int wantedCount;
    private RoundType waitType = RoundType.WAIT_ALL;

    private ConcurrentHashMap<InetSocketAddress, Optional<Maybe<R>>> resultMap = //
            new ConcurrentHashMap<InetSocketAddress, Optional<Maybe<R>>>();
    private CountDownHolder latch;
    private Function<Object, Maybe<R>> assembler;

    private static LatchCleaner cleaner = new LatchCleaner(1500);

    public CommunicationRound(List<InetSocketAddress> peers, RoundType waitType) {
        this(peers, waitType, o -> {
            throw new IllegalStateException("This round is not for message receiving");
        });
    }

    public CommunicationRound(List<InetSocketAddress> peers, RoundType waitType, Function<Object, Maybe<R>> assembler) {
        this.wantedCount = peers.size();
        this.waitType = waitType;
        this.assembler = assembler;
        this.latch = new CountDownHolder(peers.size());

        resultMap = new ConcurrentHashMap<InetSocketAddress, Optional<Maybe<R>>>();
        for (InetSocketAddress peer : peers) {
            Object prev = resultMap.putIfAbsent(peer, Optional.absent());
            if (prev != null) {
                throw new IllegalArgumentException("Duplicated peer in given peers");
            }
        }
    }

    public int wantedCount() {
        return this.wantedCount;
    }

    public Future<RoundResult<R>> getResult(ExecutorService pool, long delay) {
        if (!this.isWithinRound()) {
            return Futures.immediateFuture(this);
        }
        Callable<RoundResult<R>> task = new Callable<RoundResult<R>>() {
            public RoundResult<R> call() throws Exception {
                CommunicationRound.this.latch.setWaitMillis(delay);
                CommunicationRound.cleaner.addLatch(CommunicationRound.this.latch);
                try {
                    CommunicationRound.this.latch.await();
                } finally {
                    CommunicationRound.cleaner.removeLatch(CommunicationRound.this.latch);
                }
                return CommunicationRound.this;
            }
        };

        return pool.submit(task);
    }

    public Future<RoundResult<R>> getResult(ExecutorService pool) {
        return this.getResult(pool, 2000);
    }

    public void clearLatch() {
        this.latch.clear();
    }

    public boolean isWithinRound() {
        return !this.latch.isAllDone();
    }

    public void regMessage(InetSocketAddress node, Object message) {
        this.regResult(node, this.assembler.apply(message));
    }

    public void regResult(InetSocketAddress node, Maybe<R> result) {
        if (logger.isDebugEnabled()) {
            logger.debug("regResult " + node.toString() + ", " + result.toString());
        }
        if (!this.isWithinRound()) {
            if (logger.isDebugEnabled()) {
                logger.debug("regResult not within round, abandom result");
            }
            return;
        }

        if (!this.resultMap.containsKey(node)) {
            throw new ShouldNotHappenException(node + "is not communication target");
        }

        Optional<Maybe<R>> inserted = null;
        Optional<Maybe<R>> myValue = Optional.of(result);
        try {
            inserted = resultMap.merge(node, myValue, (p, v) -> p.isPresent() ? p : v);
        } finally {
            // prev 代表了是从原来的空 到 现在的有，否则就是被放弃的结果了
            if (inserted != myValue) {
                if (logger.isDebugEnabled()) {
                    logger.debug("result for " + node + "has registered, abandom later");
                }
                return;
            }

            // result is ok or not, all cause countDown 1
            this.latch.countDown(node);
            if (logger.isDebugEnabled()) {
                logger.debug("regMessage countDown 1 by " + node.toString());
            }

            // 成功获取了一条请求，本轮只需要一条记录，清除阻塞标记
            if (this.waitType == RoundType.WAIT_ONE && result.isRight()) {
                this.latch.clear();
            }
        }
    }

    @Override
    public boolean isAllDone() {
        return resultMap.searchValues(1000, r -> !r.isPresent() ? r : null) == null;
    }

    @Override
    public boolean isAllOK() {
        return resultMap.searchValues(1000, //
                r -> (!r.isPresent() || !r.get().isRight()) ? r : null) == null;
    }

    @Override
    public int okResultCount() {
        return resultMap.reduceValuesToInt(10000, //
                r -> r.isPresent() && r.get().isRight() ? 1 : 0, 0, Integer::sum);
    }

    @Override
    public ActionStatus getAnError() {
        ActionStatus err = resultMap.searchValues(1000, //
                r -> r.isPresent() && !r.get().isRight() ? r.get().getLeft() : null);
        if (err != null) {
            return err;
        }
        return ActionStatus.innerError("No any error");
    }

    @Override
    public List<InetSocketAddress> getSuccessNodes() {
        List<InetSocketAddress> list = new ArrayList<InetSocketAddress>();
        for (InetSocketAddress key : resultMap.keySet()) {
            Optional<Maybe<R>> opt = resultMap.get(key);
            if (opt.isPresent() && opt.get().isRight()) {
                list.add(key);
            }
        }
        return list;
    }

    @Override
    public List<R> okResults() {
        List<R> list = new ArrayList<R>();
        for (Optional<Maybe<R>> opt : resultMap.values()) {
            if (opt.isPresent() && opt.get().isRight()) {
                list.add(opt.get().getRight());
            }
        }
        return list;
    }

    private static class LatchCleaner {
        private Timer timer;
        private ArrayList<CountDownHolder> holders = new ArrayList<CountDownHolder>();
        private long lastProcessTime;

        public LatchCleaner(long interval) {
            timer = new Timer(LatchCleaner.class.getCanonicalName(), true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (logger.isDebugEnabled()) {
                        logger.debug("LatchCleaner timer process");
                    }
                    processHolders();
                }
            }, 100, interval);
            this.lastProcessTime = System.currentTimeMillis();

            NightWatch.regCloseAction("LatchCleaner", () -> timer.cancel());
        }

        private void processHolders() {
            synchronized (holders) {
                long currentTime = System.currentTimeMillis();
                long interval = currentTime - lastProcessTime;
                this.lastProcessTime = currentTime;
                ListIterator<CountDownHolder> it = holders.listIterator();
                while (it.hasNext()) {
                    CountDownHolder holder = it.next();
                    boolean keep = holder.decWaitMillis(interval);
                    if (!keep) {
                        it.remove();
                        if (logger.isDebugEnabled()) {
                            logger.debug("an item expired, removed");
                        }
                    }
                }
            }
        }

        public void addLatch(CountDownHolder holder) {
            synchronized (holders) {
                holders.add(holder);
            }
        }

        public void removeLatch(CountDownHolder holder) {
            synchronized (holders) {
                holders.remove(holder);
            }
        }
    }

    private static class CountDownHolder {
        private CountDownLatch countDown;
        private Set<InetSocketAddress> callerSet;
        private AtomicLong waitMillis;

        public CountDownHolder(int n) {
            countDown = new CountDownLatch(n);
            callerSet = new HashSet<InetSocketAddress>();
            waitMillis = new AtomicLong(0);
        }

        public void countDown(InetSocketAddress node) {
            synchronized (callerSet) {
                if (!this.callerSet.contains(node)) {
                    this.callerSet.add(node);
                    this.countDown.countDown();
                }
            }
        }

        public void await() throws InterruptedException {
            this.countDown.await();
        }

        public boolean isAllDone() {
            return this.countDown.getCount() == 0;
        }

        public void clear() {
            while (this.countDown.getCount() > 0) {
                this.countDown.countDown();
            }
        }

        public void setWaitMillis(long d) {
            this.waitMillis.set(d);
        }

        public boolean decWaitMillis(long d) {
            if (this.waitMillis.get() > 0) {
                long v = this.waitMillis.addAndGet(-d);
                if (v <= 0) {
                    this.clear();
                    return false;
                }
            }
            return true;
        }
    }
}
