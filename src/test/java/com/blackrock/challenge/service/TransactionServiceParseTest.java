package com.blackrock.challenge.service;

/*
 * TEST TYPE: Unit Test
 * VALIDATION: Transaction parsing — ceiling and remanent calculation accuracy,
 *             edge cases (amount=0, exact multiple of 100, large batches),
 *             and parallel stream behavior for >10_000 transactions.
 * RUN COMMAND: mvn test -Dtest=TransactionServiceParseTest
 *              OR: mvn test  (runs all tests)
 */

import com.blackrock.challenge.dto.TransactionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionService — Parse Tests")
class TransactionServiceParseTest {

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
    }

    // ─── Ceiling calculation ───────────────────────────────────────────────

    @ParameterizedTest(name = "amount={0} → ceiling={1}, remanent={2}")
    @CsvSource({
            "250,   300,  50",
            "375,   400,  25",
            "620,   700,  80",
            "480,   500,  20",
            "100,   100,   0",   // exact multiple — remanent must be 0
            "0,       0,   0",   // zero amount
            "1,     100,  99",   // minimum non-zero
            "999,  1000,   1",   // just below multiple
            "1519, 1600,  81",   // problem statement example
            "499999.99, 500000, 0.01"  // near constraint maximum
    })
    @DisplayName("ceiling and remanent calculation")
    void testCeilingAndRemanent(double amount, double expectedCeiling, double expectedRemanent) {
        double ceiling = TransactionService.calculateCeiling(amount);
        double remanent = Math.round((ceiling - amount) * 100.0) / 100.0;

        assertThat(ceiling).isEqualTo(expectedCeiling);
        assertThat(remanent).isEqualTo(expectedRemanent);
    }

    @Test
    @DisplayName("parse enriches all transactions correctly")
    void testParseEnrichesTransactions() {
        List<TransactionDto> input = List.of(
                TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0).build(),
                TransactionDto.builder().date("2023-02-28 15:49:20").amount(375.0).build(),
                TransactionDto.builder().date("2023-07-01 21:59:00").amount(620.0).build(),
                TransactionDto.builder().date("2023-12-17 08:09:45").amount(480.0).build()
        );

        List<TransactionDto> result = transactionService.parse(input);

        assertThat(result).hasSize(4);
        assertThat(result.get(0).getCeiling()).isEqualTo(300.0);
        assertThat(result.get(0).getRemanent()).isEqualTo(50.0);
        assertThat(result.get(1).getCeiling()).isEqualTo(400.0);
        assertThat(result.get(1).getRemanent()).isEqualTo(25.0);
        assertThat(result.get(2).getCeiling()).isEqualTo(700.0);
        assertThat(result.get(2).getRemanent()).isEqualTo(80.0);
        assertThat(result.get(3).getCeiling()).isEqualTo(500.0);
        assertThat(result.get(3).getRemanent()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("parse returns empty list for empty input")
    void testParseEmptyInput() {
        List<TransactionDto> result = transactionService.parse(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parse returns empty list for null input")
    void testParseNullInput() {
        List<TransactionDto> result = transactionService.parse(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parse preserves date strings")
    void testParseDatePreserved() {
        String dateStr = "2023-06-15 10:30:00";
        List<TransactionDto> input = List.of(
                TransactionDto.builder().date(dateStr).amount(150.0).build()
        );

        List<TransactionDto> result = transactionService.parse(input);

        assertThat(result.get(0).getDate()).isEqualTo(dateStr);
    }

    @Test
    @DisplayName("parse handles large batch (10001 transactions) via parallel stream")
    void testParseLargeBatch() {
        // Threshold for parallel: 10_000
        List<TransactionDto> input = new ArrayList<>();
        for (int i = 0; i < 10_001; i++) {
            // Use unique dates by offsetting seconds
            String date = String.format("2023-01-%02d %02d:%02d:%02d",
                    (i / 86400) % 28 + 1,
                    (i / 3600) % 24,
                    (i / 60) % 60,
                    i % 60);
            input.add(TransactionDto.builder().date(date).amount(i + 1.0).build());
        }

        List<TransactionDto> result = transactionService.parse(input);

        assertThat(result).hasSize(10_001);
        // Spot-check: amount=1 → ceiling=100, remanent=99
        assertThat(result.get(0).getCeiling()).isEqualTo(100.0);
        assertThat(result.get(0).getRemanent()).isEqualTo(99.0);
    }

    @Test
    @DisplayName("parse — amount exactly at multiple of 100 gives remanent=0")
    void testExactMultipleRemanentIsZero() {
        List<TransactionDto> input = List.of(
                TransactionDto.builder().date("2023-01-01 00:00:00").amount(200.0).build()
        );

        List<TransactionDto> result = transactionService.parse(input);

        assertThat(result.get(0).getCeiling()).isEqualTo(200.0);
        assertThat(result.get(0).getRemanent()).isEqualTo(0.0);
    }
}
