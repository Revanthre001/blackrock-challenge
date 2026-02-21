package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.util.DateTimeUtil;
import com.blackrock.challenge.util.IntervalTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bonus service providing comparison and summary endpoints.
 * Demonstrates value-add beyond the required problem statement.
 *
 * <p>Endpoints powered by this service:
 * - /returns:compare  — side-by-side NPS vs Index Fund with winner recommendation
 * - /savings:summary  — human-readable narrative for each k period
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompareService {

    private final ReturnCalculationService returnCalculationService;
    private final TransactionService transactionService;

    /**
     * Returns NPS and Index Fund results side-by-side with a recommendation.
     */
    public CompareResponse compare(ReturnsRequest request) {
        log.info("Running NPS vs Index Fund comparison for age={}", request.getAge());

        ReturnsResponse nps = returnCalculationService.calculateNpsReturns(request);
        ReturnsResponse index = returnCalculationService.calculateIndexReturns(request);

        // Determine winner by total real return across all k periods
        double npsTotal = sumProfits(nps) + sumTaxBenefits(nps);
        double indexTotal = sumProfits(index);

        String recommendation = npsTotal >= indexTotal ? "NPS" : "INDEX_FUND";
        String reasoning = buildReasoning(npsTotal, indexTotal, request.getAge());
        List<String> summaries = buildSummaries(nps, index, request);

        return CompareResponse.builder()
                .nps(nps)
                .index(index)
                .recommendation(recommendation)
                .reasoning(reasoning)
                .totalInvested(nps.getTotalTransactionAmount())
                .summaries(summaries)
                .build();
    }

    private double sumProfits(ReturnsResponse response) {
        if (response.getSavingsByDates() == null) return 0.0;
        return response.getSavingsByDates().stream()
                .mapToDouble(ReturnsResponse.SavingsByDate::getProfit)
                .sum();
    }

    private double sumTaxBenefits(ReturnsResponse response) {
        if (response.getSavingsByDates() == null) return 0.0;
        return response.getSavingsByDates().stream()
                .mapToDouble(ReturnsResponse.SavingsByDate::getTaxBenefit)
                .sum();
    }

    private String buildReasoning(double npsEffective, double indexEffective, int age) {
        int years = returnCalculationService.computeYears(age);
        if (indexEffective > npsEffective) {
            return String.format(
                    "Over %d years, Index Fund (NIFTY 50 @ 14.49%%) yields a higher real return (₹%.2f profit) " +
                            "vs NPS (₹%.2f effective incl. tax benefit). For long horizons, equities outperform fixed income.",
                    years, indexEffective, npsEffective);
        } else {
            return String.format(
                    "Over %d years, NPS (7.11%% + tax shield) is preferred with ₹%.2f effective return " +
                            "vs Index Fund ₹%.2f. Tax benefit makes NPS competitive for higher income brackets.",
                    years, npsEffective, indexEffective);
        }
    }

    private List<String> buildSummaries(ReturnsResponse nps, ReturnsResponse index, ReturnsRequest request) {
        List<String> summaries = new ArrayList<>();

        if (nps.getSavingsByDates() == null) return summaries;

        int t = returnCalculationService.computeYears(request.getAge());

        for (int i = 0; i < nps.getSavingsByDates().size(); i++) {
            ReturnsResponse.SavingsByDate npsEntry = nps.getSavingsByDates().get(i);
            ReturnsResponse.SavingsByDate indexEntry = index.getSavingsByDates().get(i);

            double invested = npsEntry.getAmount();
            double npsReturn = invested + npsEntry.getProfit() + npsEntry.getTaxBenefit();
            double indexReturn = invested + indexEntry.getProfit();
            double indexMultiplier = invested > 0 ? indexReturn / invested : 0;

            summaries.add(String.format(
                    "Period [%s → %s]: Invest ₹%.2f for %d years → " +
                            "NPS real value ₹%.2f (profit ₹%.2f + tax ₹%.2f) | " +
                            "Index real value ₹%.2f (%.1fx return). %s wins.",
                    npsEntry.getStart(), npsEntry.getEnd(),
                    invested, t,
                    npsReturn, npsEntry.getProfit(), npsEntry.getTaxBenefit(),
                    indexReturn, indexMultiplier,
                    npsReturn >= indexReturn ? "NPS" : "Index Fund"));
        }

        return summaries;
    }
}
