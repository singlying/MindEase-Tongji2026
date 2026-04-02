package com.mindease.common.util;

import java.util.*;

public class AhoCorasickUtil {

    private final TrieNode root = new TrieNode();

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        TrieNode fail = null;
        Set<String> outputs = new HashSet<>();
    }

    public void addPattern(String pattern) {
        TrieNode node = root;
        for (char c : pattern.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.outputs.add(pattern);
    }

    public void build() {
        Queue<TrieNode> queue = new LinkedList<>();
        for (Map.Entry<Character, TrieNode> entry : root.children.entrySet()) {
            TrieNode child = entry.getValue();
            child.fail = root;
            queue.offer(child);
        }
        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();
            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char ch = entry.getKey();
                TrieNode child = entry.getValue();
                TrieNode failNode = current.fail;
                while (failNode != null && !failNode.children.containsKey(ch)) {
                    failNode = failNode.fail;
                }
                child.fail = (failNode == null) ? root : failNode.children.get(ch);
                child.outputs.addAll(child.fail.outputs);
                queue.offer(child);
            }
        }
    }

    public Map<String, List<Integer>> search(String text) {
        Map<String, List<Integer>> result = new HashMap<>();
        TrieNode node = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (node != root && !node.children.containsKey(c)) {
                node = node.fail;
            }
            node = node.children.getOrDefault(c, root);
            for (String pattern : node.outputs) {
                int start = i - pattern.length() + 1;
                result.computeIfAbsent(pattern, k -> new ArrayList<>()).add(start);
            }
        }
        return result;
    }
}
