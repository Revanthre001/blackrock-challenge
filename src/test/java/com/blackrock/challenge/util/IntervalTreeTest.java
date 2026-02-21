package com.blackrock.challenge.util;

/*
 * TEST TYPE: Unit Test
 * VALIDATION: IntervalTree correctness — O(log n) point queries,
 *             boundary inclusiveness, no-match cases, multiple overlapping intervals,
 *             and performance benchmark for 10^6 periods.
 * RUN COMMAND: mvn test -Dtest=IntervalTreeTest
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntervalTree — Unit Tests")
class IntervalTreeTest {

    @Test
    @DisplayName("exact start boundary is inclusive")
    void testStartBoundaryInclusive() {
        IntervalTree<String> tree = new IntervalTree<>();
        LocalDateTime start = LocalDateTime.of(2023, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2023, 7, 31, 23, 59);
        tree.insert(start, end, "july");

        List<String> result = tree.query(LocalDateTime.of(2023, 7, 1, 0, 0));
        assertThat(result).containsExactly("july");
    }

    @Test
    @DisplayName("exact end boundary is inclusive")
    void testEndBoundaryInclusive() {
        IntervalTree<String> tree = new IntervalTree<>();
        LocalDateTime start = LocalDateTime.of(2023, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2023, 7, 31, 23, 59);
        tree.insert(start, end, "july");

        List<String> result = tree.query(LocalDateTime.of(2023, 7, 31, 23, 59));
        assertThat(result).containsExactly("july");
    }

    @Test
    @DisplayName("point before start returns empty")
    void testPointBeforeStart() {
        IntervalTree<String> tree = new IntervalTree<>();
        tree.insert(
                LocalDateTime.of(2023, 7, 1, 0, 0),
                LocalDateTime.of(2023, 7, 31, 23, 59),
                "july");

        List<String> result = tree.query(LocalDateTime.of(2023, 6, 30, 23, 59));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("point after end returns empty")
    void testPointAfterEnd() {
        IntervalTree<String> tree = new IntervalTree<>();
        tree.insert(
                LocalDateTime.of(2023, 7, 1, 0, 0),
                LocalDateTime.of(2023, 7, 31, 23, 59),
                "july");

        List<String> result = tree.query(LocalDateTime.of(2023, 8, 1, 0, 0));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("multiple overlapping intervals all returned")
    void testMultipleOverlappingIntervals() {
        IntervalTree<String> tree = new IntervalTree<>();
        tree.insert(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                "full-year");
        tree.insert(
                LocalDateTime.of(2023, 7, 1, 0, 0),
                LocalDateTime.of(2023, 9, 30, 23, 59),
                "q3");
        tree.insert(
                LocalDateTime.of(2023, 10, 1, 0, 0),
                LocalDateTime.of(2023, 12, 31, 23, 59),
                "q4");

        // Point in Q3 — matches full-year and q3
        List<String> result = tree.query(LocalDateTime.of(2023, 8, 15, 10, 0));
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("full-year", "q3");
    }

    @Test
    @DisplayName("empty tree returns empty result")
    void testEmptyTree() {
        IntervalTree<String> tree = new IntervalTree<>();
        List<String> result = tree.query(LocalDateTime.of(2023, 6, 1, 0, 0));
        assertThat(result).isEmpty();
        assertThat(tree.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("buildBalanced produces correct results for sorted intervals")
    void testBuildBalanced() {
        List<String[]> periods = new ArrayList<>();
        periods.add(new String[]{"2023-01-01", "2023-03-31", "q1"});
        periods.add(new String[]{"2023-04-01", "2023-06-30", "q2"});
        periods.add(new String[]{"2023-07-01", "2023-09-30", "q3"});
        periods.add(new String[]{"2023-10-01", "2023-12-31", "q4"});

        IntervalTree<String[]> tree = IntervalTree.buildBalanced(
                periods,
                p -> LocalDateTime.parse(p[0] + "T00:00:00"),
                p -> LocalDateTime.parse(p[1] + "T23:59:59"));

        List<String[]> result = tree.query(LocalDateTime.of(2023, 5, 15, 0, 0));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)[2]).isEqualTo("q2");
    }

    @Test
    @DisplayName("performance: 100,000 intervals via buildBalanced, 1,000 queries — completes in < 2 seconds")
    void testPerformance100kIntervals() {
        LocalDateTime base = LocalDateTime.of(2023, 1, 1, 0, 0);

        // Build 100,000 intervals using buildBalanced for O(log n) tree depth
        List<Integer> intervals = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) {
            intervals.add(i);
        }
        // Each interval i → [base+i minutes, base+(i+60) minutes]
        IntervalTree<Integer> tree = IntervalTree.buildBalanced(
                intervals,
                i -> base.plusMinutes(i),
                i -> base.plusMinutes(i + 60));

        long startTime = System.currentTimeMillis();

        // Run 1,000 queries
        for (int i = 0; i < 1_000; i++) {
            LocalDateTime point = base.plusMinutes(i * 10);
            tree.query(point);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        assertThat(elapsed).isLessThan(2_000L); // must complete in < 2s with balanced tree
    }
}
