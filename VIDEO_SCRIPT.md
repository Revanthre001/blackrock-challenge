# 🎬 VIDEO PRESENTATION SCRIPT
## BlackRock Hackathon 2026 — Retirement Auto-Savings API
### Target Duration: 5–6 minutes | Port: 5477

> **Recording Setup:**
> - Spring Boot app running (`mvn spring-boot:run` or Docker)
> - Swagger UI open at `http://localhost:5477/swagger-ui/index.html`
> - PDF viewer ready to open the downloaded report
> - IDE open on `IntervalTree.java` for architecture slide
> - Speak clearly — BlackRock evaluates communication as much as code

---

## 🎬 SCENE 1 — [0:00 – 0:30] THE PROBLEM (Hook)

> **[Screen: blank title card — "BlackRock Retirement Auto-Savings API"]**

**SAY:**

> "Most people don't save for retirement because saving requires willpower.
>
> What if every time you spent ₹375 on groceries, ₹25 was automatically
> invested for your retirement? No decisions. No friction. Just spend — and save.
>
> The challenge is building the engine that makes this scale:
> a production-grade REST API that processes millions of transactions,
> applies flexible temporal savings rules, intelligently allocates
> across NPS and Index Fund, and tells you — exactly —
> what your retirement will look like.
>
> Built in Java 17. Spring Boot 3.2.2. Port 5477. Let me show you."

---

## 🎬 SCENE 2 — [0:30 – 1:10] LIVE DEMO — PARSE + FILTER (The Core)

> **[Screen: Swagger UI → POST /transactions:parse → Try it out]**

**SAY:**

> "Step one: parsing. Four real expenses go in."

> **[Paste and execute:]**
```json
[
  {"date": "2023-02-28 15:49:20", "amount": 375},
  {"date": "2023-07-15 10:30:00", "amount": 620},
  {"date": "2023-10-12 20:15:30", "amount": 250},
  {"date": "2023-12-17 08:09:45", "amount": 480}
]
```

> "₹375 rounds to ₹400 — remanent ₹25. ₹620 becomes ₹700 — remanent ₹80.
> These remanents are your automatic savings. Every rupee you'd otherwise forget.
>
> Now watch the intelligence kick in — the filter endpoint."

> **[Switch to POST /transactions:filter → paste and execute:]**
```json
{
  "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
  "p": [{"extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59"}],
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
```

> "Three types of rules applied simultaneously using a custom Interval Tree —
> O(log n) per transaction instead of brute-force O(n×m).
>
> July savings zeroed by Q rule. October remanent boosted by ₹25 festival bonus.
> The duplicate and the negative amount — correctly rejected as invalid.
>
> This is the engine. Every rupee tracked, every rule applied, every edge case handled."

---

## 🎬 SCENE 3 — [1:10 – 2:00] LIVE DEMO — NPS vs INDEX COMPARE (The Value)

> **[Screen: Swagger UI → POST /returns:compare → Try it out]**

**SAY:**

> "Now the most important question — what are these savings worth at retirement?
>
> Age 29, monthly wage ₹50,000, inflation 5.5%.
> I'll use the compare endpoint — it runs NPS and Index Fund simultaneously."

> **[Paste and execute:]**
```json
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
    {"date": "2023-12-17 08:09:45", "amount": 480}
  ]
}
```

> "NPS at 7.11% with Section 80CCD tax benefit vs NIFTY 50 at 14.49%.
>
> The recommendation field tells you which strategy wins for this investor —
> and the reasoning explains exactly why: tax savings vs growth rate, side by side.
>
> But here's the real insight — why choose one?"

---

## 🎬 SCENE 4 — [2:00 – 2:50] LIVE DEMO — HYBRID + RETIREMENT INCOME (The Bonus)

> **[Screen: Swagger UI → POST /returns:hybrid → Try it out]**

**SAY:**

> "This is our first bonus endpoint — Tax-First Hybrid Allocation.
>
> Instead of choosing between NPS and Index Fund, the system automatically
> routes savings through NPS first — capturing the full Section 80CCD deduction
> of up to ₹2 lakh per year. Once that tax benefit is maxed out, the overflow
> goes into the Index Fund to capture higher growth.
>
> Paste the same body — execute."

> **[Execute same JSON body — point to response fields]**

> "Look at npsContribution vs indexContribution.
> The hybridCorpus is higher than pure NPS — and estimatedTaxSaved shows
> exactly how much the 80CCD deduction is worth in rupees.
> The periodAllocations table breaks down every k-period:
> how much went to NPS, how much to Index, what the tax benefit was,
> and the routing decision — FULL_NPS, SPLIT, or FULL_INDEX.
>
> Now — let me tell you what that corpus means at retirement."

> **[Switch to POST /returns:retirement-income → execute same body]**

> "Retirement Income Bridge. This converts your projected corpus
> into a monthly income number you can actually plan around.
>
> Three scenarios: Pure NPS uses PFRDA's mandated 40% annuity
> at 6.5% — guaranteed monthly income for life.
> Pure Index uses Bengen's 4% Safe Withdrawal Rate.
> And the Hybrid gives you both — an annuity floor from NPS
> plus an equity drawdown from the Index portion.
>
> The recommendation tells you which strategy produces the best
> monthly income for this investor. The reasoning explains the trade-off."

---

## 🎬 SCENE 5 — [2:50 – 3:30] LIVE DEMO — PDF REPORT (The Wow Factor)

> **[Screen: Swagger UI → POST /returns:report → Try it out]**

**SAY:**

> "Everything we just saw — corpus comparison, hybrid allocation,
> retirement income, inflation analysis, risk profile —
> all of it packaged into a single downloadable PDF report.
>
> Execute."

> **[Execute and download the PDF — open it]**

> "Four pages. Page one: an Executive KPI Dashboard — six tiles showing
> the numbers that matter most: Total Invested, Hybrid Corpus,
> Monthly Income, Tax Saved — all at a glance.
>
> Page two: detailed NPS and Index tables with a bar chart comparison.
>
> Page three: the Hybrid Allocation section —
> a stacked allocation bar, per-period routing table, and the
> Retirement Income Bridge with a monthly income comparison chart.
>
> Page four: Inflation Sensitivity Analysis — at 9 different inflation rates,
> who wins: NPS with tax benefit, or Index Fund?
> Plus a Risk Profile table with Sharpe-style scores for all three strategies.
>
> This is the kind of deliverable a client-facing team would actually hand
> to an investor. Not a JSON dump — an investment report."

---

## 🎬 SCENE 6 — [3:30 – 4:15] ARCHITECTURE (The Differentiator)

> **[Screen: IDE showing IntervalTree.java or README architecture diagram]**

**SAY:**

> "Let me show you what separates this from a basic CRUD API.
>
> **Custom Interval Tree** — an augmented Binary Search Tree where each node
> stores the maximum-end timestamp in its subtree. Date queries prune entire
> subtrees — O(log n) per transaction. For one million transactions against
> thousands of rules, this is the difference between milliseconds and hours.
> Both insert and query are iterative — no recursive StackOverflow on large datasets.
>
> **Tax-First Hybrid** — NPS uses Section 80CCD's ₹2 lakh deduction cap.
> The Hybrid service groups savings by calendar year, caps the NPS slice
> at that limit, and routes the overflow to Index Fund. Because compound
> interest is linear in principal — A = P(1+r)^t — we can compute the
> exact hybrid corpus by scaling NPS and Index returns by their allocation ratios.
> Mathematically exact. No simulation needed.
>
> **Retirement Income Bridge** — NPS annuity is governed by PFRDA rules:
> 40% must go into an annuity at 6.5%. Index Fund uses the Bengen 4% SWR —
> the academic consensus for sustainable equity withdrawals.
> The Hybrid combines both — annuity floor + equity drawdown.
>
> **Parallel Streams** — above 10,000 transactions, automatic switch to
> parallel processing. Scale without code changes.
>
> **90 tests. Zero failures. JaCoCo enforces ≥70% line coverage — the build
> fails if we drop below that.** No untested shortcuts."

---

## 🎬 SCENE 7 — [4:15 – 4:50] CLOSING (The Pitch)

> **[Screen: Swagger UI overview showing all 9 endpoints]**

**SAY:**

> "To run it:
> `docker compose -f compose.yaml up --build` and hit port 5477.
> Swagger UI at `/swagger-ui/index.html`.
> Every endpoint documented. Every edge case handled.
>
> Five required endpoints — all production-ready.
> Four bonus features: compare, hybrid allocation, retirement income bridge,
> and a full four-page PDF investment report.
>
> The Interval Tree gives you O(log n) rule lookups.
> The Tax-First Hybrid gives you mathematically exact allocation.
> The Retirement Income Bridge translates corpus into livable monthly income.
>
> This isn't just a solution to the problem statement —
> it's the kind of system a BlackRock engineering team would actually ship.
>
> Thank you."

> **[End recording — hold on Swagger UI for 2 seconds]**

---

## 📋 QUICK REFERENCE — All 9 API Endpoints

| Method | Path | Type | Description |
|--------|------|------|-------------|
| POST | `/transactions:parse` | Required | Ceiling + remanent enrichment |
| POST | `/transactions:validator` | Required | Negative + duplicate validation |
| POST | `/transactions:filter` | Required | Q/P/K period application |
| POST | `/returns:nps` | Required | NPS returns + tax benefit |
| POST | `/returns:index` | Required | NIFTY 50 returns |
| GET  | `/performance` | Required | JVM uptime, heap, threads |
| POST | `/returns:compare` | **BONUS** | NPS vs Index side-by-side |
| POST | `/returns:hybrid` | **BONUS** | Tax-First Hybrid allocation |
| POST | `/returns:retirement-income` | **BONUS** | Corpus → monthly income |
| POST | `/returns:report` | **BONUS** | Full 4-page PDF report |

> Base path: `/blackrock/challenge/v1/`

## 📋 QUICK REFERENCE — Key Numbers to Mention

- **Port**: 5477
- **NPS Rate**: 7.11% p.a.
- **Index Fund Rate**: 14.49% p.a.
- **NPS Tax Deduction cap**: ₹2,00,000/year (Section 80CCD)
- **NPS Annuity mandate**: 40% of corpus @ 6.5% (PFRDA)
- **SWR (Index)**: 4% per year (Bengen/Trinity Study)
- **Interval Tree**: O(log n) period queries, iterative BST
- **Parallel threshold**: 10,000 transactions
- **Test count**: 90 tests, 0 failures
- **Coverage**: ≥70% JaCoCo (enforced at build time)
- **Build time**: ~22 seconds (`mvn clean verify`)
- **Docker image**: `blk-hacking-ind-challenge`
- **JVM**: Java 17, G1GC, 75% MaxRAMPercentage
- **Tax slabs**: 0% ≤ ₹7L, 10% ₹7–10L, 15% ₹10–12L, 20% ₹12–15L, 30% > ₹15L

## 📋 SPEAKING TIPS

1. **Lead with the Hybrid endpoint** after Compare — it's your biggest differentiator
2. **Open the PDF on screen** — seeing a real report is more impressive than any JSON
3. **Say "90 tests, zero failures"** — up from the original 79; shows continued quality
4. **Say "mathematically exact"** when describing hybrid — explains why no Monte Carlo is needed
5. **Say "PFRDA mandate" and "Bengen 4% SWR"** — domain knowledge signals credibility
6. **Pause after the Retirement Income Bridge response** — let the evaluator read monthlyIncome
7. **Don't rush Scene 4** — hybrid + retirement income is your technical ace
8. **Mention JaCoCo build gate** — shows you enforce quality, not just write tests
9. **Keep tone confident but not arrogant** — explain like demoing to a senior engineer

---

*Script for BlackRock Hackathon 2026 — Retirement Auto-Savings API*
*Tech Stack: Java 17 · Spring Boot 3.2.2 · Custom Interval Tree · OpenPDF · Docker · JUnit 5 · JaCoCo*


> **Recording Setup:**
> - Open Swagger UI at `http://localhost:5477/swagger-ui/index.html` in one tab
> - Open a REST client (Postman / Swagger Try It Out) ready
> - IDE open on `IntervalTree.java` for architecture reveal
> - Terminal showing the running Spring Boot app
> - Speak clearly, confidently — BlackRock evaluates communication skills

---

## 🎬 SCENE 1 — [0:00 – 0:35] THE PROBLEM (Hook)

> **[Screen: blank or title card — "BlackRock Retirement Auto-Savings API"]**

**SAY:**

> "Most people don't save for retirement because saving requires willpower.
> But what if you could save automatically — every single time you spend?
>
> The concept is simple: if you spend ₹375 on groceries, your expense
> is rounded up to ₹400. The ₹25 difference is auto-invested into your
> retirement fund. No willpower needed. Just spend — and save.
>
> The challenge is building the engine that makes this scale.
> Today, I'll show you exactly that — a production-grade REST API
> that processes millions of transactions, applies flexible temporal
> savings rules, and compares retirement investment strategies side by side.
> Built in Java 17, Spring Boot 3.2.2, running on port 5477."

---

## 🎬 SCENE 2 — [0:35 – 1:20] LIVE DEMO — PARSE (The Core Concept)

> **[Screen: Swagger UI → POST /transactions:parse → Try it out]**

**SAY:**

> "Let me start with the foundation — parsing. I'll send four real-world expenses."

> **[Paste into request body and execute:]**

```json
[
  {"date": "2023-02-28 15:49:20", "amount": 375},
  {"date": "2023-07-15 10:30:00", "amount": 620},
  {"date": "2023-10-12 20:15:30", "amount": 250},
  {"date": "2023-12-17 08:09:45", "amount": 480}
]
```

> **[Click Execute — show the response]**

> "Look at the response. ₹375 rounds up to a ceiling of ₹400 — remanent ₹25.
> ₹620 becomes ₹700 — remanent ₹80. ₹250 becomes ₹300 — remanent ₹50.
>
> These remanent amounts — ₹25, ₹80, ₹50, ₹20 — these are your automatic savings.
> Every rupee a user would otherwise forget to invest.
>
> And this works for 10⁶ transactions too — the service automatically switches
> to parallel stream processing at 10,000 transactions for maximum throughput."

---

## 🎬 SCENE 3 — [1:20 – 2:10] LIVE DEMO — FILTER (The Intelligence)

> **[Screen: Swagger UI → POST /transactions:filter → Try it out]**

**SAY:**

> "Now, here's where it gets intelligent. Not every month is the same.
> Maybe your employer freezes your savings in July — a Q period with fixed=0.
> Maybe during the festival season, you want to save an extra ₹25 per transaction
> — a P period. And you want to group all of 2023 as one investment period — K.
>
> Watch how this works."

> **[Paste and execute:]**

```json
{
  "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
  "p": [{"extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59"}],
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
```

> **[Click Execute — point to the response]**

> "The result: the system correctly separates valid from invalid transactions.
> The October duplicate is rejected. The negative December amount is rejected.
>
> July's savings are set to exactly ₹0 by the Q rule.
> October's ₹50 base remanent gets a festival bonus of ₹25, becoming ₹75.
> February stays at ₹25 — no special rules apply.
>
> And the architecture behind this? Instead of a nested O(n×m) loop,
> I built a custom Interval Tree — an augmented Binary Search Tree —
> that answers 'which rules apply to this date' in O(log n) time.
> One million transactions, thousands of rules — still milliseconds."

---

## 🎬 SCENE 4 — [2:10 – 3:00] LIVE DEMO — RETURNS (The Value)

> **[Screen: Swagger UI → POST /returns:nps → Try it out]**

**SAY:**

> "Now the most important question — what are those savings worth at retirement?
>
> I'll use the exact example from the problem statement:
> age 29, monthly wage ₹50,000, inflation 5.5%."

> **[Paste and execute:]**

```json
{
  "age": 29,
  "wage": 50000,
  "inflation": 5.5,
  "q": [{"fixed": 0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
  "p": [{"extra": 25, "start": "2023-10-01 08:00:00", "end": "2023-12-31 19:59:59"}],
  "k": [{"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
  "transactions": [
    {"date": "2023-02-28 15:49:20", "amount": 375},
    {"date": "2023-07-15 10:30:00", "amount": 620},
    {"date": "2023-10-12 20:15:30", "amount": 250},
    {"date": "2023-12-17 08:09:45", "amount": 480}
  ]
}
```

> **[Click Execute]**

> "At 7.11% NPS rate, compounded annually over 31 years to retirement,
> inflation-adjusted — we get the real purchasing power of these savings.
> And crucially — a tax benefit calculation using marginal Indian tax slabs.
>
> For this wage, NPS deduction is 10% of annual income = ₹60,000 per year.
> That reduces your taxable income and the tax saved is your bonus return.
>
> Now let me compare NPS against NIFTY 50 Index Fund."

> **[Switch to POST /returns:compare → execute same body]**

> "This is our bonus endpoint — it runs both calculations simultaneously
> and gives you a recommendation. At 14.49% Index Fund rate vs 7.11% NPS,
> the recommendation and reasoning tell you exactly which strategy wins
> over your specific investment horizon."

---

## 🎬 SCENE 5 — [3:00 – 3:35] LIVE DEMO — PERFORMANCE (Production Readiness)

> **[Screen: Swagger UI → GET /performance → Try it out → Execute]**

**SAY:**

> "This is a production system — not a demo. So I've built in live observability.
>
> One GET request shows exactly how the application is performing:
> uptime since startup, heap memory usage in megabytes, and active thread count.
>
> The Tomcat thread pool is tuned with 10 spare threads scaling to 200 under load.
> The JVM is configured with G1 garbage collector, container-aware memory limits,
> and optimised random seed generation for fast startup.
>
> When deployed via Docker, this runs on an Eclipse Temurin JRE 17 Alpine container,
> under 512MB of memory, as a non-root user — production security from day one."

---

## 🎬 SCENE 6 — [3:35 – 4:20] ARCHITECTURE WALKTHROUGH (The Differentiator)

> **[Screen: Switch to IDE showing src/ structure briefly, or README architecture diagram]**

**SAY:**

> "Let me show you what separates this from a basic CRUD API.
>
> **Interval Tree** — I wrote a custom augmented Binary Search Tree where each
> node stores the maximum-end timestamp in its subtree. This allows me to prune
> entire subtrees during a date query. Finding all applicable rules for a transaction
> is O(log n) instead of O(n). For 10⁶ transactions against 1,000 rules — this is
> the difference between seconds and hours.
>
> **Iterative implementation** — Both insert and query are iterative using
> explicit stacks, not recursive. This means no StackOverflow on large datasets.
>
> **Parallel Streams** — Above 10,000 transactions, the service automatically
> switches to `.parallelStream()`, distributing work across CPU cores.
>
> **Test coverage** — 79 tests across 6 test classes. JaCoCo enforces a minimum
> 70% line coverage — the build fails if we drop below that. No untested shortcuts.
>
> **79 tests. Zero failures. Build time: 19 seconds.** Clean."

---

## 🎬 SCENE 7 — [4:20 – 4:50] CLOSING (The Pitch)

> **[Screen: README or Swagger UI overview]**

**SAY:**

> "The complete project is at:
> `C:\Users\revan\BlackRock\blackrock-challenge`
>
> To run it:
> — `docker compose -f compose.yaml up --build` and hit port 5477
> — Or `mvn spring-boot:run` locally
> — Swagger UI is live at `/swagger-ui/index.html`
>
> Every design decision is documented. Every trade-off is explained.
>
> This isn't just a solution to the problem statement —
> it's the kind of system BlackRock's engineering teams would actually ship.
>
> Thank you."

> **[End recording — hold on Swagger UI or terminal for 2 seconds before stopping]**

---

## 📋 QUICK REFERENCE — API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/blackrock/challenge/v1/transactions:parse` | Ceiling + remanent enrichment |
| POST | `/blackrock/challenge/v1/transactions:validator` | Negative + duplicate validation |
| POST | `/blackrock/challenge/v1/transactions:filter` | Q/P/K period application |
| POST | `/blackrock/challenge/v1/returns:nps` | NPS returns + tax benefit |
| POST | `/blackrock/challenge/v1/returns:index` | NIFTY 50 returns |
| GET  | `/blackrock/challenge/v1/performance` | JVM metrics |
| POST | `/blackrock/challenge/v1/returns:compare` | **BONUS:** NPS vs Index |

## 📋 QUICK REFERENCE — Key Numbers to Mention

- **Port**: 5477
- **NPS Rate**: 7.11% p.a.
- **Index Fund Rate**: 14.49% p.a.
- **Interval Tree**: O(log n) period queries
- **Parallel threshold**: 10,000 transactions
- **Test count**: 79 tests, 0 failures
- **Coverage**: ≥70% JaCoCo (enforced at build time)
- **Build time**: ~19 seconds (`mvn clean verify`)
- **Docker image**: `blk-hacking-ind-challenge`
- **JVM**: Java 17, G1GC, 75% MaxRAMPercentage
- **Tax slabs**: 0% ≤ ₹7L, 10% ₹7-10L, 15% ₹10-12L, 20% ₹12-15L, 30% > ₹15L

## 📋 SPEAKING TIPS

1. **Don't rush the filter demo** — the q/p/k explanation is the hardest concept; give it space
2. **Pause after showing the compare recommendation** — let the evaluator read it
3. **Say "interval tree" and "O(log n)"** clearly — this is your technical differentiator
4. **Say "79 tests, zero failures"** — this shows production standards
5. **Mention JaCoCo build gate** — this shows you enforce quality, not just write tests
6. **Keep tone confident but not arrogant** — explain like you're demoing to a senior engineer

---

*Script for BlackRock Hackathon 2026 — Retirement Auto-Savings API*
*Tech Stack: Java 17 · Spring Boot 3.2.2 · Custom Interval Tree · Docker · JUnit 5 · JaCoCo*
