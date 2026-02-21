package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for validator and filter endpoints.
 * Separates valid transactions from invalid ones with error messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation result with valid and invalid transactions")
public class ValidationResponse {

    @Schema(description = "List of valid transactions passing all checks")
    private List<TransactionDto> valid;

    @Schema(description = "List of invalid transactions with error messages")
    private List<TransactionDto> invalid;
}
