package com.mindease.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> {

    private final LinkedHashMap<K, V> map;
    private final int maxCapacity;

    public LRUCache(final int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.map = new LinkedHashMap<K, V>(maxCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized void clear() {
        map.clear();
    }
}