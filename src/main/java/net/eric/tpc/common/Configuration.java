package net.eric.tpc.common;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

public class Configuration {
    public static String BANK_NODE_PREFIX = "";

    private Map<String, Node> nodeMap = new HashMap<String, Node>();
    private Map<String, String> abcAccountNumberMap = new HashMap<String, String>();
    private Node mySelf = new Node("localhost", 10020);
    
    public Configuration() {
        nodeMap.put("BOC", new Node("localhost", 10021));
        nodeMap.put("CCB", new Node("localhost", 10022));
        nodeMap.put("CBRC", new Node("localhost", 10023));
        
        abcAccountNumberMap.put("BOC", "ABC");
        abcAccountNumberMap.put("CCB", "ABC");
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

    public Optional<String> getAbcAccountOn(String bankCode) {
        if (!abcAccountNumberMap.containsKey(bankCode)) {
            return Optional.absent();
        }
        return Optional.of(abcAccountNumberMap.get(bankCode));
    }
    
    public Optional<Node> getRegulatoryNode(){
        return this.getNode("CBRC");
    }
    
    public Node getMySelf(){
        return mySelf;
    }
}
