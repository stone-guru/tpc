package net.eric.tpc.proto;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Pair;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.TransStartRec;

public interface Communicator {
    ActionStatus connectPanticipants(List<InetSocketAddress> nodes);

    <B> Future<RoundResult> askBeginTrans(TransStartRec transStartRec, List<Pair<InetSocketAddress, B>> tasks);

    Future<RoundResult> notifyDecision(long xid, Decision decision, List<InetSocketAddress> nodes);

    Future<RoundResult> gatherVote(long xid, List<InetSocketAddress> nodes);

    void close();
}
