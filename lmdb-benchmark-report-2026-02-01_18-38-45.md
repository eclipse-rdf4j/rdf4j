# LMDB Benchmark Report

**Date:** 2026-02-01 18:38:45

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 566 | 176,678 | 30.65 MB | 321.4 |
| 1M | 2,377 | 420,698 | 134.06 MB | 140.6 |
| 10M | 23,603 | 423,674 | 831.06 MB | 87.1 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 735,294 | 35,714 | 555,000 | 832,625 | 1,063,829 |
| 1M | 1,715,645 | 408,384 | 833,333 | 1,538,461 | 1,651,708 |
| 10M | 1,153,680 | 784,790 | 833,333 | 1,428,571 | 1,144,272 |

## Summary

At **10M** scale:

- **Write throughput:** 423,674 statements/sec
- **Storage efficiency:** 87.1 bytes/statement
- **Full scan:** 1,153,680 statements/sec
- **Indexed lookups:** 784,790 - 1,428,571 statements/sec
