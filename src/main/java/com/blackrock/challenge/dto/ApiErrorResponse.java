package com.blackrock.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized API error response for all error conditions.
 * Used by the global exception handler.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standardized API error response")
public class ApiErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error category", example = "VALIDATION_ERROR")
    private String error;

    @Schema(description = "Human-readable error message")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/blackrock/challenge/v1/transactions:parse")
    private String path;

    @Schema(description = "Timestamp of the error", example = "2023-10-12 20:15:30")
    private String timestamp;
}
