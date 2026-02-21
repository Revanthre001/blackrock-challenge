package com.blackrock.challenge.service;

/*
 * TEST TYPE: Unit Test
 * VALIDATION: Return calculation — NPS and Index Fund compound interest formula accuracy,
 *             inflation adjustment, tax slab calculations, NPS deduction limits,
 *             k-period grouping, and the exact example from the problem statement.
 * RUN COMMAND: mvn test -Dtest=ReturnCalculationServiceTest
 */

import com.blackrock.challenge.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ReturnCalculationService — NPS and Index Fund Tests")
class ReturnCalculationServiceTest {

    private TransactionService transactionService;
    private ReturnCalculationService returnCalculationService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        returnCalculationService = new ReturnCalculationService(transactionService);
    }

    // ─── Problem Statement Example ────────────────────────────────────────

    @Test
    @DisplayName("problem statement full example — NPS: amount=145, profit≈86.88, taxBenefit=0")
    void testProblemStatementExampleNps() {
        ReturnsRequest request = buildProblemStatementRequest();

        ReturnsResponse result = returnCalculationService.calculateNpsReturns(request);

        assertThat(result.getTotalTransactionAmount()).isGreaterThan(0);
        assertThat(result.getSavingsByDates()).hasSize(2);

        // Full year k period (Jan-Dec): amount=145
        ReturnsResponse.SavingsByDate fullYear = result.getSavingsByDates().get(0);
        assertThat(fullYear.getAmount()).isEqualTo(145.0);

        // NPS: A = 145 * (1.0711)^31 ≈ 1219.45; A_real = 1219.45 / (1.055)^31 ≈ 231.9
        // profit = A_real - P = 231.9 - 145 ≈ 86.88 (approx)
        assertThat(fullYear.getProfit()).isCloseTo(86.88, within(1.0));
        assertThat(fullYear.getTaxBenefit()).isEqualTo(0.0); // annual income 6L < 7L threshold
    }

    @Test
    @DisplayName("problem statement full example — Index Fund: amount=145, return≈1829.5")
    void testProblemStatementExampleIndex() {
        ReturnsRequest request = buildProblemStatementRequest();

        ReturnsResponse result = returnCalculationService.calculateIndexReturns(request);

        assertThat(result.getSavingsByDates()).hasSize(2);

        // Full year: A = 145 * (1.1449)^31 ≈ 9619.7; A_real = 9619.7 / (1.055)^31 ≈ 1829.5
        // profit = 1829.5 - 145 ≈ 1684.5
        ReturnsResponse.SavingsByDate fullYear = result.getSavingsByDates().get(0);
        assertThat(fullYear.getAmount()).isEqualTo(145.0);

        // Index: A_real ≈ 1829.5, profit = A_real - P = 1684.5
        double expectedAReal = 145 * Math.pow(1.1449, 31) / Math.pow(1.055, 31);
        assertThat(fullYear.getProfit()).isCloseTo(expectedAReal - 145, within(1.0));
        assertThat(fullYear.getTaxBenefit()).isEqualTo(0.0); // no tax benefit for index
    }

    // ─── Years to Retirement ──────────────────────────────────────────────

    @ParameterizedTest(name = "age={0} → t={1}")
    @CsvSource({
            "29, 31",  // 60-29=31
            "55,  5",  // 60-55=5
            "60,  5",  // at retirement age → min 5
            "65,  5",  // past retirement → still min 5
            "1,  59",  // very young
    })
    @DisplayName("years to retirement: max(60-age, 5)")
    void testComputeYears(int age, int expectedYears) {
        assertThat(returnCalculationService.computeYears(age)).isEqualTo(expectedYears);
    }

    // ─── Tax Slab Tests ───────────────────────────────────────────────────

    @ParameterizedTest(name = "income={0} → tax={1}")
    @CsvSource({
            "600000,       0",      // below 7L — 0%
            "700000,       0",      // exactly 7L — 0%
            "800000,   10000",      // 7L+1L → 10% on 1L = 10000
            "1000000,  30000",      // 7L+3L → 10% on 3L = 30000
            "1100000,  45000",      // 10L+1L → 15% on 1L = 15000; +30000 = 45000
            "1200000,  60000",      // 10L+2L → 15% on 2L = 30000; +30000 = 60000
            "1350000,  90000",      // 12L+1.5L → 20% on 1.5L = 30000; +60000 = 90000
            "1500000, 120000",      // 12L+3L → 20% on 3L = 60000; +60000 = 120000
            "1800000, 210000",      // 15L+3L → 30% on 3L = 90000; +120000 = 210000
    })
    @DisplayName("marginal tax calculation per problem statement slabs")
    void testTaxCalculation(double income, double expectedTax) {
        assertThat(returnCalculationService.calculateTax(income))
                .isCloseTo(expectedTax, within(0.01));
    }

    @Test
    @DisplayName("tax benefit = 0 for income <= 7L (zero slab)")
    void testTaxBenefitZeroForLowIncome() {
        // Annual income 6L (50k/month) → in 0% slab
        double taxBenefit = returnCalculationService.calculateTaxBenefit(600_000, 145.0);
        assertThat(taxBenefit).isEqualTo(0.0);
    }

    @Test
    @DisplayName("tax benefit caps at NPS deduction limits")
    void testTaxBenefitNpsDeductionCap() {
        // High income: 2M annual. NPS invested: 300000. Cap: min(300000, 10% of 2M=200000, 200000) = 200000
        double annualWage = 2_000_000;
        double invested = 300_000;
        double maxDeduction = Math.min(invested,
                Math.min(0.10 * annualWage, ReturnCalculationService.MAX_NPS_DEDUCTION));
        assertThat(maxDeduction).isEqualTo(200_000.0);

        double taxBenefit = returnCalculationService.calculateTaxBenefit(annualWage, maxDeduction);
        // 30% slab applies → benefit = 200000 * 0.30 = 60000
        assertThat(taxBenefit).isCloseTo(60_000.0, within(1.0));
    }

    // ─── NPS Rate Accuracy ────────────────────────────────────────────────

    @Test
    @DisplayName("NPS compound interest: A = P * (1.0711)^t")
    void testNpsCompoundInterest() {
        double p = 1000.0;
        int t = 31;
        double expected = p * Math.pow(1 + ReturnCalculationService.NPS_RATE, t);
        assertThat(expected).isCloseTo(8410.0, within(5.0));
    }

    @Test
    @DisplayName("Index Fund compound interest: A = P * (1.1449)^t")
    void testIndexCompoundInterest() {
        double p = 1000.0;
        int t = 31;
        double expected = p * Math.pow(1 + ReturnCalculationService.INDEX_RATE, t);
        assertThat(expected).isCloseTo(66340.0, within(100.0));
    }

    // ─── K Period Grouping ────────────────────────────────────────────────

    @Test
    @DisplayName("k period grouping: transactions outside k range not counted")
    void testKPeriodGrouping() {
        ReturnsRequest request = ReturnsRequest.builder()
                .age(29)
                .wage(50000.0)
                .inflation(5.5)
                .q(List.of())
                .p(List.of())
                .k(List.of(
                        KPeriod.builder().start("2023-03-01 00:00:00").end("2023-11-30 23:59:59").build()
                ))
                .transactions(List.of(
                        TransactionDto.builder().date("2023-06-15 10:00:00").amount(250.0).build(), // in k → remanent=50
                        TransactionDto.builder().date("2023-12-15 10:00:00").amount(375.0).build()  // out of k
                ))
                .build();

        ReturnsResponse result = returnCalculationService.calculateNpsReturns(request);

        // Only the June transaction's remanent=50 is counted for this k period
        assertThat(result.getSavingsByDates().get(0).getAmount()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("k period returns empty/zero when no transactions fall in range")
    void testKPeriodNoMatchingTransactions() {
        ReturnsRequest request = ReturnsRequest.builder()
                .age(30)
                .wage(50000.0)
                .inflation(5.5)
                .q(List.of())
                .p(List.of())
                .k(List.of(
                        KPeriod.builder().start("2023-01-01 00:00:00").end("2023-03-31 23:59:59").build()
                ))
                .transactions(List.of(
                        TransactionDto.builder().date("2023-06-15 10:00:00").amount(250.0).build()  // outside k
                ))
                .build();

        ReturnsResponse result = returnCalculationService.calculateNpsReturns(request);

        assertThat(result.getSavingsByDates().get(0).getAmount()).isEqualTo(0.0);
        assertThat(result.getSavingsByDates().get(0).getProfit()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("index fund taxBenefit is always 0")
    void testIndexFundTaxBenefitAlwaysZero() {
        ReturnsRequest request = ReturnsRequest.builder()
                .age(25)
                .wage(200000.0)  // Very high income
                .inflation(6.0)
                .q(List.of())
                .p(List.of())
                .k(List.of(KPeriod.builder().start("2023-01-01 00:00:00").end("2023-12-31 23:59:59").build()))
                .transactions(List.of(
                        TransactionDto.builder().date("2023-06-15 10:00:00").amount(250.0).build()
                ))
                .build();

        ReturnsResponse result = returnCalculationService.calculateIndexReturns(request);

        result.getSavingsByDates().forEach(s ->
                assertThat(s.getTaxBenefit()).isEqualTo(0.0));
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    /**
     * Builds the exact request from the problem statement example.
     * Expected: full-year k period → amount=145, NPS profit≈86.88, Index profit≈1684.5
     */
    private ReturnsRequest buildProblemStatementRequest() {
        return ReturnsRequest.builder()
                .age(29)
                .wage(50000.0)
                .inflation(5.5)
                .q(List.of(
                        QPeriod.builder().fixed(0.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of(
                        PPeriod.builder().extra(25.0).start("2023-10-01 08:00:00").end("2023-12-31 19:59:59").build()
                ))
                .k(List.of(
                        KPeriod.builder().start("2023-01-01 00:00:00").end("2023-12-31 23:59:59").build(),
                        KPeriod.builder().start("2023-03-01 00:00:00").end("2023-11-30 23:59:59").build()
                ))
                .transactions(List.of(
                        TransactionDto.builder().date("2023-02-28 15:49:20").amount(375.0).build(),
                        TransactionDto.builder().date("2023-07-01 21:59:00").amount(620.0).build(),
                        TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0).build(),
                        TransactionDto.builder().date("2023-12-17 08:09:45").amount(480.0).build(),
                        TransactionDto.builder().date("2023-12-17 08:09:45").amount(-10.0).build()   // invalid: duplicate date, negative
                ))
                .build();
    }
}
