package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the Tax-First Hybrid allocation endpoint (/returns:hybrid).
 *
 * <p>Optimally splits investment between NPS (up to the ₹2,00,000 annual
 * tax-deduction cap) and NIFTY 50 Index Fund (everything above the cap).
 * Compound interest is linear in principal, so each k-period's full-allocation
 * NPS/Index profits are scaled by the computed npsRatio / indexRatio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Tax-optimal hybrid NPS + NIFTY 50 Index Fund allocation result")
public class HybridResponse {

    @Schema(description = "Total remanent invested across all valid transactions (INR)")
    private double totalInvested;

    @Schema(description = "Amount routed to NPS — capped at ₹2L/yr deduction limit (INR)")
    private double npsContribution;

    @Schema(description = "Amount routed to NIFTY 50 Index Fund — above deduction cap (INR)")
    private double indexContribution;

    @Schema(description = "Estimated income tax saved via NPS deduction under Sec. 80CCD (INR)")
    private double estimatedTaxSaved;

    @Schema(description = "NPS portion corpus at retirement (inflation-adjusted) (INR)")
    private double npsCorpus;

    @Schema(description = "Index Fund portion corpus at retirement (inflation-adjusted) (INR)")
    private double indexCorpus;

    @Schema(description = "Combined hybrid corpus = npsCorpus + indexCorpus (INR)")
    private double hybridCorpus;

    @Schema(description = "Reference: corpus if 100% invested in NPS (INR)")
    private double pureNpsCorpus;

    @Schema(description = "Reference: corpus if 100% invested in Index Fund (INR)")
    private double pureIndexCorpus;

    @Schema(description = "Hybrid advantage vs pure-NPS strategy in INR (positive = hybrid wins)")
    private double hybridAdvantageOverNps;

    @Schema(description = "Hybrid advantage vs pure-Index strategy in INR (positive = hybrid wins)")
    private double hybridAdvantageOverIndex;

    @Schema(description = "Percentage of total investment allocated to NPS")
    private double npsAllocationPct;

    @Schema(description = "Percentage of total investment allocated to Index Fund")
    private double indexAllocationPct;

    @Schema(description = "Human-readable description of the allocation strategy")
    private String allocationStrategy;

    @Schema(description = "Data-driven reasoning for the chosen split")
    private String reasoning;

    @Schema(description = "Per k-period allocation and return breakdown")
    private List<PeriodAllocation> periodAllocations;

    // ─── Inner Types ──────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Hybrid allocation details for a single k-period")
    public static class PeriodAllocation {

        @Schema(description = "k-period start datetime")
        private String start;

        @Schema(description = "k-period end datetime")
        private String end;

        @Schema(description = "Total remanent sum in this period (INR)")
        private double totalRemanent;

        @Schema(description = "NPS-routed remanent in this period (INR)")
        private double npsAmount;

        @Schema(description = "Index-routed remanent in this period (INR)")
        private double indexAmount;

        @Schema(description = "Inflation-adjusted profit from NPS portion (INR)")
        private double npsProfit;

        @Schema(description = "Inflation-adjusted profit from Index portion (INR)")
        private double indexProfit;

        @Schema(description = "Tax benefit on NPS portion (INR)")
        private double taxBenefit;

        @Schema(description = "Routing decision: FULL_NPS / SPLIT / FULL_INDEX")
        private String routing;
    }
}
