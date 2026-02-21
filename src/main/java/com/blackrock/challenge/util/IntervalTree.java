package com.blackrock.challenge.util;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Generic Interval Tree for O(log n) period range queries.
 *
 * <p>Used for efficient q/p/k period lookups against up to 10^6 transactions.
 * Naïve nested loops would be O(n × m) — this reduces it to O((n + k) log m)
 * where k is the number of matching intervals.
 *
 * <p>Implementation: Augmented BST where each node stores the maximum end
 * timestamp in its subtree, enabling fast pruning.
 *
 * @param <T> the period type (QPeriod, PPeriod, KPeriod)
 */
public class IntervalTree<T> {

    private Node root;

    /**
     * Add an interval with its associated period data.
     * Uses iterative insertion to avoid StackOverflow on degenerate / large inputs.
     */
    public void insert(LocalDateTime start, LocalDateTime end, T data) {
        Node newNode = new Node(start, end, data);
        if (root == null) {
            root = newNode;
            return;
        }

        // Iterative BST insert with max-end propagation
        Deque<Node> path = new ArrayDeque<>();
        Node current = root;
        while (true) {
            // Update maxEnd along the path
            if (end.isAfter(current.maxEnd)) {
                current.maxEnd = end;
            }
            path.push(current);

            boolean goLeft = start.isBefore(current.start)
                    || (start.isEqual(current.start) && end.isBefore(current.end));
            if (goLeft) {
                if (current.left == null) {
                    current.left = newNode;
                    break;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = newNode;
                    break;
                }
                current = current.right;
            }
        }
    }

    /**
     * Query all intervals that contain the given point (inclusive on both ends).
     * Iterative implementation to avoid StackOverflow on large/degenerate trees.
     *
     * @param point the datetime point to query
     * @return list of all matching period data
     */
    public List<T> query(LocalDateTime point) {
        List<T> result = new ArrayList<>();
        if (root == null) return result;

        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (node == null) continue;

            // Prune: if max end in subtree is before point, no match possible
            if (point.isAfter(node.maxEnd)) continue;

            // Check current node
            if (!point.isBefore(node.start) && !point.isAfter(node.end)) {
                result.add(node.data);
            }

            // Push left subtree (always, it may have overlapping intervals)
            if (node.left != null && !point.isAfter(node.left.maxEnd)) {
                stack.push(node.left);
            }

            // Push right subtree only if point >= node.start (BST property)
            if (!point.isBefore(node.start) && node.right != null) {
                stack.push(node.right);
            }
        }
        return result;
    }

    // ─── Private implementation ───────────────────────────────────────────────

    private void query(Node node, LocalDateTime point, List<T> result) {
        if (node == null) {
            return;
        }

        // Prune: if max end in subtree is before point, no match possible
        if (point.isAfter(node.maxEnd)) {
            return;
        }

        // Check left subtree first
        query(node.left, point, result);

        // Check current node
        if (!point.isBefore(node.start) && !point.isAfter(node.end)) {
            result.add(node.data);
        }

        // Only check right subtree if point >= node.start (BST property)
        if (!point.isBefore(node.start)) {
            query(node.right, point, result);
        }
    }

    private class Node {
        LocalDateTime start;
        LocalDateTime end;
        LocalDateTime maxEnd; // max end in entire subtree
        T data;
        Node left;
        Node right;

        Node(LocalDateTime start, LocalDateTime end, T data) {
            this.start = start;
            this.end = end;
            this.maxEnd = end;
            this.data = data;
        }
    }

    /**
     * Returns true if this tree has no intervals.
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Builds an IntervalTree from a list of items using an extractor function.
     * Balances insertion by sorting on start time first.
     */
    public static <T> IntervalTree<T> buildBalanced(
            List<T> items,
            java.util.function.Function<T, LocalDateTime> startExtractor,
            java.util.function.Function<T, LocalDateTime> endExtractor) {

        IntervalTree<T> tree = new IntervalTree<>();
        if (items == null || items.isEmpty()) {
            return tree;
        }

        // Sort by start for a more balanced tree
        List<T> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (a, b) -> startExtractor.apply(a).compareTo(startExtractor.apply(b)));

        // Insert median-first for balance (like a BST from sorted array)
        insertBalanced(tree, sorted, startExtractor, endExtractor, 0, sorted.size() - 1);
        return tree;
    }

    private static <T> void insertBalanced(
            IntervalTree<T> tree,
            List<T> items,
            java.util.function.Function<T, LocalDateTime> startExtractor,
            java.util.function.Function<T, LocalDateTime> endExtractor,
            int lo, int hi) {

        // Iterative median-first insertion using an explicit stack to avoid StackOverflow
        // on large inputs (e.g. 100k intervals). Each stack frame is an [lo, hi] range.
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{lo, hi});

        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int l = range[0];
            int h = range[1];
            if (l > h) continue;

            int mid = l + (h - l) / 2;
            T item = items.get(mid);
            tree.insert(startExtractor.apply(item), endExtractor.apply(item), item);

            stack.push(new int[]{l, mid - 1});
            stack.push(new int[]{mid + 1, h});
        }
    }
}
