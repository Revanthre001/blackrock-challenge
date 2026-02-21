package com.blackrock.challenge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI configuration for the BlackRock Challenge API.
 *
 * <p>Accessible at: http://localhost:5477/swagger-ui/index.html
 * OpenAPI spec:     http://localhost:5477/v3/api-docs
 *
 * <p>Hackathon: https://www.hackerrank.com/event/blackrock-hackingindia2026
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:5477}")
    private String serverPort;

    // ── Submission Links ──────────────────────────────────────────────────────
    // GitHub   : https://github.com/Revanthre001/blackrock-challenge
    // Docker   : https://hub.docker.com/r/revanthreddymedepati/blackrock-challenge
    // Video    : https://YOUR_VIDEO_LINK_HERE  ← update after recording
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public OpenAPI blackRockChallengeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BlackRock Retirement Auto-Savings API")
                        .description("""
                                Production-grade REST API for automated retirement savings through \
                                expense-based micro-investments.
                                Every expense is rounded up to the next ₹100; the difference (the \
                                *remanent*) is automatically invested for retirement.
                                Built for **BlackRock HackingIndia 2026**.

                                ---

                                ## 5 Required Endpoints
                                - **POST /transactions:parse** — Round expenses to next ₹100, compute remanent
                                - **POST /transactions:validator** — Reject negative amounts and duplicate dates
                                - **POST /transactions:filter** — Apply Q / P / K temporal period rules
                                - **POST /returns:nps** — NPS compound returns + Section 80CCD tax benefit
                                - **POST /returns:index** — NIFTY 50 Index Fund returns (14.49% p.a.)

                                ## 5 Bonus Endpoints
                                - **POST /returns:compare** — NPS vs Index Fund side-by-side with recommendation *(BONUS)*
                                - **POST /returns:hybrid** — Tax-First Hybrid: route savings through NPS up to ₹2L/yr cap, overflow to Index *(BONUS)*
                                - **POST /returns:retirement-income** — Convert corpus to monthly income via PFRDA annuity + 4% SWR *(BONUS)*
                                - **POST /returns:report** — Download full 5-page PDF investment report with charts *(BONUS)*
                                - **POST /returns:alternatives** — Compare corpus across Gold (SGB), Silver (MCX), GOI Bonds, REITs, NPS, and NIFTY 50 with diversified portfolio suggestion *(BONUS)*

                                ## 1 Observability Endpoint
                                - **GET /performance** — Live JVM metrics: uptime, heap memory, active thread count

                                ---

                                ## Period Rules (Q / P / K)
                                | Rule | Effect | Tie-break |
                                |------|--------|-----------|
                                | **Q** | Override remanent with fixed amount | Latest start wins; same start → first in list |
                                | **P** | Add extra to remanent (cumulative) | All overlapping P-periods apply |
                                | **K** | Group transactions for independent return calculation | Transactions can belong to multiple K-periods |

                                ## Key Financial Constants
                                - NPS rate: **7.11%** p.a. · Index rate: **14.49%** p.a.
                                - NPS deduction cap: **₹2,00,000/year** (Section 80CCD(1B))
                                - Retirement age: **60** · Minimum horizon: **5 years**
                                - NPS annuity mandate: **40%** of corpus at **6.5%** (PFRDA rules)
                                - Index SWR: **4%** per year (Bengen / Trinity Study)

                                ## Performance Architecture
                                - **Interval Tree** (augmented BST) — O(log n) period lookups vs O(n×m) brute-force
                                - **Parallel Streams** — automatic threshold at 10,000+ transactions
                                - **90 tests, 0 failures** · JaCoCo ≥70% line coverage enforced at build time
                                - **Alternative rates:** Gold 11.50% · Silver 9.80% · GOI Bonds 7.50% · REITs 10.20%
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BlackRock HackingIndia 2026")
                                .url("https://www.hackerrank.com/event/blackrock-hackingindia2026")
                                .email("hackathon@blackrock.com"))
                        .license(new License()
                                .name("View on GitHub")
                                .url("https://github.com/Revanthre001/blackrock-challenge")))
                .externalDocs(new ExternalDocumentation()
                        .description("HackerRank — BlackRock HackingIndia 2026")
                        .url("https://www.hackerrank.com/event/blackrock-hackingindia2026"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server (port 5477)"),
                        new Server()
                                .url("http://localhost:5477")
                                .description("Docker Container (port 5477)")))
                .components(new Components());
    }
}
