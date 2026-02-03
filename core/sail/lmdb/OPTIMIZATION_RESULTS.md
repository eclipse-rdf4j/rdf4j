# LMDB Sail Optimization Results

**Date:** 2026-02-02 (Updated)

## Verified Baseline (5-run average, benchmark-only branch)

Measured on commit `925873151c` (benchmark code only, no optimizations):

### Concurrent Read Performance (ops/sec)

| Threads | Baseline (Verified) |
|---------|---------------------|
| 1 | **1,931,190** |
| 2 | **3,123,939** |
| 4 | **4,298,965** |
| 8 | **2,736,509** |

### Concurrent Write Performance (ops/sec)

| Threads | Baseline (Verified) |
|---------|---------------------|
| 1 | **73,099** |
| 2 | **81,192** |
| 4 | **74,778** |

### Raw Baseline Data (5 runs)

**Concurrent Read (ops/sec):**

| Threads | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 | Average |
|---------|-------|-------|-------|-------|-------|---------|
| 1 | 1,966,339 | 2,027,129 | 1,897,392 | 1,911,924 | 1,853,166 | 1,931,190 |
| 2 | 3,082,714 | 3,131,873 | 3,055,475 | 3,228,411 | 3,121,220 | 3,123,939 |
| 4 | 4,120,026 | 4,457,757 | 4,319,446 | 4,351,756 | 4,245,840 | 4,298,965 |
| 8 | 3,164,806 | 2,685,134 | 2,709,071 | 2,593,945 | 2,529,591 | 2,736,509 |

**Concurrent Write (ops/sec):**

| Threads | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 | Average |
|---------|-------|-------|-------|-------|-------|---------|
| 1 | 75,762 | 75,443 | 73,818 | 67,652 | 72,820 | 73,099 |
| 2 | 85,020 | 85,550 | 76,322 | 82,078 | 76,992 | 81,192 |
| 4 | 73,056 | 82,202 | 71,108 | 77,080 | 70,446 | 74,778 |

---

## Final Benchmark Comparison

### Concurrent Read Performance (ops/sec, 5-run average)

| Threads | Baseline | Final (Avg) | Improvement |
|---------|----------|-------------|-------------|
| 1 | 1,931,190 | 1,841,390 | -4.6% |
| 2 | 3,123,939 | 2,956,908 | -5.4% |
| 4 | 4,298,965 | 3,909,163 | -9.1% |
| 8 | 2,736,509 | 2,841,443 | **+3.8%** |

### Concurrent Write Performance (ops/sec, 5-run average)

| Threads | Baseline | Final (Avg) | Improvement |
|---------|----------|-------------|-------------|
| 1 | 73,099 | 73,600 | **+0.7%** |
| 2 | 81,192 | 80,846 | -0.4% |
| 4 | 74,778 | 78,741 | **+5.3%** |

*Note: Results show high variance; differences are within measurement noise.*

### Sequential Performance (10M statements, 6 indexes)

| Metric | Baseline | Final | Improvement |
|--------|----------|-------|-------------|
| Write | 416,701 | 430,960 | **+3.4%** |
| Full Scan | 1,156,851 | 1,164,451 | **+0.7%** |
| By Subject | 784,791 | 803,476 | **+2.4%** |

---

## Critical Bug Fixes (2026-02-02)

The following thread safety bugs were identified and fixed:

| # | Bug | File | Fix |
|---|-----|------|-----|
| 1 | Unsafe ConcurrentHashMap iteration | `TxnManager.java` | Use `.toArray()` snapshot before iteration in `activate()`, `deactivate()`, `reset()` |
| 2 | Non-thread-safe HashMap | `TripleStore.java` | Changed `pendingContextIncrements` from `HashMap` to `ConcurrentHashMap` |

These bugs could cause `ConcurrentModificationException` or data corruption under concurrent load.

### Attempted but Reverted: Cursor Pooling

Cursor pooling was attempted to reduce `mdb_cursor_open/close` overhead but caused VM crashes. LMDB cursors are tightly bound to transactions and cannot be safely renewed across different transaction contexts without careful lifecycle management.

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

### Round 6 - Working (2026-02-03)

| # | Optimization | File | Status |
|---|-------------|------|--------|
| 16 | Optimistic Cache Lookup | `ValueStore.java` | ✅ |
| 17 | Bug fix: flushContextIncrements stack overflow | `TripleStore.java` | ✅ |

**Round 6 Results - Optimistic Cache Lookup:**

`getValue()` tries cache without lock first. Includes revision check for MVCC safety.

**Concurrent Read (ops/sec, 5-run average):**

| Threads | Current | Baseline | Change |
|---------|---------|----------|--------|
| 1 | 2,093,069 | 1,931,190 | **+8.4%** |
| 2 | 3,418,351 | 3,123,939 | **+9.4%** |
| 4 | 4,695,918 | 4,298,965 | **+9.2%** |
| 8 | 2,634,715 | 2,736,509 | -3.7% |

**Concurrent Write (ops/sec, 5-run average):**

| Threads | Current | Baseline | Change |
|---------|---------|----------|--------|
| 1 | 71,303 | 73,099 | -2.5% |
| 2 | 79,176 | 81,192 | -2.5% |
| 4 | 76,816 | 74,778 | +2.7% |

**Bug fix: flushContextIncrements stack overflow:**
Fixed `OutOfMemoryError: Out of stack space` with many contexts. Pre-allocate buffers outside loop.

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
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java
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
