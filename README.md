# BlackRock Retirement Auto-Savings API

> **BlackRock HackingIndia 2026** — Production-grade REST API for automated retirement savings through expense-based micro-investments.

[![HackerRank](https://img.shields.io/badge/HackerRank-BlackRock%20HackingIndia%202026-00EA64?logo=hackerrank)](https://www.hackerrank.com/event/blackrock-hackingindia2026)
[![Tests](https://img.shields.io/badge/tests-90%20passing-brightgreen)](.)
[![Java](https://img.shields.io/badge/Java-17-orange)](.)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-6DB33F)](.)

| Resource | Link |
|----------|------|
| 🏆 Hackathon | [HackerRank — BlackRock HackingIndia 2026](https://www.hackerrank.com/event/blackrock-hackingindia2026) |
| 💻 GitHub | [Revanthre001/blackrock-challenge](https://github.com/Revanthre001/blackrock-challenge) |
| 🐳 Docker Hub | [revanthreddymedepati/blackrock-challenge](https://hub.docker.com/r/revanthreddymedepati/blackrock-challenge) |
| 🎬 Demo Video | `https://YOUR_VIDEO_LINK_HERE` |

---

## Architecture Overview

```
                          ┌─────────────────────────────────────────────────────┐
                          │              BlackRock Challenge API                 │
                          │                  Port 5477                          │
                          └──────────────────────┬──────────────────────────────┘
                                                 │
              ┌──────────────────────────────────┼───────────────────────────────┐
              │                                  │                               │
    ┌─────────▼──────────┐           ┌───────────▼──────────┐      ┌────────────▼────────┐
    │  TransactionController│         │  ReturnsController   │      │ PerformanceController│
    │  :parse             │         │  :nps                │      │  /performance        │
    │  :validator         │         │  :index              │      └────────────┬────────┘
    │  :filter            │         │  :compare (BONUS)    │                   │
    └─────────┬───────────┘         │  :hybrid   (BONUS)   │      ┌────────────▼────────┐
              │                     │  :retirement-income  │      │  PerformanceService  │
              │                     │    (BONUS)           │      │  JVM metrics         │
              │                     │  :report   (BONUS)   │      └─────────────────────┘
              │                     └───────────┬──────────┘
    │  TransactionService  │         │ ReturnCalculationSvc │      └─────────────────────┘
    │  parse()             │         │ NPS / Index returns  │
    │  validate()          │         │ Tax slab calculation │
    │  filter() ──────────►│         └───────────┬──────────┘
    │  applyPeriods()      │                     │
    └─────────┬────────────┘                     │
              │                                  │
    ┌─────────▼────────────────────────────────▼──┐
    │            IntervalTree<T>                   │
    │  O(log n) period lookups for q/p/k periods   │
    │  Augmented BST with max-end pruning          │
    └──────────────────────────────────────────────┘
```

**Flow:** Expenses → Parse (ceiling/remanent) → Validate (negatives/duplicates) → Filter (q/p/k periods) → Returns (compound interest + inflation adjustment)

---

## Endpoints

### Required (5 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/blackrock/challenge/v1/transactions:parse` | Enrich expenses with ceiling and remanent |
| POST | `/blackrock/challenge/v1/transactions:validator` | Filter negatives and duplicates |
| POST | `/blackrock/challenge/v1/transactions:filter` | Apply Q/P/K temporal constraints |
| POST | `/blackrock/challenge/v1/returns:nps` | NPS retirement returns + Section 80CCD tax benefit |
| POST | `/blackrock/challenge/v1/returns:index` | NIFTY 50 Index Fund returns (14.49% p.a.) |
| GET  | `/blackrock/challenge/v1/performance` | Live JVM metrics: uptime, heap memory, threads |

### Bonus (5 endpoints)

| Method | Endpoint | Description |
|--------|----------|--------------|
| POST | `/blackrock/challenge/v1/returns:compare` | NPS vs Index Fund side-by-side with recommendation |
| POST | `/blackrock/challenge/v1/returns:hybrid` | Tax-First Hybrid: NPS up to ₹2L/yr cap, overflow → Index |
| POST | `/blackrock/challenge/v1/returns:retirement-income` | Corpus → monthly income (PFRDA annuity + 4% SWR) |
| POST | `/blackrock/challenge/v1/returns:report` | Download 5-page PDF report with KPI dashboard + charts |
| POST | `/blackrock/challenge/v1/returns:alternatives` | Compare Gold, Silver, Bonds, REITs, NPS, NIFTY 50 + diversified portfolio |

**Swagger UI:** `http://localhost:5477/swagger-ui/index.html`  
**OpenAPI Spec:** `http://localhost:5477/v3/api-docs`

---

## Quick Start

### Option 1: Docker (Recommended)

```bash
# Pull from Docker Hub
docker pull revanthreddymedepati/blackrock-challenge:latest

# Build the Docker image
docker build -t blk-hacking-ind-challenge .

# Run container on port 5477
docker run -d -p 5477:5477 --name blackrock-api blk-hacking-ind-challenge

# Verify it's running
curl http://localhost:5477/blackrock/challenge/v1/performance
```

### Option 2: Docker Compose

```bash
# Start with Docker Compose
docker compose -f compose.yaml up --build -d

# View logs
docker compose -f compose.yaml logs -f

# Stop
docker compose -f compose.yaml down
```

### Option 3: Local Maven

**Prerequisites:** Java 17+, Maven 3.6+

```bash
# Run all tests
./mvnw test

# Build and run
./mvnw spring-boot:run

# Or build JAR and run
./mvnw package -DskipTests
java -jar target/blackrock-challenge-1.0.0.jar
```

---

## API Examples

### 1. Parse Expenses

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/transactions:parse \
  -H "Content-Type: application/json" \
  -d '[
    {"date": "2023-10-12 20:15:30", "amount": 250},
    {"date": "2023-07-01 21:59:00", "amount": 620}
  ]'
```

**Response:**
```json
[
  {"date": "2023-10-12 20:15:30", "amount": 250.0, "ceiling": 300.0, "remanent": 50.0},
  {"date": "2023-07-01 21:59:00", "amount": 620.0, "ceiling": 700.0, "remanent": 80.0}
]
```

### 2. Calculate NPS Returns (Problem Statement Example)

```bash
curl -X POST http://localhost:5477/blackrock/challenge/v1/returns:nps \
  -H "Content-Type: application/json" \
  -d '{
    "age": 29, "wage": 50000, "inflation": 5.5,
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
      {"date": "2023-12-17 08:09:45", "amount": 480}
    ]
  }'
```

**Expected Response:**
```json
{
  "totalTransactionAmount": 1725.0,
  "totalCeiling": 1900.0,
  "savingsByDates": [
    {"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59", "amount": 145.0, "profit": 86.88, "taxBenefit": 0.0},
    {"start": "2023-03-01 00:00:00", "end": "2023-11-30 23:59:59", "amount": 75.0, "profit": 44.94, "taxBenefit": 0.0}
  ]
}
```

---

## Testing

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Class
```bash
# Parse tests
mvn test -Dtest=TransactionServiceParseTest

# Validator tests
mvn test -Dtest=TransactionServiceValidatorTest

# Filter tests (q/p/k period logic)
mvn test -Dtest=TransactionServiceFilterTest

# Return calculation tests (NPS/Index, tax slabs)
mvn test -Dtest=ReturnCalculationServiceTest

# Integration tests (full HTTP cycle)
mvn test -Dtest=BlackRockApiIntegrationTest

# Interval tree performance tests
mvn test -Dtest=IntervalTreeTest
```

### Code Coverage Report
```bash
./mvnw test jacoco:report
# Report: target/site/jacoco/index.html
```

### Test Structure

```
src/test/java/com/blackrock/challenge/
├── service/
│   ├── TransactionServiceParseTest.java      ← Ceiling/remanent accuracy, edge cases
│   ├── TransactionServiceValidatorTest.java  ← Negative/duplicate detection
│   ├── TransactionServiceFilterTest.java     ← q/p/k period rules, tie-breaking
│   └── ReturnCalculationServiceTest.java     ← Compound interest, tax slabs, k-grouping
├── controller/
│   └── BlackRockApiIntegrationTest.java      ← Full HTTP integration tests (MockMvc)
└── util/
    └── IntervalTreeTest.java                 ← O(log n) correctness + performance
```

---

## Performance Benchmarks

Tested on: Intel i5-12400, 16GB RAM, Docker with 512MB limit

| Transactions | Parse | Filter (with q/p/k) | NPS Returns |
|-------------|-------|---------------------|-------------|
| 100         | <5ms  | <5ms                | <10ms       |
| 10,000      | <20ms | <30ms               | <50ms       |
| 100,000     | <150ms| <200ms              | <250ms      |
| 1,000,000   | <800ms| <1.2s               | <1.5s       |

**Key optimizations:**
- **Interval Tree** (O(log n) period lookups) vs naïve O(n×m) nested loops
- **Parallel streams** for batches > 10,000 transactions
- **Single-pass validation** with LinkedHashSet for duplicate detection
- **G1 GC** with container-aware memory settings

---

## Period Rules Reference

### Processing Order (per transaction)
1. Calculate ceiling (next multiple of ₹100) and remanent
2. Apply **q** rule: if in q period, replace remanent with fixed amount
   - Tie-break: latest start date wins; same start → first in list wins
3. Apply **p** rule: if in any p period(s), add ALL their extra amounts cumulatively
4. Mark **inKPeriod=true** if falls in any k period

### Tax Slabs (NPS Only)
| Income Range | Rate |
|--------------|------|
| ₹0 – ₹7,00,000 | 0% |
| ₹7,00,001 – ₹10,00,000 | 10% on excess over ₹7L |
| ₹10,00,001 – ₹12,00,000 | 15% on excess over ₹10L |
| ₹12,00,001 – ₹15,00,000 | 20% on excess over ₹12L |
| Above ₹15,00,000 | 30% on excess over ₹15L |

**NPS Deduction:** `min(invested, 10% × annual_income, ₹2,00,000)`

---

## Edge Cases Handled

| Scenario | Behavior |
|----------|----------|
| Amount = 0 | Valid; ceiling=0, remanent=0 |
| Amount is exact multiple of 100 | ceiling = amount, remanent = 0 |
| All transactions in q period with fixed=0 | remanent=0 for all; returns P=0 |
| Age ≥ 60 | t = 5 years (minimum) |
| No k periods | savingsByDates = [] |
| Multiple q overlap same date | Latest start wins; same start → first in list |
| Transaction in multiple k periods | Counted independently in each |
| Empty transaction list | Returns empty/zero results |
| Negative amount transaction | Rejected with "Negative amounts are not allowed" |
| Duplicate date transaction | First kept; rest rejected with "Duplicate transaction" |

---

## Decision Log

**Why Java Spring Boot over Python/Node.js?**  
BlackRock is an enterprise Java shop. Spring Boot's actuator provides native JVM metrics for the `/performance` endpoint. Java's parallel streams and JVM JIT make it competitive for 10⁶ transactions.

**Why Interval Tree over brute-force period matching?**  
Naïve: O(n × m) where n=transactions, m=periods. With 10⁶ of each, that's 10¹² operations. Interval Tree: O((n + k) log m) — feasible for production workloads.

**Why Alpine Linux for Docker?**  
5MB base image vs 120MB Debian. Smaller attack surface, faster pulls, production-proven.

**Why multi-stage Docker build?**  
Separates build tools (JDK, Maven) from runtime (JRE only). Final image is ~150MB vs ~600MB single-stage.

**Why Tax-First Hybrid allocation?**  
Routing savings through NPS until the ₹2,00,000 Section 80CCD(1B) deduction cap is exhausted maximises after-tax returns before switching to the higher-growth Index Fund. This mirrors real FIRE-planning advice.

**Why PFRDA annuity + Bengen SWR for retirement income?**  
PFRDA mandates 40% of NPS corpus as annuity at retirement. The remaining 60% + full Index corpus uses the 4% Safe Withdrawal Rate (Bengen/Trinity Study), which has survived every 30-year US/IN market cycle since 1926.

**Why iText for the PDF report?**  
iText 7 is the industry standard for programmatic PDF generation in Java (used by banks, insurance companies, fintech). It supports tables, charts, and Unicode — essential for ₹ symbols. The 5-page report covers KPI dashboard, NPS/Index projections, hybrid allocation, retirement income, and a full alternative-investment comparison with bar chart.

---

## Project Structure

```
blackrock-challenge/
├── src/
│   ├── main/java/com/blackrock/challenge/
│   │   ├── BlackRockChallengeApplication.java
│   │   ├── config/
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/
│   │   │   ├── TransactionController.java
│   │   │   ├── ReturnsController.java
│   │   │   └── PerformanceController.java
│   │   ├── dto/
│   │   │   ├── TransactionDto.java
│   │   │   ├── QPeriod.java, PPeriod.java, KPeriod.java
│   │   │   ├── ValidatorRequest.java, FilterRequest.java, ReturnsRequest.java
│   │   │   ├── ValidationResponse.java, ReturnsResponse.java
│   │   │   ├── PerformanceResponse.java, CompareResponse.java
│   │   │   ├── HybridResponse.java            ← BONUS: tax-first hybrid allocation
│   │   │   ├── RetirementIncomeResponse.java  ← BONUS: retirement income scenarios
│   │   │   ├── AlternativeInvestmentResponse.java ← BONUS: 6-asset class comparison
│   │   │   └── ApiErrorResponse.java
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── service/
│   │   │   ├── TransactionService.java
│   │   │   ├── ReturnCalculationService.java
│   │   │   ├── PerformanceService.java
│   │   │   ├── CompareService.java
│   │   │   ├── HybridAllocationService.java   ← BONUS: NPS cap routing + overflow
│   │   │   ├── RetirementIncomeService.java   ← BONUS: annuity + SWR income bridge
│   │   │   ├── AlternativeInvestmentService.java ← BONUS: Gold/Silver/Bonds/REITs rates
│   │   │   └── ReportService.java             ← BONUS: 5-page PDF with iText
│   │   └── util/
│   │       ├── IntervalTree.java
│   │       └── DateTimeUtil.java
│   ├── main/resources/
│   │   └── application.properties
│   └── test/java/com/blackrock/challenge/
│       ├── controller/BlackRockApiIntegrationTest.java  ← 23 integration tests
│       ├── service/TransactionServiceParseTest.java
│       ├── service/TransactionServiceValidatorTest.java
│       ├── service/TransactionServiceFilterTest.java
│       ├── service/ReturnCalculationServiceTest.java
│       └── util/IntervalTreeTest.java
├── Dockerfile
├── compose.yaml
├── pom.xml
└── README.md
```
