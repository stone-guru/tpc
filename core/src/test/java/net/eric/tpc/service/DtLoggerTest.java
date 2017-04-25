package net.eric.tpc.service;

import java.net.InetSocketAddress;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public class DtLoggerTest {
    static InetSocketAddress boc = InetSocketAddress.createUnresolved("server.boc.org", 10021);
    static InetSocketAddress bbc = InetSocketAddress.createUnresolved("server.boc.org", 10022);

//    private static TransferBill genTransferMessage() {
//        TransferBill msg = new TransferBill();
//        msg.setTransSN("982872393");
//        msg.setLaunchTime(new Date());
//        msg.setPayer(new AccountIdentity("mike", "BOC"));
//        msg.setReceiver(new AccountIdentity("jack", "ABC"));
//        msg.setReceivingBankCode("BOC");
//        msg.setAmount(BigDecimal.valueOf(200));
//        msg.setSummary("for cigrate");
//        msg.setVoucherNumber("BIK09283-33843");
//
//        return msg;
//    }

    private static TransStartRec genTransRec(String prefix) {
        long xid = 0;//DefaultKeyGenerator.nextValue(prefix);
        TransStartRec st = new TransStartRec(xid, //
                InetSocketAddress.createUnresolved("localhost", 9001), //
                ImmutableList.of(boc, bbc));
        return st;
    }

    public static void main(String[] args) {
        final String url = "jdbc:h2:tcp://localhost:9100/bank";
        //FIXME UniFactory.register(new CorePersisterFactory("jdbc:h2:tcp://localhost:9100/data_abc"));

        TestUtil.clearTable("DT_RECORD", "org.h2.Driver", url, "sa", "");
        DtLogger dtLogger = null;//FIXME UniFactory.getObject(DtLogger.class);

        for (int i = 0; i < 30; i++) {
            TransStartRec rec = genTransRec("A");
            dtLogger.recordBeginTrans(rec, null, true);//FIXME null
            int dice = i % 3;
            if (dice == 0 || dice == 1) {
                dtLogger.recordVote(rec.getXid(), (i % 2 == 0) ? Vote.YES : Vote.NO);
            }
            if (dice == 0 || dice == 2) {
                dtLogger.recordDecision(rec.getXid(), (i % 2 == 0) ? Decision.COMMIT : Decision.ABORT);
            }
        }
    }
}
