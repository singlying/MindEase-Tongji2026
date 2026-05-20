package com.mindease.common.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * SimHash 算法，用于文本相似度比较。
 */
public class SimHashUtil {

    private static final int HASH_BITS = 64;
    private static final int HAMMING_DISTANCE_THRESHOLD = 3;

    public static BigInteger simhash(String text) {
        int[] v = new int[HASH_BITS];
        StringTokenizer tokenizer = new StringTokenizer(text);
        Map<String, Integer> wordFreq = new HashMap<>();
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
            BigInteger hash = md5Hash(entry.getKey());
            int weight = entry.getValue();
            for (int i = 0; i < HASH_BITS; i++) {
                BigInteger bitmask = BigInteger.ONE.shiftLeft(i);
                if (hash.and(bitmask).signum() != 0) {
                    v[i] += weight;
                } else {
                    v[i] -= weight;
                }
            }
        }
        BigInteger fingerprint = BigInteger.ZERO;
        for (int i = 0; i < HASH_BITS; i++) {
            if (v[i] > 0) {
                fingerprint = fingerprint.or(BigInteger.ONE.shiftLeft(i));
            }
        }
        return fingerprint;
    }

    private static BigInteger md5Hash(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes());
            return new BigInteger(1, digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int hammingDistance(BigInteger hash1, BigInteger hash2) {
        BigInteger xor = hash1.xor(hash2);
        return xor.bitCount();
    }

    public static boolean isSimilar(BigInteger hash1, BigInteger hash2) {
        return hammingDistance(hash1, hash2) <= HAMMING_DISTANCE_THRESHOLD;
    }
}