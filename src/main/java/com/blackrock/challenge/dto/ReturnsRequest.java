package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for the Returns Calculation endpoints (NPS and Index Fund).
 * Contains full investment context: age, wage, inflation, periods, and transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for retirement investment returns calculation")
public class ReturnsRequest {

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must be realistic")
    @Schema(description = "Current age of investor", example = "29")
    private Integer age;

    @NotNull(message = "Wage is required")
    @Schema(description = "Monthly wage in INR", example = "50000.0")
    private Double wage;

    @NotNull(message = "Inflation rate is required")
    @Schema(description = "Annual inflation rate as percentage", example = "5.5")
    private Double inflation;

    @Schema(description = "Q periods - fixed amount overrides")
    @Builder.Default
    private List<@Valid QPeriod> q = new ArrayList<>();

    @Schema(description = "P periods - extra amount additions")
    @Builder.Default
    private List<@Valid PPeriod> p = new ArrayList<>();

    @Schema(description = "K periods - evaluation groupings")
    @Builder.Default
    private List<@Valid KPeriod> k = new ArrayList<>();

    @NotNull(message = "Transactions list is required")
    @Schema(description = "List of raw expense transactions")
    private List<TransactionDto> transactions;
}
