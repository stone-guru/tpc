package net.eric.bank.coor;

import java.io.IOException;

import org.apache.mina.core.service.IoHandler;

import net.eric.bank.service.CommonServiceFactory;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.common.MinaServer;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.persist.PersisterFactory;

public class CoorServer extends MinaServer {
    private static final String DEFAULT_BANK_CODE = "ABC";
    private static final int DEFAULT_PORT = 10024;
    private static final String DEFAULT_DB_URL = "jdbc:h2:tcp://localhost:9100/data_abc";

    public CoorServer(ServerConfig config) {
        super(config);
    }

    public static void main(String[] args) throws IOException {
        ServerConfig config = new ServerConfig(args, DEFAULT_BANK_CODE, DEFAULT_PORT, DEFAULT_DB_URL);

        CoorServer.registerFactories(config);

        CoorServer server = new CoorServer(config);

        server.start();
    }

    public static void registerFactories(ServerConfig config) {
        UniFactory.register(new PersisterFactory(config.getDbUrl()));
        UniFactory.register(new CommonServiceFactory());
        UniFactory.register(new CoordinatorFactory(config));
    }
    // KeyGenerator.init();
    // PersisterFactory.initialize("jdbc:h2:tcp://localhost:9100/bank");
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
    // ActionStatus r = transManager.transaction(msg);
    // System.out.println(r);
    // }
    //
    // CoordinatorFactory.shutDown();

    @Override
    protected IoHandler getIoHandler() {
        return UniFactory.getObject(CoorIoHandler.class);
    }

    @Override
    protected String getSplashText(String bankCode) {
        if (bankCode.equalsIgnoreCase("abc")) {
            return splashText;
        }
        return null;
    }

    private static String splashText = //
            "          /$$$$$$  /$$$$$$$   /$$$$$$ \n"//
                    + "         /$$__  $$| $$__  $$ /$$__  $$\n"//
                    + "        | $$  \\ $$| $$  \\ $$| $$  \\__/\n"//
                    + "        | $$$$$$$$| $$$$$$$ | $$      \n"//
                    + "        | $$__  $$| $$__  $$| $$      \n"//
                    + "        | $$  | $$| $$  \\ $$| $$    $$\n"//
                    + "        | $$  | $$| $$$$$$$/|  $$$$$$/\n"//
                    + "        |__/  |__/|_______/  \\______/ ";

}
