package net.eric.tpc.coor.main;

import java.math.BigDecimal;
import java.util.Date;

import net.eric.tpc.biz.AccountIdentity;
import net.eric.tpc.biz.TransferMessage;
import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.BankException;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.proto.CoorTransManager;

public class CoorMain {
    public static void main(String[] args) throws BankException {
        KeyGenerator.init();
        
        TransferMessage msg = new TransferMessage();
        msg.setTransSN("982872393");
        msg.setTransType(TransferMessage.INCOME);
        msg.setLaunchTime(new Date());
        msg.setAccount(new AccountIdentity("mike", "BOC"));
        msg.setOppositeAccount(new AccountIdentity("jack", "CCB"));
        msg.setReceivingBankCode("ABC");
        msg.setAmount(BigDecimal.valueOf(200));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        CoorTransManager<TransferMessage> transManager = CoorFactory.getCoorTransManager();
        ActionResult r = transManager.transaction(msg);
        System.out.println(r);
    }
}
