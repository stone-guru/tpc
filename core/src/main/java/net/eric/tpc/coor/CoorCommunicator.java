package net.eric.tpc.coor;

import static net.eric.tpc.base.Pair.asPair;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;
import net.eric.tpc.net.BasicCommunicator;
import net.eric.tpc.net.CommandCodes;
import net.eric.tpc.net.CommunicationRound.RoundType;
import net.eric.tpc.net.PeerChannel;
import net.eric.tpc.net.TaskPoolProvider;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.Communicator;
import net.eric.tpc.proto.RoundResult;
import net.eric.tpc.proto.Types.Decision;
import net.eric.tpc.proto.Types.ErrorCode;
import net.eric.tpc.proto.Types.TransStartRec;
import net.eric.tpc.proto.Types.Vote;

public class CoorCommunicator extends BasicCommunicator<Message> implements Communicator {
    private static final Logger logger = LoggerFactory.getLogger(CoorCommunicator.class);

    public CoorCommunicator(TaskPoolProvider poolProvider, List<PeerChannel<Message>> channels) {
        super(poolProvider, channels);
    }

    @Override
    public <B> Future<RoundResult<Boolean>> askBeginTrans(TransStartRec transStartRec,
            List<Pair<InetSocketAddress, B>> tasks) {
        List<Pair<InetSocketAddress, Message>> requests = Lists.newArrayList();
        for (Pair<InetSocketAddress, B> p : tasks) {
            final Message packet = new Message(transStartRec.getXid(), (short) 0, //
                    CommandCodes.BEGIN_TRANS, (short) 0, transStartRec, p.snd());
            requests.add(asPair(p.fst(), packet));
        }

        @SuppressWarnings("unchecked")
        final Future<RoundResult<Boolean>> result = super.communicate(requests, //
                RoundType.WAIT_ALL, //
                new YesOrNoAssembler(transStartRec.getXid(), //
                        CommandCodes.BEGIN_TRANS_ANSWER, ErrorCode.REFUSE_TRANS));

        return result;
    }

    @Override
    public Future<RoundResult<Boolean>> gatherVote(long xid, List<InetSocketAddress> nodes) {
        List<Pair<InetSocketAddress, Message>> requests = Lists.newArrayList();
        for (InetSocketAddress node : nodes) {
            final Message packet = new Message(xid, (short) 1, CommandCodes.VOTE_REQ);
            requests.add(asPair(node, packet));
        }

        return super.communicate(requests, RoundType.WAIT_ALL,//
                new YesOrNoAssembler(xid, CommandCodes.VOTE_ANSWER, ErrorCode.REFUSE_COMMIT));
    }

    @Override
    public Future<RoundResult<Boolean>> notifyDecision(long xid, Decision decision, List<InetSocketAddress> nodes) {
        List<Pair<InetSocketAddress, Message>> requests = Lists.newArrayList();

        for (InetSocketAddress node : nodes) {
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

            final Message packet = new Message(xid, (short) 2, CommandCodes.TRANS_DECISION, code);
            requests.add(asPair(node, packet));
        }

        return this.notify(requests, RoundType.WAIT_ALL);
    }

    /**
     * TPC 事务中的通讯结果解码类。执行如下基本检查
     * <ul>
     * <li>底层传递上来的是否是DataPacket类型</li>
     * <li>此DataPacket的code是否为希望的code, (code在构造函数中指定)</li>
     * <li>此DataPacket的param1是否为希望的事务号xid(xid在构造函数中指定</li>
     * </ul>
     * 若满足上述检查，就调用{@link DataPacketAssembler#processDataPacket}来进行进一步的处理，子类须实现此方法。
     *
     */
    private static abstract class DataPacketAssembler<R> implements Function<Object, Maybe<R>> {
        private short code;
        private long xid;

        public DataPacketAssembler(long xid, short code) {
            this.code = code;
            this.xid = xid;
        }

        @Override
        public Maybe<R> apply(Object obj) {
            if (obj == null || !(obj instanceof Message)) {
                return Maybe.fail(ErrorCode.BAD_DATA_PACKET, "message is not DataPacket");
            }
            Message message = (Message) obj;
            ActionStatus status = message.assureCommand(this.xid, this.code);
            if (!status.isOK()) {
                return Maybe.fail(status);
            }
            return this.processDataPacket(message);
        }

        /**
         * 处理收到的DataPacket，
         * 
         * @param node 送回此packet的节点
         * @param packet 收到的dataPacket, 此DataPacket已满足前述要求。
         * @return PeerResult not null, 通讯结果
         */
        abstract protected Maybe<R> processDataPacket(Message packet);
    }

    private static final class YesOrNoAssembler extends DataPacketAssembler<Boolean> {
        private short refuseCode;

        public YesOrNoAssembler(long xid, short commandCode, short refuseCode) {
            super(xid, commandCode);
            this.refuseCode = refuseCode;
        }

        @Override
        protected Maybe<Boolean> processDataPacket(Message packet) {
            Maybe<Boolean> maybe = Message.checkYesOrNo(packet.getCommandAnswer());
            if (!maybe.isRight() || maybe.getRight() == true) {
                return maybe;
            }
            return Maybe.fail(this.refuseCode, //
                    this.formatReason(packet.getSender(), packet.paramAsActionStatus()));
        }

        private String formatReason(SocketAddress node, Object param3) {
            if (ActionStatus.class.isInstance(param3)) {
                ActionStatus status = (ActionStatus) param3;
                return String.format("%s, %s, %s", node.toString(), status.getCode(), status.getDescription());
            }
            return String.valueOf(param3);
        }
    }

}
