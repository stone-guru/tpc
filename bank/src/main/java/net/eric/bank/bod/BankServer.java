package net.eric.bank.bod;

import java.io.IOException;
import java.util.List;

import org.apache.mina.core.service.IoHandler;

import net.eric.bank.biz.AccountRepository;
import net.eric.bank.entity.Account;
import net.eric.bank.net.PeerIoHandler;
import net.eric.bank.service.CommonServiceFactory;
import net.eric.bank.util.Util;
import net.eric.tpc.base.UniFactory;
import net.eric.tpc.common.MinaServer;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.persist.PersisterFactory;

public class BankServer extends MinaServer {

    private static final String DEFAULT_BANK_CODE = "BOC";
    private static final int DEFAULT_PORT = 10021;
    private static final String DEFAULT_DB_URL = "jdbc:h2:tcp://localhost:9100/data_boc";

    public static void main(String[] args) throws IOException {
        ServerConfig config = new ServerConfig(args, DEFAULT_BANK_CODE, DEFAULT_PORT, DEFAULT_DB_URL);

        initFactory(config);

        BankServer server = new BankServer(config);

        server.start();

        server.displayAllAccount();
    }

    private static void initFactory(ServerConfig config) {
        UniFactory.register(new PersisterFactory(config.getDbUrl()));
        UniFactory.register(new CommonServiceFactory());
        UniFactory.register(new BankServiceFactory());
    }

    public BankServer(ServerConfig config) {
        super(config);
    }

    @Override
    protected IoHandler getIoHandler() {
        return UniFactory.getObject(PeerIoHandler.class, "BANK");
    }

    @Override
    protected String getSplashText(String bankCode) {
        if (bankCode.equalsIgnoreCase("CCB")) {
            return "          /$$$$$$   /$$$$$$  /$$$$$$$ \n"//
                    + "         /$$__  $$ /$$__  $$| $$__  $$\n"//
                    + "        | $$  \\__/| $$  \\__/| $$  \\ $$\n"//
                    + "        | $$      | $$      | $$$$$$$ \n"//
                    + "        | $$      | $$      | $$__  $$\n"//
                    + "        | $$    $$| $$    $$| $$  \\ $$\n"//
                    + "        |  $$$$$$/|  $$$$$$/| $$$$$$$/\n"//
                    + "        \\______/  \\______/ |_______/  ";

        }
        if (bankCode.equalsIgnoreCase("BOC")) {
            return "         /$$$$$$$   /$$$$$$   /$$$$$$ \n"//
                    + "        | $$__  $$ /$$__  $$ /$$__  $$\n"//
                    + "        | $$  \\ $$| $$  \\ $$| $$  \\__/\n"//
                    + "        | $$$$$$$ | $$  | $$| $$      \n"//
                    + "        | $$__  $$| $$  | $$| $$      \n"//
                    + "        | $$  \\ $$| $$  | $$| $$    $$\n"//
                    + "        | $$$$$$$/|  $$$$$$/|  $$$$$$/\n"//
                    + "        |_______/  \\______/  \\______/ ";
        }
        return null;
    }

    private void displayAllAccount() {
        AccountRepository accountRepo = UniFactory.getObject(AccountRepositoryImpl.class);
        List<Account> accounts = accountRepo.getAllAccount();
        Util.displayAccounts(accounts);
    }

}
