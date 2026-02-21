# BlackRock Retirement Auto-Savings API — Test Payloads

> **Persona:** Priya Sharma | Software Engineer, Bengaluru | Age 29 | ₹75,000/month
> **Investment Horizon:** 31 years (to retirement age 60)
> **Annual Income:** ₹9,00,000 → falls in the 10% marginal slab (excess over ₹7L)
> **Base URL:** `http://localhost:5477/blackrock/challenge/v1`
> **Swagger UI:** `http://localhost:5477/swagger-ui/index.html`

---

## Shared Payload Reference

All multi-step endpoints (returns, hybrid, retirement-income, report, alternatives) use this **exact same body** so the demo flows smoothly as a coherent story.

### Transactions (23 realistic expenses across 2023)

| Date | Description | Amount | Ceiling | Remanent | Period |
|------|-------------|--------|---------|----------|--------|
| 2023-01-08 | Coffee + snacks | ₹285 | ₹300 | ₹15 | K1 |
| 2023-01-21 | Dinner out | ₹847 | ₹900 | ₹53 | K1 |
| 2023-02-05 | Grocery shopping | ₹2,253 | ₹2,300 | ₹47 | K1 |
| 2023-02-17 | Electricity bill | ₹1,742 | ₹1,800 | ₹58 | K1 |
| 2023-03-11 | Pharmacy | ₹368 | ₹400 | ₹32 | K1 |
| 2023-03-28 | Clothes shopping | ₹3,499 | ₹3,500 | ₹1 | K1 |
| 2023-04-07 | Online order | ₹1,379 | ₹1,400 | ₹21 | K1 |
| 2023-04-19 | Restaurant bill | ₹1,156 | ₹1,200 | ₹44 | K1 |
| 2023-05-03 | Petrol | ₹2,847 | ₹2,900 | ₹53 | K1 |
| 2023-05-22 | Movie + popcorn | ₹648 | ₹700 | ₹52 | K1 |
| 2023-06-14 | Grocery | ₹1,935 | ₹2,000 | ₹65 | K1 |
| 2023-06-29 | Internet bill | ₹749 | ₹800 | ₹51 | K1 |
| 2023-07-10 | Flight tickets | ₹8,647 | ₹8,700 | **₹50 (Q override)** | K2 |
| 2023-08-05 | Hotel stay | ₹5,234 | ₹5,300 | ₹66 | K2 |
| 2023-08-20 | Books + courses | ₹1,249 | ₹1,300 | ₹51 | K2 |
| 2023-09-12 | Restaurant | ₹2,147 | ₹2,200 | ₹53 | K2 |
| 2023-09-28 | Grocery | ₹2,863 | ₹2,900 | ₹37 | K2 |
| 2023-10-14 | Diwali shopping | ₹4,736 | ₹4,800 | ₹64 + ₹200 P = **₹264** | K2 |
| 2023-10-30 | Electronics | ₹12,549 | ₹12,600 | ₹51 + ₹200 P = **₹251** | K2 |
| 2023-11-11 | Restaurant | ₹1,648 | ₹1,700 | ₹52 + ₹200 P = **₹252** | K2 |
| 2023-11-25 | Grocery | ₹2,762 | ₹2,800 | ₹38 + ₹200 P = **₹238** | K2 |
| 2023-12-08 | Year-end party | ₹3,847 | ₹3,900 | ₹53 + ₹200 P = **₹253** | K2 |
| 2023-12-23 | Travel tickets | ₹6,249 | ₹6,300 | ₹51 + ₹200 P = **₹251** | K2 |

**Period rules applied:**
- **Q period** (Jul 2023): vacation month → fixed ₹50 (overrides the ₹53 natural remanent on flight tickets)
- **P period** (Oct–Dec 2023): Diwali + year-end bonus season → +₹200 extra per transaction
- **K period 1** (H1 2023: Jan–Jun): 12 transactions, total remanent ≈ ₹492
- **K period 2** (H2 2023: Jul–Dec): 11 transactions, total remanent ≈ ₹1,566 (with P bonuses)

---

## 1. POST /transactions:parse

**Purpose:** Round each expense to the next ₹100, compute ceiling and remanent.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/transactions:parse \
  -H "Content-Type: application/json" \
  -d '[
    {"date": "2023-01-08 11:30:00", "amount": 285.0},
    {"date": "2023-01-21 19:45:00", "amount": 847.0},
    {"date": "2023-02-05 09:15:00", "amount": 2253.0},
    {"date": "2023-05-03 08:30:00", "amount": 2847.0},
    {"date": "2023-07-10 11:00:00", "amount": 8647.0},
    {"date": "2023-10-14 11:30:00", "amount": 4736.0},
    {"date": "2023-12-23 14:00:00", "amount": 6249.0}
  ]'
```

**Expected highlights:**
```json
[
  {"date":"2023-01-08 11:30:00","amount":285.0,"ceiling":300.0,"remanent":15.0},
  {"date":"2023-01-21 19:45:00","amount":847.0,"ceiling":900.0,"remanent":53.0},
  {"date":"2023-02-05 09:15:00","amount":2253.0,"ceiling":2300.0,"remanent":47.0},
  {"date":"2023-05-03 08:30:00","amount":2847.0,"ceiling":2900.0,"remanent":53.0},
  {"date":"2023-07-10 11:00:00","amount":8647.0,"ceiling":8700.0,"remanent":53.0},
  {"date":"2023-10-14 11:30:00","amount":4736.0,"ceiling":4800.0,"remanent":64.0},
  {"date":"2023-12-23 14:00:00","amount":6249.0,"ceiling":6300.0,"remanent":51.0}
]
```
> **Demo point:** Notice ₹8,647 → ceiling ₹8,700, remanent ₹53. That ₹53 gets overridden to ₹50 by the Q period in subsequent endpoints.

---

## 2. POST /transactions:validator

**Purpose:** Reject negative amounts and duplicate dates. Keep first occurrence of duplicates.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/transactions:validator \
  -H "Content-Type: application/json" \
  -d '{
    "wage": 75000.0,
    "transactions": [
      {"date": "2023-01-08 11:30:00", "amount": 285.0},
      {"date": "2023-03-11 14:30:00", "amount": 368.0},
      {"date": "2023-03-11 14:30:00", "amount": 999.0},
      {"date": "2023-05-22 17:45:00", "amount": -250.0},
      {"date": "2023-06-14 13:00:00", "amount": 1935.0},
      {"date": "2023-08-20 09:00:00", "amount": 1249.0}
    ]
  }'
```

**Expected response:**
```json
{
  "valid": [
    {"date":"2023-01-08 11:30:00","amount":285.0,"ceiling":300.0,"remanent":15.0},
    {"date":"2023-03-11 14:30:00","amount":368.0,"ceiling":400.0,"remanent":32.0},
    {"date":"2023-06-14 13:00:00","amount":1935.0,"ceiling":2000.0,"remanent":65.0},
    {"date":"2023-08-20 09:00:00","amount":1249.0,"ceiling":1300.0,"remanent":51.0}
  ],
  "invalid": [
    {"date":"2023-03-11 14:30:00","amount":999.0,"message":"Duplicate transaction date"},
    {"date":"2023-05-22 17:45:00","amount":-250.0,"message":"Negative amounts are not allowed"}
  ]
}
```
> **Demo point:** 2 rejected (1 duplicate + 1 negative), 4 accepted. Shows data quality enforcement.

---

## 3. POST /transactions:filter

**Purpose:** Apply Q / P / K period rules to enriched transactions.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/transactions:filter \
  -H "Content-Type: application/json" \
  -d '{
    "wage": 75000.0,
    "q": [
      {
        "fixed": 50.0,
        "start": "2023-07-01 00:00:00",
        "end": "2023-07-31 23:59:59"
      }
    ],
    "p": [
      {
        "extra": 200.0,
        "start": "2023-10-01 00:00:00",
        "end": "2023-12-31 23:59:59"
      }
    ],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-06-30 23:59:59"},
      {"start": "2023-07-01 00:00:00", "end": "2023-12-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-06-29 20:15:00", "amount": 749.0},
      {"date": "2023-07-10 11:00:00", "amount": 8647.0},
      {"date": "2023-10-14 11:30:00", "amount": 4736.0},
      {"date": "2023-12-23 14:00:00", "amount": 6249.0}
    ]
  }'
```

**Expected highlights:**
```json
[
  {"date":"2023-06-29 20:15:00","amount":749.0,"ceiling":800.0,"remanent":51.0,"inKPeriod":true},
  {"date":"2023-07-10 11:00:00","amount":8647.0,"ceiling":8700.0,"remanent":50.0,"inKPeriod":true},
  {"date":"2023-10-14 11:30:00","amount":4736.0,"ceiling":4800.0,"remanent":264.0,"inKPeriod":true},
  {"date":"2023-12-23 14:00:00","amount":6249.0,"ceiling":6300.0,"remanent":251.0,"inKPeriod":true}
]
```
> **Demo point:** 2023-07-10 natural remanent ₹53 → Q override → **₹50**. Diwali transaction ₹64 + P ₹200 = **₹264**. All 4 are `inKPeriod: true`.

---

## 4. POST /returns:nps

**Purpose:** NPS compound returns (7.11% p.a.) + Section 80CCD tax benefit.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:nps \
  -H "Content-Type: application/json" \
  -d '{
    "age": 29,
    "wage": 75000.0,
    "inflation": 6.0,
    "q": [{"fixed": 50.0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 200.0, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-06-30 23:59:59"},
      {"start": "2023-07-01 00:00:00", "end": "2023-12-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-01-08 11:30:00", "amount": 285.0},
      {"date": "2023-01-21 19:45:00", "amount": 847.0},
      {"date": "2023-02-05 09:15:00", "amount": 2253.0},
      {"date": "2023-02-17 20:00:00", "amount": 1742.0},
      {"date": "2023-03-11 14:30:00", "amount": 368.0},
      {"date": "2023-03-28 18:00:00", "amount": 3499.0},
      {"date": "2023-04-07 12:00:00", "amount": 1379.0},
      {"date": "2023-04-19 21:00:00", "amount": 1156.0},
      {"date": "2023-05-03 08:30:00", "amount": 2847.0},
      {"date": "2023-05-22 17:45:00", "amount": 648.0},
      {"date": "2023-06-14 13:00:00", "amount": 1935.0},
      {"date": "2023-06-29 20:15:00", "amount": 749.0},
      {"date": "2023-07-10 11:00:00", "amount": 8647.0},
      {"date": "2023-08-05 16:30:00", "amount": 5234.0},
      {"date": "2023-08-20 09:00:00", "amount": 1249.0},
      {"date": "2023-09-12 20:00:00", "amount": 2147.0},
      {"date": "2023-09-28 14:00:00", "amount": 2863.0},
      {"date": "2023-10-14 11:30:00", "amount": 4736.0},
      {"date": "2023-10-30 19:00:00", "amount": 12549.0},
      {"date": "2023-11-11 12:00:00", "amount": 1648.0},
      {"date": "2023-11-25 15:30:00", "amount": 2762.0},
      {"date": "2023-12-08 21:00:00", "amount": 3847.0},
      {"date": "2023-12-23 14:00:00", "amount": 6249.0}
    ]
  }'
```

> **Demo point:** K1 (H1) remanent ≈ ₹492 → grows at 7.11% for 31 years. K2 (H2) remanent ≈ ₹1,566 (Diwali P-boost + Q override) → same rate. Each k-period shows `taxBenefit` as savings under Section 80CCD. Annual income ₹9L is in the 10% slab → meaningful tax benefit.

---

## 5. POST /returns:index

**Purpose:** NIFTY 50 Index Fund returns (14.49% p.a., no tax restrictions).

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:index \
  -H "Content-Type: application/json" \
  -d '{
    "age": 29,
    "wage": 75000.0,
    "inflation": 6.0,
    "q": [{"fixed": 50.0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 200.0, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-06-30 23:59:59"},
      {"start": "2023-07-01 00:00:00", "end": "2023-12-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-01-08 11:30:00", "amount": 285.0},
      {"date": "2023-01-21 19:45:00", "amount": 847.0},
      {"date": "2023-02-05 09:15:00", "amount": 2253.0},
      {"date": "2023-02-17 20:00:00", "amount": 1742.0},
      {"date": "2023-03-11 14:30:00", "amount": 368.0},
      {"date": "2023-03-28 18:00:00", "amount": 3499.0},
      {"date": "2023-04-07 12:00:00", "amount": 1379.0},
      {"date": "2023-04-19 21:00:00", "amount": 1156.0},
      {"date": "2023-05-03 08:30:00", "amount": 2847.0},
      {"date": "2023-05-22 17:45:00", "amount": 648.0},
      {"date": "2023-06-14 13:00:00", "amount": 1935.0},
      {"date": "2023-06-29 20:15:00", "amount": 749.0},
      {"date": "2023-07-10 11:00:00", "amount": 8647.0},
      {"date": "2023-08-05 16:30:00", "amount": 5234.0},
      {"date": "2023-08-20 09:00:00", "amount": 1249.0},
      {"date": "2023-09-12 20:00:00", "amount": 2147.0},
      {"date": "2023-09-28 14:00:00", "amount": 2863.0},
      {"date": "2023-10-14 11:30:00", "amount": 4736.0},
      {"date": "2023-10-30 19:00:00", "amount": 12549.0},
      {"date": "2023-11-11 12:00:00", "amount": 1648.0},
      {"date": "2023-11-25 15:30:00", "amount": 2762.0},
      {"date": "2023-12-08 21:00:00", "amount": 3847.0},
      {"date": "2023-12-23 14:00:00", "amount": 6249.0}
    ]
  }'
```

> **Demo point:** Same transaction set, but 14.49% vs 7.11%. `taxBenefit` = 0 for all k-periods (no NPS deduction). Corpus is significantly higher than NPS, but no tax shield. Compare side-by-side in the next endpoint.

---

## 6. GET /performance

**Purpose:** Live JVM health metrics.

```bash
curl -s http://localhost:5477/blackrock/challenge/v1/performance | python -m json.tool
```

**Expected:**
```json
{
  "uptimeMillis": 342891,
  "uptimeFormatted": "5m 42s",
  "heapUsedMb": 87.34,
  "heapMaxMb": 256.0,
  "heapUsedPercent": 34.12,
  "activeThreads": 23,
  "availableProcessors": 8,
  "javaVersion": "17.0.x",
  "springProfile": "default"
}
```
> **Demo point:** Low heap usage confirms the API is production-lean. Runs inside Docker with 256MB limit.

---

## 7. POST /returns:compare  *(BONUS)*

**Purpose:** NPS vs Index Fund side-by-side with recommendation. Same body as `/returns:nps`.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:compare \
  -H "Content-Type: application/json" \
  -d '{
    "age": 29,
    "wage": 75000.0,
    "inflation": 6.0,
    "q": [{"fixed": 50.0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 200.0, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-06-30 23:59:59"},
      {"start": "2023-07-01 00:00:00", "end": "2023-12-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-01-08 11:30:00", "amount": 285.0},
      {"date": "2023-01-21 19:45:00", "amount": 847.0},
      {"date": "2023-02-05 09:15:00", "amount": 2253.0},
      {"date": "2023-02-17 20:00:00", "amount": 1742.0},
      {"date": "2023-03-11 14:30:00", "amount": 368.0},
      {"date": "2023-03-28 18:00:00", "amount": 3499.0},
      {"date": "2023-04-07 12:00:00", "amount": 1379.0},
      {"date": "2023-04-19 21:00:00", "amount": 1156.0},
      {"date": "2023-05-03 08:30:00", "amount": 2847.0},
      {"date": "2023-05-22 17:45:00", "amount": 648.0},
      {"date": "2023-06-14 13:00:00", "amount": 1935.0},
      {"date": "2023-06-29 20:15:00", "amount": 749.0},
      {"date": "2023-07-10 11:00:00", "amount": 8647.0},
      {"date": "2023-08-05 16:30:00", "amount": 5234.0},
      {"date": "2023-08-20 09:00:00", "amount": 1249.0},
      {"date": "2023-09-12 20:00:00", "amount": 2147.0},
      {"date": "2023-09-28 14:00:00", "amount": 2863.0},
      {"date": "2023-10-14 11:30:00", "amount": 4736.0},
      {"date": "2023-10-30 19:00:00", "amount": 12549.0},
      {"date": "2023-11-11 12:00:00", "amount": 1648.0},
      {"date": "2023-11-25 15:30:00", "amount": 2762.0},
      {"date": "2023-12-08 21:00:00", "amount": 3847.0},
      {"date": "2023-12-23 14:00:00", "amount": 6249.0}
    ]
  }'
```

> **Demo point:** Response includes `recommendation` field ("INDEX_FUND" or "NPS" based on effective return) and `summaries` — a human-readable narrative for each k-period. Great for the video.

---

## 8. POST /returns:hybrid  *(BONUS)*

**Purpose:** Tax-First allocation — NPS up to ₹2L/yr cap, overflow → Index Fund.

*(Use the same body as `/returns:compare` above.)*

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:hybrid \
  -H "Content-Type: application/json" \
  -d '{ ...same body as compare... }'
```

**Expected highlights:**
```json
{
  "totalInvested": 2058.0,
  "npsContribution": 2058.0,
  "indexContribution": 0.0,
  "estimatedTaxSaved": ...,
  "npsCorpus": ...,
  "indexCorpus": 0.0,
  "hybridCorpus": ...,
  "npsAllocationPct": 100.0,
  "indexAllocationPct": 0.0,
  "allocationStrategy": "100% NPS — total annual investment ₹2,058 is below the ₹2,00,000 deduction cap",
  "reasoning": "All remanent fits within the NPS deduction cap. Full NPS allocation captures tax benefit on every rupee."
}
```
> **Demo point:** Annual remanent ≈ ₹2,058 << ₹2,00,000 cap → 100% NPS this year. Priya isn't yet hitting the cap — if wage doubles, overflow goes to Index. The `periodAllocations` array breaks down each k-period individually.

---

## 9. POST /returns:retirement-income  *(BONUS)*

**Purpose:** Convert projected corpus into monthly retirement income.

*(Use the same body as `/returns:compare` above.)*

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:retirement-income \
  -H "Content-Type: application/json" \
  -d '{ ...same body as compare... }'
```

**Expected highlights:**
```json
{
  "age": 29,
  "yearsToRetirement": 31,
  "projectedMonthlyWageAtRetirement": 456402.34,
  "nps": {
    "name": "NPS Pure Strategy",
    "totalCorpus": ...,
    "monthlyIncome": ...,
    "annuityCorpus": ...,
    "lumpSumAtRetirement": ...,
    "structure": "40% corpus → annuity at 6.5% p.a. (PFRDA) + 60% lump sum",
    "notes": "Annuity income is taxable as salary. Lump sum is tax-free."
  },
  "index": {
    "name": "Index Fund Strategy",
    "totalCorpus": ...,
    "monthlyIncome": ...,
    "structure": "4% Safe Withdrawal Rate (Bengen/Trinity Study)",
    "notes": "Corpus remains invested; withdrawals taxed as LTCG above ₹1.25L/year."
  },
  "hybrid": {
    "name": "Tax-Optimal Hybrid",
    "monthlyIncome": ...,
    "structure": "NPS annuity income + Index Fund SWR withdrawal"
  },
  "recommendation": "TAX_HYBRID",
  "reasoning": "..."
}
```
> **Demo point:** Priya's projected monthly wage at age 60 (after 6% inflation × 31 years) is **₹4.56 lakh** — shows why investing even ₹50-250 of remanent per transaction, compounded over 31 years, matters. This is the emotional hook of the product.

---

## 10. POST /returns:report  *(BONUS — downloads PDF)*

**Purpose:** Download a full 5-page PDF investment report.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:report \
  -H "Content-Type: application/json" \
  -o priya_sharma_report.pdf \
  -d '{
    "age": 29,
    "wage": 75000.0,
    "inflation": 6.0,
    "q": [{"fixed": 50.0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 200.0, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-06-30 23:59:59"},
      {"start": "2023-07-01 00:00:00", "end": "2023-12-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-01-08 11:30:00", "amount": 285.0},
      {"date": "2023-01-21 19:45:00", "amount": 847.0},
      {"date": "2023-02-05 09:15:00", "amount": 2253.0},
      {"date": "2023-02-17 20:00:00", "amount": 1742.0},
      {"date": "2023-03-11 14:30:00", "amount": 368.0},
      {"date": "2023-03-28 18:00:00", "amount": 3499.0},
      {"date": "2023-04-07 12:00:00", "amount": 1379.0},
      {"date": "2023-04-19 21:00:00", "amount": 1156.0},
      {"date": "2023-05-03 08:30:00", "amount": 2847.0},
      {"date": "2023-05-22 17:45:00", "amount": 648.0},
      {"date": "2023-06-14 13:00:00", "amount": 1935.0},
      {"date": "2023-06-29 20:15:00", "amount": 749.0},
      {"date": "2023-07-10 11:00:00", "amount": 8647.0},
      {"date": "2023-08-05 16:30:00", "amount": 5234.0},
      {"date": "2023-08-20 09:00:00", "amount": 1249.0},
      {"date": "2023-09-12 20:00:00", "amount": 2147.0},
      {"date": "2023-09-28 14:00:00", "amount": 2863.0},
      {"date": "2023-10-14 11:30:00", "amount": 4736.0},
      {"date": "2023-10-30 19:00:00", "amount": 12549.0},
      {"date": "2023-11-11 12:00:00", "amount": 1648.0},
      {"date": "2023-11-25 15:30:00", "amount": 2762.0},
      {"date": "2023-12-08 21:00:00", "amount": 3847.0},
      {"date": "2023-12-23 14:00:00", "amount": 6249.0}
    ]
  }'
```

**PDF Contents (5 pages):**
| Page | Content |
|------|---------|
| **1** | Header + KPI Dashboard (corpus, tax saved, monthly income, hybrid advantage) + Investor Profile |
| **2** | NPS detailed returns table + NIFTY 50 detailed returns table + horizontal bar chart |
| **3** | Hybrid allocation analysis (pie chart) + Retirement Income Bridge (3 scenarios) |
| **4** | Breakeven inflation table + Risk Profile matrix (volatility, Sharpe, risk rating) + Recommendation |
| **5** | ✨ **NEW** — 6 Asset Class Comparison table + Corpus bar chart + Diversified Portfolio suggestion |

> **Demo point:** Open `priya_sharma_report.pdf` on screen. Flip through 5 pages. Page 5 is the new alternative investments comparison — **Gold vs Silver vs REITs vs GOI Bonds vs NPS vs NIFTY 50** side by side with a diversified portfolio suggestion.

---

## 11. POST /returns:alternatives  *(BONUS — NEW)*

**Purpose:** Compare remanent invested across Gold, Silver, Bonds, REITs vs NPS + Index Fund.

```bash
curl -s -X POST http://localhost:5477/blackrock/challenge/v1/returns:alternatives \
  -H "Content-Type: application/json" \
  -d '{
    "age": 29,
    "wage": 75000.0,
    "inflation": 6.0,
    "q": [{"fixed": 50.0, "start": "2023-07-01 00:00:00", "end": "2023-07-31 23:59:59"}],
    "p": [{"extra": 200.0, "start": "2023-10-01 00:00:00", "end": "2023-12-31 23:59:59"}],
    "k": [
      {"start": "2023-01-01 00:00:00", "end": "2023-06-30 23:59:59"},
      {"start": "2023-07-01 00:00:00", "end": "2023-12-31 23:59:59"}
    ],
    "transactions": [
      {"date": "2023-01-08 11:30:00", "amount": 285.0},
      {"date": "2023-01-21 19:45:00", "amount": 847.0},
      {"date": "2023-02-05 09:15:00", "amount": 2253.0},
      {"date": "2023-02-17 20:00:00", "amount": 1742.0},
      {"date": "2023-03-11 14:30:00", "amount": 368.0},
      {"date": "2023-03-28 18:00:00", "amount": 3499.0},
      {"date": "2023-04-07 12:00:00", "amount": 1379.0},
      {"date": "2023-04-19 21:00:00", "amount": 1156.0},
      {"date": "2023-05-03 08:30:00", "amount": 2847.0},
      {"date": "2023-05-22 17:45:00", "amount": 648.0},
      {"date": "2023-06-14 13:00:00", "amount": 1935.0},
      {"date": "2023-06-29 20:15:00", "amount": 749.0},
      {"date": "2023-07-10 11:00:00", "amount": 8647.0},
      {"date": "2023-08-05 16:30:00", "amount": 5234.0},
      {"date": "2023-08-20 09:00:00", "amount": 1249.0},
      {"date": "2023-09-12 20:00:00", "amount": 2147.0},
      {"date": "2023-09-28 14:00:00", "amount": 2863.0},
      {"date": "2023-10-14 11:30:00", "amount": 4736.0},
      {"date": "2023-10-30 19:00:00", "amount": 12549.0},
      {"date": "2023-11-11 12:00:00", "amount": 1648.0},
      {"date": "2023-11-25 15:30:00", "amount": 2762.0},
      {"date": "2023-12-08 21:00:00", "amount": 3847.0},
      {"date": "2023-12-23 14:00:00", "amount": 6249.0}
    ]
  }'
```

**Expected highlights:**
```json
{
  "age": 29,
  "yearsToRetirement": 31,
  "totalInvested": 2058.0,
  "inflationRatePct": 6.0,
  "nps":      {"name":"NPS","annualRatePct":7.11,"realCorpus":...,"riskLevel":"LOW","liquidity":"LOW","rank":6},
  "indexFund":{"name":"NIFTY 50 Index Fund","annualRatePct":14.49,"realCorpus":...,"riskLevel":"HIGH","liquidity":"HIGH","rank":1},
  "gold":     {"name":"Gold","annualRatePct":11.50,"realCorpus":...,"riskLevel":"MEDIUM","liquidity":"MEDIUM","rank":2},
  "silver":   {"name":"Silver","annualRatePct":9.80,"realCorpus":...,"riskLevel":"HIGH","liquidity":"MEDIUM","rank":4},
  "bonds":    {"name":"GOI Bonds","annualRatePct":7.50,"realCorpus":...,"riskLevel":"LOW","liquidity":"MEDIUM","rank":5},
  "reits":    {"name":"REITs","annualRatePct":10.20,"realCorpus":...,"riskLevel":"MEDIUM","liquidity":"HIGH","rank":3},
  "topPick": "NIFTY 50 Index Fund",
  "ranking": [
    "#1 NIFTY 50 Index Fund — ₹...",
    "#2 Gold — ₹...",
    "#3 REITs — ₹...",
    "#4 Silver — ₹...",
    "#5 GOI Bonds — ₹...",
    "#6 NPS — ₹..."
  ],
  "portfolioSuggestion": "40% NIFTY 50 Index Fund + 25% NPS (₹2L tax cap) + 15% Gold (SGB) + 10% REITs (Embassy/Brookfield) + 7% Silver + 3% GOI Bonds → Projected corpus: ₹...",
  "diversifiedCorpus": ...,
  "diversifiedAdvantageOverNps": ...
}
```

> **Demo point:** Gold beats NPS, REITs are competitive, NIFTY 50 wins overall. The `portfolioSuggestion` is the money quote — it shows judges you understand real-world portfolio construction, not just pension math.

---

## Quick Reference — Annual Return Rates

| Instrument | Rate | Basis |
|-----------|------|-------|
| NIFTY 50 Index Fund | 14.49% | NSE NIFTY 50 10-yr CAGR (2014–2024) |
| Gold (SGB) | 11.50% | RBI Sovereign Gold Bond 10-yr CAGR |
| REITs | 10.20% | Embassy/Brookfield/Mindspace avg CAGR (2019–2024) |
| Silver | 9.80% | MCX Silver 10-yr CAGR |
| GOI Bonds | 7.50% | RBI 10-yr Gilt yield (Dec 2024) |
| NPS | 7.11% | PFRDA NPS Tier-I blended 10-yr avg |

---

## Edge Case Demo Payloads

### Near-Retirement Investor (Age 58)
```json
{
  "age": 58,
  "wage": 150000.0,
  "inflation": 6.0,
  "k": [{"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
  "transactions": [
    {"date": "2023-03-15 10:00:00", "amount": 4750.0},
    {"date": "2023-07-22 14:30:00", "amount": 8300.0},
    {"date": "2023-11-08 09:15:00", "amount": 12999.0}
  ]
}
```
> Age ≥ 60 clamps to minimum 5-year horizon. ₹1,50,000 wage → higher tax slab (30% above ₹15L annual) → significant NPS tax benefit.

### High-Earner Hitting ₹2L NPS Cap (Age 35)
```json
{
  "age": 35,
  "wage": 250000.0,
  "inflation": 5.5,
  "q": [{"fixed": 5000.0, "start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
  "k": [{"start": "2023-01-01 00:00:00", "end": "2023-12-31 23:59:59"}],
  "transactions": [
    {"date": "2023-02-10 12:00:00", "amount": 45000.0},
    {"date": "2023-05-20 15:00:00", "amount": 38500.0},
    {"date": "2023-08-14 11:00:00", "amount": 52000.0},
    {"date": "2023-11-30 20:00:00", "amount": 61000.0}
  ]
}
```
> Q period fixed ₹5,000 per transaction × 4 transactions = ₹20,000 annual remanent. ₹30L annual income → 30% slab. Big tax benefit visible in NPS, and hybrid still recommends 100% NPS (₹20k << ₹2L cap).
