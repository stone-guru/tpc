package net.eric.bank.common;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.eric.bank.entity.TransferBill;
import net.eric.tpc.proto.PeerTransactionManager;

public class ServerConfig {
    private String bankCode;
    private int port;
    private String dbUrl;
    private InetSocketAddress mySelf;

    public ServerConfig(String bankCode, int port, String dbUrl) {
        this.bankCode = bankCode;
        this.port = port;
        this.dbUrl = dbUrl;
        this.mySelf = this.getMySelf(port);
    }

    public ServerConfig(String[] args) {
        if (args == null || args.length != 3) {
            throw new IllegalArgumentException("");
        }

        this.bankCode = args[0].toUpperCase();
        this.port = Integer.parseInt(args[1]);
        this.dbUrl = args[2];
        this.mySelf = this.getMySelf(this.port);
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

    public InetSocketAddress getWorkingNode() {
        return this.mySelf;
    }

    private InetSocketAddress getMySelf(int port) {
        InetAddress address;
        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return new InetSocketAddress(address.getHostAddress(), port);
    }
}
