package net.eric.tpc.common;

import static net.eric.tpc.base.Pair.asPair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import net.eric.tpc.base.Pair;
import net.eric.tpc.base.UnImplementedException;

abstract public class UniFactory {
    private static final Logger logger = LoggerFactory.getLogger(UniFactory.class);

    private static Map<Class<?>, Object> paramMap = new HashMap<Class<?>, Object>();
    private static Map<Pair<Class<?>, String>, Object> objectMap = new HashMap<Pair<Class<?>, String>, Object>();
    private static List<UniFactory> factories = null;
    private static List<Pair<Integer, Class<?>>> classes = Lists.newArrayList();

    public static <T extends UniFactory> void register(Class<?> factoryClass, int order) {
        //logger.debug("register class " + factoryClass.toString());

        Preconditions.checkArgument(factories == null, "factories initialized already, do reg earlier");
        Preconditions.checkNotNull(factoryClass);
        synchronized (classes) {
            if (factoryRegistered(factoryClass)) {
                throw new IllegalArgumentException(factoryClass.toString() + " registered already");
            }
            classes.add(asPair(order, factoryClass));
        }
    }

    public static <T extends UniFactory> void setParam(Class<?> factoryClass, Object param) {
        Preconditions.checkArgument(factories == null, "factories initialized already, do setParam earlier");
        Preconditions.checkNotNull(factoryClass);
        Preconditions.checkNotNull(param);

        if (!factoryRegistered(factoryClass)) {
            throw new IllegalArgumentException(factoryClass.toString() + " not registered");
        }
        synchronized (paramMap) {
            if (paramMap.containsKey(factoryClass)) {
                if (paramMap.get(factoryClass) != null) {
                    throw new IllegalStateException("param for " + factoryClass.toString() + " has been set");
                }
            }
            paramMap.put(factoryClass, param);
        }
    }

    public static <T> T getObject(Class<T> clz) {
        return (T) getObject(clz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(Class<T> clz, String classifier) {
        Pair<Class<?>, String> key = asPair(clz, classifier);

        synchronized (objectMap) {
            if (!objectMap.containsKey(key)) {
                if (factories == null) {
                    factories = instantiateFactories();
                }
                Object obj = null;
                for (UniFactory factory : factories) {
                    obj = factory.createObject(clz, classifier);
                    if(obj != null){
                        break;
                    }
                }
                if (obj == null) {
                    throw new UnImplementedException("no factory create for " + clz.toString());
                }
                objectMap.put(key, obj);
            }
            return (T) objectMap.get(key);
        }
    }


    public static <T> Optional<T> getObjectMaybe(Class<T> clz, String classifier) {
        final Pair<Class<?>, String> key = asPair(clz, classifier);
        synchronized (objectMap) {
            if (!objectMap.containsKey(key)) {
                return Optional.absent();
            }
            @SuppressWarnings("unchecked")
            final T obj = (T) objectMap.get(key);
            return Optional.of(obj);
        }
    }

    synchronized public static <T> void shutdown() {
        if (factories == null) {
            return;
        }
        for (UniFactory factory : Lists.reverse(factories)) {
            try {
                factory.close();
            } catch (Exception e) {
                logger.error(factory.getClass().toString() + ".close", e);
            }
        }
    }

    private static boolean factoryRegistered(Class<?> clz) {
        for (Pair<Integer, Class<?>> p : classes) {
            if (p.snd().equals(clz)) {
                return true;
            }
        }
        return false;
    }

    private static List<UniFactory> instantiateFactories() {
        @SuppressWarnings("unchecked")
        Pair<Integer, Class<?>>[] cx = classes.toArray(new Pair[classes.size()]);
        Arrays.sort(cx, Pair.fstPartComparator());

        factories = Lists.newArrayList();
        for (Pair<Integer, Class<?>> clz : cx) {
            try {
                UniFactory factory = (UniFactory) clz.snd().newInstance();
                Object param = paramMap.get(clz.snd());
                factory.init(param);// null is ok here
                factories.add(factory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return factories;
    }

    abstract protected <T> T createObject(Class<T> clz, String classifier);

    protected void init(Object param) {
    }

    protected void close() {
    }
}
