# LMDB Sail Optimization Results

**Date:** 2026-02-02

## Final Benchmark Comparison

### Concurrent Read Performance (ops/sec, 5-run average)

| Threads | Baseline | Final (Avg) | Improvement |
|---------|----------|-------------|-------------|
| 1 | 1,879,176 | 1,882,446 | **+0.2%** |
| 2 | 2,709,194 | 3,044,976 | **+12.4%** |
| 4 | 3,130,677 | 4,238,690 | **+35.4%** |
| 8 | 3,332,780 | 2,747,443 | -17.6% |

### Sequential Performance (10M statements, 6 indexes)

| Metric | Baseline | Final | Improvement |
|--------|----------|-------|-------------|
| Write | 416,701 | 430,960 | **+3.4%** |
| Full Scan | 1,156,851 | 1,164,451 | **+0.7%** |
| By Subject | 784,791 | 803,476 | **+2.4%** |

---

## Implemented Optimizations (15 total)

### Round 1 - Working

| # | Optimization | File | Status |
|---|-------------|------|--------|
| 1 | Index Selection Caching | `TripleStore.java` | ✅ |
| 2 | Iterator Lock Batching | `LmdbRecordIterator.java` | ✅ |
| 3 | GroupMatcher Pre-creation | `LmdbRecordIterator.java` | ✅ |
| 4 | Larger Default Caches | `LmdbStoreConfig.java` | ✅ |
| 5 | Context Counter Batching | `TripleStore.java` | ✅ |

### Round 2 - Working

| # | Optimization | File | Status |
|---|-------------|------|--------|
| 6 | Fast Quad Read Path | `Varint.java` | ✅ |
| 7 | ConcurrentHashMap in TxnManager | `TxnManager.java` | ✅ |
| 8 | Thread-Local Transaction Cache | `TxnManager.java` | ✅ |
| 9 | Record Object Pooling | `TxnRecordCache.java` | ✅ |

### Round 3 - Working

| # | Optimization | File | Status |
|---|-------------|------|--------|
| 10 | Fast Quad Write Path | `Varint.java` | ✅ |
| 11 | LmdbStatementIterator optimization | `LmdbStatementIterator.java` | ✅ |

### Round 4 - Working (2026-02-01)

| # | Optimization | File | Status |
|---|-------------|------|--------|
| 12 | Increased Lock Batch Size (100→256) | `LmdbRecordIterator.java` | ✅ |
| 13 | PointerBuffer Pooling | `Pool.java`, `LmdbRecordIterator.java` | ✅ |

**Round 4 Results:** ~5-10% improvement in concurrent reads at 1-4 threads.

### Round 5 - Working (2026-02-02)

| # | Optimization | File | Status |
|---|-------------|------|--------|
| 14 | Specialized KeyReader | `TripleStore.java`, `LmdbRecordIterator.java` | ✅ |
| 15 | Quad Array Pooling | `Pool.java`, `LmdbRecordIterator.java` | ✅ |

**Round 5 Results:** Confirmed improvement at 2-4 threads (5-run average):
- 2 threads: +12% over baseline (~3.0M ops/sec)
- 4 threads: +35% over baseline (~4.2M ops/sec)

**KeyReader Optimization Details:**
Pre-computes skip pattern and target indices at iterator creation time, eliminating 12 array lookups per record (8 indexMap + 4 originalQuad). Uses 16 specialized lambda functions matching all possible skip patterns.

---

## Reverted (Caused Regression)

| Optimization | File | Issue |
|-------------|------|-------|
| Write Lock Striping | `LmdbSailStore.java` | LMDB single-writer architecture |
| GroupMatcher Pooling | `Pool.java`, `TripleStore.java` | Caused 5x write regression |
| ValueStore Revision Fast-path | `ValueStore.java` | Caused 5x write regression |
| FNV-1a Hash | `ValueStore.java` | Part of revision fast-path |

---

## Key Findings

1. **Multi-threaded reads significantly improved** - 2 threads +12%, 4 threads +35%
2. **Sequential writes improved 3.4%**
3. **KeyReader specialization** - pre-computing skip patterns eliminates hot-path array lookups
4. **ConcurrentHashMap + thread-local caching** helps TxnManager
5. **Varint fast paths** help for small IDs
6. **ValueStore optimizations caused severe regression** - reverted
7. **LMDB single-writer** limits write parallelism gains
8. **8-thread regression** - likely due to CPU cache pressure or core contention

---

## Files Modified (Final)

```
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStatementIterator.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/Pool.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnManager.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnRecordCache.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/Varint.java
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java
```

---

## Test Results

- **816 tests passing** (2 skipped)
- No regressions

---

## Next Session Ideas

1. **Profile with async-profiler** to find remaining hot spots
2. **Batch value lookups** in LmdbStoreConnection
3. **Cursor pooling** for repeated queries - complex due to transaction lifecycle
4. **LMDB MDB_NORDAHEAD** flag for random access (value = 0x800000)
5. **Off-heap value cache** to reduce GC
6. **Investigate ValueStore regression** - why did fast-path hurt?
7. **Dynamic lock batch sizing** - adjust batch size based on workload
8. **Investigate 8-thread regression** - may be CPU core contention
9. **Pool lazy value objects** - LmdbIRI/LmdbLiteral/LmdbBNode created per statement
10. **Specialize GroupMatcher per index** - similar to KeyReader approach
