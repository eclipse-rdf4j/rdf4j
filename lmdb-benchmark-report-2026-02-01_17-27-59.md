# LMDB Benchmark Report

**Date:** 2026-02-01 17:27:59

**Index Configuration:** `spoc,sopc,posc,ospc,opsc,cspo`

**Java Version:** 25.0.1

**OS:** Mac OS X aarch64

## Write Performance

| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |
|-------|-----------|------------------|------------|------------|
| 100K | 521 | 191,938 | 30.65 MB | 321.4 |
| 1M | 2,423 | 412,711 | 134.13 MB | 140.6 |
| 10M | 24,288 | 411,725 | 830.45 MB | 87.1 |
| 100M | 575,159 | 173,864 | 6719.21 MB | 70.5 |

## Read Performance (stmts/sec)

| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |
|-------|-----------|------------|--------------|-----------|------------|
| 100K | 704,225 | 38,461 | 555,000 | 951,571 | 1,298,701 |
| 1M | 1,651,708 | 442,416 | 769,230 | 1,428,571 | 1,710,128 |
| 10M | 1,147,782 | 784,790 | 769,230 | 1,428,571 | 1,142,719 |
| 100M | 889,363 | 800,968 | 714,285 | 1,333,333 | 888,722 |

## Summary

At **100M** scale:

- **Write throughput:** 173,864 statements/sec
- **Storage efficiency:** 70.5 bytes/statement
- **Full scan:** 889,363 statements/sec
- **Indexed lookups:** 714,285 - 1,333,333 statements/sec
