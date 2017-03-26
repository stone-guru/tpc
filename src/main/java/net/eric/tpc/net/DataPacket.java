package net.eric.tpc.net;

public class DataPacket {

    public static DataPacket readOnlyPacket(String code, String param1) {
        return new DataPacket(code, param1) {
            @Override
            public void setCode(String s) {
                throw new UnsupportedOperationException("code of readonlyPacket cannot be changed");
            }

            @Override
            public void setParam1(String content) {
                throw new UnsupportedOperationException("param1 of readonlyPacket cannot be changed");
            }
            
            @Override
            public void setParam2(String param2) {
                throw new UnsupportedOperationException("param2 of readonlyPacket cannot be changed");
            }
        };
    }

    
    public static final String HEART_BEAT = "HEART_BEAT";
    public static final String HEART_BEAT_ANSWER = "HEART_BEAT_ANSWER";

    public static final String BEGIN_TRANS = "BEGIN_TRANS";
    public static final String BEGIN_TRANS_ANSWER = "BEGIN_TRANS_ANSWER";

    public static final String TRANS_DECISION = "TRANS_DECISION";

    public static final String BIZ_REQUEST = "BIZ_REQUEST";
    public static final String BIZ_ACCEPT = "BIZ_ACCEPT";

    public static final String BAD_COMMNAD = "BAD_COMMAND";

    public static final String ERROR = "ERROR";
    
    public static final String YES = "YES";
    public static final String NO = "NO";

    private String code;
    private String param1;
    private String param2;

    public DataPacket(String code, String param1) {
        this(code, param1, "");
    }

    public DataPacket(String code, String param1, String param2) {
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

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    
    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    @Override
    public String toString() {
        return "DataPacket [" + code + ", " + param1 + ", " + param2 + "]";
    }
}
