package com.blackrock.challenge.controller;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for transaction operations.
 *
 * <p>Endpoints:
 * - POST /blackrock/challenge/v1/transactions:parse     → enrich with ceiling/remanent
 * - POST /blackrock/challenge/v1/transactions:validator → validate negatives and duplicates
 * - POST /blackrock/challenge/v1/transactions:filter    → apply q/p/k period constraints
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction parsing, validation, and temporal constraint filtering")
public class TransactionController {

    private final TransactionService transactionService;

    // ─── Parse ────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Parse expenses into transactions",
            description = "Enriches a list of raw expenses with ceiling (next multiple of ₹100) and remanent (ceiling - amount). " +
                    "Supports up to 10^6 transactions using parallel stream processing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully parsed",
                    content = @Content(schema = @Schema(implementation = TransactionDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/transactions:parse")
    public ResponseEntity<List<TransactionDto>> parseTransactions(
            @Valid @RequestBody List<TransactionDto> transactions) {

        log.info("POST /transactions:parse — {} transactions", transactions.size());
        List<TransactionDto> result = transactionService.parse(transactions);
        return ResponseEntity.ok(result);
    }

    // ─── Validator ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Validate transactions",
            description = "Validates transactions: rejects negative amounts and duplicate dates. " +
                    "Returns separate valid and invalid lists with error messages."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation complete",
                    content = @Content(schema = @Schema(implementation = ValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/transactions:validator")
    public ResponseEntity<ValidationResponse> validateTransactions(
            @Valid @RequestBody ValidatorRequest request) {

        log.info("POST /transactions:validator — {} transactions", request.getTransactions().size());
        ValidationResponse result = transactionService.validate(request);
        return ResponseEntity.ok(result);
    }

    // ─── Filter ───────────────────────────────────────────────────────────────

    @Operation(
            summary = "Apply temporal constraints (q/p/k periods)",
            description = "Applies q (fixed override), p (extra addition), and k (grouping) period rules to transactions. " +
                    "Uses interval tree for O(log n) period lookups. " +
                    "Processing order: ceiling → q → p → k. Also validates negatives and duplicates."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filter complete",
                    content = @Content(schema = @Schema(implementation = ValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/transactions:filter")
    public ResponseEntity<ValidationResponse> filterTransactions(
            @Valid @RequestBody FilterRequest request) {

        log.info("POST /transactions:filter — {} transactions, q={}, p={}, k={}",
                request.getTransactions().size(),
                request.getQ().size(),
                request.getP().size(),
                request.getK().size());
        ValidationResponse result = transactionService.filter(request);
        return ResponseEntity.ok(result);
    }
}
