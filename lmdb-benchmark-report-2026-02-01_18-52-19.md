# LMDB Benchmark Report

**Date:** 2026-02-01 18:52:19

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 543 | 184,162 | 30.65 MB | 321.4 |
| 1M | 2,602 | 384,319 | 134.09 MB | 140.6 |
| 10M | 23,862 | 419,076 | 830.93 MB | 87.1 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 769,230 | 35,714 | 475,714 | 832,625 | 1,063,829 |
| 1M | 1,743,770 | 408,384 | 1,000,000 | 1,538,461 | 1,672,484 |
| 10M | 1,149,741 | 803,476 | 833,333 | 1,428,571 | 1,152,496 |

## Summary

At **10M** scale:

- **Write throughput:** 419,076 statements/sec
- **Storage efficiency:** 87.1 bytes/statement
- **Full scan:** 1,149,741 statements/sec
- **Indexed lookups:** 803,476 - 1,428,571 statements/sec
