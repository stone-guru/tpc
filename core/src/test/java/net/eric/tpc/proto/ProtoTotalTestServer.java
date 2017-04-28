package net.eric.tpc.proto;

import static net.eric.tpc.base.Pair.asPair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Pair;
import net.eric.tpc.net.mina.MinaTransService;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

import sun.misc.Signal;  
import sun.misc.SignalHandler;  

@SuppressWarnings("restriction")
public class ProtoTotalTestServer {
    private static final Logger logger = LoggerFactory.getLogger(ProtoTotalTestServer.class);

    private Service service1, service2;

    public static void main(String[] args) throws Exception {
        final ProtoTotalTestServer app = new ProtoTotalTestServer();
        app.startServer();
        
        SignalHandler handler = new SignalHandler(){
            @Override
            public void handle(Signal arg0) {
                app.stopServer();
            }
        };
        Signal.handle(new Signal("TERM"), handler);
        Signal.handle(new Signal("INT"), handler);
    }
    
    public void startServer(){
        this.service1 = createAndStartServer(10021, "tm1");
        this.service2 = createAndStartServer(10022, "tm2");
    }
    
    public Service createAndStartServer(int port, String tmName) {
        Service service = new MinaTransService<Integer>(port, new IntTransactionManager(tmName), ImmutableList.of(new IntCodec()));
        service.startAsync();
        service.awaitRunning();
        return service;
    }
    
    public void stopServer(){
        service1.stopAsync();
        service2.stopAsync();
        service1.awaitTerminated();
        service2.awaitTerminated();
    }
    
    class ServiceStopSignalHandler implements SignalHandler {
        @Override
        public void handle(Signal arg0) {
            ProtoTotalTestServer.this.stopServer();
        }
    }
    
    private static class IntTransactionManager  implements PeerTransactionManager<Integer>{

        private String tmName;
        
        public IntTransactionManager(String tmName){
            this.tmName = tmName;
        }
        
        @Override
        public ActionStatus beginTrans(TransStartRec transNode, Integer i) {
            logger.info(tmName + " BeginTrans " + transNode + ", " + String.valueOf(i));
            
            if(i % 2 == 1)
                return ActionStatus.OK;
            else
                return new ActionStatus((short)2001, "I dislike even");
        }

        @Override
        public ActionStatus processVoteReq(long xid) {
            logger.info(tmName + " processVoteReq for XID " + xid);
            if((xid / 100000000) % 2 == 1)
                return ActionStatus.OK;
            else
                return new ActionStatus((short)2002, "I am busy");
        }

        @Override
        public void processTransDecision(long xid, Decision decision) {
            logger.info(tmName + " processTransDecision for " + xid + ", " + decision);
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


        @Override
        public void close() throws IOException {
        }
    }
}
