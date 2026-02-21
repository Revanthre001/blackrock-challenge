package com.blackrock.challenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * BlackRock Retirement Auto-Savings Challenge API
 *
 * <p>Production-grade Spring Boot application implementing automated
 * retirement savings through expense-based micro-investments.
 *
 * <p>Key architectural decisions:
 * - Java 17 with Spring Boot 3.2.2 for enterprise-grade reliability
 * - Interval Tree data structure for O(log n) q/p/k period lookups
 * - Parallel streams for 10^6 transaction batches
 * - Full OpenAPI 3 / Swagger documentation
 * - JaCoCo code coverage enforcement (≥70%)
 *
 * <p>Runs on port 5477 (containerized and local).
 *
 * @author BlackRock Hackathon Participant
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class BlackRockChallengeApplication implements ApplicationRunner {

    // Explicit Logger declaration — no annotation processing required, works in any IDE
    private static final Logger log = LoggerFactory.getLogger(BlackRockChallengeApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BlackRockChallengeApplication.class, args);
    }

    /**
     * Runs after Spring context is fully initialized — logs startup banner.
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║   BlackRock Retirement Auto-Savings API — STARTED        ║");
        log.info("║   Port: 5477                                             ║");
        log.info("║   Swagger: http://localhost:5477/swagger-ui/index.html   ║");
        log.info("║   API Docs: http://localhost:5477/v3/api-docs            ║");
        log.info("╚══════════════════════════════════════════════════════════╝");
    }
}
