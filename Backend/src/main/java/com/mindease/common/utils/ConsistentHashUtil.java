package com.mindease.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希工具类，用于分布式缓存或负载均衡场景。
 */

public class ConsistentHashUtil {

    private final SortedMap<Long, String> hashRing = new TreeMap<>();
    private final int virtualNodeCount;

    public ConsistentHashUtil(int virtualNodeCount) {
        this.virtualNodeCount = virtualNodeCount;
    }

    public void addNode(String node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            long hash = hash(node + "#" + i);
            hashRing.put(hash, node);
        }
    }

    public void removeNode(String node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            long hash = hash(node + "#" + i);
            hashRing.remove(hash);
        }
    }

    public String getNode(String key) {
        if (hashRing.isEmpty()) return null;
        long hash = hash(key);
        SortedMap<Long, String> tailMap = hashRing.tailMap(hash);
        Long nodeHash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
        return hashRing.get(nodeHash);
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long hash = ((long) (digest[3] & 0xFF) << 24) |
                    ((long) (digest[2] & 0xFF) << 16) |
                    ((long) (digest[1] & 0xFF) << 8) |
                    ((long) (digest[0] & 0xFF));
            return hash & 0xffffffffL;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }
}