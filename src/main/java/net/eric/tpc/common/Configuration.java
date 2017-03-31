package net.eric.tpc.common;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import net.eric.tpc.base.Node;

public class Configuration {
    public static String BANK_NODE_PREFIX = "";

    private Map<String, Node> nodeMap = new HashMap<String, Node>();
    private Map<String, String> abcAccountNumberMap = new HashMap<String, String>();
    private Node mySelf = new Node("localhost", 10020);
    
    public Configuration() {
        nodeMap.put("BOC", new Node("localhost", 10021));
        nodeMap.put("CCB", new Node("localhost", 10022));
        nodeMap.put("CBRC", new Node("localhost", 10023));
        
        abcAccountNumberMap.put("BOC", "abc");
        abcAccountNumberMap.put("CCB", "abc");
    }

    public boolean isBankNodeExists(String bankCode) {
        Preconditions.checkNotNull(bankCode);
        synchronized (nodeMap) {
            return this.nodeMap.containsKey(bankCode.toUpperCase());
        }
    }

    public Optional<Node> getNode(String bankCode) {
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
    
    public Optional<Node> getRegulatoryNode(){
        return this.getNode("CBRC");
    }
    
    public Node getMySelf(){
        return mySelf;
    }
}
