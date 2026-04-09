# LMDB Benchmark Report

**Date:** 2026-02-01 19:11:48

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 498 | 200,803 | 30.65 MB | 321.4 |
| 1M | 2,398 | 417,014 | 134.20 MB | 140.7 |
| 10M | 23,204 | 430,960 | 830.65 MB | 87.1 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 763,358 | 40,000 | 555,000 | 951,571 | 1,111,111 |
| 1M | 1,699,201 | 442,416 | 1,000,000 | 1,818,181 | 1,710,128 |
| 10M | 1,164,451 | 803,476 | 833,333 | 1,428,571 | 1,159,639 |

## Summary

At **10M** scale:

- **Write throughput:** 430,960 statements/sec
- **Storage efficiency:** 87.1 bytes/statement
- **Full scan:** 1,164,451 statements/sec
- **Indexed lookups:** 803,476 - 1,428,571 statements/sec
