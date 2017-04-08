package net.eric.bank.biz;

public class BizCode {
    public static final short MISS_FIELD = 2000;
    public static final short NO_BANK_NODE = 2001;
    public static final short TIME_IS_FUTURE = 2002;
    public static final short AMOUNT_LE_ZERO = 2003;
    public static final short AMOUNT_FMT_WRONG = 2004;
    public static final short SAME_ACCOUNT = 2005;
    public static final short NODE_UNREACH = 2006;
    public static final short INNER_EXCEPTION = 2007;
    public static final short ACCOUNT_NOT_EXISTS = 2008;
    public static final short NOT_MY_ACCOUNT = 2009;
    public static final short ACCOUNT_LOCKED = 2010;
    public static final short INSUFFICIENT_BALANCE = 2011;
    public static final short BEYOND_OVERDRAFT_LIMIT = 2012;
    public static final short FIELD_TOO_LONG = 2013;
    public static final short AMOUNT_PRECISION_ERROR = 2014;
    
    public static final short BEYOND_TRANS_AMOUNT_LIMIT = 2015;


    public static final short COMMAND_SYNTAX_ERROR = 2018;
    public static final short ACCOUNT_SYNTAX_ERROR = 2019;
    public static final short AMOUNT_NOT_INT = 2020;
    
    private BizCode(){}
}
