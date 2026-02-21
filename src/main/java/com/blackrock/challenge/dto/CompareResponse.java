package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the /returns:compare bonus endpoint.
 * Returns NPS and Index Fund results side-by-side with a winner recommendation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Side-by-side comparison of NPS vs Index Fund returns")
public class CompareResponse {

    @Schema(description = "NPS (National Pension Scheme) returns")
    private ReturnsResponse nps;

    @Schema(description = "Index Fund (NIFTY 50) returns")
    private ReturnsResponse index;

    @Schema(description = "Recommended investment vehicle", example = "INDEX_FUND")
    private String recommendation;

    @Schema(description = "Human-readable recommendation reasoning")
    private String reasoning;

    @Schema(description = "Total invested amount (same for both)", example = "145.0")
    private Double totalInvested;

    @Schema(description = "Human-readable savings summary per k period")
    private List<String> summaries;
}
