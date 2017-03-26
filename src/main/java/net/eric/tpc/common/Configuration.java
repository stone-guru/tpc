package net.eric.tpc.common;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

import net.eric.tpc.proto.Node;

public class Configuration {
    private Map<String, Node> nodeMap = new HashMap<String, Node>();

    public Configuration() {

    }

    public boolean isBankNodeExists(String bankCode) {
        synchronized (nodeMap) {
            return this.nodeMap.containsKey(bankCode);
        }
    }

    public Optional<Node> geNode(String bankCode) {
        synchronized (nodeMap) {
            if (!nodeMap.containsKey(bankCode)) {
                return Optional.absent();
            }
            return Optional.of(nodeMap.get(bankCode));
        }
    }
}
