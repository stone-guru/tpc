package net.eric.tpc.proto;

import static net.eric.tpc.base.Pair.asPair;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import net.eric.tpc.CoorModule;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.base.Pair;
import net.eric.tpc.entity.DtRecord;
import net.eric.tpc.net.CoorCommunicatorFactory;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.persist.CorePersistModule;
import net.eric.tpc.persist.DtRecordDao;
import net.eric.tpc.persist.KeyRecord;
import net.eric.tpc.persist.KeyStoreDao;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

public class ProtoTotalTest {
    private static final Logger logger = LoggerFactory.getLogger(ProtoTotalTest.class);
    public static final InetSocketAddress PEER1 = new InetSocketAddress("localhost", 10021);
    public static final InetSocketAddress PEER2 = new InetSocketAddress("localhost", 10022);
    public static final InetSocketAddress SELF = new InetSocketAddress("localhost", 10024);

    public static void main(String[] args) throws Exception {
        ProtoTotalTest instance = new ProtoTotalTest();
        try {
            //instance.doTrans1();
            instance.doTrans2();
        } finally {
            NightWatch.executeCloseActions();
        }
    }

    private Injector injector;
    private TransactionManager<Integer> tm;
    private CoorCommunicatorFactory ccf;

    private ProtoTotalTest() {
        this.injector = this.initModules();

        this.ccf = injector.getInstance(CoorCommunicatorFactory.class);
        NightWatch.regCloseable("CoorCommunicatorFactory", ccf);

        this.tm = injector.getInstance(Key.get(new TypeLiteral<TransactionManager<Integer>>() {
        }));
        NightWatch.regCloseable("Coordinator", tm);
    }
    
    private void doTrans2() throws Exception {
        tm.transaction(3);
        Thread.sleep(100);
    }

    private void doTrans1() throws Exception {
        List<InetSocketAddress> peers = ImmutableList.of(PEER1);
        Maybe<Communicator> mc = ccf.getCommunicator(peers);
        if (!mc.isRight()) {
            logger.error(mc.getLeft().toString());
            return;
        }

        Communicator c = mc.getRight();
        // CoorBizStrategy<Integer> cbs = in2.getInstance(Key.get(new
        // TypeLiteral<CoorBizStrategy<Integer>>(){}));
        // cbs.splitTask(1, 100);
        Future<RoundResult<Boolean>> f = c.askBeginTrans(startRec(1), ImmutableList.of(asPair(PEER1, 9)));
        RoundResult<Boolean> btr = f.get();
        System.out.println(btr.isAllOK() ? "beginTransOK" : "BeginTrans fail " + btr.getAnError());
        if (!btr.isAllOK())
            return;

        RoundResult<Boolean> vtr = c.gatherVote(1, peers).get();
        System.out.println(vtr.isAllOK() ? "VOTE OK" : "VOTE fail " + btr.getAnError());
        if (!vtr.isAllOK())
            return;

        c.notifyDecision(1, Decision.COMMIT, peers).get();

        Thread.sleep(500);
    }

    private Injector initModules() {
        final String jdbcUrl2 = "jdbc:h2:/home/bison/workspace/tpc/deploy/database/data_abc";
        Module cm1 = new CorePersistModule(jdbcUrl2);
//        Module cm1 = new Module() {
//            @Override
//            public void configure(Binder binder) {
//                binder.bind(DtRecordDao.class).to(DummyDtRecordDao.class);
//                binder.bind(KeyStoreDao.class).to(DummyKeyStoreDao.class);
//            }
//        };

        Module cm2 = new CoorModule<Integer>(Integer.class) {
            @Override
            protected List<ObjectCodec> getExtraCodecs() {
                return ImmutableList.of(new IntCodec());
            }

            @Override
            protected InetSocketAddress getCoordinatorAddress() {
                return SELF;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Class<CoorBizStrategy<Integer>> getCoorBizStrategyClass() {
                final Object clz = IntCoorBizStrategy.class;
                return (Class<CoorBizStrategy<Integer>>) clz;
            }
        };

        Injector inj = Guice.createInjector(cm1, cm2);
        return inj;
    }

    private static TransStartRec startRec(long xid) {
        TransStartRec rec = new TransStartRec();
        rec.setXid(xid);
        rec.setCoordinator(SELF);
        rec.setParticipants(ImmutableList.of(PEER1));
        return rec;
    }

    public static class IntCoorBizStrategy implements CoorBizStrategy<Integer> {

        public IntCoorBizStrategy() {
        }

        @Override
        public Maybe<TaskPartition<Integer>> splitTask(long xid, Integer i) {
            logger.debug("splitTask " + xid + ", " + i);
            return Maybe.success(//
                    new TaskPartition<Integer>(1, ImmutableList.of(asPair(PEER1, i - 2))));
        }

        @Override
        public ActionStatus prepareCommit(long xid, Integer b) {
            logger.debug("CoorBizStrategy.prepareCommit " + xid + ", " + b);
            return ActionStatus.OK;
        }

        @Override
        public boolean commit(long xid) {
            logger.debug("CoorBizStrategy.commit " + xid);
            return true;
        }

        @Override
        public boolean abort(long xid) {
            logger.debug("CoorBizStrategry.abort" + xid);
            return true;
        }
    }

    @SuppressWarnings("unused")
    private static class DummyDtRecordDao implements DtRecordDao {
        @Override
        public void insert(DtRecord dtRecord) {
        }

        @Override
        public void updateVote(DtRecord dtRecord) {
        }

        @Override
        public void updateDecision(DtRecord dtRecord) {
        }

        @Override
        public void updateFinishTime(Pair<Long, Date> param) {
        }

        @Override
        public Decision selectDecision(long xid) {
            return Decision.ABORT;
        }
    }

    @SuppressWarnings("unused")
    private static class DummyKeyStoreDao implements KeyStoreDao {
        @Override
        public List<KeyRecord> selectAll() {
            return Collections.emptyList();
        }

        @Override
        public KeyRecord selectByPrefix(String prefix) {
            return null;
        }

        @Override
        public void insert(KeyRecord keyRecord) {
        }

        @Override
        public void update(KeyRecord keyRecord) {
        }
    }
}
