package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Core transaction DTO representing a single financial expense.
 * Used across parse, validator, filter and returns endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Financial transaction representing an expense")
public class TransactionDto {

    @NotBlank(message = "Date is required")
    @Schema(description = "Transaction date-time", example = "2023-10-12 20:15:30")
    private String date;

    @Schema(description = "Transaction amount in INR", example = "250.0")
    private Double amount;

    @Schema(description = "Ceiling (next multiple of 100)", example = "300.0")
    private Double ceiling;

    @Schema(description = "Remanent to be invested (ceiling - amount)", example = "50.0")
    private Double remanent;

    @Schema(description = "Whether the transaction falls within any k period", example = "true")
    private Boolean inKPeriod;

    @Schema(description = "Validation error message for invalid transactions")
    private String message;
}
