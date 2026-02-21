package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.util.DateTimeUtil;
import com.blackrock.challenge.util.IntervalTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating investment returns for NPS and Index Fund.
 *
 * <p>Computation pipeline:
 * 1. Parse raw transactions (ceiling + remanent)
 * 2. Validate (remove negatives and duplicates)
 * 3. Apply q/p period rules per transaction (interval tree lookups)
 * 4. Group by k periods (each k period calculated independently)
 * 5. For each k period: apply compound interest + inflation adjustment
 * 6. For NPS: calculate marginal tax benefit
 *
 * <p>Compound interest: A = P × (1 + r)^t   (annual compounding, n=1)
 * <p>Inflation adjustment: A_real = A / (1 + inflation/100)^t
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnCalculationService {

    // Interest rates (annual)
    public static final double NPS_RATE = 0.0711;
    public static final double INDEX_RATE = 0.1449;

    // NPS tax deduction cap
    public static final double MAX_NPS_DEDUCTION = 200_000.0;
    public static final double NPS_DEDUCTION_PERCENT = 0.10;

    // Minimum investment years (when age >= 60)
    public static final int MIN_YEARS = 5;
    public static final int RETIREMENT_AGE = 60;

    private final TransactionService transactionService;

    // ─── NPS Returns ─────────────────────────────────────────────────────────

    /**
     * Calculates NPS retirement returns with tax benefit.
     */
    public ReturnsResponse calculateNpsReturns(ReturnsRequest request) {
        log.info("Calculating NPS returns for age={}, wage={}, transactions={}",
                request.getAge(), request.getWage(), request.getTransactions().size());
        return calculateReturns(request, true);
    }

    // ─── Index Fund Returns ───────────────────────────────────────────────────

    /**
     * Calculates NIFTY 50 index fund retirement returns (no tax benefit).
     */
    public ReturnsResponse calculateIndexReturns(ReturnsRequest request) {
        log.info("Calculating Index Fund returns for age={}, wage={}, transactions={}",
                request.getAge(), request.getWage(), request.getTransactions().size());
        return calculateReturns(request, false);
    }

    // ─── Core Calculation Pipeline ────────────────────────────────────────────

    /**
     * Unified return calculation for both NPS and Index Fund.
     *
     * @param request  the returns request
     * @param isNps    true for NPS, false for Index Fund
     */
    public ReturnsResponse calculateReturns(ReturnsRequest request, boolean isNps) {
        double rate = isNps ? NPS_RATE : INDEX_RATE;
        double inflation = request.getInflation() / 100.0;
        double annualWage = request.getWage() * 12.0;
        int t = computeYears(request.getAge());

        List<QPeriod> qPeriods = request.getQ() == null ? Collections.emptyList() : request.getQ();
        List<PPeriod> pPeriods = request.getP() == null ? Collections.emptyList() : request.getP();
        List<KPeriod> kPeriods = request.getK() == null ? Collections.emptyList() : request.getK();

        // Step 1+2: Parse raw transactions then validate
        List<TransactionDto> parsed = transactionService.parse(request.getTransactions());
        ValidationResponse validated = transactionService.splitValidInvalid(parsed);
        List<TransactionDto> validTxs = validated.getValid();

        // Step 3: Apply q/p rules to all valid transactions (build trees once)
        IntervalTree<TransactionService.IndexedQPeriod> qTree = transactionService.buildQTree(qPeriods);
        IntervalTree<PPeriod> pTree = transactionService.buildPTree(pPeriods);
        IntervalTree<KPeriod> kTree = transactionService.buildKTree(kPeriods);

        List<TransactionDto> processedTxs = transactionService.applyPeriods(validTxs, qTree, pTree, kTree);

        // Step 4: Aggregate totals for valid transactions
        double totalAmount = validTxs.stream()
                .mapToDouble(TransactionDto::getAmount)
                .sum();
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        double totalCeiling = validTxs.stream()
                .mapToDouble(TransactionDto::getCeiling)
                .sum();
        totalCeiling = Math.round(totalCeiling * 100.0) / 100.0;

        // Step 5: Group by k periods and calculate returns per period
        List<ReturnsResponse.SavingsByDate> savingsByDates = new ArrayList<>();
        for (KPeriod kPeriod : kPeriods) {
            LocalDateTime kStart = DateTimeUtil.parse(kPeriod.getStart());
            LocalDateTime kEnd = DateTimeUtil.parse(kPeriod.getEnd());

            // Sum remanents for transactions within this k period
            double p = processedTxs.stream()
                    .filter(tx -> {
                        LocalDateTime txDate = DateTimeUtil.parse(tx.getDate());
                        return !txDate.isBefore(kStart) && !txDate.isAfter(kEnd);
                    })
                    .mapToDouble(TransactionDto::getRemanent)
                    .sum();
            p = Math.round(p * 100.0) / 100.0;

            // Compound interest: A = P * (1 + r)^t
            double a = p * Math.pow(1 + rate, t);

            // Inflation adjustment: A_real = A / (1 + inflation)^t
            double aReal = a / Math.pow(1 + inflation, t);
            aReal = Math.round(aReal * 100.0) / 100.0;

            double profit = Math.round((aReal - p) * 100.0) / 100.0;

            // Tax benefit (NPS only)
            double taxBenefit = 0.0;
            if (isNps && p > 0) {
                double npsDeduction = Math.min(p, Math.min(NPS_DEDUCTION_PERCENT * annualWage, MAX_NPS_DEDUCTION));
                taxBenefit = Math.round(calculateTaxBenefit(annualWage, npsDeduction) * 100.0) / 100.0;
            }

            savingsByDates.add(ReturnsResponse.SavingsByDate.builder()
                    .start(kPeriod.getStart())
                    .end(kPeriod.getEnd())
                    .amount(p)
                    .profit(profit)
                    .taxBenefit(taxBenefit)
                    .build());
        }

        return ReturnsResponse.builder()
                .totalTransactionAmount(totalAmount)
                .totalCeiling(totalCeiling)
                .savingsByDates(savingsByDates)
                .build();
    }

    // ─── Tax Calculation (Marginal Slabs) ────────────────────────────────────

    /**
     * Calculates the tax benefit for NPS using marginal slab system.
     * Tax_Benefit = Tax(annual_income) - Tax(annual_income - NPS_Deduction)
     *
     * @param annualIncome  pre-tax annual income
     * @param npsDeduction  eligible NPS deduction amount
     * @return tax benefit in INR
     */
    public double calculateTaxBenefit(double annualIncome, double npsDeduction) {
        return calculateTax(annualIncome) - calculateTax(annualIncome - npsDeduction);
    }

    /**
     * Marginal tax calculation per Indian tax slabs (simplified, as per problem statement).
     *
     * <p>Slabs:
     * ₹0 - ₹7,00,000       →  0%
     * ₹7,00,001 - ₹10,00,000  → 10% on excess over ₹7L
     * ₹10,00,001 - ₹12,00,000 → 15% on excess over ₹10L
     * ₹12,00,001 - ₹15,00,000 → 20% on excess over ₹12L
     * Above ₹15,00,000     → 30% on excess over ₹15L
     *
     * @param income taxable income in INR
     * @return computed tax in INR
     */
    public double calculateTax(double income) {
        if (income <= 0) return 0.0;

        double tax = 0.0;

        if (income > 1_500_000) {
            tax += (income - 1_500_000) * 0.30;
            income = 1_500_000;
        }
        if (income > 1_200_000) {
            tax += (income - 1_200_000) * 0.20;
            income = 1_200_000;
        }
        if (income > 1_000_000) {
            tax += (income - 1_000_000) * 0.15;
            income = 1_000_000;
        }
        if (income > 700_000) {
            tax += (income - 700_000) * 0.10;
        }

        return Math.round(tax * 100.0) / 100.0;
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    /**
     * Computes investment years: max(60 - age, 5).
     * If the investor is >= 60, defaults to 5 years.
     */
    public int computeYears(int age) {
        return Math.max(RETIREMENT_AGE - age, MIN_YEARS);
    }
}
