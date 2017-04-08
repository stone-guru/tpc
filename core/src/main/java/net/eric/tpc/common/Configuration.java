package net.eric.tpc.common;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class Configuration {
    public static String BANK_NODE_PREFIX = "";

    private Map<String, InetSocketAddress> nodeMap = new HashMap<String, InetSocketAddress>();
    private Map<String, String> abcAccountNumberMap = new HashMap<String, String>();
    private InetSocketAddress mySelf = InetSocketAddress.createUnresolved("localhost", 10020);

    public Configuration() {
        nodeMap.put("BOC", InetSocketAddress.createUnresolved("localhost", 10021));
        nodeMap.put("CCB", InetSocketAddress.createUnresolved("localhost", 10022));
        nodeMap.put("CBRC", InetSocketAddress.createUnresolved("localhost", 10023));

        abcAccountNumberMap.put("BOC", "abc");
        abcAccountNumberMap.put("CCB", "abc");
    }

    public boolean isBankNodeExists(String bankCode) {
        Preconditions.checkNotNull(bankCode);
        synchronized (nodeMap) {
            return this.nodeMap.containsKey(bankCode.toUpperCase());
        }
    }

    public Optional<InetSocketAddress> getNode(String bankCode) {
        Preconditions.checkNotNull(bankCode);
        final String upperCase = bankCode.toUpperCase();
        synchronized (nodeMap) {
            if (!nodeMap.containsKey(upperCase)) {
                return Optional.absent();
            }
            return Optional.of(nodeMap.get(upperCase));
        }
    }

    public Optional<String> getAbcAccountOn(String bankCode) {
        final String upperCase = bankCode.toUpperCase();
        if (!abcAccountNumberMap.containsKey(upperCase)) {
            return Optional.absent();
        }
        return Optional.of(abcAccountNumberMap.get(upperCase));
    }

    public Optional<InetSocketAddress> getRegulatoryNode() {
        return this.getNode("CBRC");
    }

    public InetSocketAddress getMySelf() {
        return mySelf;
    }
}
