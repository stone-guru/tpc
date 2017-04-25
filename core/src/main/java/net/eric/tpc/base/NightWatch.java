package net.eric.tpc.base;

import static net.eric.tpc.base.Pair.asPair;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NightWatch {
    private static final Logger logger = LoggerFactory.getLogger(NightWatch.class);
    private static final NightWatch instance = new NightWatch();

    public static void regCloseable(Closeable closeable) {
        regCloseable("No name", closeable);
    }

    public static void regCloseable(String actionName, Closeable closeable) {
        instance.addCloseAction(actionName, () -> {
            try {
                closeable.close();
            } catch (IOException e) {
                logger.error("exception when close ", e);
            }
        });
    }

    public static void regCloseAction(Runnable action) {
        instance.addCloseAction("No name", action);
    }

    public static void regCloseAction(String actionName, Runnable action) {
        instance.addCloseAction(actionName, action);
    }

    public static void executeCloseActions() {
        instance.execute();
    }

    private List<Pair<String, Runnable>> closeActions = new ArrayList<Pair<String, Runnable>>();

    private NightWatch() {
    }

    private void addCloseAction(String actionName, Runnable action) {
        synchronized (closeActions) {
            closeActions.add(asPair(actionName, action));
        }
    }

    private void execute() {
        this.execute(this.closeActions.iterator());
    }

    private void execute(Iterator<Pair<String, Runnable>> it) {
        if (!it.hasNext()) {
            return;
        }
        final Pair<String, Runnable> p = it.next();
        execute(it);// 递归执行后面的先
        try {
            logger.info("close backend service " + p.fst());
            p.snd().run();
        } catch (Exception e) {
            logger.error("Terminator execute close action", e);
        }
    }
}
