# LMDB Benchmark Report

**Date:** 2026-02-01 18:14:44

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 505 | 198,019 | 30.65 MB | 321.4 |
| 1M | 2,426 | 412,201 | 134.13 MB | 140.6 |
| 10M | 23,272 | 429,700 | 831.27 MB | 87.2 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 699,300 | 43,478 | 555,000 | 951,571 | 1,136,363 |
| 1M | 1,677,760 | 442,416 | 1,000,000 | 1,538,461 | 1,743,770 |
| 10M | 1,181,611 | 843,650 | 833,333 | 1,333,333 | 1,174,195 |

## Summary

At **10M** scale:

- **Write throughput:** 429,700 statements/sec
- **Storage efficiency:** 87.2 bytes/statement
- **Full scan:** 1,181,611 statements/sec
- **Indexed lookups:** 833,333 - 1,333,333 statements/sec
