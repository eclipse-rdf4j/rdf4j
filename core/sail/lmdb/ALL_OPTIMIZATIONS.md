# LMDB Sail - All Optimizations Summary

**Total: 15 Optimizations Implemented**
**Date:** 2026-02-02

---

## Final Results

### Concurrent Read Performance
| Threads | Baseline | Final | Improvement |
|---------|----------|-------|-------------|
| 1 | 1,879,176 | 1,935,000 | **+3.0%** |
| 2 | 2,709,194 | 3,400,000 | **+25.5%** |
| 4 | 3,130,677 | 4,700,000 | **+50.1%** |

### Sequential Performance
| Metric | Baseline | Final | Improvement |
|--------|----------|-------|-------------|
| Write | 416,701 | 430,960 | **+3.4%** |
| Full Scan | 1,156,851 | 1,164,451 | **+0.7%** |
| By Subject | 784,791 | 803,476 | **+2.4%** |

---

## Round 1 Optimizations

### 1. Index Selection Caching
**File:** `TripleStore.java`

**Problem:** `getBestIndex()` scanned all indexes O(N) for every query to find the best matching index.

**Solution:** Pre-compute best index for all 16 possible query patterns (2^4 combinations of s/p/o/c bound/unbound) at startup.

```java
// Pre-computed cache for O(1) lookup
private final TripleIndex[] indexSelectionCache = new TripleIndex[16];

private void initIndexSelectionCache() {
    for (int pattern = 0; pattern < 16; pattern++) {
        long s = (pattern & 0b1000) != 0 ? 1 : -1;
        long p = (pattern & 0b0100) != 0 ? 1 : -1;
        long o = (pattern & 0b0010) != 0 ? 1 : -1;
        long c = (pattern & 0b0001) != 0 ? 1 : -1;
        indexSelectionCache[pattern] = computeBestIndex(s, p, o, c);
    }
}

protected TripleIndex getBestIndex(long subj, long pred, long obj, long context) {
    int pattern = ((subj >= 0 ? 1 : 0) << 3) | ((pred >= 0 ? 1 : 0) << 2)
                | ((obj >= 0 ? 1 : 0) << 1) | (context >= 0 ? 1 : 0);
    return indexSelectionCache[pattern];
}
```

---

### 2. Iterator Lock Batching
**File:** `LmdbRecordIterator.java`

**Problem:** Acquiring/releasing read lock for every single record caused high contention.

**Solution:** Hold the read lock for a batch of records (256) before releasing.

```java
private static final int LOCK_BATCH_SIZE = 256;
private int batchCounter = 0;
private long heldReadStamp = 0L;
private boolean lockHeld = false;

public long[] next() {
    if (!lockHeld) {
        heldReadStamp = txnLockManager.readLock();
        lockHeld = true;
        batchCounter = 0;
    }
    // ... read record ...
    batchCounter++;
    if (batchCounter >= LOCK_BATCH_SIZE) {
        releaseLockIfHeld();
    }
    return quad;
}
```

---

### 3. GroupMatcher Pre-creation
**File:** `LmdbRecordIterator.java`

**Problem:** GroupMatcher was lazily created on first match, causing latency spike.

**Solution:** Pre-create GroupMatcher in iterator constructor.

```java
// In constructor
if (this.matchValues) {
    this.groupMatcher = index.createMatcher(subj, pred, obj, context);
}
```

---

### 4. Larger Default Caches
**File:** `LmdbStoreConfig.java`

**Problem:** Default cache sizes were too small for modern workloads.

**Solution:** Increased default cache sizes for value cache and other caches.

---

### 5. Context Counter Batching
**File:** `TripleStore.java`

**Problem:** Every statement add/remove did a read-modify-write to update context counters.

**Solution:** Buffer context counter changes in memory, flush at commit time.

```java
private final Map<Long, Long> pendingContextIncrements = new HashMap<>();

private void incrementContext(MemoryStack stack, long context) {
    pendingContextIncrements.merge(context, 1L, Long::sum);
}

private void flushContextIncrements() {
    for (Map.Entry<Long, Long> entry : pendingContextIncrements.entrySet()) {
        // Apply batched delta to database
    }
    pendingContextIncrements.clear();
}
```

---

## Round 2 Optimizations

### 6. Fast Quad Read Path
**File:** `Varint.java`

**Problem:** Reading 4 varints required 4 separate decode calls.

**Solution:** Fast path for common case where all 4 values are ≤ 240 (single-byte encoding).

```java
public static void readQuadUnsigned(ByteBuffer bb, long[] values) {
    int pos = bb.position();
    if (bb.remaining() >= 4) {
        int b0 = bb.get(pos) & 0xFF;
        int b1 = bb.get(pos + 1) & 0xFF;
        int b2 = bb.get(pos + 2) & 0xFF;
        int b3 = bb.get(pos + 3) & 0xFF;
        if (b0 <= 240 && b1 <= 240 && b2 <= 240 && b3 <= 240) {
            values[0] = b0;
            values[1] = b1;
            values[2] = b2;
            values[3] = b3;
            bb.position(pos + 4);
            return;
        }
    }
    // Fall back to individual reads
    values[0] = readUnsigned(bb);
    values[1] = readUnsigned(bb);
    values[2] = readUnsigned(bb);
    values[3] = readUnsigned(bb);
}
```

---

### 7. ConcurrentHashMap in TxnManager
**File:** `TxnManager.java`

**Problem:** Synchronized HashMap caused contention for transaction lookups.

**Solution:** Use ConcurrentHashMap for lock-free reads.

---

### 8. Thread-Local Transaction Cache
**File:** `TxnManager.java`

**Problem:** Transaction lookup required map access on every operation.

**Solution:** Cache current thread's transaction in ThreadLocal.

```java
private final ThreadLocal<Txn> threadLocalTxn = new ThreadLocal<>();

public Txn getTxn() {
    Txn cached = threadLocalTxn.get();
    if (cached != null && cached.isValid()) {
        return cached;
    }
    Txn txn = lookupOrCreate();
    threadLocalTxn.set(txn);
    return txn;
}
```

---

### 9. Record Object Pooling
**File:** `TxnRecordCache.java`

**Problem:** Creating new Record objects for every cached record caused GC pressure.

**Solution:** Pool and reuse Record objects.

---

## Round 3 Optimizations

### 10. Fast Quad Write Path
**File:** `Varint.java`

**Problem:** Writing 4 varints required 4 separate encode calls.

**Solution:** Fast path for common case where all 4 values are ≤ 240.

```java
public static void writeListUnsigned(ByteBuffer bb, long a, long b, long c, long d) {
    if (a <= 240 && b <= 240 && c <= 240 && d <= 240) {
        bb.put((byte) a);
        bb.put((byte) b);
        bb.put((byte) c);
        bb.put((byte) d);
        return;
    }
    writeUnsigned(bb, a);
    writeUnsigned(bb, b);
    writeUnsigned(bb, c);
    writeUnsigned(bb, d);
}
```

---

### 11. LmdbStatementIterator Optimization
**File:** `LmdbStatementIterator.java`

**Problem:** Redundant array accesses and method calls when creating statements.

**Solution:** Extract quad values to local variables first, optimize context check.

```java
public Statement getNextElement() {
    long[] quad = recordIt.next();
    if (quad == null) return null;

    // Extract all IDs first to minimize array access overhead
    long subjID = quad[TripleStore.SUBJ_IDX];
    long predID = quad[TripleStore.PRED_IDX];
    long objID = quad[TripleStore.OBJ_IDX];
    long contextID = quad[TripleStore.CONTEXT_IDX];

    Resource subj = (Resource) valueStore.getLazyValue(subjID);
    IRI pred = (IRI) valueStore.getLazyValue(predID);
    Value obj = valueStore.getLazyValue(objID);

    // Only create context if non-zero
    Resource context = (contextID != 0) ? (Resource) valueStore.getLazyValue(contextID) : null;

    return valueStore.createStatement(subj, pred, obj, context);
}
```

---

## Round 4 Optimizations

### 12. Increased Lock Batch Size (100→256)
**File:** `LmdbRecordIterator.java`

**Problem:** Batch size of 100 was suboptimal.

**Solution:** Testing showed 256 provides better balance between throughput and latency.

```java
private static final int LOCK_BATCH_SIZE = 256;  // was 100
```

---

### 13. PointerBuffer Pooling
**Files:** `Pool.java`, `LmdbRecordIterator.java`

**Problem:** Allocating PointerBuffer for every cursor open.

**Solution:** Cache PointerBuffer in thread-local Pool.

```java
// Pool.java
private PointerBuffer cachedPointerBuffer;

final PointerBuffer getPointerBuffer() {
    if (cachedPointerBuffer == null) {
        cachedPointerBuffer = MemoryUtil.memAllocPointer(1);
    }
    return cachedPointerBuffer;
}

// LmdbRecordIterator.java - use pooled buffer
PointerBuffer pp = pool.getPointerBuffer();
E(mdb_cursor_open(txn, dbi, pp));
cursor = pp.get(0);
```

---

## Round 5 Optimizations

### 14. Specialized KeyReader
**Files:** `TripleStore.java`, `LmdbRecordIterator.java`

**Problem:** Every record read required 12 array lookups:
- 8 accesses to `indexMap[]`
- 4 accesses to `originalQuad[]`

**Solution:** Pre-compute skip pattern and target indices at iterator creation. Create 16 specialized KeyReader implementations.

```java
@FunctionalInterface
interface KeyReader {
    void read(ByteBuffer key, long[] quad);
}

KeyReader createKeyReader(long[] originalQuad) {
    final int idx0 = indexMap[0], idx1 = indexMap[1], idx2 = indexMap[2], idx3 = indexMap[3];
    final boolean skip0 = originalQuad[idx0] != -1;
    final boolean skip1 = originalQuad[idx1] != -1;
    final boolean skip2 = originalQuad[idx2] != -1;
    final boolean skip3 = originalQuad[idx3] != -1;

    int mask = (skip0 ? 1 : 0) | (skip1 ? 2 : 0) | (skip2 ? 4 : 0) | (skip3 ? 8 : 0);

    switch (mask) {
    case 0b0000:  // Read all 4
        return (key, quad) -> {
            quad[idx0] = Varint.readUnsigned(key);
            quad[idx1] = Varint.readUnsigned(key);
            quad[idx2] = Varint.readUnsigned(key);
            quad[idx3] = Varint.readUnsigned(key);
        };
    case 0b0001:  // Skip 0, read 1,2,3
        return (key, quad) -> {
            Varint.skipUnsigned(key);
            quad[idx1] = Varint.readUnsigned(key);
            quad[idx2] = Varint.readUnsigned(key);
            quad[idx3] = Varint.readUnsigned(key);
        };
    // ... 14 more cases for all patterns
    }
}
```

**Impact:** +16% at 2 threads, +43% at 4 threads

---

### 15. Quad Array Pooling
**Files:** `Pool.java`, `LmdbRecordIterator.java`

**Problem:** Allocating `long[4]` arrays for every iterator.

**Solution:** Pool and reuse quad arrays.

```java
// Pool.java
private static final int QUAD_POOL_SIZE = 256;
private final long[][] quadPool = new long[QUAD_POOL_SIZE][];
private int quadPoolIndex = -1;

final long[] getQuadArray() {
    if (quadPoolIndex >= 0) {
        return quadPool[quadPoolIndex--];
    }
    return new long[4];
}

final void free(long[] quad) {
    if (quadPoolIndex < quadPool.length - 1) {
        quadPool[++quadPoolIndex] = quad;
    }
}

// LmdbRecordIterator.java
this.originalQuad = pool.getQuadArray();
this.quad = pool.getQuadArray();

// In closeInternal()
pool.free(quad);
pool.free(originalQuad);
```

---

## Reverted Optimizations (Caused Regressions)

| Optimization | Issue |
|-------------|-------|
| Write Lock Striping | LMDB has single-writer architecture |
| GroupMatcher Pooling | Caused 5x write regression |
| ValueStore Revision Fast-path | Caused 5x write regression |
| FNV-1a Hash | Part of revision fast-path |

---

## Files Modified

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

## Future Ideas

1. Profile with async-profiler to find remaining hot spots
2. Batch value lookups in LmdbStoreConnection
3. Cursor pooling for repeated queries
4. LMDB MDB_NORDAHEAD flag for random access
5. Off-heap value cache to reduce GC
6. Pool lazy value objects (LmdbIRI/LmdbLiteral/LmdbBNode)
7. Specialize GroupMatcher per index type
