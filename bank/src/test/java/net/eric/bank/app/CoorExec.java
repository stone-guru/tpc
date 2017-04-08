package net.eric.bank.app;

import java.math.BigDecimal;
import java.util.Date;

import net.eric.bank.entity.AccountIdentity;
import net.eric.bank.entity.TransferBill;
import net.eric.bank.service.CommonServiceFactory;
import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.coor.CoordinatorFactory;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.proto.TransactionManager;
import net.eric.tpc.service.KeyGenerators;

public class CoorExec {

    public static void main(String[] args) {
        
        String dbUrl = "jdbc:h2:tcp://localhost:9100/data_abc";

        ServerConfig config = new ServerConfig("ABC", 10088, dbUrl);
        UniFactory.register(new PersisterFactory(config.getDbUrl()));
        UniFactory.register(new CommonServiceFactory());
        UniFactory.register(new CoordinatorFactory(config));

        CoorExec coor = new CoorExec();
        coor.execute();
    }

    private TransferBill createBill(String transSn) {
        TransferBill msg = new TransferBill();
        msg.setTransSN(transSn);
        msg.setLaunchTime(new Date());
        msg.setPayer(new AccountIdentity("james", "BOC"));
        msg.setReceiver(new AccountIdentity("lori", "CCB"));
        msg.setReceivingBankCode("ABC");
        msg.setAmount(BigDecimal.valueOf(12.8));
        msg.setSummary("for cigrate");
        msg.setVoucherNumber("BIK09283-33843");

        return msg;
    }

    public void execute() {
        // SingletonFactory factory =
        // SingletonFactory.getInstance(CoorFactory.class, new
        // ServerConfig("abc", 10024, "jdbc:h2:tcp://localhost:9100/data_abc"));
        @SuppressWarnings("unchecked")
        TransactionManager<TransferBill> transManager = UniFactory.getObject(TransactionManager.class);

        ActionStatus r = transManager.transaction(createBill(KeyGenerators.nextKey("BILL")));

        System.out.println(r);
        //
        // for (int i = 0; i < 5; i++) {
        // TransferBill msg = new TransferBill();
        // msg.setTransSN("982872393" + i);
        // msg.setLaunchTime(new Date());
        // msg.setAccount(new AccountIdentity("mike", "BOC"));
        // msg.setOppositeAccount(new AccountIdentity("jack", "CCB"));
        // msg.setReceivingBankCode("ABC");
        // msg.setAmount(BigDecimal.valueOf(200));
        // msg.setSummary("for cigrate");
        // msg.setVoucherNumber("BIK09283-33843");
        //
        // TransactionManager<TransferBill> transManager =
        // CoordinatorFactory.getCoorTransManager();

        // }
        //
        //
    }
}
