package com.blackrock.challenge.service;

/*
 * TEST TYPE: Unit Test
 * VALIDATION: Temporal constraint filter — q period override, p period addition,
 *             k period marking, combined q+p, multiple overlapping p periods,
 *             q tie-breaking (latest start wins), and inKPeriod flag accuracy.
 *             Uses the exact example from the problem statement as the gold standard.
 * RUN COMMAND: mvn test -Dtest=TransactionServiceFilterTest
 */

import com.blackrock.challenge.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionService — Filter Tests (q/p/k Periods)")
class TransactionServiceFilterTest {

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
    }

    // ─── Problem Statement Example ────────────────────────────────────────

    @Test
    @DisplayName("problem statement example: q sets july to 0, p adds 25 in oct-dec, k groups")
    void testProblemStatementExample() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(0.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of(
                        PPeriod.builder().extra(25.0).start("2023-10-01 08:00:00").end("2023-12-31 19:59:59").build()
                ))
                .k(List.of(
                        KPeriod.builder().start("2023-01-01 00:00:00").end("2023-12-31 23:59:59").build()
                ))
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-02-28 15:49:20").amount(375.0).build(),
                        TransactionDto.builder().date("2023-07-15 10:30:00").amount(620.0).build(),
                        TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0).build(),
                        TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0).build(), // duplicate
                        TransactionDto.builder().date("2023-12-17 08:09:45").amount(-480.0).build() // negative
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // 3 valid (Feb, Jul with q=0, Oct), 2 invalid (Oct duplicate, Dec negative)
        // Q period with fixed=0 means: still valid, but saves ₹0 in July
        assertThat(result.getValid()).hasSize(3);
        assertThat(result.getInvalid()).hasSize(2);

        // 2023-02-28: not in q or p — ceiling=400, remanent=25
        TransactionDto feb = result.getValid().stream()
                .filter(t -> t.getDate().startsWith("2023-02")).findFirst().orElseThrow();
        assertThat(feb.getRemanent()).isEqualTo(25.0);
        assertThat(feb.getInKPeriod()).isTrue();

        // 2023-07-15: in q (fixed=0) — transaction is valid, but saves ₹0
        TransactionDto jul = result.getValid().stream()
                .filter(t -> t.getDate().startsWith("2023-07")).findFirst().orElseThrow();
        assertThat(jul.getRemanent()).isEqualTo(0.0); // q overrides to 0
        assertThat(jul.getInKPeriod()).isTrue();

        // 2023-10-12: in p (oct-dec) → remanent = 50 + 25 = 75
        TransactionDto oct = result.getValid().stream()
                .filter(t -> t.getDate().startsWith("2023-10")).findFirst().orElseThrow();
        assertThat(oct.getRemanent()).isEqualTo(75.0); // ceiling of 250 is 300, remanent=50, +25p = 75
        // Note: the filter input has raw amounts, so ceiling/remanent are computed first:
        // 250 → ceiling=300, remanent=50, +25(p) = 75
    }

    // ─── Q Period Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("q period replaces remanent with fixed amount")
    void testQPeriodOverridesRemanent() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(0.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of())
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-07-15 10:00:00").amount(620.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        assertThat(result.getValid()).hasSize(1);
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("q period boundary: transaction on exact start date is included")
    void testQPeriodBoundaryStart() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(10.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of())
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-07-01 00:00:00").amount(250.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("q period tie-breaking: latest start date wins")
    void testQPeriodTieBreakLatestStart() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(10.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build(),
                        QPeriod.builder().fixed(20.0).start("2023-07-15 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of())
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-07-20 10:00:00").amount(250.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // Latest start is 2023-07-15, so fixed=20 wins
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("q period tie-breaking: same start date — first in list wins")
    void testQPeriodTieBreakSameStartFirstWins() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(5.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build(),
                        QPeriod.builder().fixed(99.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of())
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-07-10 10:00:00").amount(250.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // Same start → first in list (fixed=5) wins
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(5.0);
    }

    // ─── P Period Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("p period adds extra to remanent cumulatively")
    void testPPeriodAddsExtra() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of())
                .p(List.of(
                        PPeriod.builder().extra(25.0).start("2023-10-01 00:00:00").end("2023-12-31 23:59:59").build()
                ))
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // remanent of 250 = 50 (ceiling 300 - 250), + 25 (p) = 75
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("multiple overlapping p periods all add cumulatively")
    void testMultiplePPeriodsAddCumulatively() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of())
                .p(List.of(
                        PPeriod.builder().extra(10.0).start("2023-10-01 00:00:00").end("2023-12-31 23:59:59").build(),
                        PPeriod.builder().extra(15.0).start("2023-10-01 00:00:00").end("2023-12-31 23:59:59").build(),
                        PPeriod.builder().extra(5.0).start("2023-11-01 00:00:00").end("2023-12-31 23:59:59").build()
                ))
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-11-15 10:00:00").amount(250.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // base remanent = 50, + 10 + 15 + 5 = 80
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(80.0);
    }

    // ─── Combined Q+P ─────────────────────────────────────────────────────

    @Test
    @DisplayName("q then p: q sets fixed, then p adds extra on top")
    void testQThenPBothApply() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(0.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of(
                        PPeriod.builder().extra(25.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-07-15 10:00:00").amount(620.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // q sets remanent=0, then p adds 25 → final=25
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(25.0);
    }

    // ─── K Period Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("k period marks inKPeriod=true for transactions in range")
    void testKPeriodMarksTransactions() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of())
                .p(List.of())
                .k(List.of(
                        KPeriod.builder().start("2023-03-01 00:00:00").end("2023-11-30 23:59:59").build()
                ))
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-06-15 10:00:00").amount(200.0).build(),  // in k
                        TransactionDto.builder().date("2023-12-01 10:00:00").amount(200.0).build()   // outside k
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        assertThat(result.getValid().get(0).getInKPeriod()).isTrue();
        assertThat(result.getValid().get(1).getInKPeriod()).isFalse();
    }

    @Test
    @DisplayName("k period: transaction can fall in multiple k periods simultaneously")
    void testTransactionInMultipleKPeriods() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of())
                .p(List.of())
                .k(List.of(
                        KPeriod.builder().start("2023-01-01 00:00:00").end("2023-12-31 23:59:59").build(),
                        KPeriod.builder().start("2023-03-01 00:00:00").end("2023-11-30 23:59:59").build()
                ))
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-06-15 10:00:00").amount(200.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        // inKPeriod=true (falls in at least one k period)
        assertThat(result.getValid().get(0).getInKPeriod()).isTrue();
    }

    // ─── Edge Cases ───────────────────────────────────────────────────────

    @Test
    @DisplayName("empty q/p/k lists — transactions pass through unchanged (except ceiling/remanent)")
    void testEmptyPeriods() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of())
                .p(List.of())
                .k(List.of())
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-06-15 10:00:00").amount(375.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        assertThat(result.getValid()).hasSize(1);
        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(25.0);
        assertThat(result.getValid().get(0).getInKPeriod()).isFalse();
    }

    @Test
    @DisplayName("transaction outside all periods — remanent unchanged, inKPeriod=false")
    void testTransactionOutsideAllPeriods() {
        FilterRequest request = FilterRequest.builder()
                .q(List.of(
                        QPeriod.builder().fixed(0.0).start("2023-07-01 00:00:00").end("2023-07-31 23:59:59").build()
                ))
                .p(List.of(
                        PPeriod.builder().extra(25.0).start("2023-10-01 00:00:00").end("2023-12-31 23:59:59").build()
                ))
                .k(List.of(
                        KPeriod.builder().start("2023-03-01 00:00:00").end("2023-09-30 23:59:59").build()
                ))
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-01-15 10:00:00").amount(375.0).build()  // outside all
                ))
                .build();

        ValidationResponse result = transactionService.filter(request);

        assertThat(result.getValid().get(0).getRemanent()).isEqualTo(25.0);
        assertThat(result.getValid().get(0).getInKPeriod()).isFalse();
    }
}
