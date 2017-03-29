package net.eric.tpc.coor;

import java.math.BigDecimal;
import java.util.Date;

import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.BankException;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.entity.AccountIdentity;
import net.eric.tpc.entity.TransType;
import net.eric.tpc.entity.TransferBill;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.proto.TransactionManager;

public class CoorMain {
    public static void main(String[] args) throws BankException {
        KeyGenerator.init();
        PersisterFactory.initialize("jdbc:h2:tcp://localhost:9100/bank");
        
        for (int i = 0; i < 100; i++) {
            TransferBill msg = new TransferBill();
            msg.setTransSN("982872393" + i);
            msg.setLaunchTime(new Date());
            msg.setAccount(new AccountIdentity("mike", "BOC"));
            msg.setOppositeAccount(new AccountIdentity("jack", "CCB"));
            msg.setReceivingBankCode("ABC");
            msg.setAmount(BigDecimal.valueOf(200));
            msg.setSummary("for cigrate");
            msg.setVoucherNumber("BIK09283-33843");

            TransactionManager<TransferBill> transManager = CoordinatorFactory.getCoorTransManager();
            ActionStatus r = transManager.transaction(msg);
            System.out.println(r);
        }
        
        CoordinatorFactory.shutDown();
    }
}
