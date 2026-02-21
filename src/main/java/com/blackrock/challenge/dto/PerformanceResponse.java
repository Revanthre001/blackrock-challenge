package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the /performance endpoint.
 * Reports JVM uptime, heap memory usage, and live thread count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System performance metrics")
public class PerformanceResponse {

    @Schema(description = "Application uptime since startup", example = "00:11:54.135")
    private String time;

    @Schema(description = "JVM used heap memory in MB", example = "25.11 MB")
    private String memory;

    @Schema(description = "Number of active JVM threads", example = "16")
    private Integer threads;
}
