package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for the Transaction Validator endpoint.
 * Contains wage and list of pre-parsed transactions to validate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for transaction validation")
public class ValidatorRequest {

    @Schema(description = "Monthly wage in INR", example = "50000.0")
    @NotNull(message = "Wage is required")
    private Double wage;

    @NotNull(message = "Transactions list is required")
    @Valid
    @Schema(description = "List of transactions to validate")
    private List<TransactionDto> transactions;
}
