package net.eric.tpc.net.binary;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 6971835564417011087L;

    public static class TypeCode {
        public static final short ACTION_STATUS = 1001;
        public static final short TRANS_START_REC = 1002;

        private TypeCode() {
        };
    }

    public static Message fromRequest(Message request, short command, short answer){
        return new Message(request.xid, request.round, command, answer);
    }

    public static Message fromRequest(Message request, short command, short answer, Object param){
        return new Message(request.xid, request.round, command, answer, param, null);
    }

    private short version = 1;
    private long xid = 7878748;
    private short round = 3;
    private short commandCode = 1;
    private short commandAnswer = 2;
    private Object param;
    private Object content;

    public Message(){
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

    @Override
    public String toString() {
        return "Message [version=" + version + ", xid=" + xid + ", round=" + round + ", commandCode=" + commandCode
                + ", commandAnswer=" + commandAnswer + ", param=" + String.valueOf(param) + ", content="
                + String.valueOf(content) + "]";
    }
}
