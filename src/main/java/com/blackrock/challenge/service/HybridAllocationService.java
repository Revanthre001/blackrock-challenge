package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.util.DateTimeUtil;
import com.blackrock.challenge.util.IntervalTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BONUS Service: Tax-First Hybrid allocation between NPS and Index Fund.
 *
 * <h3>Strategy</h3>
 * NPS gives a tax deduction up to ₹2,00,000 per year (Sec. 80C + 80CCD(1B)).
 * Beyond that cap, additional NPS investment earns only 7.11% with zero extra
 * tax benefit, while the same rupee in NIFTY 50 earns 14.49%.
 *
 * <p>Optimal rule:
 * <pre>
 *   NPS contribution  = min(annual_remanent_per_year, ₹2,00,000)
 *   Index contribution = annual_remanent_per_year − NPS_contribution
 * </pre>
 *
 * <h3>Math justification</h3>
 * Compound interest A = P·(1+r)^t is linear in P, so
 * {@code hybrid_corpus = npsRatio·nps_full_corpus + indexRatio·index_full_corpus}
 * is exact (not an approximation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridAllocationService {

    private final TransactionService       transactionService;
    private final ReturnCalculationService returnCalculationService;

    // ─── Public API ──────────────────────────────────────────────────────────

    public HybridResponse computeHybrid(ReturnsRequest request) {
        log.info("Computing tax-optimal hybrid allocation: age={}, wage={}",
                request.getAge(), request.getWage());

        List<QPeriod> qPeriods = safe(request.getQ());
        List<PPeriod> pPeriods = safe(request.getP());
        List<KPeriod> kPeriods = safe(request.getK());

        // ── Full transaction pipeline (same as ReturnCalculationService) ──────
        List<TransactionDto> parsed    = transactionService.parse(request.getTransactions());
        ValidationResponse   validated = transactionService.splitValidInvalid(parsed);
        List<TransactionDto> validTxs  = validated.getValid();

        IntervalTree<TransactionService.IndexedQPeriod> qTree = transactionService.buildQTree(qPeriods);
        IntervalTree<PPeriod>  pTree = transactionService.buildPTree(pPeriods);
        IntervalTree<KPeriod>  kTree = transactionService.buildKTree(kPeriods);
        List<TransactionDto>   processed = transactionService.applyPeriods(validTxs, qTree, pTree, kTree);

        // ── Compute optimal NPS allocation ratio ──────────────────────────────
        // Group remanent by calendar year, cap each year at MAX_NPS_DEDUCTION
        Map<Integer, Double> remanentByYear = processed.stream()
                .filter(tx -> tx.getRemanent() != null && tx.getRemanent() > 0)
                .collect(Collectors.groupingBy(
                        tx -> {
                            try { return DateTimeUtil.parse(tx.getDate()).getYear(); }
                            catch (Exception e) { return 0; }
                        },
                        Collectors.summingDouble(TransactionDto::getRemanent)));

        double totalRemanent   = processed.stream()
                .filter(tx -> tx.getRemanent() != null)
                .mapToDouble(TransactionDto::getRemanent).sum();

        double totalNpsOptimal = remanentByYear.values().stream()
                .mapToDouble(yr -> Math.min(yr, ReturnCalculationService.MAX_NPS_DEDUCTION))
                .sum();
        double totalIdxOptimal = Math.max(0, totalRemanent - totalNpsOptimal);

        double npsRatio   = totalRemanent > 0 ? totalNpsOptimal / totalRemanent : 1.0;
        double indexRatio = 1.0 - npsRatio;

        // ── Get full-allocation returns (linearity: scale by ratio) ───────────
        ReturnsResponse fullNps   = returnCalculationService.calculateNpsReturns(request);
        ReturnsResponse fullIndex = returnCalculationService.calculateIndexReturns(request);

        // ── Scale per k-period ────────────────────────────────────────────────
        List<HybridResponse.PeriodAllocation> periods = new ArrayList<>();
        double npsCorpus   = 0;
        double indexCorpus = 0;
        double totalTax    = 0;

        List<ReturnsResponse.SavingsByDate> npsSavings = fullNps.getSavingsByDates();
        List<ReturnsResponse.SavingsByDate> idxSavings = fullIndex.getSavingsByDates();
        int len = npsSavings == null ? 0 : npsSavings.size();

        for (int i = 0; i < len; i++) {
            ReturnsResponse.SavingsByDate np = npsSavings.get(i);
            ReturnsResponse.SavingsByDate ix = (idxSavings != null && i < idxSavings.size())
                    ? idxSavings.get(i) : null;

            double periodRem  = np.getAmount();
            double npsAmt     = r2(periodRem  * npsRatio);
            double idxAmt     = r2(periodRem  * indexRatio);
            double npsProfit  = r2(np.getProfit() * npsRatio);
            double idxProfit  = r2(ix != null ? ix.getProfit() * indexRatio : 0.0);
            double taxBen     = r2(np.getTaxBenefit() * npsRatio);

            npsCorpus   += npsAmt  + npsProfit;
            indexCorpus += idxAmt  + idxProfit;
            totalTax    += taxBen;

            String routing = npsRatio >= 0.999 ? "FULL_NPS"
                           : indexRatio >= 0.999 ? "FULL_INDEX"
                           : String.format("SPLIT  %.0f%% NPS + %.0f%% Index",
                                   npsRatio * 100, indexRatio * 100);

            periods.add(HybridResponse.PeriodAllocation.builder()
                    .start(np.getStart())
                    .end(np.getEnd())
                    .totalRemanent(r2(periodRem))
                    .npsAmount(npsAmt)
                    .indexAmount(idxAmt)
                    .npsProfit(npsProfit)
                    .indexProfit(idxProfit)
                    .taxBenefit(taxBen)
                    .routing(routing)
                    .build());
        }

        double hybridCorpus = r2(npsCorpus + indexCorpus);
        double pureNps  = npsSavings == null ? 0 : npsSavings.stream()
                .mapToDouble(s -> s.getAmount() + s.getProfit()).sum();
        double pureIdx  = idxSavings == null ? 0 : idxSavings.stream()
                .mapToDouble(s -> s.getAmount() + s.getProfit()).sum();

        String strategy  = buildStrategy(npsRatio, indexRatio, totalNpsOptimal, totalIdxOptimal);
        String reasoning = buildReasoning(npsRatio, indexRatio, totalTax);

        return HybridResponse.builder()
                .totalInvested(r2(totalRemanent))
                .npsContribution(r2(totalNpsOptimal))
                .indexContribution(r2(totalIdxOptimal))
                .estimatedTaxSaved(r2(totalTax))
                .npsCorpus(r2(npsCorpus))
                .indexCorpus(r2(indexCorpus))
                .hybridCorpus(hybridCorpus)
                .pureNpsCorpus(r2(pureNps))
                .pureIndexCorpus(r2(pureIdx))
                .hybridAdvantageOverNps(r2(hybridCorpus - pureNps))
                .hybridAdvantageOverIndex(r2(hybridCorpus - pureIdx))
                .npsAllocationPct(r2(npsRatio * 100))
                .indexAllocationPct(r2(indexRatio * 100))
                .allocationStrategy(strategy)
                .reasoning(reasoning)
                .periodAllocations(periods)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildStrategy(double npsRatio, double indexRatio,
                                  double npsAmt, double idxAmt) {
        if (npsRatio >= 0.999)
            return "Full NPS — total investment is within the ₹2,00,000/yr tax deduction limit";
        if (indexRatio >= 0.999)
            return "Full Index Fund — no NPS deduction headroom (investment below minimum threshold)";
        return String.format(
                "Tax-First Hybrid: %.0f%% NPS (₹%.2f — exhausts ₹%.0f/yr deduction cap) + " +
                "%.0f%% NIFTY 50 Index Fund (₹%.2f — above deduction cap)",
                npsRatio * 100, npsAmt,
                ReturnCalculationService.MAX_NPS_DEDUCTION,
                indexRatio * 100, idxAmt);
    }

    private String buildReasoning(double npsRatio, double indexRatio, double taxSaved) {
        if (npsRatio >= 0.999)
            return "Total annual investment stays within the NPS deduction cap. " +
                   "Full NPS allocation maximises tax benefit.";
        return String.format(
                "%.0f%% routed to NPS exhausts the ₹%.0f annual tax deduction, saving ₹%.2f in taxes. " +
                "Remaining %.0f%% invested in NIFTY 50 (%.2f%% p.a.) captures higher equity growth. " +
                "This hybrid dominates both pure-NPS and pure-Index strategies.",
                npsRatio * 100, ReturnCalculationService.MAX_NPS_DEDUCTION, taxSaved,
                indexRatio * 100, ReturnCalculationService.INDEX_RATE * 100);
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static double r2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
