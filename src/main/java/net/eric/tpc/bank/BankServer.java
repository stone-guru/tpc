package net.eric.tpc.bank;

import java.io.IOException;

import org.apache.mina.core.service.IoHandler;

import net.eric.tpc.common.MinaServer;
import net.eric.tpc.common.ServerConfig;

public class BankServer extends MinaServer {

    private static final String DEFAULT_BANK_CODE = "BOC";
    private static final int DEFAULT_PORT = 10021;
    private static final String DEFAULT_DB_URL = "jdbc:h2:tcp://localhost:9100/bank_boc";

    public static void main(String[] args) throws IOException {
        ServerConfig config = new ServerConfig(args, DEFAULT_BANK_CODE, DEFAULT_PORT, DEFAULT_DB_URL);

        BankServer server = new BankServer(config);

        server.start();
    }

    public BankServer(ServerConfig config) {
        super(config);
    }

    @Override
    protected IoHandler getIoHandler() {
        return BankServiceFactory.newIoHandlerAdapter();
    }

    @Override
    protected String getSplashText(String bankCode) {
        if(bankCode.equals("CCB")){
            return "          /$$$$$$   /$$$$$$  /$$$$$$$ \n"//
                    +"         /$$__  $$ /$$__  $$| $$__  $$\n"//
                    +"        | $$  \\__/| $$  \\__/| $$  \\ $$\n"//
                    +"        | $$      | $$      | $$$$$$$ \n"//
                    +"        | $$      | $$      | $$__  $$\n"//
                    +"        | $$    $$| $$    $$| $$  \\ $$\n"//
                    +"        |  $$$$$$/|  $$$$$$/| $$$$$$$/\n"//
                    +"        \\______/  \\______/ |_______/  ";

        }
        if(bankCode.equals("BOC")){
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

}
