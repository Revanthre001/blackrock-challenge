package com.blackrock.challenge.controller;

import com.blackrock.challenge.dto.ReturnsRequest;
import com.blackrock.challenge.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * BONUS endpoint: generates a downloadable PDF investment comparison report.
 *
 * <p>Accepts the same {@link ReturnsRequest} body as /returns:compare and
 * returns a styled A4 PDF showing NPS vs Index Fund returns, tax benefits,
 * and a data-driven recommendation for retirement planning.
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
@Tag(name = "Report", description = "PDF retirement savings report generation (BONUS)")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
        summary     = "Generate PDF retirement savings report",
        description = "Calculates NPS and Index Fund returns from the provided investor profile "
                    + "and returns a fully styled A4 PDF report with comparison tables, tax benefit "
                    + "breakdown, and a data-driven recommendation. "
                    + "Download via 'Save As' from the response body (Content-Disposition: attachment)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF report generated successfully",
                     content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "500", description = "PDF generation error",
                     content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping(
        value    = "/returns:report",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateReport(
            @Valid @RequestBody ReturnsRequest request) {

        byte[] pdf = reportService.generateReport(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"retirement-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
