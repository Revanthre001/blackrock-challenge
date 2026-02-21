package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Q Period: fixed amount override period.
 * When a transaction falls in this range, remanent is replaced by fixed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Q Period: overrides remanent with a fixed investment amount")
public class QPeriod {

    @Schema(description = "Fixed investment amount (replaces remanent)", example = "0.0")
    private double fixed;

    @NotBlank(message = "Q period start date is required")
    @Schema(description = "Period start datetime (inclusive)", example = "2023-07-01 00:00:00")
    private String start;

    @NotBlank(message = "Q period end date is required")
    @Schema(description = "Period end datetime (inclusive)", example = "2023-07-31 23:59:59")
    private String end;
}
