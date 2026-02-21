package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the Alternative Investments endpoint (/returns:alternatives).
 *
 * <p>Compares the investor's remanent invested across six asset classes:
 * <ul>
 *   <li>NPS          — 7.11% p.a. (Pension Fund Regulatory and Development Authority)</li>
 *   <li>NIFTY 50     — 14.49% p.a. (NIFTY 50 10-year CAGR)</li>
 *   <li>Gold         — 11.50% p.a. (Sovereign Gold Bond 10-yr CAGR)</li>
 *   <li>Silver       — 9.80% p.a. (MCX Silver 10-yr CAGR)</li>
 *   <li>GOI Bonds    — 7.50% p.a. (10-yr Government of India Gilt yield)</li>
 *   <li>REITs        — 10.20% p.a. (Embassy/Brookfield/Mindspace avg since 2019)</li>
 * </ul>
 * All returns are inflation-adjusted using the investor's provided inflation rate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Inflation-adjusted retirement corpus comparison across NPS, Index Fund, Gold, Silver, Bonds, and REITs")
public class AlternativeInvestmentResponse {

    @Schema(description = "Investor's current age")
    private int age;

    @Schema(description = "Investment horizon in years until retirement (age 60)")
    private int yearsToRetirement;

    @Schema(description = "Total remanent invested (from valid transactions, after Q/P period rules)")
    private double totalInvested;

    @Schema(description = "Annual inflation rate used for adjustment (%)")
    private double inflationRatePct;

    // ── Six asset-class scenarios ─────────────────────────────────────────────

    @Schema(description = "NPS — National Pension Scheme (7.11% p.a., tax benefit Sec. 80CCD)")
    private AlternativeScenario nps;

    @Schema(description = "NIFTY 50 Index Fund (14.49% p.a., highest growth potential)")
    private AlternativeScenario indexFund;

    @Schema(description = "Gold — Sovereign Gold Bond (11.50% p.a., inflation hedge)")
    private AlternativeScenario gold;

    @Schema(description = "Silver — MCX Digital Silver (9.80% p.a., industrial + store of value)")
    private AlternativeScenario silver;

    @Schema(description = "GOI Bonds — Government of India 10-yr Gilt (7.50% p.a., capital safety)")
    private AlternativeScenario bonds;

    @Schema(description = "REITs — Real Estate Investment Trusts (10.20% p.a., rental income + growth)")
    private AlternativeScenario reits;

    // ── Portfolio Intelligence ────────────────────────────────────────────────

    @Schema(description = "Top-ranked asset class by inflation-adjusted corpus")
    private String topPick;

    @Schema(description = "All 6 scenarios ranked 1st to 6th by corpus")
    private List<String> ranking;

    @Schema(description = "Suggested diversified portfolio allocation across asset classes")
    private String portfolioSuggestion;

    @Schema(description = "Estimated corpus from the suggested diversified portfolio")
    private double diversifiedCorpus;

    @Schema(description = "Advantage of the diversified portfolio over pure NPS (INR)")
    private double diversifiedAdvantageOverNps;

    // ── Inner Types ───────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Retirement corpus projection for a single asset class")
    public static class AlternativeScenario {

        @Schema(description = "Asset class name")
        private String name;

        @Schema(description = "Financial instrument details")
        private String instrument;

        @Schema(description = "Annual return rate (%)")
        private double annualRatePct;

        @Schema(description = "Nominal corpus before inflation adjustment (INR)")
        private double nominalCorpus;

        @Schema(description = "Inflation-adjusted (real) corpus at retirement (INR)")
        private double realCorpus;

        @Schema(description = "Real profit over total invested (INR)")
        private double realProfit;

        @Schema(description = "Return on investment ratio (realCorpus / totalInvested)")
        private double roiMultiple;

        @Schema(description = "Risk level: LOW / MEDIUM / HIGH")
        private String riskLevel;

        @Schema(description = "Liquidity: LOW / MEDIUM / HIGH")
        private String liquidity;

        @Schema(description = "Tax treatment on gains")
        private String taxTreatment;

        @Schema(description = "Key regulatory or structural notes")
        private String notes;

        @Schema(description = "Rank among all 6 alternatives (1 = best corpus)")
        private int rank;
    }
}
