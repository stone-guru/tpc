package net.eric.tpc.proto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static net.eric.tpc.base.Pair.asPair;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public class TransProtoTest {
    private static final Logger logger = LoggerFactory.getLogger(TransProtoTest.class);

    public static final InetSocketAddress PEER1 = new InetSocketAddress("localhost", 1);
    public static final InetSocketAddress PEER2 = new InetSocketAddress("localhost", 2);
    public static final InetSocketAddress PEER3 = new InetSocketAddress("localhost", 3);
    public static final InetSocketAddress SELF = new InetSocketAddress("localhost", 10);
    public static final List<InetSocketAddress> PEERS = ImmutableList.of(PEER1, PEER2, PEER3);

    private Coordinator coor;
    private KeyGenerator keyGenerator = new OneKeyGenerator();
    private List<IntPeerTransactionManager> peerManagers;
    private DummyDtLogger dtLogger;

    @BeforeMethod
    public void initCoor() {
        this.peerManagers = ImmutableList.copyOf(transform(PEERS, n -> new IntPeerTransactionManager(n)));
        this.dtLogger = new DummyDtLogger();
        
        coor = new Coordinator();
        coor.setBizStrategy(new IntCoorStrategy());
        coor.setCommunicatorFactory(new LocalCommunicatorFactory(ImmutableList.copyOf(this.peerManagers)));
        coor.setDtLogger(this.dtLogger);
        coor.setKeyGenerator(this.keyGenerator);
        coor.setSelf(SELF);
    }

    @Test
    public void testProcessOK(){
        ActionStatus s = coor.transaction(1);
        assertTrue(s.isOK());
        
        assertEquals(peer(0).getLastDecision().snd(), Decision.COMMIT);
        assertEquals(peer(1).getLastDecision().snd(), Decision.COMMIT);
        assertEquals(peer(2).getLastDecision().snd(), Decision.COMMIT);
        
        assertEquals(this.dtLogger.getDecision(), Decision.COMMIT);
        assertTrue(this.dtLogger.awaitGetFinished());
    }
    
    @Test
    public void testBeginTransFail1() {
        this.peerManagers.get(0).setAllowBeginTrans(false);
        
        ActionStatus s = coor.transaction(1);
        assertTrue(!s.isOK());
        assertNull(peer(0).getLastDecision());
        assertEquals(peer(1).getLastDecision().snd(), Decision.ABORT);
        assertEquals(peer(2).getLastDecision().snd(), Decision.ABORT);
        
        assertEquals(this.dtLogger.getDecision(), Decision.ABORT);
        assertTrue(this.dtLogger.awaitGetFinished());
    }
    
    @Test
    public void testBeginTransFailAll() {
        for(int i = 0; i < 3; i++)
            this.peerManagers.get(i).setAllowBeginTrans(false);
        
        ActionStatus s = coor.transaction(1);
        assertTrue(!s.isOK());
        
        assertNull(this.peerManagers.get(0).getLastDecision());
        assertNull(this.peerManagers.get(1).getLastDecision());
        assertNull(this.peerManagers.get(2).getLastDecision());
        
        assertEquals(this.dtLogger.getDecision(), Decision.ABORT);
        assertTrue(this.dtLogger.awaitGetFinished());
    }    

    @Test
    public void testVoteNo1() {
        peer(2).setAllowVoteYes(false);
        
        ActionStatus s = coor.transaction(1);
        assertTrue(!s.isOK());
        
        assertNull(peer(2).getLastDecision());
        
        assertEquals(peer(0).getLastDecision().snd(), Decision.ABORT);
        assertEquals(peer(1).getLastDecision().snd(), Decision.ABORT);
        
        assertEquals(this.dtLogger.getDecision(), Decision.ABORT);
        assertTrue(this.dtLogger.awaitGetFinished());
    }
    

    @Test
    public void testVoteNo2() {
        peer(1).setAllowVoteYes(false);
        peer(2).setAllowVoteYes(false);
        
        ActionStatus s = coor.transaction(1);
        assertTrue(!s.isOK());
        
        assertNull(peer(1).getLastDecision());
        assertNull(peer(2).getLastDecision());
        
        assertEquals(peer(0).getLastDecision().snd(), Decision.ABORT);
        
        assertEquals(this.dtLogger.getDecision(), Decision.ABORT);
        assertTrue(this.dtLogger.awaitGetFinished());
    }   
    
    private IntPeerTransactionManager peer(int i){
        return this.peerManagers.get(i);
    }
    
    
    static class LocalCommunicator implements Communicator {
        private List<PeerTransactionManager> transManagers;
        private List<InetSocketAddress> notifiedNodes; 

        public LocalCommunicator(List<PeerTransactionManager> managers) {
            this.transManagers = ImmutableList.copyOf(managers);
        }

        @Override
        public <B> Future<RoundResult<Boolean>> askBeginTrans(TransStartRec transStartRec,
                List<Pair<InetSocketAddress, B>> tasks) {
            LocalRound<Boolean> round = new LocalRound<Boolean>(Pair.projectFirst(tasks));
            tasks.forEach(p -> {
                ActionStatus r = transManagers.get(p.fst().getPort() - 1).beginTrans(transStartRec, (Integer) p.snd());
                round.regResult(p.fst(), Maybe.might(r, true));
            });
            return Futures.immediateFuture(round);
        }

        @Override
        public Future<RoundResult<Boolean>> notifyDecision(long xid, Decision decision, List<InetSocketAddress> nodes) {
            LocalRound<Boolean> round = new LocalRound<Boolean>(nodes);
            nodes.forEach(n -> {
                final int i = n.getPort() - 1;
                transManagers.get(i).processTransDecision(xid, decision);
                round.regResult(n, Maybe.success(true));
            });
            return Futures.immediateFuture(round);
        }

        @Override
        public Future<RoundResult<Boolean>> gatherVote(long xid, List<InetSocketAddress> nodes) {
            LocalRound<Boolean> round = new LocalRound<Boolean>(nodes);
            nodes.forEach(n -> {
                final int i = n.getPort() - 1;
                final ActionStatus r = transManagers.get(i).processVoteReq(xid);
                round.regResult(n, Maybe.might(r, true));
            });
            return Futures.immediateFuture(round);
        }

        public List<InetSocketAddress> getNotifiedNodes() {
            return notifiedNodes;
        }
        
        static class LocalRound<B> implements RoundResult<B> {
            private final Predicate<Optional<Maybe<B>>> IsOK = r -> r.isPresent() && r.get().isRight();

            Map<InetSocketAddress, Optional<Maybe<B>>> results;

            public LocalRound(List<InetSocketAddress> peers) {
                results = new HashMap<InetSocketAddress, Optional<Maybe<B>>>();
                peers.forEach(n -> results.put(n, Optional.absent()));
            }

            @Override
            public int wantedCount() {
                return results.size();
            }

            @Override
            public boolean isAllDone() {
                return all(results.values(), r -> r.isPresent());
            }

            @Override
            public boolean isAllOK() {
                return all(results.values(), IsOK);
            }

            @Override
            public List<B> okResults() {
                return ImmutableList.copyOf(transform(filter(results.values(), IsOK), r -> r.get().getRight()));
            }

            @Override
            public int okResultCount() {
                return size(filter(results.values(), IsOK));
            }

            @Override
            public ActionStatus getAnError() {
                Optional<Optional<Maybe<B>>> opt = tryFind(results.values(), r -> r.isPresent() && !r.get().isRight());
                if (!opt.isPresent()) {
                    return ActionStatus.INNER_ERROR;
                }
                return opt.get().get().getLeft();
            }

            @Override
            public List<InetSocketAddress> getSuccessNodes() {
                List<InetSocketAddress> peers = new ArrayList<InetSocketAddress>(5);
                for(InetSocketAddress p : results.keySet()){
                    if(IsOK.apply(results.get(p)))
                        peers.add(p);
                }
                return peers;
            }

            public void regResult(InetSocketAddress peer, Maybe<B> r) {
                if (results.get(peer).isPresent()) {
                    throw new IllegalStateException("results for " + peer + " already exists");
                }
                results.put(peer, Optional.of(r));
            }
        }
    }

    static class IntPeerTransactionManager implements PeerTransactionManager{
        private boolean allowBeginTrans = true;
        private boolean allowVoteYes = true;
        private InetSocketAddress self;
        
        public IntPeerTransactionManager(InetSocketAddress self){
            this.self = self;
        }
        
        @Override
        public ActionStatus beginTrans(TransStartRec transNode, Object i) {
            logger.info(self + " BeginTrans " + transNode + ", " + String.valueOf(i));
            if (allowBeginTrans)
                return ActionStatus.OK;
            else
                return new ActionStatus((short) 2001, "I dislike even");
        }

        @Override
        public ActionStatus processVoteReq(long xid) {
            logger.info(self + " processVoteReq for XID " + xid);
            if (allowVoteYes)
                return ActionStatus.OK;
            else
                return new ActionStatus((short) 2002, "I am busy");
        }

        @Override
        public void processTransDecision(long xid, Decision decision) {
            logger.info(self + " processTransDecision for " + xid + ", " + decision);
            this.lastDecision = asPair(xid, decision);
        }

        @Override
        public Optional<Decision> queryDecision(long xid) {
            return Optional.absent();
        }

        public Pair<Long, Decision> getLastDecision() {
            return lastDecision;
        }

        private Pair<Long, Decision> lastDecision;

        public boolean isAllowBeginTrans() {
            return allowBeginTrans;
        }

        public void setAllowBeginTrans(boolean allowBeginTrans) {
            this.allowBeginTrans = allowBeginTrans;
        }

        public boolean isAllowVoteYes() {
            return allowVoteYes;
        }

        public void setAllowVoteYes(boolean allowVoteYes) {
            this.allowVoteYes = allowVoteYes;
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
            
        }

    }

    static class IntCoorStrategy implements CoorBizStrategy{

        @Override
        public Maybe<TaskPartition<Integer>> splitTask(long xid, Object obj) {
            int b = (Integer)obj;
            List<Pair<InetSocketAddress, Integer>> tasks = ImmutableList.copyOf(transform(PEERS, n -> asPair(n, b + n.getPort())));
            return Maybe.success(new TaskPartition<Integer>(b, tasks));
        }

        @Override
        public ActionStatus prepareCommit(long xid, Object b) {
            return ActionStatus.OK;
        }

        @Override
        public boolean commit(long xid){
            return true;
        }

        @Override
        public boolean abort(long xid){
            return true;
        }
    }

    static class IntBizStrategy implements PeerBizStrategy {
        private int delta;

        public IntBizStrategy(int delta) {
            this.delta = delta;
        }

        @Override
        public ActionStatus checkAndPrepare(long xid, Object b) {

            int i = ((Integer) b).intValue();
            logger.info("BeginTrans " + xid + ", " + String.valueOf(i));
            ActionStatus r = (i % delta == 1) ? //
                    new ActionStatus((short) 2001, "I dislike even") : ActionStatus.OK;
            return r;
        }

        @Override
        public boolean commit(long xid) {
            lastDecision = asPair(xid, Decision.COMMIT);
            return true;
        }

        @Override
        public boolean abort(long xid) {
            lastDecision = asPair(xid, Decision.ABORT);
            return true;
        }

        public Pair<Long, Decision> getLastDecision() {
            return lastDecision;
        }

        private Pair<Long, Decision> lastDecision;
    }

    static class LocalCommunicatorFactory implements CommunicatorFactory {
        private List<PeerTransactionManager> peerManagers;

        public LocalCommunicatorFactory(List<PeerTransactionManager> peerManagers) {
            super();
            this.peerManagers = peerManagers;
        }

        @Override
        public Maybe<Communicator> getCommunicator(List<InetSocketAddress> peers) {
            return Maybe.success(new LocalCommunicator(peerManagers.subList(0, peers.size())));
        }

        @Override
        public void releaseCommunicator(Communicator c) {
        }

        @Override
        public void close() throws IOException {
        }

    }

    static class OneKeyGenerator implements KeyGenerator {
        private long value = 100;

        @Override
        public String nextKeyWithName(String keyName) {
            if (!keyName.equals(Coordinator.XID_PREFIX))
                throw new UnImplementedException();
            return keyName + nextKey(keyName);
        }

        @Override
        public long nextKey(String keyName) {
            return value;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    static class DummyDtLogger implements DtLogger {
        private TransStartRec startRec;
        private Object bizEntity;
        private boolean initiator;
        private Vote vote;
        private Decision decision;
        private boolean finished = false;
        
        @Override
        public void recordBeginTrans(TransStartRec startRec, Object b, boolean isInitiator) {
            this.startRec = startRec;
            this.bizEntity = b;
            this.initiator = isInitiator;
        }

        @Override
        public void recordVote(long xid, Vote vote) {
            checkArgument(xid == startRec.xid());
            checkArgument(vote == null);
            this.vote = vote;
        }

        @Override
        public void recordDecision(long xid, Decision decision) {
            checkArgument(xid == startRec.xid());
            checkArgument(this.decision == null);
            this.decision = decision;
        }

        @Override
        public void markTransFinished(long xid) {
            logger.debug("DtLogger mark finished " + xid);
            checkArgument(xid == startRec.xid());
            checkArgument(!finished);
            this.finished = true;
            synchronized(this){
                this.notifyAll();
            }
        }

        @Override
        public Optional<Decision> getDecisionFor(long xid) {
            // TODO Auto-generated method stub
            return null;
        }

        public TransStartRec getStartRec() {
            return startRec;
        }

        public Object getBizEntity() {
            return bizEntity;
        }

        public boolean isInitiator() {
            return initiator;
        }

        public Vote getVote() {
            return vote;
        }

        public Decision getDecision() {
            return decision;
        }

         public boolean awaitGetFinished() {
            Runnable r = new Runnable(){
                @Override
                public void run() {
                    synchronized(DummyDtLogger.this){
                        try {
                            DummyDtLogger.this.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                }
            };
            Thread t = new Thread(r);
            t.start();
            return finished;
        }
    }
}
