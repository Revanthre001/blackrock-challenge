package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.util.IntervalTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BONUS Service: Alternative Investment comparison across 6 asset classes.
 *
 * <h3>Asset Classes and Annual Rates</h3>
 * <pre>
 *   NPS          — 7.11%  (PFRDA average 10-yr NPS Tier-1 equity + debt blend)
 *   NIFTY 50     — 14.49% (NIFTY 50 10-year CAGR as of 2024)
 *   Gold         — 11.50% (Sovereign Gold Bond 10-yr CAGR; RBI SGB scheme)
 *   Silver       — 9.80%  (MCX Silver 10-yr CAGR; higher volatility than gold)
 *   GOI Bonds    — 7.50%  (10-yr Government of India Gilt yield; RBI)
 *   REITs        — 10.20% (Embassy REIT + Brookfield + Mindspace avg since 2019)
 * </pre>
 *
 * <h3>Formula</h3>
 * <pre>
 *   Nominal = P × (1 + rate)^t
 *   Real    = Nominal / (1 + inflation)^t
 * </pre>
 *
 * <h3>Diversified Portfolio Suggestion</h3>
 * <pre>
 *   40% NIFTY 50  — maximum long-term growth
 *   25% NPS       — tax deduction benefit under Sec. 80CCD
 *   15% Gold      — inflation hedge + crisis protection
 *   10% REITs     — passive rental income + real-estate exposure
 *    7% Silver    — industrial demand + monetary metal
 *    3% GOI Bonds — capital safety + liquidity floor
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlternativeInvestmentService {

    // ── Annual Return Rates ───────────────────────────────────────────────────
    public static final double GOLD_RATE   = 0.1150;  // Sovereign Gold Bond 10-yr CAGR
    public static final double SILVER_RATE = 0.0980;  // MCX Silver 10-yr CAGR
    public static final double BONDS_RATE  = 0.0750;  // GOI 10-yr Gilt yield
    public static final double REITS_RATE  = 0.1020;  // Indian REIT average CAGR

    // ── Diversified Portfolio Weights ─────────────────────────────────────────
    private static final double W_INDEX  = 0.40;
    private static final double W_NPS    = 0.25;
    private static final double W_GOLD   = 0.15;
    private static final double W_REITS  = 0.10;
    private static final double W_SILVER = 0.07;
    private static final double W_BONDS  = 0.03;

    private final TransactionService       transactionService;
    private final ReturnCalculationService returnCalculationService;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Computes inflation-adjusted retirement corpuses for all 6 asset classes
     * and generates a diversified portfolio suggestion.
     *
     * @param request the investor's profile and transaction data
     * @return full alternative investment comparison
     */
    public AlternativeInvestmentResponse compute(ReturnsRequest request) {
        log.info("Computing alternative investments for age={}, wage={}", request.getAge(), request.getWage());

        List<QPeriod> qPeriods = safe(request.getQ());
        List<PPeriod> pPeriods = safe(request.getP());
        List<KPeriod> kPeriods = safe(request.getK());

        // ── Transaction pipeline (identical to ReturnCalculationService) ──────
        List<TransactionDto> parsed    = transactionService.parse(request.getTransactions());
        ValidationResponse   validated = transactionService.splitValidInvalid(parsed);
        List<TransactionDto> validTxs  = validated.getValid();

        IntervalTree<TransactionService.IndexedQPeriod> qTree = transactionService.buildQTree(qPeriods);
        IntervalTree<PPeriod> pTree = transactionService.buildPTree(pPeriods);
        IntervalTree<KPeriod> kTree = transactionService.buildKTree(kPeriods);
        List<TransactionDto> processed = transactionService.applyPeriods(validTxs, qTree, pTree, kTree);

        double totalInvested = processed.stream()
                .filter(tx -> tx.getRemanent() != null)
                .mapToDouble(TransactionDto::getRemanent)
                .sum();
        totalInvested = r2(totalInvested);

        int    t         = returnCalculationService.computeYears(request.getAge());
        double inflation = request.getInflation() / 100.0;

        // ── Compute each scenario ─────────────────────────────────────────────
        AlternativeInvestmentResponse.AlternativeScenario npsScen    = buildScenario("NPS",
                "National Pension Scheme — PFRDA regulated, Tier-I",
                ReturnCalculationService.NPS_RATE, totalInvested, t, inflation,
                "LOW", "LOW",
                "60% lump-sum tax-free; 40% annuity taxable as income (PFRDA mandate)",
                "Section 80CCD tax deduction up to ₹2,00,000/year");

        AlternativeInvestmentResponse.AlternativeScenario indexScen  = buildScenario("NIFTY 50 Index Fund",
                "NIFTY 50 ETF / Index Mutual Fund — BSE/NSE listed",
                ReturnCalculationService.INDEX_RATE, totalInvested, t, inflation,
                "HIGH", "HIGH",
                "LTCG: 12.5% on gains above ₹1.25L/year (Budget 2024)",
                "No withdrawal restrictions; highest historical CAGR of 6 options");

        AlternativeInvestmentResponse.AlternativeScenario goldScen   = buildScenario("Gold",
                "Sovereign Gold Bond (SGB) — RBI issued, 2.5% interest + price appreciation",
                GOLD_RATE, totalInvested, t, inflation,
                "MEDIUM", "MEDIUM",
                "LTCG with indexation benefit; SGB maturity proceeds tax-free",
                "8-year lock-in (SGB); hedge against currency devaluation and inflation");

        AlternativeInvestmentResponse.AlternativeScenario silverScen = buildScenario("Silver",
                "MCX Silver / Digital Silver ETF — industrial + monetary metal",
                SILVER_RATE, totalInvested, t, inflation,
                "HIGH", "MEDIUM",
                "LTCG: 12.5% on gains above ₹1.25L/year (Budget 2024)",
                "Higher volatility than gold; strong industrial demand (EVs, solar panels)");

        AlternativeInvestmentResponse.AlternativeScenario bondsScen  = buildScenario("GOI Bonds",
                "Government of India 10-Year Gilt Bond — RBI / Sovereign guarantee",
                BONDS_RATE, totalInvested, t, inflation,
                "LOW", "MEDIUM",
                "Interest taxable as income; no LTCG on government bonds",
                "Zero default risk; suitable as capital safety floor in diversified portfolio");

        AlternativeInvestmentResponse.AlternativeScenario reitsScen  = buildScenario("REITs",
                "Real Estate Investment Trusts — Embassy / Brookfield / Mindspace REIT",
                REITS_RATE, totalInvested, t, inflation,
                "MEDIUM", "HIGH",
                "Dividend income taxable; LTCG 12.5% on capital gains (Budget 2024)",
                "90% income distributed as dividends; real-estate exposure without property ownership");

        // ── Rank by real corpus ───────────────────────────────────────────────
        List<AlternativeInvestmentResponse.AlternativeScenario> all = new ArrayList<>(List.of(
                npsScen, indexScen, goldScen, silverScen, bondsScen, reitsScen));
        all.sort(Comparator.comparingDouble(
                AlternativeInvestmentResponse.AlternativeScenario::getRealCorpus).reversed());
        for (int i = 0; i < all.size(); i++) all.get(i).setRank(i + 1);
        List<String> ranking = all.stream()
                .map(s -> String.format("#%d %s — ₹%.0f", s.getRank(), s.getName(), s.getRealCorpus()))
                .collect(Collectors.toList());

        // ── Diversified portfolio corpus ──────────────────────────────────────
        double divCorpus = r2(
                W_INDEX  * indexScen.getRealCorpus() +
                W_NPS    * npsScen.getRealCorpus()   +
                W_GOLD   * goldScen.getRealCorpus()  +
                W_REITS  * reitsScen.getRealCorpus() +
                W_SILVER * silverScen.getRealCorpus()+
                W_BONDS  * bondsScen.getRealCorpus());

        double divAdvantage = r2(divCorpus - npsScen.getRealCorpus());

        String portfolioSuggestion = String.format(
                "40%% NIFTY 50 Index Fund + 25%% NPS (₹2L tax cap) + 15%% Gold (SGB) " +
                "+ 10%% REITs (Embassy/Brookfield) + 7%% Silver + 3%% GOI Bonds " +
                "→ Projected corpus: ₹%.0f (real, inflation-adjusted)",
                divCorpus);

        return AlternativeInvestmentResponse.builder()
                .age(request.getAge())
                .yearsToRetirement(t)
                .totalInvested(totalInvested)
                .inflationRatePct(request.getInflation())
                .nps(npsScen)
                .indexFund(indexScen)
                .gold(goldScen)
                .silver(silverScen)
                .bonds(bondsScen)
                .reits(reitsScen)
                .topPick(all.get(0).getName())
                .ranking(ranking)
                .portfolioSuggestion(portfolioSuggestion)
                .diversifiedCorpus(divCorpus)
                .diversifiedAdvantageOverNps(divAdvantage)
                .build();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private AlternativeInvestmentResponse.AlternativeScenario buildScenario(
            String name, String instrument,
            double rate, double principal,
            int years, double inflation,
            String risk, String liquidity,
            String taxTreatment, String notes) {

        double nominal = principal * Math.pow(1 + rate, years);
        double real    = nominal   / Math.pow(1 + inflation, years);
        double profit  = r2(real - principal);
        double roi     = principal > 0 ? r2(real / principal) : 0.0;

        return AlternativeInvestmentResponse.AlternativeScenario.builder()
                .name(name)
                .instrument(instrument)
                .annualRatePct(rate * 100)
                .nominalCorpus(r2(nominal))
                .realCorpus(r2(real))
                .realProfit(profit)
                .roiMultiple(roi)
                .riskLevel(risk)
                .liquidity(liquidity)
                .taxTreatment(taxTreatment)
                .notes(notes)
                .build();
    }

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    @SuppressWarnings("unchecked")
    private static <T> List<T> safe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
