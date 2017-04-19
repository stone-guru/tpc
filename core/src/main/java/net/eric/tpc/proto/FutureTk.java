package net.eric.tpc.proto;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.eric.tpc.base.ActionStatus;

public class FutureTk {
    
    public static ActionStatus force(Future<ActionStatus> future) {
        try {
            ActionStatus result = future.get();
            return result;
        } catch (Exception e) {
            return ActionStatus.innerError(e.getMessage());
        }
    }
    
    
    public static <T> ActionStatus waiting(Future<RoundResult<T>> future, long millis) {
        try {
            RoundResult<T> result = future.get(millis, TimeUnit.MILLISECONDS);
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
