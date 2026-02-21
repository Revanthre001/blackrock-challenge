package com.blackrock.challenge.service;

/*
 * TEST TYPE: Unit Test
 * VALIDATION: Transaction validator — negative amount rejection, duplicate date detection,
 *             ordering (first occurrence valid, subsequent are duplicates),
 *             and correct error messages per problem statement.
 * RUN COMMAND: mvn test -Dtest=TransactionServiceValidatorTest
 */

import com.blackrock.challenge.dto.TransactionDto;
import com.blackrock.challenge.dto.ValidatorRequest;
import com.blackrock.challenge.dto.ValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionService — Validator Tests")
class TransactionServiceValidatorTest {

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
    }

    // ─── Negative amounts ─────────────────────────────────────────────────

    @Test
    @DisplayName("negative amount is rejected with correct message")
    void testNegativeAmountRejected() {
        ValidatorRequest request = ValidatorRequest.builder()
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-07-10 09:15:00").amount(-250.0)
                                .ceiling(200.0).remanent(30.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.validate(request);

        assertThat(result.getValid()).isEmpty();
        assertThat(result.getInvalid()).hasSize(1);
        assertThat(result.getInvalid().get(0).getMessage())
                .isEqualTo("Negative amounts are not allowed");
        assertThat(result.getInvalid().get(0).getAmount()).isEqualTo(-250.0);
    }

    @Test
    @DisplayName("zero amount is valid (boundary — problem says x < 5×10^5, 0 is valid)")
    void testZeroAmountIsValid() {
        ValidatorRequest request = ValidatorRequest.builder()
                .wage(50000.0)
                .transactions(List.of(
                        TransactionDto.builder().date("2023-01-01 00:00:00").amount(0.0)
                                .ceiling(0.0).remanent(0.0).build()
                ))
                .build();

        ValidationResponse result = transactionService.validate(request);

        assertThat(result.getValid()).hasSize(1);
        assertThat(result.getInvalid()).isEmpty();
    }

    // ─── Duplicate dates ──────────────────────────────────────────────────

    @Test
    @DisplayName("duplicate date is rejected with correct message")
    void testDuplicateDateRejected() {
        List<TransactionDto> transactions = List.of(
                TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0)
                        .ceiling(300.0).remanent(50.0).build(),
                TransactionDto.builder().date("2023-10-12 20:15:30").amount(250.0)
                        .ceiling(300.0).remanent(50.0).build()  // duplicate
        );

        ValidationResponse result = transactionService.splitValidInvalid(transactions);

        assertThat(result.getValid()).hasSize(1);
        assertThat(result.getInvalid()).hasSize(1);
        assertThat(result.getInvalid().get(0).getMessage())
                .isEqualTo("Duplicate transaction");
    }

    @Test
    @DisplayName("first occurrence of duplicate date is valid, second is invalid")
    void testFirstOccurrenceValidSecondInvalid() {
        List<TransactionDto> transactions = List.of(
                TransactionDto.builder().date("2023-05-01 10:00:00").amount(300.0)
                        .ceiling(300.0).remanent(0.0).build(),    // first — valid
                TransactionDto.builder().date("2023-05-01 10:00:00").amount(300.0)
                        .ceiling(300.0).remanent(0.0).build()    // second — duplicate
        );

        ValidationResponse result = transactionService.splitValidInvalid(transactions);

        assertThat(result.getValid().get(0).getDate()).isEqualTo("2023-05-01 10:00:00");
        assertThat(result.getInvalid().get(0).getMessage()).isEqualTo("Duplicate transaction");
    }

    // ─── Mixed scenario ───────────────────────────────────────────────────

    @Test
    @DisplayName("mixed: valid, negative, and duplicate all processed correctly")
    void testMixedValidNegativeDuplicate() {
        List<TransactionDto> transactions = List.of(
                TransactionDto.builder().date("2023-01-15 10:30:00").amount(2000.0)
                        .ceiling(300.0).remanent(50.0).build(),
                TransactionDto.builder().date("2023-03-20 14:45:00").amount(3500.0)
                        .ceiling(400.0).remanent(70.0).build(),
                TransactionDto.builder().date("2023-06-10 09:15:00").amount(1500.0)
                        .ceiling(200.0).remanent(30.0).build(),
                TransactionDto.builder().date("2023-07-10 09:15:00").amount(-250.0)
                        .ceiling(200.0).remanent(30.0).build()   // negative
        );

        ValidationResponse result = transactionService.splitValidInvalid(transactions);

        assertThat(result.getValid()).hasSize(3);
        assertThat(result.getInvalid()).hasSize(1);
        assertThat(result.getInvalid().get(0).getMessage())
                .isEqualTo("Negative amounts are not allowed");
    }

    @Test
    @DisplayName("negative check takes priority before duplicate check")
    void testNegativeCheckedBeforeDuplicate() {
        List<TransactionDto> transactions = List.of(
                TransactionDto.builder().date("2023-01-01 00:00:00").amount(-100.0)
                        .ceiling(0.0).remanent(0.0).build(),
                TransactionDto.builder().date("2023-01-01 00:00:00").amount(-100.0)
                        .ceiling(0.0).remanent(0.0).build()  // same date, also negative
        );

        ValidationResponse result = transactionService.splitValidInvalid(transactions);

        // Both should be invalid — first for negative, second for negative AND duplicate
        // But since negative check runs first, both get "Negative amounts are not allowed"
        assertThat(result.getValid()).isEmpty();
        assertThat(result.getInvalid()).hasSize(2);
        assertThat(result.getInvalid().get(0).getMessage())
                .isEqualTo("Negative amounts are not allowed");
    }

    @Test
    @DisplayName("all valid transactions returned unchanged")
    void testAllValidReturnsAll() {
        List<TransactionDto> transactions = List.of(
                TransactionDto.builder().date("2023-01-01 00:00:00").amount(100.0).build(),
                TransactionDto.builder().date("2023-02-01 00:00:00").amount(200.0).build(),
                TransactionDto.builder().date("2023-03-01 00:00:00").amount(300.0).build()
        );

        ValidationResponse result = transactionService.splitValidInvalid(transactions);

        assertThat(result.getValid()).hasSize(3);
        assertThat(result.getInvalid()).isEmpty();
    }

    @Test
    @DisplayName("empty transaction list gives empty valid and invalid")
    void testEmptyTransactions() {
        ValidationResponse result = transactionService.splitValidInvalid(List.of());

        assertThat(result.getValid()).isEmpty();
        assertThat(result.getInvalid()).isEmpty();
    }
}
