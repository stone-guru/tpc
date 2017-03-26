package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import net.eric.tpc.common.Pair;

public interface Communicator<B> {
    boolean connectPanticipants(List<Node> nodes);
    Future<CommuResult> askBeginTrans(TransStartRec transStartRec, List<Pair<Node, B>> tasks);
    //Pair<List<Node>, List<Node>> dispatchTask(List<Pair<Node, B>> tasks);
    void notifyDecision(String xid, Decision decision, List<Node> nodes);
    Future<VoteResult> gatherVote(String xid, List<Node> nodes);
    void closeConnections();
    void shutdown();
}
