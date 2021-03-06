package net.eric.tpc.regulator;

import java.io.IOException;

import org.apache.mina.core.service.IoHandler;

import net.eric.tpc.base.UniFactory;
import net.eric.tpc.common.MinaServer;
import net.eric.tpc.common.ServerConfig;
import net.eric.tpc.net.PeerIoHandler;
import net.eric.tpc.persist.PersisterFactory;
import net.eric.tpc.service.CommonServiceFactory;

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
        UniFactory.register(new PersisterFactory(config.getDbUrl()));
        UniFactory.register(new CommonServiceFactory());
        UniFactory.register(new RegulatorServiceFactory());
    }

    public RegulatorServer(ServerConfig config) {
        super(config);
    }

    @Override
    protected IoHandler getIoHandler() {
        return UniFactory.getObject(PeerIoHandler.class, "REGULATOR");
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
