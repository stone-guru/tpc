package net.eric.tpc.net;

import java.io.Serializable;
import java.util.Objects;

import net.eric.tpc.base.ActionStatus;
import net.eric.tpc.base.Maybe;

public class DataPacket implements Serializable {

    private static final long serialVersionUID = 6971835564417011087L;

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

    public static DataPacket fromActionStatus(String code, ActionStatus status) {
        if (status.isOK()) {
            return new DataPacket(code, DataPacket.YES);
        }
        return new DataPacket(code, DataPacket.NO, status);
    }

    public static Maybe<Boolean> checkYesOrNo(Object s) {
        if (DataPacket.YES.equals(s)) {
            return Maybe.success(true);
        } else if (DataPacket.NO.equals(s)) {
            return Maybe.success(false);
        } else {
            return Maybe.fail(DataPacket.PEER_PRTC_ERROR,
                    "want " + DataPacket.YES + " or " + DataPacket.NO + ", but got " + s);
        }
    }

    public static DataPacket readOnlyPacket(String code, Object param1, Object param2) {
        return new DataPacket(code, param1, param2) {

            private static final long serialVersionUID = -6995521228594324183L;

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

            @Override
            public void setParam3(Object param3) {
                throw new UnsupportedOperationException("param3 of readonlyPacket cannot be changed");
            }
        };
    }

    private String code;
    private Object param1;
    private Object param2;
    private Object param3;

    public static final String PEER_PRTC_ERROR = "PEER_PRTC_ERROR";

    public DataPacket(String code) {
        this(code, "", "", "");
    }

    public DataPacket(String code, Object param1) {
        this(code, param1, "", "");
    }

    public DataPacket(String code, Object param1, Object param2) {
        this(code, param1, param2, "");
    }

    public DataPacket(String code, Object param1, Object param2, Object param3) {
        this.code = code;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
    }

    public DataPacket() {
    }

    public ActionStatus assureParam1(String code, String param1) {
        if (!Objects.equals(code, this.code)) {
            return ActionStatus.create(DataPacket.WRONG_ANSWER, "want " + code + " but got " + this.code);
        }
        if (!Objects.equals(this.param1, param1)) {
            return ActionStatus.create(DataPacket.WRONG_ANSWER, "want " + param1 + " but got " + this.param1);
        }
        return ActionStatus.OK;
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

    public Object getParam3() {
        return param3;
    }

    public void setParam3(Object param3) {
        this.param3 = param3;
    }

    @Override
    public String toString() {
        return "DataPacket [" + code + ", " + param1 + ", " + param2 + ", " + param3 + "]";
    }
}
