package net.eric.bank.regulator;

import java.io.IOException;

import org.apache.mina.core.service.IoHandler;

import net.eric.tpc.common.MinaServer;
import net.eric.tpc.common.ServerConfig;

public class RegulatorServer extends MinaServer {

    private static final String DEFAULT_BANK_CODE = "CBRC";
    private static final int DEFAULT_PORT = 10023;
    private static final String DEFAULT_DB_URL = "jdbc:h2:tcp://localhost:9100/data_cbrc";

    public static void main(String[] args) throws IOException {
        ServerConfig config = new ServerConfig(args, DEFAULT_BANK_CODE, DEFAULT_PORT, DEFAULT_DB_URL);

        initFactory(config);

        RegulatorServer server = new RegulatorServer(config);

        server.start();
    }

    private static void initFactory(ServerConfig config) {
        //FIXME UniFactory.register(new PersisterFactory(config.getDbUrl()));
        //FIXME UniFactory.register(new CommonServiceFactory());
        //FIXME UniFactory.register(new RegulatorServiceFactory());
    }

    public RegulatorServer(ServerConfig config) {
        super(config);
    }

    @Override
    protected IoHandler getIoHandler() {
        return null;//FIXME UniFactory.getObject(PeerIoHandler.class, "REGULATOR");
    }

    @Override
    protected String getSplashText(String bankCode) {
        if (bankCode.equalsIgnoreCase("CBRC")) {
            return "          /$$$$$$  /$$$$$$$  /$$$$$$$   /$$$$$$ \n"//
                    + "         /$$__  $$| $$__  $$| $$__  $$ /$$__  $$\n"//
                    + "        | $$  \\__/| $$  \\ $$| $$  \\ $$| $$  \\__/\n"//
                    + "        | $$      | $$$$$$$ | $$$$$$$/| $$      \n"//
                    + "        | $$      | $$__  $$| $$__  $$| $$      \n"//
                    + "        | $$    $$| $$  \\ $$| $$  \\ $$| $$    $$\n"//
                    + "        |  $$$$$$/| $$$$$$$/| $$  | $$|  $$$$$$/\n"//
                    + "         \\______/ |_______/ |__/  |__/ \\______/ ";
        }
        return null;
    }
}
