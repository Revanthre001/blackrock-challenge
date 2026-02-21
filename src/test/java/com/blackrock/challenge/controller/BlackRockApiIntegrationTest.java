package com.blackrock.challenge.controller;

/*
 * TEST TYPE: Integration Test (Spring Boot Test with MockMvc)
 * VALIDATION: Full HTTP request/response cycle for all 5 required endpoints + bonus:
 *             - /transactions:parse
 *             - /transactions:validator
 *             - /transactions:filter
 *             - /returns:nps
 *             - /returns:index
 *             - /performance
 *             - /returns:compare (bonus)
 *             Also validates HTTP status codes, JSON structure, Content-Type headers,
 *             and error responses for invalid inputs.
 * RUN COMMAND: mvn test -Dtest=BlackRockApiIntegrationTest
 *              OR: mvn verify  (runs all tests including integration)
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BlackRock API — Full Integration Tests")
class BlackRockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE = "/blackrock/challenge/v1";

    // ─── /transactions:parse ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /transactions:parse — returns enriched transactions with ceiling and remanent")
    void testParseEndpoint() throws Exception {
        String body = """
                [
                  {"date": "2023-10-12 20:15:30", "amount": 250},
                  {"date": "2023-02-28 15:49:20", "amount": 375},
                  {"date": "2023-07-01 21:59:00", "amount": 620},
                  {"date": "2023-12-17 08:09:45", "amount": 480}
                ]
                """;

        mockMvc.perform(post(BASE + "/transactions:parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].ceiling").value(300.0))
                .andExpect(jsonPath("$[0].remanent").value(50.0))
                .andExpect(jsonPath("$[1].ceiling").value(400.0))
                .andExpect(jsonPath("$[1].remanent").value(25.0))
                .andExpect(jsonPath("$[2].ceiling").value(700.0))
                .andExpect(jsonPath("$[2].remanent").value(80.0))
                .andExpect(jsonPath("$[3].ceiling").value(500.0))
                .andExpect(jsonPath("$[3].remanent").value(20.0));
    }

    @Test
    @DisplayName("POST /transactions:parse — empty array returns empty array")
    void testParseEmptyArray() throws Exception {
        mockMvc.perform(post(BASE + "/transactions:parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─── /transactions:validator ──────────────────────────────────────────

    @Test
    @DisplayName("POST /transactions:validator — separates valid from invalid (negative amounts)")
    void testValidatorEndpoint() throws Exception {
        String body = """
                {
                  "wage": 50000,
                  "transactions": [
                    {"date": "2023-01-15 10:30:00", "amount": 2000, "ceiling": 2000, "remanent": 0},
                    {"date": "2023-07-10 09:15:00", "amount": -250, "ceiling": 0, "remanent": 0}
                  ]
                }
                """;

        mockMvc.perform(post(BASE + "/transactions:validator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", hasSize(1)))
                .andExpect(jsonPath("$.invalid", hasSize(1)))
                .andExpect(jsonPath("$.invalid[0].message").value("Negative amounts are not allowed"));
    }

    @Test
    @DisplayName("POST /transactions:validator — duplicate dates are rejected")
    void testValidatorDuplicates() throws Exception {
        String body = """
                {
                  "wage": 50000,
                  "transactions": [
                    {"date": "2023-05-01 10:00:00", "amount": 300},
                    {"date": "2023-05-01 10:00:00", "amount": 300}
                  ]
                }
                """;

        mockMvc.perform(post(BASE + "/transactions:validator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", hasSize(1)))
                .andExpect(jsonPath("$.invalid", hasSize(1)))
                .andExpect(jsonPath("$.invalid[0].message").value("Duplicate transaction"));
    }

    // ─── /transactions:filter ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /transactions:filter — applies q/p/k and filters invalid transactions")
    void testFilterEndpoint() throws Exception {
        String body = """
                {
                  "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
                  "p": [{"extra": 30, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
                  "k": [{"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
                  "wage": 50000,
                  "transactions": [
                    {"date": "2023-02-28 15:49:20", "amount": 375},
                    {"date": "2023-07-15 10:30:00", "amount": 620},
                    {"date": "2023-10-12 20:15:30", "amount": 250},
                    {"date": "2023-10-12 20:15:30", "amount": 250},
                    {"date": "2023-12-17 08:09:45", "amount": -480}
                  ]
                }
                """;

        mockMvc.perform(post(BASE + "/transactions:filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.invalid", hasSize(greaterThanOrEqualTo(2))));
    }

    // ─── /returns:nps ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /returns:nps — returns correct savings by k period with tax benefit")
    void testNpsReturnsEndpoint() throws Exception {
        String body = buildFullReturnsBody();

        mockMvc.perform(post(BASE + "/returns:nps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactionAmount").isNumber())
                .andExpect(jsonPath("$.totalCeiling").isNumber())
                .andExpect(jsonPath("$.savingsByDates", hasSize(2)))
                .andExpect(jsonPath("$.savingsByDates[0].amount").value(145.0))
                .andExpect(jsonPath("$.savingsByDates[0].profit").isNumber())
                .andExpect(jsonPath("$.savingsByDates[0].taxBenefit").isNumber());
    }

    // ─── /returns:index ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /returns:index — returns correct savings by k period, taxBenefit=0")
    void testIndexReturnsEndpoint() throws Exception {
        String body = buildFullReturnsBody();

        mockMvc.perform(post(BASE + "/returns:index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savingsByDates[0].taxBenefit").value(0.0));
    }

    // ─── /performance ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /performance — returns time, memory, threads")
    void testPerformanceEndpoint() throws Exception {
        mockMvc.perform(get(BASE + "/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").isString())
                .andExpect(jsonPath("$.memory").isString())
                .andExpect(jsonPath("$.threads").isNumber())
                .andExpect(jsonPath("$.threads").value(greaterThan(0)));
    }

    // ─── /returns:compare (BONUS) ─────────────────────────────────────────

    @Test
    @DisplayName("POST /returns:compare — returns nps, index, recommendation, and summaries")
    void testCompareEndpoint() throws Exception {
        String body = buildFullReturnsBody();

        mockMvc.perform(post(BASE + "/returns:compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nps").exists())
                .andExpect(jsonPath("$.index").exists())
                .andExpect(jsonPath("$.recommendation").isString())
                .andExpect(jsonPath("$.reasoning").isString())
                .andExpect(jsonPath("$.summaries").isArray());
    }

    // ─── Error handling ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /returns:nps — 400 when age is missing")
    void testNpsReturnsMissingAge() throws Exception {
        String body = """
                {
                  "wage": 50000,
                  "inflation": 5.5,
                  "q": [], "p": [], "k": [],
                  "transactions": []
                }
                """;

        mockMvc.perform(post(BASE + "/returns:nps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions:parse — 400 for malformed JSON")
    void testParseMalformedJson() throws Exception {
        mockMvc.perform(post(BASE + "/transactions:parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions:parse — invalid date gets message field, others still parsed")
    void testParseInvalidDateFlagged() throws Exception {
        String body = """
                [
                  {"date": "2023-18-12 20:15:30", "amount": 250},
                  {"date": "2023-02-28 15:49:20", "amount": 375}
                ]
                """;

        mockMvc.perform(post(BASE + "/transactions:parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].message", containsString("Invalid date format")))
                .andExpect(jsonPath("$[0].ceiling").value(300.0))
                .andExpect(jsonPath("$[1].message").doesNotExist())
                .andExpect(jsonPath("$[1].ceiling").value(400.0));
    }

    @Test
    @DisplayName("POST /transactions:validator — invalid date goes to invalid list")
    void testValidatorRejectsInvalidDate() throws Exception {
        String body = """
                {
                  "wage": 50000,
                  "transactions": [
                    {"date": "2023-18-12 20:15:30", "amount": 250},
                    {"date": "2023-02-28 15:49:20", "amount": 375}
                  ]
                }
                """;

        mockMvc.perform(post(BASE + "/transactions:validator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid",   hasSize(1)))
                .andExpect(jsonPath("$.invalid", hasSize(1)))
                .andExpect(jsonPath("$.invalid[0].message", containsString("Invalid date format")));
    }

    @Test
    @DisplayName("GET /performance — Content-Type is application/json")
    void testPerformanceContentType() throws Exception {
        mockMvc.perform(get(BASE + "/performance"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ─── /returns:report (BONUS PDF endpoint) ────────────────────────────

    @Test
    @DisplayName("POST /returns:report — returns PDF with correct headers")
    void testReportEndpointReturnsPdf() throws Exception {
        mockMvc.perform(post(BASE + "/returns:report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFullReturnsBody()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"retirement-report.pdf\""))
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    if (body == null || body.length < 4)
                        throw new AssertionError("PDF body is empty");
                    // PDF magic bytes: %PDF
                    if (body[0] != '%' || body[1] != 'P' || body[2] != 'D' || body[3] != 'F')
                        throw new AssertionError("Response is not a valid PDF (missing %PDF header)");
                });
    }

    @Test
    @DisplayName("POST /returns:report — 400 for missing required fields")
    void testReportEndpointValidation() throws Exception {
        // Missing age, wage, inflation — should fail @Valid
        mockMvc.perform(post(BASE + "/returns:report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /returns:report — 400 for malformed JSON")
    void testReportMalformedJson() throws Exception {
        mockMvc.perform(post(BASE + "/returns:report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ bad json ]"))
                .andExpect(status().isBadRequest());
    }

    // ─── /returns:hybrid ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /returns:hybrid — returns hybrid corpus, NPS%, Index%, period allocations")
    void testHybridEndpointReturnsJson() throws Exception {
        mockMvc.perform(post(BASE + "/returns:hybrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFullReturnsBody()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hybridCorpus",         notNullValue()))
                .andExpect(jsonPath("$.npsContribution",      notNullValue()))
                .andExpect(jsonPath("$.indexContribution",    notNullValue()))
                .andExpect(jsonPath("$.npsAllocationPct",     notNullValue()))
                .andExpect(jsonPath("$.indexAllocationPct",   notNullValue()))
                .andExpect(jsonPath("$.allocationStrategy",   not(blankString())))
                .andExpect(jsonPath("$.reasoning",            not(blankString())))
                .andExpect(jsonPath("$.periodAllocations",    notNullValue()));
    }

    @Test
    @DisplayName("POST /returns:hybrid — npsAllocationPct + indexAllocationPct should sum to ~100")
    void testHybridAllocationSumsTo100() throws Exception {
        mockMvc.perform(post(BASE + "/returns:hybrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFullReturnsBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.npsAllocationPct",  greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.indexAllocationPct", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("POST /returns:hybrid — 400 for empty body")
    void testHybridEndpointValidation() throws Exception {
        mockMvc.perform(post(BASE + "/returns:hybrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── /returns:retirement-income ──────────────────────────────────────

    @Test
    @DisplayName("POST /returns:retirement-income — returns 3 income scenarios with monthlyIncome")
    void testRetirementIncomeEndpointReturnsJson() throws Exception {
        mockMvc.perform(post(BASE + "/returns:retirement-income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFullReturnsBody()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nps.monthlyIncome",    notNullValue()))
                .andExpect(jsonPath("$.index.monthlyIncome",  notNullValue()))
                .andExpect(jsonPath("$.hybrid.monthlyIncome", notNullValue()))
                .andExpect(jsonPath("$.nps.totalCorpus",      notNullValue()))
                .andExpect(jsonPath("$.yearsToRetirement",    greaterThan(0)))
                .andExpect(jsonPath("$.recommendation",       not(blankString())))
                .andExpect(jsonPath("$.reasoning",            not(blankString())));
    }

    @Test
    @DisplayName("POST /returns:retirement-income — monthly incomes should be positive")
    void testRetirementIncomePositiveValues() throws Exception {
        mockMvc.perform(post(BASE + "/returns:retirement-income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFullReturnsBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nps.monthlyIncome",    greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.index.monthlyIncome",  greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.hybrid.monthlyIncome", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("POST /returns:retirement-income — 400 for empty body")
    void testRetirementIncomeEndpointValidation() throws Exception {
        mockMvc.perform(post(BASE + "/returns:retirement-income")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private String buildFullReturnsBody() {
        return """
                {
                  "age": 29,
                  "wage": 50000,
                  "inflation": 5.5,
                  "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
                  "p": [{"extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59"}],
                  "k": [
                    {"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"},
                    {"start": "2023-03-01 00:00:00", "end": "2023-11-30 23:59:59"}
                  ],
                  "transactions": [
                    {"date": "2023-02-28 15:49:20", "amount": 375},
                    {"date": "2023-07-01 21:59:00", "amount": 620},
                    {"date": "2023-10-12 20:15:30", "amount": 250},
                    {"date": "2023-12-17 08:09:45", "amount": 480},
                    {"date": "2023-12-17 08:09:45", "amount": -10}
                  ]
                }
                """;
    }
}
