package net.eric.tpc.net;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.proto.RoundResult;

public class CommunicationRoundTest {
    public static final InetSocketAddress PEER1 = new InetSocketAddress("localhost", 10021);
    public static final InetSocketAddress PEER2 = new InetSocketAddress("localhost", 10022);
    public static final InetSocketAddress SELF = new InetSocketAddress("localhost", 10024);
    public static final List<InetSocketAddress> PEERS = ImmutableList.of(PEER1, PEER2);

    private ExecutorService pool;

    @BeforeTest
    public void init() {
        pool = Executors.newCachedThreadPool();
    }

    @AfterTest
    public void afterTest() {
        pool.shutdownNow();
    }

    @Test
    public void testTimeoutAll0() throws Exception {
        CommunicationRound<Integer> round = new CommunicationRound<Integer>(PEERS, RoundType.WAIT_ALL);

        Future<RoundResult<Integer>> future = round.getResult(pool, 1000);

        RoundResult<Integer> result = future.get();

        assertFalse(result.isAllDone());
        assertFalse(result.isAllOK());
        assertEquals(0, result.okResultCount());
    }

    @Test
    public void testTimeoutAll1() throws Exception {
        CommunicationRound<Integer> round = new CommunicationRound<Integer>(PEERS, RoundType.WAIT_ALL, o-> Maybe.success((Integer)o));
        
        Future<RoundResult<Integer>> future = round.getResult(pool, 1000);
        
        round.regMessage(PEER1, 1);
        
        RoundResult<Integer> result = future.get();
        
        assertFalse(result.isAllDone());
        assertFalse(result.isAllOK());
        
        assertEquals(1, result.okResultCount());
    }
    
    @Test
    public void testTimeoutAny0() throws Exception {
        CommunicationRound<Integer> round = new CommunicationRound<Integer>(PEERS, RoundType.WAIT_ONE);

        Future<RoundResult<Integer>> future = round.getResult(pool, 1000);

        RoundResult<Integer> result = future.get();

        assertFalse(result.isAllDone());
        assertFalse(result.isAllOK());
        assertEquals(0, result.okResultCount());
    }

    @Test
    public void testTimeoutAny1() throws Exception {
        CommunicationRound<Integer> round = new CommunicationRound<Integer>(PEERS, RoundType.WAIT_ONE, o-> Maybe.success((Integer)o));
        
        Future<RoundResult<Integer>> future = round.getResult(pool, 1000);
        
        round.regMessage(PEER1, 1);
        
        RoundResult<Integer> result = future.get();
        
        assertFalse(result.isAllDone());
        assertFalse(result.isAllOK());
        
        assertEquals(1, result.okResultCount());
    }
}
