package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for returns calculation endpoints.
 * Reports total transaction totals and per-k-period investment breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Returns calculation result with per-period investment breakdown")
public class ReturnsResponse {

    @Schema(description = "Sum of valid transaction amounts", example = "1725.0")
    private Double totalTransactionAmount;

    @Schema(description = "Sum of valid transaction ceilings", example = "1900.0")
    private Double totalCeiling;

    @Schema(description = "Investment breakdown per k period")
    private List<SavingsByDate> savingsByDates;

    /**
     * Per-k-period investment summary with profit and tax benefit.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Investment summary for a specific k period")
    public static class SavingsByDate {

        @Schema(description = "K period start datetime", example = "2023-01-01 00:00:00")
        private String start;

        @Schema(description = "K period end datetime", example = "2023-12-31 23:59:59")
        private String end;

        @Schema(description = "Total invested amount for this period", example = "145.0")
        private Double amount;

        @Schema(description = "Inflation-adjusted profit (A_real - P)", example = "86.88")
        private Double profit;

        @Schema(description = "NPS tax benefit (0 for index fund)", example = "0.0")
        private Double taxBenefit;
    }
}
