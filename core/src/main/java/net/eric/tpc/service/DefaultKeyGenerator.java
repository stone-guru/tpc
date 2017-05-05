package net.eric.tpc.service;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;

import net.eric.tpc.base.Pair;
import net.eric.tpc.proto.KeyGenerator;

/**
 * 对KeyGenerator的实现。
 * <p>
 * 产生的键值格式为前缀 + 当日序号 + 日期
 * </p>
 *
 * @author Eric.G
 */
public class DefaultKeyGenerator implements KeyGenerator {

    private static final long SERIAL_MAX = 10000000L;

    private static class KeyHolder {
        public AtomicInteger dateDigit;
        public AtomicInteger serial;
        public String prefix;

        public KeyHolder(String keyName, int dateDigit, int serial) {
            this.prefix = keyName;
            this.dateDigit = new AtomicInteger(dateDigit);
            this.serial = new AtomicInteger(serial);
        }

        public KeyHolder(String keyName) {
            this(keyName, 0, 0);
        }
    }

    private ConcurrentHashMap<String, KeyHolder> generatorMap;

    private KeyPersister keyPersister;

    @Inject
    public DefaultKeyGenerator(KeyPersister persister) {
        assert (keyPersister != null);
        if (generatorMap != null) {
            throw new IllegalStateException("KeyGenerator has been initialized");
        }
        this.keyPersister = persister;
        generatorMap = new ConcurrentHashMap<String, KeyHolder>();
        Map<String, Pair<Integer, Integer>> storedKeys = persister.loadStoredKeys();
        for (String keyName : storedKeys.keySet()) {
            Pair<Integer, Integer> keyPair = storedKeys.get(keyName);
            generatorMap.put(keyName, new KeyHolder(keyName, keyPair.fst(), keyPair.snd()));
        }
    }

    @Override
    public String nextKeyWithName(String keyName) {
        return getNextKey(getOrCreateHolder(keyName));
    }

    @Override
    public long nextKey(String keyName) {
        return getNextValue(getOrCreateHolder(keyName));
    }

    private KeyHolder getOrCreateHolder(String keyName) {
        KeyHolder holder = generatorMap.computeIfAbsent(keyName, k -> new KeyHolder(k));
        return holder;
    }

    private String getNextKey(KeyHolder holder) {
        long v = getNextValue(holder);
        int d = (int) (v / SERIAL_MAX);
        int x = (int) (v % SERIAL_MAX);
        BigInteger a;
        return this.formatId(holder.prefix, d, x);
    }

    private long getNextValue(KeyHolder holder) {
        synchronized (holder) {
            final int currentDateDigit = currentDateDigit();
            final int storedDateDigit = holder.dateDigit.get();
            if (currentDateDigit != storedDateDigit) {
                if (holder.dateDigit.compareAndSet(storedDateDigit, currentDateDigit)) {
                    holder.serial.set(0);
                }
            }
            final int nextSerial = holder.serial.incrementAndGet();

            this.keyPersister.storeKey(holder.prefix, currentDateDigit, nextSerial);

            return currentDateDigit * SERIAL_MAX + nextSerial ;
        }
    }

    private String formatId(String prefix, int dateDigit, int serial) {
        return String.format("%s%04dD%d", prefix, serial, dateDigit);
    }

    private int currentDateDigit() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        //return (year % 100) * 10000 + month * 100 + day;
        return year * 10000 + month * 100 + day;
    }
}
