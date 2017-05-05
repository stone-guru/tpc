package net.eric.tpc.net;

public class CommandCodes {
    public static final short HEART_BEAT = 100;
    public static final short HEART_BEAT_ANSWER = 101;

    public static final short BEGIN_TRANS = 110;
    public static final short VOTE_REQ = 120;
    public static final short ABORT_TRANS = 130;
    public static final short COMMIT_TRANS = 131;
    public static final short DECISION_QUERY = 140;

    public static final short BAD_COMMNAD = 1000;
    public static final short SERVER_ERROR = 119;

    public static final short YES = 1;
    public static final short NO = 2;
    public static final short UNKNOWN = 10000;

    private CommandCodes() {
    }
}
