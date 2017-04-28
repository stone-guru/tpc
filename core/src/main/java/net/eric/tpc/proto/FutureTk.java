package net.eric.tpc.proto;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import net.eric.tpc.base.ActionStatus;

public class FutureTk {
    private static final Logger logger = LoggerFactory.getLogger(FutureTk.class);

    public static ActionStatus force(Future<ActionStatus> future) {
        Preconditions.checkNotNull(future);
        try {
            ActionStatus result = future.get();
            return result;
        } catch (Exception e) {
            logger.error("forece Future error", e);
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
            logger.error("Waiting RoundResult error", e);
            return ActionStatus.innerError(e.getMessage());
        }
    }

    public static Callable<ActionStatus> tryFunction(Callable<ActionStatus> f) {
        return () -> {
            try {
                return f.call();
            } catch (Exception e) {
                logger.error("submit function ", e);
                return ActionStatus.innerError(e.getMessage());
            }
        };
    }

    public static ExecutorService newCachedThreadPool() {
        // ThreadPoolExecutor e = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
        // 2L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        // e.allowCoreThreadTimeOut(true);
        return Executors.newFixedThreadPool(3);
    }

}
