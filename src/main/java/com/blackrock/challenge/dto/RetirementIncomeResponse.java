package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for the Retirement Income Bridge endpoint (/returns:retirement-income).
 *
 * <p>Converts the projected retirement corpus into a monthly income estimate
 * for three strategies:
 * <ul>
 *   <li>NPS (Pure)  — 40% into annuity @ 6.5% p.a. (PFRDA mandate) + 60% lump sum</li>
 *   <li>Index Fund  — 4% Safe Withdrawal Rate (Bengen / Trinity study)</li>
 *   <li>Tax-Hybrid  — NPS annuity income + Index SWR withdrawal (dual income streams)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Retirement monthly income projections across NPS, Index Fund, and Tax-Optimal Hybrid")
public class RetirementIncomeResponse {

    @Schema(description = "Investor's current age")
    private int age;

    @Schema(description = "Years remaining until retirement (age 60)")
    private int yearsToRetirement;

    @Schema(description = "Projected monthly wage at retirement (inflation-adjusted, INR)")
    private double projectedMonthlyWageAtRetirement;

    @Schema(description = "NPS pure strategy retirement income")
    private IncomeScenario nps;

    @Schema(description = "Index Fund pure strategy retirement income")
    private IncomeScenario index;

    @Schema(description = "Tax-Optimal Hybrid strategy retirement income")
    private IncomeScenario hybrid;

    @Schema(description = "Recommended strategy: NPS / INDEX_FUND / TAX_HYBRID")
    private String recommendation;

    @Schema(description = "Data-driven reasoning for the recommendation")
    private String reasoning;

    // ─── Inner Types ──────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Retirement income projection for a single investment strategy")
    public static class IncomeScenario {

        @Schema(description = "Strategy name")
        private String name;

        @Schema(description = "Total retirement corpus (inflation-adjusted, INR)")
        private double totalCorpus;

        @Schema(description = "Estimated monthly income at retirement (INR)")
        private double monthlyIncome;

        @Schema(description = "Estimated annual income at retirement (INR)")
        private double annualIncome;

        @Schema(description = "Lump-sum available at retirement (INR). NPS = 60% corpus; Index = 100% corpus.")
        private double lumpSumAtRetirement;

        @Schema(description = "Annuity corpus portion (NPS = 40% corpus; Index = 0)")
        private double annuityCorpus;

        @Schema(description = "Income structure description")
        private String structure;

        @Schema(description = "Additional notes on this scenario")
        private String notes;
    }
}
