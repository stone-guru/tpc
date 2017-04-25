package net.eric.tpc.net;

import static net.eric.tpc.base.Pair.asPair;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.MessageAssemblers.YesOrNoAssembler;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;
import net.eric.tpc.proto.Types.TransStartRec;

public class CoorCommunicator extends BasicCommunicator<Message> implements Communicator {
    private static final Logger logger = LoggerFactory.getLogger(CoorCommunicator.class);

    public CoorCommunicator(TaskPoolProvider poolProvider, List<PeerChannel<Message>> channels) {
        super(poolProvider, channels);
    }

    @Override
    public <B> Future<RoundResult<Boolean>> askBeginTrans(//
            TransStartRec transStartRec, //
            List<Pair<InetSocketAddress, B>> tasks) {
        List<Pair<InetSocketAddress, Message>> requests = Pair.map2(tasks, //
                (addr, b) -> {
                    return new Message(transStartRec.getXid(), (short) 0, //
                            CommandCodes.BEGIN_TRANS, (short) 0, transStartRec, b);
                });

        final Future<RoundResult<Boolean>> result = super.communicate(requests, //
                RoundType.WAIT_ALL, //
                new YesOrNoAssembler(transStartRec.getXid(), //
                        CommandCodes.BEGIN_TRANS_ANSWER, ErrorCode.REFUSE_TRANS));

        return result;
    }

    @Override
    public Future<RoundResult<Boolean>> gatherVote(long xid, List<InetSocketAddress> nodes) {
        List<Pair<InetSocketAddress, Message>> requests = Lists.transform(nodes, //
                node -> asPair(node, new Message(xid, (short) 1, CommandCodes.VOTE_REQ)));

        return super.communicate(requests, RoundType.WAIT_ALL, //
                new YesOrNoAssembler(xid, CommandCodes.VOTE_ANSWER, ErrorCode.REFUSE_COMMIT));
    }

    @Override
    public Future<RoundResult<Boolean>> notifyDecision(long xid, Decision decision, List<InetSocketAddress> nodes) {
        if (logger.isDebugEnabled()) {
            logger.debug("notifyDecision " + xid + decision + nodes);
        }
        List<Pair<InetSocketAddress, Message>> requests = Pair.map1(nodes, //
                node -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Send " + decision + " to " + node);
                    }
                    short code = 0;
                    if (decision == Decision.COMMIT) {
                        code = CommandCodes.YES;
                    } else if (decision == Decision.ABORT) {
                        code = CommandCodes.NO;
                    } else {
                        throw new UnImplementedException();
                    }

                    return new Message(xid, (short) 2, CommandCodes.TRANS_DECISION, code);
                });

        return this.notify(requests, RoundType.WAIT_ALL);
    }
}
