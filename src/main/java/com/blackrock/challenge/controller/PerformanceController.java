package com.blackrock.challenge.controller;

import com.blackrock.challenge.dto.ApiErrorResponse;
import com.blackrock.challenge.dto.PerformanceResponse;
import com.blackrock.challenge.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system performance metrics.
 *
 * <p>Endpoint:
 * - GET /blackrock/challenge/v1/performance → JVM uptime, heap memory, thread count
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Performance", description = "System performance and JVM metrics reporting")
public class PerformanceController {

    private final PerformanceService performanceService;

    @Operation(
            summary = "Get system performance metrics",
            description = "Returns current JVM performance metrics: " +
                    "uptime since application start (HH:mm:ss.SSS), " +
                    "heap memory usage in MB, and active thread count. " +
                    "Demonstrates production-grade observability."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Performance metrics retrieved",
                    content = @Content(schema = @Schema(implementation = PerformanceResponse.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/performance")
    public ResponseEntity<PerformanceResponse> getPerformance() {
        log.debug("GET /performance requested");
        PerformanceResponse response = performanceService.getPerformance();
        return ResponseEntity.ok(response);
    }
}
