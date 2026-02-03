# LMDB Benchmark Report

**Date:** 2026-02-01 19:10:08

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 560 | 178,571 | 30.88 MB | 323.8 |
| 1M | 3,236 | 309,023 | 134.20 MB | 140.7 |
| 10M | 126,388 | 79,121 | 845.07 MB | 88.6 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 704,225 | 41,666 | 555,000 | 951,571 | 1,250,000 |
| 1M | 1,683,069 | 379,214 | 1,000,000 | 1,538,461 | 1,646,594 |
| 10M | 1,161,239 | 784,790 | 833,333 | 1,428,571 | 1,155,263 |

## Summary

At **10M** scale:

- **Write throughput:** 79,121 statements/sec
- **Storage efficiency:** 88.6 bytes/statement
- **Full scan:** 1,161,239 statements/sec
- **Indexed lookups:** 784,790 - 1,428,571 statements/sec
