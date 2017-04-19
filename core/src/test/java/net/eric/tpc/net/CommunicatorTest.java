package net.eric.tpc.net;

import static net.eric.tpc.base.Pair.asPair;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.net.mina.MinaTransService;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.CommunicatorFactory;
import net.eric.tpc.proto.PeerTransactionManager;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;
import net.eric.tpc.proto.Types.TransStartRec;

public class CommunicatorTest {
    private static final Logger logger = LoggerFactory.getLogger(CommunicatorTest.class);
    
    public static final InetSocketAddress PEER1 = new InetSocketAddress("localhost", 10021);
    public static final InetSocketAddress PEER2 = new InetSocketAddress("localhost", 10022);
    public static final InetSocketAddress SELF = new InetSocketAddress("localhost", 10024);
    public static final List<InetSocketAddress> PEERS = ImmutableList.of(PEER1, PEER2);

    private CommunicatorFactory cf = new CoorCommunicatorFactory(ImmutableList.of(new IntCodec()));
    private Communicator c;
    private MinaTransService transService1, transService2;
    private IntTransactionManager peerTransManager = new IntTransactionManager();
    
    public static void main(String[] args) throws Exception {
        CommunicatorTest app = new CommunicatorTest();
        app.startServer();
    }
    
    @BeforeTest
    public void startServer() throws InterruptedException {
        transService1 = new MinaTransService(10021, this.peerTransManager, ImmutableList.of(new IntCodec()));
        transService1.startAsync();
        
        transService2 = new MinaTransService(10022, this.peerTransManager, ImmutableList.of(new IntCodec()));
        transService2.startAsync();

        transService1.awaitRunning();
        transService2.awaitRunning();
    }

    @AfterTest
    public void stopServer() throws IOException{
        cf.close();
        transService1.stopAsync();
        transService2.stopAsync();
        transService1.awaitTerminated();
        transService2.awaitTerminated();
    }
    
    @BeforeMethod
    public void initCommunicator() {
        Maybe<Communicator> maybe = cf.getCommunicator(PEERS);
        if(!maybe.isRight()){
            throw new RuntimeException(maybe.getLeft().toString());
        }
        c = maybe.getRight();
    }

    @AfterMethod
    public void releaseCommunicator(){
        if(c != null)
            cf.releaseCommunicator(c);
    }
    
    @Test
    public void testStartTrans1() throws InterruptedException, ExecutionException {
        List<Pair<InetSocketAddress, Integer>> tasks = ImmutableList.of(asPair(PEER1, 1), asPair(PEER2, 3));
        Future<RoundResult<Boolean>> future = c.askBeginTrans(startRec(1L), tasks);
        RoundResult<Boolean> result = future.get();
        assertTrue(result.isAllOK());
    }
    
    @Test
    public void testStartTrans2() throws InterruptedException, ExecutionException {
        List<Pair<InetSocketAddress, Integer>> tasks = ImmutableList.of(asPair(PEER1, 2), asPair(PEER2, 3));
        Future<RoundResult<Boolean>> future = c.askBeginTrans(startRec(2L), tasks);
        RoundResult<Boolean> result = future.get();
        assertTrue(result.isAllDone());
        assertFalse(result.isAllOK());
        assertEquals(1, result.okResultCount(), "ok result count");
        ActionStatus err = result.getAnError();
        assertNotNull(err);
        assertEquals(ErrorCode.REFUSE_TRANS, err.getCode());
        assertTrue(err.getDescription().indexOf("2001") > 0);
        assertTrue(err.getDescription().indexOf("I dislike even") > 0);
        //assertTrue(result.isAllOK());
    }
    
    @Test 
    public void testVoteReq1() throws InterruptedException, ExecutionException{
        RoundResult<Boolean> result = c.gatherVote(1L, ImmutableList.of(PEER1)).get();
        assertTrue(result.isAllOK());
    }
    
    @Test
    public void testVoteReq2() throws InterruptedException, ExecutionException{
        RoundResult<Boolean> result = c.gatherVote(2L, ImmutableList.of(PEER1)).get();
        assertEquals(0, result.okResultCount(), "ok result count");
        ActionStatus err = result.getAnError();
        assertNotNull(err);
        assertEquals(ErrorCode.REFUSE_COMMIT, err.getCode());
        assertTrue(err.getDescription().indexOf("2002") > 0);
        assertTrue(err.getDescription().indexOf("I am busy") > 0);
    }
    
    @Test
    public void testNotifyDecision1() throws InterruptedException, ExecutionException {
        RoundResult<Boolean> result = c.notifyDecision(1L, Decision.COMMIT, ImmutableList.of(PEER1)).get();
        assertTrue(result.isAllOK());
        assertNotNull(this.peerTransManager.getLastDecision());
        assertEquals(asPair(1L, Decision.COMMIT), this.peerTransManager.getLastDecision());
    }
    
    @Test
    public void testNotifyDecision2() throws InterruptedException, ExecutionException {
        RoundResult<Boolean> result = c.notifyDecision(1L, Decision.ABORT, ImmutableList.of(PEER1)).get();
        assertTrue(result.isAllOK());
        assertNotNull(this.peerTransManager.getLastDecision());
        assertEquals(this.peerTransManager.getLastDecision(), asPair(1L, Decision.ABORT));
    }
    
    private TransStartRec startRec(long xid){
        TransStartRec rec = new TransStartRec();
        rec.setXid(xid);
        rec.setCoordinator(SELF);
        rec.setParticipants(PEERS);
        return rec;
    }
    
    
    private static class IntCodec implements ObjectCodec {

        @Override
        public short getTypeCode() {
            return 2001;
        }

        @Override
        public Class<?> getObjectClass() {
            return Integer.class;
        }

        @Override
        public void encode(Object entity, IoBuffer buf) throws Exception {
            buf.putInt((Integer) entity);
        }

        @Override
        public Object decode(int length, IoBuffer in) throws Exception {
            return in.getInt();
        }
    }
    
    private static class IntTransactionManager  implements PeerTransactionManager<Integer>{

        @Override
        public ActionStatus beginTrans(TransStartRec transNode, Integer i) {
            logger.info("BeginTrans " + transNode + ", " + String.valueOf(i));
            if(i.intValue() % 2 == 1)
                return ActionStatus.OK;
            else
                return new ActionStatus((short)2001, "I dislike even");
        }

        @Override
        public ActionStatus processVoteReq(long xid) {
            logger.info("processVoteReq for XID " + xid);
            if(xid % 2 == 1)
                return ActionStatus.OK;
            else
                return new ActionStatus((short)2002, "I am busy");
        }

        @Override
        public void processTransDecision(long xid, Decision decision) {
            logger.info("processTransDecision for " + xid + ", " + decision);
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
    }
}
