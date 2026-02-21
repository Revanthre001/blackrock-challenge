package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * K Period: evaluation grouping period.
 * Transactions are summed independently per k period.
 * A transaction can belong to multiple k periods simultaneously.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "K Period: groups transactions for independent return calculation")
public class KPeriod {

    @NotBlank(message = "K period start date is required")
    @Schema(description = "Period start datetime (inclusive)", example = "2023-01-01 00:00:00")
    private String start;

    @NotBlank(message = "K period end date is required")
    @Schema(description = "Period end datetime (inclusive)", example = "2023-12-31 23:59:59")
    private String end;
}
