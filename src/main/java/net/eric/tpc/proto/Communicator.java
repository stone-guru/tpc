package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;

import net.eric.tpc.common.ActionStatus;
import net.eric.tpc.common.Node;
import net.eric.tpc.common.Pair;

public interface Communicator<B> {
    ActionStatus connectPanticipants(List<Node> nodes);

    Future<RoundResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, B>> tasks);

    void notifyDecision(String xid, Decision decision, List<Node> nodes);

    Future<RoundResult> gatherVote(String xid, List<Node> nodes);

    void close();
}
