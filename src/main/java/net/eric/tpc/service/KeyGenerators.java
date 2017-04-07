package net.eric.tpc.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.eric.tpc.base.Pair;
import net.eric.tpc.proto.KeyGenerator;

/**
 * 对KeyGenerator的实现。
 * <p>
 * 产生的键值格式为前缀 + 当日序号 + 日期
 * </p>
 * 
 * @author Eric.G
 *
 */
public class KeyGenerators {

    /**
     * 键存储器接口
     */
    public static interface KeyPersister {
        /**
         * 加载存储的所有key值
         * 
         * @return Map&lt;keyName&gt;, Pair&lt;dateDigit, serial&gt;&gt; not
         *         null
         */
        Map<String, Pair<Integer, Integer>> loadStoredKeys();

        /**
         * 存储key值
         * <p>
         * <em>ATTENTION</em> 在并发环境下，调用方不能保证小的序号一定先于大的序号，实现方须自行处理此情况
         * </p>
         * 
         * @param keyName
         * @param dateDigit
         * @param serial
         */
        void storeKey(String keyName, int dateDigit, int serial);
    }

    private static ConcurrentHashMap<String, KeyGenerators> generatorMap;
    private static KeyPersister keyPersister;
    public static KeyGenerator instance = new KeyGenerator() {
        @Override
        public String nextKeyAsString(String keyName) {
            return KeyGenerators.nextKey(keyName);
        }

        @Override
        public long nextKey(String keyName) {
            return KeyGenerators.nextValue(keyName);
        }
    };

    public static void init() {
        KeyPersister dummyPersister = new KeyPersister() {
            public Map<String, Pair<Integer, Integer>> loadStoredKeys() {
                return Collections.emptyMap();
            }

            public void storeKey(String keyName, int dateDigit, int serial) {
            }
        };
        KeyGenerators.init(dummyPersister);
    }

    synchronized public static void init(KeyPersister persister) {
        assert (keyPersister != null);
        if (generatorMap != null) {
            throw new IllegalStateException("KeyGenerator has been initialized");
        }
        KeyGenerators.keyPersister = persister;
        generatorMap = new ConcurrentHashMap<String, KeyGenerators>();
        Map<String, Pair<Integer, Integer>> storedKeys = persister.loadStoredKeys();
        for (String keyName : storedKeys.keySet()) {
            Pair<Integer, Integer> keyPair = storedKeys.get(keyName);
            generatorMap.put(keyName, new KeyGenerators(keyName, keyPair.fst(), keyPair.snd()));
        }
    }

    public static KeyGenerator getInstance() {
        return KeyGenerators.instance;
    }

    public static String nextKey(String keyName) {
        return getOrCreateGenerator(keyName).getNextKey();
    }

    public static long nextValue(String keyName){
        return getOrCreateGenerator(keyName).getNextValue();
    }
    
    private static KeyGenerators getOrCreateGenerator(String keyName){
        if (generatorMap == null || keyPersister == null) {
            throw new IllegalStateException("not initialized, call KeyGenerator.init first");
        }
        assert (keyName != null);
        if (!generatorMap.containsKey(keyName)) {
            generatorMap.putIfAbsent(keyName, new KeyGenerators(keyName));
        }
        return generatorMap.get(keyName);
    }
    
    private AtomicInteger dateDigit;
    private AtomicInteger serial;

    private String prefix;

    private KeyGenerators(String keyName) {
        this.prefix = keyName;
        this.dateDigit = new AtomicInteger(this.currentDateDigit());
        this.serial = new AtomicInteger(0);
    }

    private KeyGenerators(String keyName, int dateDigit, int serial) {
        this.prefix = keyName;
        this.dateDigit = new AtomicInteger(dateDigit);
        this.serial = new AtomicInteger(serial);
    }

    private String getNextKey() {
        long v = getNextValue();
        int d = (int) (v % 100000000);
        int x = (int) (v / 100000000);
        return this.formatId(this.prefix, d, x);
    }
    
    private long getNextValue(){
        final int currentDateDigit = currentDateDigit();
        final int storedDateDigit = this.dateDigit.get();
        if (currentDateDigit != storedDateDigit) {
            if (this.dateDigit.compareAndSet(storedDateDigit, currentDateDigit)) {
                this.serial.set(0);
            }
        }
        final int nextSerial = this.serial.incrementAndGet();
        
        KeyGenerators.keyPersister.storeKey(this.prefix, currentDateDigit, nextSerial);
        return nextSerial * 100000000 + currentDateDigit;
    }
    
    private String formatId(String prefix, int dateDigit, int serial) {
        return String.format("%s%04dD%d", prefix, serial, dateDigit);
    }

    protected int currentDateDigit() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return year * 10000 + month * 100 + day;
    }

}
