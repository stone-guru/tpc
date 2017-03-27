package net.eric.tpc.common;

import java.util.concurrent.Future;

public class ActionResult {

    public static final ActionResult OK = ActionResult.create("OK", "");
    public static final ActionResult PEER_NO_REPLY = ActionResult.create("PEER_NO_REPLY", "");
    public static final ActionResult INNER_ERROR = ActionResult.create("INNER_ERROR", "");

    public static final ActionResult innerError(String description){
        return new ActionResult("INNER_ERROR", description);
    }
    
    public static ActionResult force(Future<ActionResult> future) {
        try {
            ActionResult result = future.get();
            return result;
        } catch (Exception e) {
            return ActionResult.innerError(e.getMessage());
        }
    }
    
    public static ActionResult create(String code, String description) {
        return new ActionResult(code, description);
    }

    public static boolean nullOrFail(ActionResult r) {
        return r == null || !r.isOK();
    }

    private String code;
    private String description;

    public ActionResult(String code, String description) {
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
        return ActionResult.OK.getCode().equals(this.code);
    }

    @Override
    public String toString() {
        return "ErrorMessage [code=" + code + ", description=" + description + "]";
    }
}