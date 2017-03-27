package net.eric.tpc.net;

import java.io.Serializable;

public class DataPacket implements Serializable {

    private static final long serialVersionUID = 6971835564417011087L;

    public static DataPacket readOnlyPacket(String code, Object param1, Object param2) {
        return new DataPacket(code, param1, param2) {
            @Override
            public void setCode(String s) {
                throw new UnsupportedOperationException("code of readonlyPacket cannot be changed");
            }

            @Override
            public void setParam1(Object content) {
                throw new UnsupportedOperationException("param1 of readonlyPacket cannot be changed");
            }
            
            @Override
            public void setParam2(Object param2) {
                throw new UnsupportedOperationException("param2 of readonlyPacket cannot be changed");
            }
        };
    }

    
    public static final String HEART_BEAT = "HEART_BEAT";
    public static final String HEART_BEAT_ANSWER = "HEART_BEAT_ANSWER";

    public static final String BEGIN_TRANS = "BEGIN_TRANS";
    public static final String BEGIN_TRANS_ANSWER = "BEGIN_TRANS_ANSWER";

    public static final String VOTE_REQ = "VOTE_REQ";
    public static final String VOTE_ANSWER = "VOTE_ANSWER";
    
    public static final String TRANS_DECISION = "TRANS_DECISION";

    public static final String BIZ_REQUEST = "BIZ_REQUEST";
    public static final String BIZ_ACCEPT = "BIZ_ACCEPT";

    public static final String BAD_COMMNAD = "BAD_COMMAND";

    public static final String ERROR = "ERROR";
    
    public static final String YES = "YES";
    public static final String NO = "NO";

    private String code;
    private Object param1;
    private Object param2;

    public DataPacket(String code, Object param1) {
        this(code, param1, "");
    }

    public DataPacket(String code, Object param1, Object param2) {
        this.code = code;
        this.param1 = param1;
        this.param2 = param2;
    }

    public DataPacket(String code) {
        this(code, "", "");

    }

    public DataPacket() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getParam1() {
        return param1;
    }

    public void setParam1(Object param1) {
        this.param1 = param1;
    }

    
    public Object getParam2() {
        return param2;
    }

    public void setParam2(Object param2) {
        this.param2 = param2;
    }

    @Override
    public String toString() {
        return "DataPacket [" + code + ", " + param1 + ", " + param2 + "]";
    }
}
