package net.eric.tpc.proto;

import java.util.concurrent.Future;

import net.eric.tpc.base.ActionStatus;

public class FutureTk {
    
    public static ActionStatus statusForce(Future<ActionStatus> future) {
        try {
            ActionStatus result = future.get();
            return result;
        } catch (Exception e) {
            return ActionStatus.innerError(e.getMessage());
        }
    }
    
    
    public static <T> ActionStatus resultForce(Future<RoundResult<T>> future) {
        try {
            RoundResult<T> result = future.get();
            if (result.isAllOK()) {
                return ActionStatus.OK;
            } else {
                return result.getAnError();
            }
        } catch (Exception e) {
            return ActionStatus.innerError(e.getMessage());
        }
    }

}
