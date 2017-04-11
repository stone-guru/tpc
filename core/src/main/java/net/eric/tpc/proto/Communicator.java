package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Pair;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public interface Communicator {
    <B> Future<RoundResult<Boolean>> askBeginTrans(TransStartRec transStartRec, List<Pair<InetSocketAddress, B>> tasks);

    Future<RoundResult<Boolean>> notifyDecision(long xid, Decision decision, List<InetSocketAddress> nodes);

    Future<RoundResult<Boolean>> gatherVote(long xid, List<InetSocketAddress> nodes);
}
