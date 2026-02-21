package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for the Temporal Constraints Filter endpoint.
 * Accepts q/p/k periods along with transactions to filter and enrich.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for temporal constraint filtering")
public class FilterRequest {

    @Schema(description = "Q periods - fixed amount overrides")
    @Builder.Default
    private List<@Valid QPeriod> q = new ArrayList<>();

    @Schema(description = "P periods - extra amount additions")
    @Builder.Default
    private List<@Valid PPeriod> p = new ArrayList<>();

    @Schema(description = "K periods - evaluation groupings")
    @Builder.Default
    private List<@Valid KPeriod> k = new ArrayList<>();

    @NotNull(message = "Wage is required")
    @Schema(description = "Monthly wage in INR", example = "50000.0")
    private Double wage;

    @NotNull(message = "Transactions list is required")
    @Schema(description = "List of raw transactions (amount only required, ceiling/remanent computed if missing)")
    private List<TransactionDto> transactions;
}
