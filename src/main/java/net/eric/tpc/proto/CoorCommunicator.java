package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;

import net.eric.tpc.common.ActionResult;
import net.eric.tpc.common.Pair;

public interface CoorCommunicator<B> {
    ActionResult connectPanticipants(List<Node> nodes);

    Future<CoorCommuResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, B>> tasks);

    void notifyDecision(String xid, Decision decision, List<Node> nodes);

    Future<CoorCommuResult> gatherVote(String xid, List<Node> nodes);

    void close();
}
