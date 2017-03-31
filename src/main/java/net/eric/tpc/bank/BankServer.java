package net.eric.tpc.bank;

import java.io.IOException;
import java.util.List;

import org.apache.mina.core.service.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.eric.tpc.biz.AccountRepository;
import net.eric.tpc.common.KeyGenerator;
import net.eric.tpc.common.KeyGenerator.KeyPersister;
import net.eric.tpc.common.MinaServer;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.common.UniFactory;
import net.eric.tpc.entity.Account;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.service.CommonServiceFactory;

public class BankServer extends MinaServer {
    private static final Logger logger = LoggerFactory.getLogger(BankServer.class);
    
    private static final String DEFAULT_BANK_CODE = "BOC";
    private static final int DEFAULT_PORT = 10021;
    private static final String DEFAULT_DB_URL = "jdbc:h2:tcp://localhost:9100/data_boc";

    public static void main(String[] args) throws IOException {
        ServerConfig config = new ServerConfig(args, DEFAULT_BANK_CODE, DEFAULT_PORT, DEFAULT_DB_URL);

        initFactory(config.getDbUrl());
        
        BankServer server = new BankServer(config);

        server.start();

       
       server.displayAllAccount();
    }

    private static void initFactory(String dbUrl){
        PersisterFactory.register();
        CommonServiceFactory.register();
        BankServiceFactory.register();
        
        UniFactory.setParam(PersisterFactory.class, dbUrl);
        KeyGenerator.init(UniFactory.getObject(KeyPersister.class));
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
        if(bankCode.equalsIgnoreCase("CCB")){
            return "          /$$$$$$   /$$$$$$  /$$$$$$$ \n"//
                    +"         /$$__  $$ /$$__  $$| $$__  $$\n"//
                    +"        | $$  \\__/| $$  \\__/| $$  \\ $$\n"//
                    +"        | $$      | $$      | $$$$$$$ \n"//
                    +"        | $$      | $$      | $$__  $$\n"//
                    +"        | $$    $$| $$    $$| $$  \\ $$\n"//
                    +"        |  $$$$$$/|  $$$$$$/| $$$$$$$/\n"//
                    +"        \\______/  \\______/ |_______/  ";

        }
        if(bankCode.equalsIgnoreCase("BOC")){
            return "         /$$$$$$$   /$$$$$$   /$$$$$$ \n"//
            +"        | $$__  $$ /$$__  $$ /$$__  $$\n"//
            +"        | $$  \\ $$| $$  \\ $$| $$  \\__/\n"//
            +"        | $$$$$$$ | $$  | $$| $$      \n"//
            +"        | $$__  $$| $$  | $$| $$      \n"//
            +"        | $$  \\ $$| $$  | $$| $$    $$\n"//
            +"        | $$$$$$$/|  $$$$$$/|  $$$$$$/\n"//
            +"        |_______/  \\______/  \\______/ ";
        }
        return null;

    }
    
    private void displayAllAccount() {
        AccountRepository accountRepo = UniFactory.getObject(AccountRepositoryImpl.class);
        List<Account> accounts = accountRepo.getAllAccount();
        
       logger.info("Current accounts");
        for(Account acct : accounts){
            logger.info(acct.toString());
        }
    }

}
