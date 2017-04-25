package net.eric.tpc.proto;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

import net.eric.tpc.PeerModule;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.persist.CorePersistModule;

public class TransProtoPeerServer {
    
    private static final Logger logger = LoggerFactory.getLogger(TransProtoPeerServer.class);

    public static class IntPeerBizStrategy implements PeerBizStrategy<Integer> {

        public IntPeerBizStrategy() {
        }

        @Override
        public ActionStatus checkAndPrepare(long xid, Integer b) {
            logger.debug("PeerBizStrategy.checkAndPrepare " + xid + ", " + b);
            return ActionStatus.OK;
        }

        @Override
        public boolean commit(long xid) {
            logger.debug("PeerBizStrategy.commit " + xid);
            return true;
        }

        @Override
        public boolean abort(long xid) {
            logger.debug("PeerBizStrategy.abort " + xid);
            return true;
        }

    }
    
    public static Service getPeerService(int port, String jdbcUrl) {
        Module m1 = new CorePersistModule(jdbcUrl);
        Module m2 = new PeerModule<Integer>(Integer.class) {
            @SuppressWarnings("unchecked")
            @Override
            public Class<PeerBizStrategy<Integer>> getPeerBizStrategyClass() {
                final Object o = IntPeerBizStrategy.class;
                return (Class<PeerBizStrategy<Integer>>) o;
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public List<ObjectCodec> getExtraCodecs() {
                return ImmutableList.of(new IntCodec());
            }
        };

        Injector in = Guice.createInjector(m1, m2);
        Service peerService = in.getInstance(Key.get(Service.class));
        return peerService;
    }
    
    public static void main(String[] args) {
        final String bankCode = "boc";
        final String jdbcUrl = "jdbc:h2:/home/bison/workspace/tpc/deploy/database/data_" + bankCode.toLowerCase();
 
        Service s = getPeerService(10021, jdbcUrl);
        s.startAsync();
        s.awaitRunning();
    }

}
