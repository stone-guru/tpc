package net.eric.tpc.proto;

/**
 * 不可重复的键值生成器
 * 
 * @author Eric.G
 *
 */
public interface KeyGenerator {

    /**
     * 下一个键值。须保证正真的全局唯一，即使在系统重启后也不应出现重复。
     * 
     * @param keyName 键值名称 not empty
     * @return 键值 not empty result.length <= 24
     */
    String nextKey(String keyName);
}
