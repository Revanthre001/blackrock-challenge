package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * P Period: extra amount addition period.
 * When a transaction falls in this range, extra is ADDED to remanent (cumulative, not replacing).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "P Period: adds extra amount to the remanent (cumulative)")
public class PPeriod {

    @Schema(description = "Extra investment amount added on top of remanent", example = "25.0")
    private double extra;

    @NotBlank(message = "P period start date is required")
    @Schema(description = "Period start datetime (inclusive)", example = "2023-10-01 08:00:00")
    private String start;

    @NotBlank(message = "P period end date is required")
    @Schema(description = "Period end datetime (inclusive)", example = "2023-12-31 19:59:59")
    private String end;
}
