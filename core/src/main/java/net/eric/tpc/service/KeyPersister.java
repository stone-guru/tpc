package net.eric.tpc.service;

import java.util.Map;

import net.eric.tpc.base.Pair;

/**
 * 键存储器接口
 */
public interface KeyPersister {
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