package net.eric.tpc.base;

import static net.eric.tpc.base.Pair.asPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

abstract public class UniFactory {
    private static final Logger logger = LoggerFactory.getLogger(UniFactory.class);
    private static List<UniFactory> factories = new ArrayList<UniFactory>();
    private static final int MAX_NEST_DEPTH = 100;

    public static void register(UniFactory factory) {
        logger.debug("register factory " + factory.getClass().toString());
        Preconditions.checkNotNull(factory);

        synchronized (factories) {
            for(UniFactory f : factories){
                if(f.getClass().equals(factory.getClass())){
                    throw new IllegalArgumentException("Factory with same class exists already " + factory.getClass().getCanonicalName());
                }
            }
            factories.add(factory);
        }
    }

    public static <T> T getObject(Class<T> clz) {
        return (T) getObject(clz, null);
    }

    public static <T> T getObject(Class<T> clz, Object classifier) {
        try {
            synchronized (factories) {
                for (UniFactory f : factories) {
                    Optional<T> object = f.getObjectMaybe(clz, classifier);
                    if (object.isPresent()) {
                        return object.get();
                    }
                }
            }
        } catch (ObjectRequirementDeadLockException e) {
            throw new RuntimeException("Dead locked when create " + clz.toString());
        }
        throw new UnImplementedException("no factory create for " + clz.toString());
    }

    private static class ObjectRequirementDeadLockException extends RuntimeException {

        private static final long serialVersionUID = 1L;

    }

    private Map<Pair<Class<?>, Object>, Object> objectMap = new HashMap<Pair<Class<?>, Object>, Object>();
    private int nestDepth = 0;

    public <T> Optional<T> getObjectMaybe(Class<T> clz, Object classifier) {
        this.nestDepth += 1;
        if (this.nestDepth > MAX_NEST_DEPTH) {
            throw new ObjectRequirementDeadLockException();
        }
        try {
            final Pair<Class<?>, Object> key = asPair(clz, classifier);
            synchronized (objectMap) {
                if (objectMap.containsKey(key)) {
                    @SuppressWarnings("unchecked")
                    T t =   (T) objectMap.get(key);
                    return Optional.of(t);
                }
                Optional<Pair<T, Boolean>> opt = this.createObject(clz, classifier);
                if (opt.isPresent()) {
                    final T object = opt.get().fst();
                    final boolean asSingleton = opt.get().snd();
                    if (asSingleton) {
                        objectMap.put(key, object);
                    }
                    return Optional.of(object);
                }
                return Optional.absent();
            }
        } finally {
            this.nestDepth -= 1;
        }
    }

    abstract protected <T> Optional<Pair<T, Boolean>> createObject(Class<T> clz, Object classifier);

    protected void init(Object param) {
    }
}
