package net.eric.tpc.trans.common;

import java.math.BigDecimal;
import java.util.Date;

import com.google.common.collect.ImmutableList;

import net.eric.tpc.bank.BankServiceFactory;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.common.Node;
import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.proto.Decision;
import net.eric.tpc.proto.DtLogger;
import net.eric.tpc.proto.TransStartRec;
import net.eric.tpc.proto.Vote;

public class JavaSerialize {
    static Node boc = new Node("server.boc.org", 10021);
    static Node bbc = new Node("server.boc.org", 10022);
    private static TransferBill genTransferMessage() {
        TransferBill msg = new TransferBill();
        msg.setTransSN("982872393");
        msg.setLaunchTime(new Date());
        msg.setAccount(new AccountIdentity("mike", "BOC"));
        msg.setOppositeAccount(new AccountIdentity("jack", "ABC"));
        msg.setReceivingBankCode("BOC");
        msg.setAmount(BigDecimal.valueOf(200));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        return msg;
    }

    private static TransStartRec genTransRec(String prefix) {
        String xid = KeyGenerator.nextKey(prefix); 
        TransStartRec st = new TransStartRec(xid, new Node("localhost", 9001), ImmutableList.of(boc, bbc));
        return st;
    }

    public static void main(String[] args) {
        KeyGenerator.init();
        PersisterFactory.initialize(PersisterFactory.DEFAULT_JDBC_URL);
        DtLogger<TransferBill> dtLogger = BankServiceFactory.getDtLogger();
        for(int i = 0; i < 30; i++){
            TransStartRec rec = genTransRec("A");
            dtLogger.recordBeginTrans( rec, genTransferMessage(), true);
            int dice = i % 3;
            if(dice == 0 || dice == 1){
                dtLogger.recordVote(rec.getXid(), (i % 2 == 0)? Vote.YES : Vote.NO);
            }
            if(dice == 0 || dice == 2){
                dtLogger.recordDecision(rec.getXid(), (i % 2 == 0)? Decision.COMMIT : Decision.ABORT);
            }
        }
//        for(int i = 0; i < 30; i++)
//            
        
        
    }
}
