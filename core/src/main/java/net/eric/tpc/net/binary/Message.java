package net.eric.tpc.net.binary;

import java.io.Serializable;
import java.net.SocketAddress;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;
import net.eric.tpc.net.CommandCodes;
import net.eric.tpc.proto.Types.ErrorCode;

public class Message implements Serializable {

    private static final long serialVersionUID = 6971835564417011087L;

    public static class TypeCode {
        public static final short ACTION_STATUS = 1001;
        public static final short TRANS_START_REC = 1002;

        private TypeCode() {
        };
    }

    public static Message fromRequest(Message request, short command, short answer) {
        return new Message(request.xid, request.round, command, answer);
    }

    public static Message fromRequest(Message request, short command, short answer, Object param) {
        return new Message(request.xid, request.round, command, answer, param, null);
    }

    public static Maybe<Boolean> checkYesOrNo(short answer) {
        if(answer == CommandCodes.SERVER_ERROR){
            return Maybe.fail(ErrorCode.PEER_INNER_ERROR, "Peer inner error");
        }
        return Maybe.fromCondition(answer == CommandCodes.YES || answer == CommandCodes.NO, //
                CommandCodes.YES == answer, //
                ErrorCode.PEER_PRTC_ERROR,
                "want " + CommandCodes.YES + " or " + CommandCodes.NO + ", but got " + answer);
    }

    private short version = 1;
    private long xid;
    private short round;
    private short commandCode;
    private short commandAnswer;
    private Object param;
    private Object content;
    private SocketAddress sender;
    
    public Message() {
    }

    public Message(long xid, short round, short commandCode) {
        this(xid, round, commandCode, (short) 0, null, null);
    }

    public Message(long xid, short round, short commandCode, short commandAnswer) {
        this(xid, round, commandCode, commandAnswer, null, null);
    }

    public Message(long xid, short round, short commandCode, short commandAnswer, Object param, Object content) {
        this.version = 11;
        this.xid = xid;
        this.round = round;
        this.commandCode = commandCode;
        this.commandAnswer = commandAnswer;
        this.param = param;
        this.content = content;
    }

    public ActionStatus assureCommand(long xid, short command) {
        if (command != this.commandCode) {
            return ActionStatus.create(ErrorCode.WRONG_ANSWER, "want " + command + " but got " + this.commandCode);
        }
        if (xid != this.xid) {
            return ActionStatus.create(ErrorCode.WRONG_ANSWER, "want xid " + xid + " but got " + this.xid);
        }
        return ActionStatus.OK;
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public long getXid() {
        return xid;
    }

    public void setXid(long xid) {
        this.xid = xid;
    }

    public short getRound() {
        return round;
    }

    public void setRound(short round) {
        this.round = round;
    }

    public short getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(short commandCode) {
        this.commandCode = commandCode;
    }

    public short getCommandAnswer() {
        return commandAnswer;
    }

    public void setCommandAnswer(short commandAnswer) {
        this.commandAnswer = commandAnswer;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public ActionStatus paramAsActionStatus(){
        return (ActionStatus)this.param;
    }
    
    
    public SocketAddress getSender() {
        return sender;
    }

    public void setSender(SocketAddress sender) {
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "Message [version=" + version + ", xid=" + xid + ", round=" + round + ", commandCode=" + commandCode
                + ", commandAnswer=" + commandAnswer + ", param=" + String.valueOf(param) + ", content="
                + String.valueOf(content) + "]";
    }
}
