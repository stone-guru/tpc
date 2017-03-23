package net.eric.tpc.proto;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import net.eric.tpc.common.Pair;

public interface Communicator<B> {
	FutureTask<CommunicateResult> askBeginTrans(TransactionNodes transNodes);
	Pair<List<Node>, List<Node>> dispatchTask(List<Pair<Node, B>> tasks);
	void notifyDecision(String xid, Decision decision, List<Node> nodes);
	Future<VoteResult> gatherVote(String xid, List<Node> nodes);
}
