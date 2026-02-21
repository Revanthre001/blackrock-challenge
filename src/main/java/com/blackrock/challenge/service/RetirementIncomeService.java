package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * BONUS Service: Retirement Income Bridge.
 *
 * <p>Converts the projected corpus into monthly income estimates for three strategies:
 *
 * <ul>
 *   <li><b>NPS (Pure)</b> — PFRDA mandate: 40% corpus into annuity @ 6.5% p.a.
 *       generating guaranteed monthly income; 60% available as tax-free lump sum.</li>
 *   <li><b>Index Fund (Pure)</b> — Full corpus, 4% Safe Withdrawal Rate (SWR).
 *       Based on William Bengen / Trinity Study: sustains withdrawals 30+ years.</li>
 *   <li><b>Tax-Optimal Hybrid</b> — NPS portion: annuity income; Index portion: SWR.
 *       Dual income streams: guaranteed floor + flexible equity withdrawal.</li>
 * </ul>
 *
 * <p>Endpoint: POST /blackrock/challenge/v1/returns:retirement-income
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetirementIncomeService {

    // PFRDA mandate: at retirement, 40% corpus → annuity; 60% tax-free lump sum
    public static final double NPS_ANNUITY_PCT = 0.40;
    public static final double NPS_LUMPSUM_PCT = 0.60;

    // Annuity rate — approximate market rate (LIC / PFRDA empanelled insurer)
    public static final double ANNUITY_RATE    = 0.065;

    // Safe Withdrawal Rate — Bengen (1994) / Trinity Study consensus
    public static final double SWR_RATE        = 0.04;

    private final ReturnCalculationService returnCalculationService;
    private final HybridAllocationService  hybridAllocationService;

    // ─── Public API ──────────────────────────────────────────────────────────

    public RetirementIncomeResponse computeRetirementIncome(ReturnsRequest request) {
        log.info("Computing retirement income bridge: age={}, wage={}",
                request.getAge(), request.getWage());

        ReturnsResponse npsRes   = returnCalculationService.calculateNpsReturns(request);
        ReturnsResponse indexRes = returnCalculationService.calculateIndexReturns(request);
        HybridResponse  hybrid   = hybridAllocationService.computeHybrid(request);

        int    years         = returnCalculationService.computeYears(request.getAge());
        double inflation     = request.getInflation() / 100.0;
        double projectedWage = request.getWage() * Math.pow(1 + inflation, years);

        RetirementIncomeResponse.IncomeScenario npsScenario    = buildNpsScenario(corpus(npsRes));
        RetirementIncomeResponse.IncomeScenario indexScenario  = buildIndexScenario(corpus(indexRes));
        RetirementIncomeResponse.IncomeScenario hybridScenario = buildHybridScenario(hybrid);

        double best = Math.max(
                Math.max(npsScenario.getMonthlyIncome(), indexScenario.getMonthlyIncome()),
                hybridScenario.getMonthlyIncome());

        String recommendation = hybridScenario.getMonthlyIncome() >= best - 0.01 ? "TAX_HYBRID"
                              : indexScenario.getMonthlyIncome()  >= best - 0.01 ? "INDEX_FUND"
                              : "NPS";

        return RetirementIncomeResponse.builder()
                .age(request.getAge())
                .yearsToRetirement(years)
                .projectedMonthlyWageAtRetirement(r2(projectedWage))
                .nps(npsScenario)
                .index(indexScenario)
                .hybrid(hybridScenario)
                .recommendation(recommendation)
                .reasoning(buildReasoning(npsScenario, indexScenario, hybridScenario, years))
                .build();
    }

    // ─── Scenario Builders ───────────────────────────────────────────────────

    private RetirementIncomeResponse.IncomeScenario buildNpsScenario(double totalCorpus) {
        double annuityPortion = totalCorpus * NPS_ANNUITY_PCT;
        double lumpSum        = totalCorpus * NPS_LUMPSUM_PCT;
        double monthlyIncome  = annuityPortion * ANNUITY_RATE / 12.0;

        return RetirementIncomeResponse.IncomeScenario.builder()
                .name("NPS — Pure Strategy")
                .totalCorpus(r2(totalCorpus))
                .monthlyIncome(r2(monthlyIncome))
                .annualIncome(r2(monthlyIncome * 12))
                .lumpSumAtRetirement(r2(lumpSum))
                .annuityCorpus(r2(annuityPortion))
                .structure("40% corpus → annuity @ 6.5% p.a. (PFRDA mandate) + 60% tax-free lump sum")
                .notes("Guaranteed monthly income for life. Lump sum can be reinvested in equity.")
                .build();
    }

    private RetirementIncomeResponse.IncomeScenario buildIndexScenario(double totalCorpus) {
        double monthlyIncome = totalCorpus * SWR_RATE / 12.0;

        return RetirementIncomeResponse.IncomeScenario.builder()
                .name("NIFTY 50 Index Fund — Pure Strategy")
                .totalCorpus(r2(totalCorpus))
                .monthlyIncome(r2(monthlyIncome))
                .annualIncome(r2(monthlyIncome * 12))
                .lumpSumAtRetirement(r2(totalCorpus))
                .annuityCorpus(0.0)
                .structure("4% Safe Withdrawal Rate — William Bengen / Trinity Study")
                .notes("Full corpus stays invested. Flexible withdrawals sustainable 30+ years.")
                .build();
    }

    private RetirementIncomeResponse.IncomeScenario buildHybridScenario(HybridResponse hybrid) {
        double npsCorpus   = hybrid.getNpsCorpus();
        double indexCorpus = hybrid.getIndexCorpus();
        double total       = hybrid.getHybridCorpus();

        // NPS portion: PFRDA annuity rule
        double npsMonthly   = npsCorpus   * NPS_ANNUITY_PCT * ANNUITY_RATE / 12.0;
        // Index portion: 4% SWR
        double indexMonthly = indexCorpus * SWR_RATE / 12.0;
        double totalMonthly = npsMonthly + indexMonthly;

        return RetirementIncomeResponse.IncomeScenario.builder()
                .name("Tax-Optimal Hybrid")
                .totalCorpus(r2(total))
                .monthlyIncome(r2(totalMonthly))
                .annualIncome(r2(totalMonthly * 12))
                .lumpSumAtRetirement(r2(npsCorpus * NPS_LUMPSUM_PCT + indexCorpus))
                .annuityCorpus(r2(npsCorpus * NPS_ANNUITY_PCT))
                .structure("NPS annuity floor + Index Fund SWR — dual income stream approach")
                .notes("Maximises tax efficiency, guaranteed income floor + flexible equity drawdown.")
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Sum of (amount + profit) across all k-periods = total real corpus. */
    private double corpus(ReturnsResponse r) {
        if (r == null || r.getSavingsByDates() == null) return 0.0;
        return r.getSavingsByDates().stream()
                .mapToDouble(s -> s.getAmount() + s.getProfit())
                .sum();
    }

    private String buildReasoning(RetirementIncomeResponse.IncomeScenario nps,
                                   RetirementIncomeResponse.IncomeScenario index,
                                   RetirementIncomeResponse.IncomeScenario hybrid,
                                   int years) {
        return String.format(
                "Over %d years: Tax-Hybrid generates ₹%.2f/month (guaranteed NPS annuity + flexible Index SWR). " +
                "Pure Index Fund: ₹%.2f/month (higher but no guaranteed floor). " +
                "Pure NPS: ₹%.2f/month (guaranteed for life, lower due to 7.11%% rate). " +
                "Hybrid recommendation: balances guaranteed income security with flexible equity drawdown.",
                years, hybrid.getMonthlyIncome(), index.getMonthlyIncome(), nps.getMonthlyIncome());
    }

    private static double r2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
