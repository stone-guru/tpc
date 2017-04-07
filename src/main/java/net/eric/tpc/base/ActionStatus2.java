package net.eric.tpc.base;

import java.io.Serializable;
import java.util.concurrent.Future;

public class ActionStatus2 implements Serializable {

    private static final long serialVersionUID = -5309876481730374690L;
    
    public static class Codes {
        public static final short OK = 0;
        public static final short PEER_NO_REPLY = 1;
        public static final short INNER_ERROR = 2;
        private Codes(){}
    }
    
    public static final ActionStatus2 OK = ActionStatus2.create(Codes.OK, "");
    public static final ActionStatus2 PEER_NO_REPLY = ActionStatus2.create(Codes.PEER_NO_REPLY, "");
    public static final ActionStatus2 INNER_ERROR = ActionStatus2.create(Codes.INNER_ERROR, "");

    public static final ActionStatus2 innerError(String description){
        return new ActionStatus2(Codes.INNER_ERROR, description);
    }
    
    public static final ActionStatus2 peerNoReplay(Node node){
        return new ActionStatus2(Codes.PEER_NO_REPLY, node.toString());
    }
    
    public static ActionStatus2 force(Future<ActionStatus2> future) {
        try {
            ActionStatus2 result = future.get();
            return result;
        } catch (Exception e) {
            return ActionStatus2.innerError(e.getMessage());
        }
    }
    
    public static ActionStatus2 create(short code, String description) {
        return new ActionStatus2(code, description);
    }

    public static boolean nullOrFail(ActionStatus2 r) {
        return r == null || !r.isOK();
    }

    private short code;
    private String description;

    public ActionStatus2(short code, String description) {
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
        return "ErrorMessage [code=" + code + ", description=" + description + "]";
    }
}
