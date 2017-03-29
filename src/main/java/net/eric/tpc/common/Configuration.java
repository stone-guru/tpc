package net.eric.tpc.common;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

public class Configuration {
    public static String BANK_NODE_PREFIX = "";
    
    private Map<String, Node> nodeMap = new HashMap<String, Node>();

    public Configuration() {
        nodeMap.put("BOC", new Node("localhost", 10021));
        nodeMap.put("CCB", new Node("localhost", 10022));
    }

    public boolean isBankNodeExists(String bankCode) {
        synchronized (nodeMap) {
            return this.nodeMap.containsKey(bankCode);
        }
    }

    public Optional<Node> getNode(String bankCode) {
        synchronized (nodeMap) {
            if (!nodeMap.containsKey(bankCode)) {
                return Optional.absent();
            }
            return Optional.of(nodeMap.get(bankCode));
        }
    }
}
