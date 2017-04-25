package net.eric.tpc.proto;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

public final class Types {
    public static final class ErrorCode {
        public static final short XID_EXISTS = 1001;
        public static final short PEER_PRTC_ERROR = 1002;
        public static final short BAD_DATA_PACKET = 1003;
        public static final short NO_RESPONSE = 1004;
        public static final short WRONG_ANSWER = 1005;
        public static final short SEND_ERROR = 1006;
        public static final short PEER_NOT_CONNECTED = 1007;
        public static final short REFUSE_TRANS = 1008;
        public static final short REFUSE_COMMIT = 1009;
        public static final short PEER_NO_REPLY = 1010;
        public static final short CONNECTION_CLOSED = 1011;
        private ErrorCode() {
        }
    }

    public static enum Vote {
        YES("YES"), NO("NO");

        public static Vote fromCode(String code) {
            for (Vote v : Vote.values()) {
                if (v.code().equalsIgnoreCase(code)) {
                    return v;
                }
            }
            return null;
        }

        private Vote(String s) {
            this.code = s;
        }

        public String code() {
            return this.code;
        }

        private String code;
    }

    public static enum Decision {
        COMMIT("COMMIT"), ABORT("ABORT");

        public static Decision fromCode(String code) {
            for (Decision v : Decision.values()) {
                if (v.code().equalsIgnoreCase(code)) {
                    return v;
                }
            }
            return null;
        }

        private Decision(String s) {
            this.code = s;
        }

        public String code() {
            return this.code;
        }

        private String code;
    }


    public static class TransStartRec implements Serializable {

        private static final long serialVersionUID = -2699980236712941307L;

        private long xid;
        private InetSocketAddress coordinator;
        private List<InetSocketAddress> participants = Collections.emptyList();

        public TransStartRec() {
        }

        public TransStartRec(long xid, InetSocketAddress coordinator, List<InetSocketAddress> participants) {
            super();
            this.xid = xid;
            this.coordinator = coordinator;
            this.participants = participants;
        }

        public long xid() {
            return xid;
        }

        public long getXid() {
            return xid;
        }

        public void setXid(long xid) {
            this.xid = xid;
        }

        public InetSocketAddress getCoordinator() {
            return coordinator;
        }

        public void setCoordinator(InetSocketAddress coordinator) {
            this.coordinator = coordinator;
        }

        public List<InetSocketAddress> getParticipants() {
            return participants;
        }

        public void setParticipants(List<InetSocketAddress> participants) {
            this.participants = participants;
        }

        @Override
        public String toString() {
            return "TransStartRec [xid=" + xid + ", coordinator=" + coordinator + ", participants=" + participants
                    + "]";
        }
    }

    private Types() {
    }
}
