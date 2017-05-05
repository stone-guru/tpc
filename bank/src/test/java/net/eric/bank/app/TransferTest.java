package net.eric.bank.app;

import static com.google.common.collect.ImmutableList.of;
import static net.eric.tpc.base.Pair.asPair;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.net.TransferBillCodec;
import net.eric.bank.persist.BankPersistModule;
import net.eric.tpc.CoorModule;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.NightWatch;
import net.eric.tpc.net.binary.ObjectCodec;
import net.eric.tpc.proto.CoorBizStrategy;
import net.eric.tpc.proto.TransactionManager;

public class TransferTest {
    private static final Logger logger = LoggerFactory.getLogger(TransferTest.class);
    public static final InetSocketAddress PEER1 = new InetSocketAddress("localhost", 10021);
    public static final InetSocketAddress PEER2 = new InetSocketAddress("localhost", 10022);

    public static final String JDBC_URL = "jdbc:h2:~/Workspace/tpc/deploy/database/data_abc";

    public static void main(String[] args) throws Exception {
        TransactionManager<TransferBill> tm = getTm();

        long si = System.nanoTime() % 1000000;
        TransferBill bill = new TransferBill();
        bill.setTransSN("B" + si);
        bill.setLaunchTime(new Date());
        bill.setReceivingBankCode("ABC");
        bill.setPayer(new AccountIdentity("ryan", "BOC"));
        bill.setReceiver(new AccountIdentity("betty", "BOC"));
        bill.setAmount(BigDecimal.ONE);
        bill.setVoucherNumber("VC" + si);
        bill.setSummary("Happy holiday");

        try {
            tm.transaction(bill);
        } finally {
            Thread.sleep(200);
            NightWatch.executeCloseActions();
        }
    }

        public static TransactionManager<TransferBill> getTm () {
            Module cm1 = new BankPersistModule(JDBC_URL);
            Module cm2 = new CoorModule<TransferBill>(TransferBill.class) {
                @Override
                protected List<ObjectCodec> getExtraCodecs() {
                    return of(new TransferBillCodec());
                }

                @Override
                protected InetSocketAddress getCoordinatorAddress() {
                    return new InetSocketAddress("localhost", 10024);
                }

                @Override
                protected Class<CoorBizStrategy<TransferBill>> getCoorBizStrategyClass() {
                    Class<?> c = TransferStrategy.class;
                    return (Class<CoorBizStrategy<TransferBill>>) c;
                }
            };

            Injector inj = Guice.createInjector(cm1, cm2);
            TransactionManager<TransferBill> tm = inj.getInstance(Key.get(new TypeLiteral<TransactionManager<TransferBill>>() {
            }));

            return tm;
        }

        public static class TransferStrategy implements CoorBizStrategy<TransferBill> {

            @Override
            public Maybe<TaskPartition<TransferBill>> splitTask(long xid, TransferBill b) {
                logger.info("splitTask");
                TransferBill toCCB = b.copy();
                toCCB.setPayer(new AccountIdentity(b.getPayer().getNumber(), "CCB"));
                toCCB.setReceiver(new AccountIdentity(b.getReceiver().getNumber(), "CCB"));

                return Maybe.success(new TaskPartition<TransferBill>(b, of(asPair(PEER1, b))));//, asPair(PEER2, toCCB))));
            }

            @Override
            public ActionStatus prepareCommit(long xid, TransferBill b) {
                logger.info("prepare commit" + xid);
                return ActionStatus.OK;
            }

            @Override
            public boolean commit(long xid) {
                return true;
            }

            @Override
            public boolean abort(long xid) {
                return true;
            }

        }
    }
