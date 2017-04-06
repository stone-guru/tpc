package net.eric.tpc.net.binary;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {

    private static final long serialVersionUID = 6971835564417011087L;

    public static class TypeCode {
        public static final short ACTION_STATUS = 1;
        public static final short TRANS_START_REC = 2;
        public static final short TRANSFER_BILL = 3;

        private TypeCode() {
        };
    }

    public static final String HEART_BEAT = "HEART_BEAT";
    public static final String HEART_BEAT_ANSWER = "HEART_BEAT_ANSWER";

    public static final String BEGIN_TRANS = "BEGIN_TRANS";
    public static final String BEGIN_TRANS_ANSWER = "BEGIN_TRANS_ANSWER";

    public static final String VOTE_REQ = "VOTE_REQ";
    public static final String VOTE_ANSWER = "VOTE_ANSWER";

    public static final String TRANS_DECISION = "TRANS_DECISION";

    public static final String DECISION_QUERY = "DECISION_QUERY";
    public static final String DECISION_ANSWER = "DECISION_ANSWER";

    public static final String BAD_COMMNAD = "BAD_COMMAND";

    public static final String ERROR = "ERROR";

    public static final String YES = "YES";
    public static final String NO = "NO";
    public static final String UNKNOWN = "UNKNOWN";

    public static final String TRANS_BILL = "TRANS_BILL";
    public static final String TRANS_BILL_ANSWER = "TRANS_BILL_ANSWER";

    public static final String BAD_DATA_PACKET = "BAD_DATA_PACKET";
    public static final String WRONG_ANSWER = "WRONG_ANSWER";

    public static final String PEER_PRTC_ERROR = "PEER_PRTC_ERROR";

    private short version = 1;
    private long xid = 7878748;
    private short round = 3;
    private short commandCode = 1;
    private short commandAnswer = 2;
    private Object param ;
    private Object content;

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
                + ", commandAnswer=" + commandAnswer + ", param=" + String.valueOf(param)
                + ", content=" + String.valueOf(content) + "]";
    }
}
