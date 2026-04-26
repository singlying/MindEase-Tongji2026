package com.mindease.common.util;

import java.util.BitSet;

/**
 * 布隆过滤器工具类（简单实现）
 * 用于判断一个元素是否可能存在于集合中，存在误判率。
 */
public class BloomFilterUtil {

    private final BitSet bitSet;
    private final int bitSize;
    private final int hashCount;

    /**
     * 构造布隆过滤器
     * @param expectedInsertions 预期插入数量
     * @param falsePositiveRate 期望误判率
     */
    public BloomFilterUtil(int expectedInsertions, double falsePositiveRate) {
        this.bitSize = optimalBitSize(expectedInsertions, falsePositiveRate);
        this.hashCount = optimalHashCount(expectedInsertions, bitSize);
        this.bitSet = new BitSet(bitSize);
    }

    private int optimalBitSize(int n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    private int optimalHashCount(int n, int m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    private int[] hash(Object obj, int k) {
        int[] hashes = new int[k];
        int h1 = obj.hashCode();
        int h2 = h1 >>> 16;
        for (int i = 0; i < k; i++) {
            hashes[i] = Math.abs((h1 + i * h2) % bitSize);
        }
        return hashes;
    }

    public void add(Object element) {
        for (int hash : hash(element, hashCount)) {
            bitSet.set(hash);
        }
    }

    public boolean mightContain(Object element) {
        for (int hash : hash(element, hashCount)) {
            if (!bitSet.get(hash)) return false;
        }
        return true;
    }

    // 静态工厂方法，方便使用
    public static BloomFilterUtil create(int expectedInsertions, double falsePositiveRate) {
        return new BloomFilterUtil(expectedInsertions, falsePositiveRate);
    }
}