package net.eric.tpc.common;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ServerConfig {
    private String bankCode;
    private int port;
    private String dbUrl;
    private InetSocketAddress mySelf;
    
    public ServerConfig(String bankCode, int port, String dbUrl) {
        this.bankCode = bankCode;
        this.port = port;
        this.dbUrl = dbUrl;
        this.mySelf = this.getMySelf(port, bankCode);
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
        
        this.mySelf = this.getMySelf(this.port, bankCode);
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
    
    public InetSocketAddress getWorkingNode()
    {
        return this.mySelf;
    }
    
    private InetSocketAddress getMySelf(int port, String bankCode){
        InetAddress address;
        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return new InetSocketAddress(address.getHostAddress(), port);
    }
}
