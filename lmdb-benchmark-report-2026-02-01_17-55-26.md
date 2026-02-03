# LMDB Benchmark Report

**Date:** 2026-02-01 17:55:26

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 514 | 194,552 | 30.65 MB | 321.4 |
| 1M | 2,444 | 409,165 | 134.04 MB | 140.6 |
| 10M | 24,431 | 409,316 | 830.88 MB | 87.1 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 724,637 | 41,666 | 555,000 | 951,571 | 1,315,789 |
| 1M | 1,168,901 | 442,416 | 833,333 | 1,666,666 | 1,704,647 |
| 10M | 1,167,681 | 803,476 | 833,333 | 1,428,571 | 1,156,453 |

## Summary

At **10M** scale:

- **Write throughput:** 409,316 statements/sec
- **Storage efficiency:** 87.1 bytes/statement
- **Full scan:** 1,167,681 statements/sec
- **Indexed lookups:** 803,476 - 1,428,571 statements/sec
