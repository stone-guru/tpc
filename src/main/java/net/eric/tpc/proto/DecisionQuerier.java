package net.eric.tpc.proto;

import java.util.List;

import com.google.common.base.Optional;

import net.eric.tpc.base.Node;

public interface DecisionQuerier {
    Optional<Decision> queryDecision(String xid, List<Node> peers);
}
