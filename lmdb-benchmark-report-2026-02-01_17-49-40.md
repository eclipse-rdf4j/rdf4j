# LMDB Benchmark Report

**Date:** 2026-02-01 17:49:40

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 522 | 191,570 | 30.65 MB | 321.4 |
| 1M | 2,401 | 416,493 | 134.06 MB | 140.6 |
| 10M | 24,010 | 416,493 | 830.98 MB | 87.1 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 666,666 | 45,454 | 555,000 | 1,110,166 | 1,369,863 |
| 1M | 1,743,770 | 482,636 | 909,090 | 1,666,666 | 1,688,412 |
| 10M | 1,179,129 | 803,476 | 833,333 | 1,333,333 | 1,175,835 |

## Summary

At **10M** scale:

- **Write throughput:** 416,493 statements/sec
- **Storage efficiency:** 87.1 bytes/statement
- **Full scan:** 1,179,129 statements/sec
- **Indexed lookups:** 803,476 - 1,333,333 statements/sec
