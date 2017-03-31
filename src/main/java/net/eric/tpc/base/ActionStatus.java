package net.eric.tpc.base;

import java.io.Serializable;
import java.util.concurrent.Future;

public class ActionStatus implements Serializable {

    private static final long serialVersionUID = -5309876481730374690L;
    
    public static final ActionStatus OK = ActionStatus.create("OK", "");
    public static final ActionStatus PEER_NO_REPLY = ActionStatus.create("PEER_NO_REPLY", "");
    public static final ActionStatus INNER_ERROR = ActionStatus.create("INNER_ERROR", "");

    public static final ActionStatus innerError(String description){
        return new ActionStatus("INNER_ERROR", description);
    }
    
    public static ActionStatus force(Future<ActionStatus> future) {
        try {
            ActionStatus result = future.get();
            return result;
        } catch (Exception e) {
            return ActionStatus.innerError(e.getMessage());
        }
    }
    
    public static ActionStatus create(String code, String description) {
        return new ActionStatus(code, description);
    }

    public static boolean nullOrFail(ActionStatus r) {
        return r == null || !r.isOK();
    }

    private String code;
    private String description;

    public ActionStatus(String code, String description) {
        super();
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOK() {
        return ActionStatus.OK.getCode().equals(this.code);
    }

    @Override
    public String toString() {
        return "ErrorMessage [code=" + code + ", description=" + description + "]";
    }
}
