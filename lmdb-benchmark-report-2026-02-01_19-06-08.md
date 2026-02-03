# LMDB Benchmark Report

**Date:** 2026-02-01 19:06:08

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 602 | 166,112 | 30.88 MB | 323.8 |
| 1M | 3,727 | 268,312 | 134.18 MB | 140.7 |
| 10M | 127,841 | 78,222 | 845.26 MB | 88.6 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 699,300 | 38,461 | 475,714 | 350,578 | 1,111,111 |
| 1M | 1,537,138 | 331,812 | 666,666 | 1,111,111 | 1,053,168 |
| 10M | 1,179,129 | 766,954 | 833,333 | 1,428,571 | 1,158,044 |

## Summary

At **10M** scale:

- **Write throughput:** 78,222 statements/sec
- **Storage efficiency:** 88.6 bytes/statement
- **Full scan:** 1,179,129 statements/sec
- **Indexed lookups:** 766,954 - 1,428,571 statements/sec
