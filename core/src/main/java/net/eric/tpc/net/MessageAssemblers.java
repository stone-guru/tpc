package net.eric.tpc.net;

import java.net.SocketAddress;

import com.google.common.base.Function;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.binary.Message;
import net.eric.tpc.proto.Types.ErrorCode;

public final class MessageAssemblers {

    
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
    public static abstract class DataPacketAssembler<R> implements Function<Object, Maybe<R>> {
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

    public static final class YesOrNoAssembler extends DataPacketAssembler<Boolean> {
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

    private MessageAssemblers(){}
}
