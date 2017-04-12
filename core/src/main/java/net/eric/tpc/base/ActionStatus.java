package net.eric.tpc.base;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ActionStatus implements Serializable {

    private static final long serialVersionUID = -8266863958701176496L;

    public static class Codes {
        public static final short OK = 0;
        public static final short PEER_NO_REPLY = 1;
        public static final short INNER_ERROR = 2;
        
        private Codes(){}
    }
    
    public static final ActionStatus OK = ActionStatus.create(Codes.OK, "");
    public static final ActionStatus PEER_NO_REPLY = ActionStatus.create(Codes.PEER_NO_REPLY, "");
    public static final ActionStatus INNER_ERROR = ActionStatus.create(Codes.INNER_ERROR, "");

    public static final ActionStatus innerError(String description){
        return new ActionStatus(Codes.INNER_ERROR, description);
    }
    
    public static final ActionStatus peerNoReplay(InetSocketAddress node){
        return new ActionStatus(Codes.PEER_NO_REPLY, node.toString());
    }

    public static ActionStatus create(short code, String description) {
        return new ActionStatus(code, description);
    }

    public static boolean nullOrFail(ActionStatus r) {
        return r == null || !r.isOK();
    }

    private short code;
    private String description;

    public ActionStatus(short code, String description) {
        super();
        this.code = code;
        this.description = description;
    }

    public short getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOK() {
        return this.code == Codes.OK;
    }

    @Override
    public String toString() {
        return "ActionStatus[" + code + ", " + description + "]";
    }
}
