package net.eric.tpc.common;

public class ServerConfig {
    private String bankCode;
    private int port;
    private String dbUrl;

    public ServerConfig(String bankCode, int port, String dbUrl) {
        this.bankCode = bankCode;
        this.port = port;
        this.dbUrl = dbUrl;
    }

    public ServerConfig(String[] args, String defaultBankCode, int defaultPort, String defaultDbUrl) {
        this.bankCode = defaultBankCode;
        this.port = defaultPort;
        this.dbUrl = defaultDbUrl;
        
        if (!(args == null)) {
            if (args.length >= 1) {
                this.bankCode = args[0].toUpperCase();
            }
            if (args.length >= 2) {
                this.port = Integer.parseInt(args[1]);
            }
            if (args.length >= 3) {
                this.dbUrl = args[2];
            }
        }
    }

    public String getBankCode() {
        return bankCode;
    }

    public int getPort() {
        return port;
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
