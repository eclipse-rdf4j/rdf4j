# LMDB Benchmark Report

**Date:** 2026-02-01 17:36:50

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 21.0.9

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 538 | 185,873 | 30.65 MB | 321.4 |
| 1M | 2,399 | 416,840 | 134.13 MB | 140.6 |
| 10M | 24,033 | 416,094 | 830.54 MB | 87.1 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 632,911 | 25,641 | 333,000 | 832,625 | 1,075,268 |
| 1M | 1,626,452 | 482,636 | 833,333 | 1,538,461 | 1,699,201 |
| 10M | 996,624 | 784,790 | 833,333 | 1,333,333 | 1,133,488 |

## Summary

At **10M** scale:

- **Write throughput:** 416,094 statements/sec
- **Storage efficiency:** 87.1 bytes/statement
- **Full scan:** 996,624 statements/sec
- **Indexed lookups:** 784,790 - 1,333,333 statements/sec
