package net.eric.tpc.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NightWatch {
    private static final Logger logger = LoggerFactory.getLogger(NightWatch.class);
    private static final NightWatch instance = new NightWatch();
    
    public static void regCloseAction(Runnable action){
        instance.addCloseAction(action);
    }
    
    public static void executeCloseActions(){
        instance.execute();
    }
    
    private List<Runnable> closeActions = new ArrayList<Runnable>();

    private NightWatch() {
    }

    private void addCloseAction(Runnable action) {
        synchronized (closeActions) {
            closeActions.add(action);
        }
    }

    private void execute(){
        this.execute(this.closeActions.iterator());
    }
    
    private void execute(Iterator<Runnable> it) {
        if (!it.hasNext()) {
            return;
        }
        final Runnable action = it.next();
        execute(it);// 递归执行后面的先
        try {
            action.run();
        } catch (Exception e) {
            logger.error("Terminator execute close action", e);
        }
    }
}
