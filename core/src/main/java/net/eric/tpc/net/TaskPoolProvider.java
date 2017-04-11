package net.eric.tpc.net;

import java.util.concurrent.ExecutorService;

public interface TaskPoolProvider {
    ExecutorService getSequenceTaskPool();
    ExecutorService getCommuTaskPool();
}
