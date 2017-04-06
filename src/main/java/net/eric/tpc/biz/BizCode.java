package net.eric.tpc.biz;

public class BizCode {
    public static final String MISS_FIELD = "MISS_FIELD";
    public static final String NO_BANK_NODE = "NO_BANK_NODE";
    public static final String TIME_IS_FUTURE = "TIME_IS_FUTURE";
    public static final String AMOUNT_LE_ZERO = "AMOUNT_LE_ZERO";
    public static final String AMOUNT_FMT_WRONG = "AMOUNT_FMT_WRONG";
    public static final String SAME_ACCOUNT = "SAME_ACCOUNT";
    public static final String NODE_UNREACH = "NODE_UNREACH";
    public static final String INNER_EXCEPTION = "INNER_EXCEPTION";
    public static final String ACCOUNT_NOT_EXISTS = "ACCOUNT_NOT_EXISTS";
    public static final String NOT_MY_ACCOUNT = "NOT_MY_ACCOUNT";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String BEYOND_OVERDRAFT_LIMIT = "BEYOND_OVERDRAFT_LIMIT";
    public static final String FIELD_TOO_LONG = "FIELD_TOO_LONG";
    public static final String AMOUNT_PRECISION_ERROR = "AMOUNT_PRECISION_ERROR";
    
    public static final String BEYOND_TRANS_AMOUNT_LIMIT = "BEYOND_TRANS_AMOUNT_LIMIT";
    
    public static final String REFUSE_TRANS = "REFUSE_TRANS";
    public static final String REFUSE_COMMIT = "REFUSE_COMMIT";

    public static final String COMMAND_SYNTAX_ERROR = "COMMAND_SYNTAX_ERROR";
    public static final String ACCOUNT_SYNTAX_ERROR = "ACCOUNT_SYNTAX_ERROR";
    public static final String AMOUNT_NOT_INT = "AMOUNT_NOT_INT";
    
    public static final String XID_EXISTS = "XID_EXISTS";
    
    private BizCode(){}
}
