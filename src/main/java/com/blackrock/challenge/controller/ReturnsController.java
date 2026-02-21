package com.blackrock.challenge.controller;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.service.AlternativeInvestmentService;
import com.blackrock.challenge.service.CompareService;
import com.blackrock.challenge.service.HybridAllocationService;
import com.blackrock.challenge.service.RetirementIncomeService;
import com.blackrock.challenge.service.ReturnCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for investment returns calculation.
 *
 * <p>Endpoints:
 * - POST /blackrock/challenge/v1/returns:nps              → NPS returns with tax benefit
 * - POST /blackrock/challenge/v1/returns:index            → NIFTY 50 index fund returns
 * - POST /blackrock/challenge/v1/returns:compare          → Side-by-side comparison (BONUS)
 * - POST /blackrock/challenge/v1/returns:hybrid           → Tax-optimal hybrid (BONUS)
 * - POST /blackrock/challenge/v1/returns:retirement-income → Monthly retirement income (BONUS)
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Returns", description = "Investment return calculations for NPS and Index Fund (NIFTY 50)")
public class ReturnsController {

    private final ReturnCalculationService  returnCalculationService;
    private final CompareService             compareService;
    private final HybridAllocationService    hybridAllocationService;
    private final RetirementIncomeService    retirementIncomeService;
    private final AlternativeInvestmentService alternativeInvestmentService;

    // ─── NPS Returns ─────────────────────────────────────────────────────────

    @Operation(
            summary = "Calculate NPS retirement returns",
            description = "Calculates inflation-adjusted NPS returns with tax benefit. " +
                    "NPS rate: 7.11% p.a. compounded annually. " +
                    "Tax benefit = Tax(income) - Tax(income - NPS_Deduction) using marginal slabs. " +
                    "NPS deduction capped at min(invested, 10% of annual income, ₹2,00,000)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "NPS returns calculated",
                    content = @Content(schema = @Schema(implementation = ReturnsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/returns:nps")
    public ResponseEntity<ReturnsResponse> calculateNpsReturns(
            @Valid @RequestBody ReturnsRequest request) {

        log.info("POST /returns:nps — age={}, wage={}, transactions={}",
                request.getAge(), request.getWage(), request.getTransactions().size());
        ReturnsResponse result = returnCalculationService.calculateNpsReturns(request);
        return ResponseEntity.ok(result);
    }

    // ─── Index Fund Returns ───────────────────────────────────────────────────

    @Operation(
            summary = "Calculate Index Fund (NIFTY 50) retirement returns",
            description = "Calculates inflation-adjusted NIFTY 50 returns. " +
                    "Index Fund rate: 14.49% p.a. compounded annually. No tax restrictions or benefits."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Index Fund returns calculated",
                    content = @Content(schema = @Schema(implementation = ReturnsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/returns:index")
    public ResponseEntity<ReturnsResponse> calculateIndexReturns(
            @Valid @RequestBody ReturnsRequest request) {

        log.info("POST /returns:index — age={}, wage={}, transactions={}",
                request.getAge(), request.getWage(), request.getTransactions().size());
        ReturnsResponse result = returnCalculationService.calculateIndexReturns(request);
        return ResponseEntity.ok(result);
    }

    // ─── BONUS: Compare ───────────────────────────────────────────────────────

    @Operation(
            summary = "[BONUS] Compare NPS vs Index Fund side-by-side",
            description = "Runs both NPS and Index Fund calculations simultaneously and returns a side-by-side " +
                    "comparison with a recommendation and human-readable narrative. " +
                    "Saves clients from calling two endpoints and comparing manually."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparison complete",
                    content = @Content(schema = @Schema(implementation = CompareResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/returns:compare")
    public ResponseEntity<CompareResponse> compareReturns(
            @Valid @RequestBody ReturnsRequest request) {

        log.info("POST /returns:compare — age={}, wage={}", request.getAge(), request.getWage());
        CompareResponse result = compareService.compare(request);
        return ResponseEntity.ok(result);
    }

    // ─── BONUS: Hybrid ────────────────────────────────────────────────────────

    @Operation(
            summary = "[BONUS] Tax-optimal hybrid NPS + Index Fund allocation",
            description = "Computes the mathematically optimal split between NPS and NIFTY 50 Index Fund. " +
                    "Routes investment to NPS up to the ₹2,00,000/yr tax deduction cap (Sec. 80CCD), " +
                    "then routes the remainder to Index Fund to capture 14.49% equity returns. " +
                    "Uses principal linearity of compound interest to scale per k-period accurately. " +
                    "Returns full comparison: Pure NPS vs Pure Index vs Hybrid corpus."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hybrid allocation computed",
                    content = @Content(schema = @Schema(implementation = HybridResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/returns:hybrid")
    public ResponseEntity<HybridResponse> computeHybrid(
            @Valid @RequestBody ReturnsRequest request) {

        log.info("POST /returns:hybrid — age={}, wage={}", request.getAge(), request.getWage());
        HybridResponse result = hybridAllocationService.computeHybrid(request);
        return ResponseEntity.ok(result);
    }

    // ─── BONUS: Retirement Income Bridge ─────────────────────────────────────

    @Operation(
            summary = "[BONUS] Retirement income bridge — corpus to monthly income",
            description = "Converts the projected retirement corpus into monthly income estimates for three strategies: " +
                    "NPS (40% annuity @ 6.5% p.a. per PFRDA mandate + 60% lump sum), " +
                    "Index Fund (4% Safe Withdrawal Rate — Bengen/Trinity Study), " +
                    "and Tax-Optimal Hybrid (NPS annuity floor + Index SWR drawdown). " +
                    "Also shows projected wage at retirement after inflation adjustment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retirement income computed",
                    content = @Content(schema = @Schema(implementation = RetirementIncomeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/returns:retirement-income")
    public ResponseEntity<RetirementIncomeResponse> computeRetirementIncome(
            @Valid @RequestBody ReturnsRequest request) {

        log.info("POST /returns:retirement-income — age={}, wage={}", request.getAge(), request.getWage());
        RetirementIncomeResponse result = retirementIncomeService.computeRetirementIncome(request);
        return ResponseEntity.ok(result);
    }

    // ─── BONUS: Alternative Investments ──────────────────────────────────────

    @Operation(
            summary = "[BONUS] Compare returns across 6 asset classes",
            description = "Compares the investor's remanent invested across six asset classes: " +
                    "NPS (7.11%), NIFTY 50 Index Fund (14.49%), Gold / SGB (11.50%), " +
                    "Silver / MCX (9.80%), GOI Bonds (7.50%), and REITs (10.20%). " +
                    "All returns are inflation-adjusted to real (purchasing-power) terms. " +
                    "Also generates a suggested diversified portfolio allocation with expected corpus. " +
                    "Rates are based on 10-year historical CAGRs from PFRDA, NSE, RBI, and MCX data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alternative investment comparison computed",
                    content = @Content(schema = @Schema(implementation = AlternativeInvestmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/returns:alternatives")
    public ResponseEntity<AlternativeInvestmentResponse> computeAlternatives(
            @Valid @RequestBody ReturnsRequest request) {

        log.info("POST /returns:alternatives — age={}, wage={}", request.getAge(), request.getWage());
        AlternativeInvestmentResponse result = alternativeInvestmentService.compute(request);
        return ResponseEntity.ok(result);
    }
}
