package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.util.DateTimeUtil;
import com.blackrock.challenge.util.IntervalTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core service for all transaction operations:
 * - Parse: ceiling and remanent calculation
 * - Validate: negative and duplicate detection
 * - Filter: q/p/k period application with interval tree lookups (O(log n))
 *
 * <p>Thread-safe by design — no shared mutable state in processing paths.
 * Parallel streams are used for large batches (> 10_000 transactions).
 */
@Service
@Slf4j
public class TransactionService {

    private static final int ROUNDING_MULTIPLE = 100;
    private static final int PARALLEL_THRESHOLD = 10_000;

    // ─── Parse ────────────────────────────────────────────────────────────────

    /**
     * Enriches a list of raw expenses with ceiling and remanent fields.
     * Uses parallel streams for large batches for maximum throughput.
     *
     * @param transactions raw expense list
     * @return enriched transaction list
     */
    public List<TransactionDto> parse(List<TransactionDto> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Parsing {} transactions", transactions.size());

        // Use parallel streams for large batches
        if (transactions.size() > PARALLEL_THRESHOLD) {
            return transactions.parallelStream()
                    .map(this::enrichTransaction)
                    .collect(Collectors.toList());
        }

        return transactions.stream()
                .map(this::enrichTransaction)
                .collect(Collectors.toList());
    }

    /**
     * Calculates ceiling (next multiple of 100) and remanent (ceiling - amount).
     * Also validates the date format; sets {@code message} on the returned DTO if invalid.
     * Special case: if amount is already a multiple of 100, ceiling == amount, remanent == 0.
     */
    private TransactionDto enrichTransaction(TransactionDto tx) {
        double amount = tx.getAmount();
        double ceiling = calculateCeiling(amount);
        double remanent = Math.round((ceiling - amount) * 100.0) / 100.0;

        // Validate date format — surfaces as message field (does NOT throw)
        String dateMsg = null;
        try {
            DateTimeUtil.parse(tx.getDate());
        } catch (IllegalArgumentException e) {
            dateMsg = "Invalid date format: '" + tx.getDate() + "'. Expected: YYYY-MM-DD HH:mm:ss";
            log.warn("Transaction has invalid date: {}", tx.getDate());
        }

        return TransactionDto.builder()
                .date(tx.getDate())
                .amount(amount)
                .ceiling(ceiling)
                .remanent(remanent)
                .message(dateMsg)
                .build();
    }

    /**
     * Calculates the next multiple of 100 for a given amount.
     * Example: 250 → 300, 300 → 300, 620 → 700
     */
    public static double calculateCeiling(double amount) {
        if (amount % ROUNDING_MULTIPLE == 0) {
            return amount;
        }
        return Math.ceil(amount / ROUNDING_MULTIPLE) * ROUNDING_MULTIPLE;
    }

    // ─── Validate ─────────────────────────────────────────────────────────────

    /**
     * Validates transactions: rejects negatives and duplicates.
     * Duplicate detection is based on date (exact datetime match).
     * Processing order: negative check first, then duplicate.
     *
     * @param request validator request with wage and transactions
     * @return validation result with valid and invalid lists
     */
    public ValidationResponse validate(ValidatorRequest request) {
        log.info("Validating {} transactions", request.getTransactions().size());
        return splitValidInvalid(request.getTransactions());
    }

    /**
     * Core validation split logic — used by both validator and filter endpoints.
     * Rules applied in order:
     *  0. Invalid date format → invalid
     *  1. Negative amounts    → invalid
     *  2. Duplicate date      → invalid
     */
    public ValidationResponse splitValidInvalid(List<TransactionDto> transactions) {
        List<TransactionDto> valid = new ArrayList<>();
        List<TransactionDto> invalid = new ArrayList<>();
        Set<String> seenDates = new LinkedHashSet<>();

        for (TransactionDto tx : transactions) {
            // Rule 0: invalid date format
            // (already flagged by enrichTransaction; re-check for raw transactions from :validator)
            String existingMsg = tx.getMessage();
            if (existingMsg == null) {
                try {
                    DateTimeUtil.parse(tx.getDate());
                } catch (IllegalArgumentException e) {
                    existingMsg = "Invalid date format: '" + tx.getDate() + "'. Expected: YYYY-MM-DD HH:mm:ss";
                }
            }
            if (existingMsg != null) {
                invalid.add(TransactionDto.builder()
                        .date(tx.getDate())
                        .amount(tx.getAmount())
                        .ceiling(tx.getCeiling())
                        .remanent(tx.getRemanent())
                        .message(existingMsg)
                        .build());
                continue;
            }

            // Rule 1: negative amounts are invalid
            if (tx.getAmount() != null && tx.getAmount() < 0) {
                invalid.add(TransactionDto.builder()
                        .date(tx.getDate())
                        .amount(tx.getAmount())
                        .ceiling(tx.getCeiling())
                        .remanent(tx.getRemanent())
                        .message("Negative amounts are not allowed")
                        .build());
                continue;
            }

            // Rule 2: duplicate dates are invalid
            if (!seenDates.add(tx.getDate())) {
                invalid.add(TransactionDto.builder()
                        .date(tx.getDate())
                        .amount(tx.getAmount())
                        .ceiling(tx.getCeiling())
                        .remanent(tx.getRemanent())
                        .message("Duplicate transaction")
                        .build());
                continue;
            }

            valid.add(tx);
        }

        return ValidationResponse.builder()
                .valid(valid)
                .invalid(invalid)
                .build();
    }

    // ─── Filter ───────────────────────────────────────────────────────────────

    /**
     * Applies temporal constraints (q/p/k periods) to transactions.
     * <p>
     * Processing order per transaction (as per problem statement):
     * 1. Calculate base ceiling and remanent
     * 2. Apply q rule: replace remanent with fixed (latest-start wins; tie-break: first in list)
     * 3. Apply p rule: add all matching p-period extras cumulatively
     * 4. Mark inKPeriod = true if falls in any k period
     * 5. Validate (negatives and duplicates) — invalids are excluded from period processing
     *
     * @param request filter request with all periods and transactions
     * @return filtered and enriched transactions
     */
    public ValidationResponse filter(FilterRequest request) {
        log.info("Filtering {} transactions with q={}, p={}, k={}",
                request.getTransactions().size(),
                request.getQ() == null ? 0 : request.getQ().size(),
                request.getP() == null ? 0 : request.getP().size(),
                request.getK() == null ? 0 : request.getK().size());

        List<TransactionDto> transactions = request.getTransactions();

        // Step 1: Enrich raw transactions with ceiling/remanent if not already set
        List<TransactionDto> enriched = transactions.stream()
                .map(tx -> {
                    if (tx.getCeiling() == null || tx.getRemanent() == null) {
                        return enrichTransaction(tx);
                    }
                    return tx;
                })
                .collect(Collectors.toList());

        // Step 2: Validate (split valid/invalid)
        ValidationResponse validated = splitValidInvalid(enriched);

        // Step 3: Build interval trees for O(log n) period lookups
        List<QPeriod> qPeriods = request.getQ() == null ? Collections.emptyList() : request.getQ();
        List<PPeriod> pPeriods = request.getP() == null ? Collections.emptyList() : request.getP();
        List<KPeriod> kPeriods = request.getK() == null ? Collections.emptyList() : request.getK();

        IntervalTree<IndexedQPeriod> qTree = buildQTree(qPeriods);
        IntervalTree<PPeriod> pTree = buildPTree(pPeriods);
        IntervalTree<KPeriod> kTree = buildKTree(kPeriods);

        // Step 4: Apply q/p/k to valid transactions
        List<TransactionDto> processedValid = applyPeriods(validated.getValid(), qTree, pTree, kTree);

        return ValidationResponse.builder()
                .valid(processedValid)
                .invalid(validated.getInvalid())
                .build();
    }

    /**
     * Applies q/p/k period rules to a list of already-validated transactions.
     * Exposed for reuse by the returns service.
     */
    public List<TransactionDto> applyPeriods(
            List<TransactionDto> validTransactions,
            IntervalTree<IndexedQPeriod> qTree,
            IntervalTree<PPeriod> pTree,
            IntervalTree<KPeriod> kTree) {

        boolean useParallel = validTransactions.size() > PARALLEL_THRESHOLD;

        var stream = useParallel
                ? validTransactions.parallelStream()
                : validTransactions.stream();

        return stream
                .map(tx -> applyPeriodsToTransaction(tx, qTree, pTree, kTree))
                .collect(Collectors.toList());
    }

    private TransactionDto applyPeriodsToTransaction(
            TransactionDto tx,
            IntervalTree<IndexedQPeriod> qTree,
            IntervalTree<PPeriod> pTree,
            IntervalTree<KPeriod> kTree) {

        LocalDateTime txDate = DateTimeUtil.parse(tx.getDate());
        double remanent = tx.getRemanent();

        // Apply q rule: latest start wins; tie-break: first in list (lowest index)
        List<IndexedQPeriod> matchingQ = qTree.query(txDate);
        if (!matchingQ.isEmpty()) {
            IndexedQPeriod chosen = matchingQ.stream()
                    .max(Comparator
                            .comparing((IndexedQPeriod iq) -> DateTimeUtil.parse(iq.period().getStart()))
                            .thenComparing(Comparator.comparingInt(IndexedQPeriod::index).reversed()))
                    .orElseThrow();
            remanent = chosen.period().getFixed();
        }

        // Apply p rule: sum all matching p-period extras (cumulative)
        List<PPeriod> matchingP = pTree.query(txDate);
        for (PPeriod pp : matchingP) {
            remanent += pp.getExtra();
        }
        remanent = Math.round(remanent * 100.0) / 100.0;

        // Mark inKPeriod
        boolean inKPeriod = !kTree.query(txDate).isEmpty();

        return TransactionDto.builder()
                .date(tx.getDate())
                .amount(tx.getAmount())
                .ceiling(tx.getCeiling())
                .remanent(remanent)
                .inKPeriod(inKPeriod)
                .build();
    }

    // ─── Interval Tree Builders ───────────────────────────────────────────────

    public IntervalTree<IndexedQPeriod> buildQTree(List<QPeriod> qPeriods) {
        List<IndexedQPeriod> indexed = new ArrayList<>();
        for (int i = 0; i < qPeriods.size(); i++) {
            indexed.add(new IndexedQPeriod(i, qPeriods.get(i)));
        }
        return IntervalTree.buildBalanced(
                indexed,
                iq -> DateTimeUtil.parse(iq.period().getStart()),
                iq -> DateTimeUtil.parse(iq.period().getEnd()));
    }

    public IntervalTree<PPeriod> buildPTree(List<PPeriod> pPeriods) {
        return IntervalTree.buildBalanced(
                pPeriods,
                pp -> DateTimeUtil.parse(pp.getStart()),
                pp -> DateTimeUtil.parse(pp.getEnd()));
    }

    public IntervalTree<KPeriod> buildKTree(List<KPeriod> kPeriods) {
        return IntervalTree.buildBalanced(
                kPeriods,
                kp -> DateTimeUtil.parse(kp.getStart()),
                kp -> DateTimeUtil.parse(kp.getEnd()));
    }

    /**
     * Wrapper to preserve the original list index of a QPeriod (for tie-breaking).
     */
    public record IndexedQPeriod(int index, QPeriod period) {}
}
