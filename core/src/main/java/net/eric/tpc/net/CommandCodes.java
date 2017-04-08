package net.eric.tpc.net;

public class CommandCodes {
    public static final short HEART_BEAT = 100;
    public static final short HEART_BEAT_ANSWER = 101;

    public static final short BEGIN_TRANS = 110;
    public static final short BEGIN_TRANS_ANSWER = 111;

    public static final short VOTE_REQ = 120;
    public static final short VOTE_ANSWER = 121;

    public static final short TRANS_DECISION = 130;

    public static final short DECISION_QUERY = 140;
    public static final short DECISION_ANSWER = 141;

    public static final short BAD_COMMNAD = 1000;

    public static final short ERROR = 1000;

    public static final short YES = 1;
    public static final short NO = 2;
    public static final short UNKNOWN = 1000;

    private CommandCodes() {
    }
}
