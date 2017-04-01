package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Node;
import net.eric.tpc.base.Pair;

public interface Communicator<B> {
    ActionStatus connectPanticipants(List<Node> nodes);

    Future<RoundResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, B>> tasks);

    Future<RoundResult> notifyDecision(String xid, Decision decision, List<Node> nodes);

    Future<RoundResult> gatherVote(String xid, List<Node> nodes);

    void close();
}
