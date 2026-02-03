# Lock Striping Architecture

## Thread Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Application Threads                              │
└─────────────────────────────────────────────────────────────────────────┘
           │                    │                    │
           │                    │                    │
    addStatement(S1)     addStatement(S2)     addStatement(S3)
           │                    │                    │
           ▼                    ▼                    ▼
    ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
    │ getLockStripe│     │ getLockStripe│     │ getLockStripe│
    │   (hash S1)  │     │   (hash S2)  │     │   (hash S3)  │
    └──────┬───────┘     └──────┬───────┘     └──────┬───────┘
           │ = 3                │ = 7                │ = 3
           │                    │                    │
           │                    │                    │
           ▼                    ▼                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    writeLockStripes Array [0..15]                        │
│  ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┤
│  │ 0  │ 1  │ 2  │ 3  │ 4  │ 5  │ 6  │ 7  │ 8  │ 9  │ 10 │ 11 │ 12 │... │
│  └────┴────┴────┴─▲──┴────┴────┴────┴─▲──┴────┴────┴────┴────┴────┴────┤
│                   │                    │                                 │
│                   │ Contention!        │ Thread 2 can                    │
│                   │ Thread 1 & 3       │ proceed in parallel             │
│                   │ share lock         │                                 │
└───────────────────┼────────────────────┼─────────────────────────────────┘
                    │                    │
         ┌──────────┴────────┐           │
         │ Thread 1 acquires │           │
         │ (Thread 3 waits)  │           │
         └──────────┬────────┘           │
                    │                    │
                    ▼                    ▼
            ┌───────────────────┬───────────────────┐
            │  ValueStore       │  ValueStore       │
            │  storeValue(...)  │  storeValue(...)  │
            └───────┬───────────┴───────┬───────────┘
                    │                    │
                    ▼                    ▼
            ┌───────────────────┬───────────────────┐
            │  TripleStore      │  TripleStore      │
            │  storeTriple(...) │  storeTriple(...) │
            └───────────────────┴───────────────────┘
```

## Data Structure Layout

```
LmdbSailStore
├── writeLockStripes: ReentrantLock[16]
│   ├── [0] ──→ ReentrantLock (subjects with hash % 16 = 0)
│   ├── [1] ──→ ReentrantLock (subjects with hash % 16 = 1)
│   ├── ...
│   └── [15] ─→ ReentrantLock (subjects with hash % 16 = 15)
│
├── sinkStoreAccessLock: ReentrantLock
│   └── Used for: flush(), removeStatements(), namespace operations
│
├── valueStore: ValueStore
│   └── Thread-safe internal locking
│
└── tripleStore: TripleStore
    └── Thread-safe internal locking
```

## Hash Distribution Example

```
Subject URIs                    Hash Code       Stripe    Lock
─────────────────────────────────────────────────────────────────
http://ex.org/Person1          -123456789  →      3    →  Lock[3]
http://ex.org/Person2           987654321  →      1    →  Lock[1]
http://ex.org/Company1         1234567890  →     14    →  Lock[14]
http://ex.org/Product1         -987654321  →      7    →  Lock[7]
http://ex.org/Person3           -98765432  →      3    →  Lock[3]  ← Contends with Person1

Calculation: (hashCode & 0x7FFFFFFF) & 0xF
             └─ Ensure positive ─┘    └─ Mask to 0-15 ─┘
```

## Lock Acquisition Patterns

### Pattern 1: Single Statement Add
```
addStatement(subject, pred, obj, context)
│
├─→ stripe = getLockStripe(subject)     // e.g., 5
│
├─→ writeLockStripes[5].lock()          // Acquire stripe lock
│
├─→ startTransaction()                   // Start if needed
│
├─→ valueStore.storeValue(...)          // Store values
│
├─→ tripleStore.storeTriple(...)        // Store triple
│
└─→ writeLockStripes[5].unlock()        // Release
```

### Pattern 2: Bulk Add (approveAll)
```
approveAll(Set<Statement> statements)
│
├─→ Group by stripe:
│   statements = [S1(stripe=3), S2(stripe=7), S3(stripe=3), S4(stripe=1)]
│
│   Stripe 1: [S4]
│   Stripe 3: [S1, S3]
│   Stripe 7: [S2]
│
├─→ Determine needed locks: [1, 3, 7]
│
├─→ Acquire in order:                    // Deadlock prevention
│   writeLockStripes[1].lock()
│   writeLockStripes[3].lock()
│   writeLockStripes[7].lock()
│
├─→ Process all statements
│
└─→ Release in reverse:
    writeLockStripes[7].unlock()
    writeLockStripes[3].unlock()
    writeLockStripes[1].unlock()
```

### Pattern 3: Remove Statements (Global Lock)
```
removeStatements(subj, pred, obj, contexts)
│
├─→ sinkStoreAccessLock.lock()           // Global lock (all stripes)
│
├─→ startTransaction()
│
├─→ tripleStore.removeTriples(...)       // May affect many subjects
│
└─→ sinkStoreAccessLock.unlock()
```

## Contention Scenarios

### Scenario A: Low Contention (Different Subjects)
```
Time →

Thread 1: [──Lock[3]──][────Work────][Unlock]
Thread 2:    [──Lock[7]──][────Work────][Unlock]
Thread 3:       [──Lock[1]──][────Work────][Unlock]
Thread 4:          [──Lock[12]─][────Work────][Unlock]

Result: All threads run in parallel (4x speedup)
```

### Scenario B: Moderate Contention (Some Hash Collisions)
```
Time →

Thread 1: [──Lock[3]──][────Work────][Unlock]
Thread 2:    [──Lock[7]──][────Work────][Unlock]
Thread 3:       [Wait...][──Lock[3]──][────Work────][Unlock]
Thread 4:          [──Lock[12]─][────Work────][Unlock]

Result: Partial parallelism (Thread 3 waits for Thread 1)
```

### Scenario C: High Contention (Same Subject)
```
Time →

Thread 1: [──Lock[3]──][────Work────][Unlock]
Thread 2:    [Wait........][──Lock[3]──][────Work────][Unlock]
Thread 3:       [Wait........................][──Lock[3]──][Work][Unlock]
Thread 4:          [Wait.......................................][Lock[3]]

Result: Serial execution (like single lock, but only for this subject)
```

## Deadlock Prevention in approveAll

### Why Ordering Matters
```
BAD (Can deadlock):
─────────────────────
Thread A needs locks: [3, 7]
Thread B needs locks: [7, 3]

Thread A: Lock[3] ✓  → Lock[7] ⏳ (waiting for Thread B)
Thread B: Lock[7] ✓  → Lock[3] ⏳ (waiting for Thread A)

Result: DEADLOCK! ✗


GOOD (Ordered acquisition):
────────────────────────────
Thread A needs locks: [3, 7]  → acquires in order [3, 7]
Thread B needs locks: [3, 7]  → acquires in order [3, 7]

Thread A: Lock[3] ✓  → Lock[7] ✓  → Process → Unlock
Thread B: Wait...........................Lock[3] ✓ → Lock[7] ✓

Result: No deadlock! ✓
```

## Memory Layout

```
Object                              Size (approx)    Count    Total
─────────────────────────────────────────────────────────────────────
ReentrantLock (stripe array)        ~128 bytes       16      ~2 KB
ReentrantLock (global)              ~128 bytes        1      128 bytes
LmdbSailStore base                  ~varies          1       ~varies
─────────────────────────────────────────────────────────────────────
Overhead from lock striping:                                 ~2 KB
```

## Performance Model

```
Theoretical Speedup = min(NumThreads, NumStripes, NumUniqueSubjects)

Examples:
┌────────────┬────────────┬─────────────────┬──────────────────┐
│  Threads   │  Subjects  │  Stripe Hits    │  Expected Speedup│
├────────────┼────────────┼─────────────────┼──────────────────┤
│     4      │    1000    │  ~4 different   │      ~4x         │
│     8      │    1000    │  ~8 different   │      ~8x         │
│    16      │    1000    │ ~16 different   │     ~16x         │
│    32      │    1000    │  16 max         │     ~16x (cap)   │
│    16      │      1     │  1 stripe       │      ~1x         │
│    16      │     10     │ ~10 stripes     │     ~10x         │
└────────────┴────────────┴─────────────────┴──────────────────┘

Real-world: 0.7 * Theoretical (due to overhead, hash collisions)
```
