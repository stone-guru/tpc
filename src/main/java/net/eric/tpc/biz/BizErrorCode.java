package net.eric.tpc.biz;

public class BizErrorCode {
    public static final String MISS_FIELD = "MISS_FIELD";
    public static final String NO_BANK_NODE = "NO_BANK_NODE";
    public static final String TIME_IS_FUTURE = "TIME_IS_FUTURE";
    public static final String AMOUNT_LE_ZERO = "AMOUNT_LE_ZERO";
    public static final String SAME_ACCOUNT = "SAME_ACCOUNT";
    public static final String NODE_UNREACH = "NODE_UNREACH";
    public static final String INNER_EXCEPTION = "INNER_EXCEPTION";
    
    public static final String PEER_PRTC_ERROR = "PEER_PRTC_ERROR";
    public static final String REFUSE_TRANS = "REFUSE_TRANS";
    public static final String REFUSE_COMMIT = "REFUSE_COMMIT";
    private BizErrorCode(){}
}
