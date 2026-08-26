# Harden LMDB direct adjacency against commit, lifetime, and scale failures

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB direct-adjacency subsystem is a derived, in-memory index that accelerates graph-pattern reads while LMDB
remains authoritative. After this work, every dataset will either read a revision-exact direct view or fall back to
LMDB; a partially successful commit cannot leave durable LMDB data hidden behind an old direct view; native LMDB
transactions and off-heap arenas cannot be closed under active readers; rebuild recovery cannot livelock; and the
builder will respect its memory budget at the intended multi-billion-triple scale.

The result is observable through deterministic regression tests in `core/sail/lmdb`: each test first reproduces one
of the former failure windows and then passes with the hardened implementation. Performance-only edits are retained
only when the repository's JMH wrapper and JFR evidence support them.

## Progress

- [x] (2026-07-30 17:51Z) Read repository instructions, performance guidance, Maven runner guidance, and
  `.agent/PLANS.md`.
- [x] (2026-07-30 17:51Z) Run the mandatory offline root clean install; all modules passed in 39.443 seconds.
- [x] (2026-07-30 18:02Z) Add and capture failing transaction-publication and dataset-acquisition tests.
- [x] (2026-07-30 18:02Z) Implement atomic commit publication and revision-exact unpinned dataset acquisition;
  both focused regressions pass.
- [x] (2026-07-30 18:09Z) Add and capture failing worker-cancellation, lease-state, and serialized-growth tests.
- [x] (2026-07-30 18:09Z) Implement safe worker joining, iterator lease transfer, CAS lease accounting, and serialized
  arena growth; all focused regressions pass.
- [x] (2026-07-30 18:19Z) Add and capture the failing generation-construction accounting regression; add charge
  growth, shrink, refusal, reclassification, and transfer variants.
- [x] (2026-07-30 18:19Z) Implement transferable memory charges and adopt them in base build, delta application, and
  generation consolidation; focused ownership and late-failure tests pass.
- [x] (2026-07-30 18:54Z) Add and capture failing stale-gap and rebuild-coalescing regressions.
- [x] (2026-07-30 18:54Z) Implement sequence-stamped gaps and coalesced quiescent rebuild recovery; both focused
  regressions and the seven-test consolidation class pass.
- [x] (2026-07-30 19:07Z) Add and capture failing catalog-sentinel, BLOCK_FOR offset, bulk-range, and scanner-plane
  regressions.
- [x] (2026-07-30 19:07Z) Reserve slot 255, validate accessed BLOCK_FOR blocks and bulk ranges, and propagate actual
  planes; focused tests plus the 29-test codec and six-test catalog suites pass.
- [x] (2026-07-30 19:20Z) Capture failing fixed-buffer and Pass-1/Pass-3 inline-layout regressions, replace
  whole-build collectors with a 256-key streaming writer, and charge compact published metadata; inline and sequential
  and parallel base-builder suites pass.
- [x] (2026-07-30 19:24Z) Capture distinct root-scan and doubly-bound fallback regressions, record both reasons, and
  pass all 21 direct-adjacency query tests with unchanged LMDB result parity.
- [x] (2026-07-30 21:10Z) Add matched JMH/JFR coverage and retain batched node decoding, exact-pair early exit,
  direct-segment small-run writing, and retained-probe iterator reuse after repeatable improvements.
- [x] (2026-07-31 00:10Z) Run copyright/header checks, formatting, final root install, focused regressions, the full
  LMDB module suite, and final audits. The scoped tests and all 2,740 unit tests pass; the module's sole integration
  failure reproduces unchanged from a clean `HEAD` archive and is recorded below as a pre-existing blocker.

## Surprises & Discoveries

- Observation: the mandatory clean install is currently green, so every behavior-changing repair requires a new
  smallest-scope failing regression rather than relying on an existing red test.
  Evidence: the root reactor reported `BUILD SUCCESS`, including `LmdbStore`, at 2026-07-30T19:51:27+02:00.
- Observation: `TripleStore.updateFromCache()` can perform additional physical commits while replaying the record
  cache after the first commit in `endTransaction()`. Therefore a Boolean describing only the outer commit is
  insufficient; commit progress must cover every physical write transaction in the replay loop.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` commits and reopens the write
  transaction inside the resize branch of `updateFromCache()`.
- Observation: a revision recheck without the transaction-manager read lock cannot close the unpinned dataset race.
  A reader can observe the old revision both before and after opening an LMDB transaction in the interval after
  `txnManager.reset()` and before `dataRevision.incrementAndGet()`.
  Evidence: `LmdbSailStore.LmdbSailDataset` currently calls `createReadTxn()` and samples `getDataRevision()` in
  separate, unlocked operations.
- Observation: the existing direct-adjacency commit listener is the exact deterministic seam for both headline
  transaction races. It runs under the manager write lock after `txnManager.reset()` and before revision publication,
  so no new general-purpose commit-phase hook was needed.
  Evidence: the two new focused tests failed with an old revision and an unblocked dataset respectively, then passed
  after the commit state machine and atomic read registration were installed.
- Observation: the three accounting bugs shared one ownership error: a numeric reservation variable continued to be
  treated as authoritative after the live charge had changed size or kind.
  Evidence: the generation-construction regression left 4,096 DELTA bytes charged before the owner object and returns
  to zero after it; the base Pass-3 failure and repeated consolidation/rebuild regressions also pass.
- Observation: comparing only the minimum gap revision cannot distinguish a catch-up overflow from a gap already
  covered by the rebuild's pinned base. Sequence identity provides the missing generation boundary, while leaving the
  lower minimum in place to fence readers.
  Evidence: the old implementation published a one-row index after a second row committed during catch-up; the new
  marker aborts that generation and its retry publishes both rows. A separate burst test fell from 21 scans to no
  more than two.
- Observation: checking only the total BLOCK_FOR payload allocation protects the native segment but does not protect
  the logical block boundaries. A corrupted directory can redirect a valid ordinal to another valid block and return
  believable garbage without crossing the segment.
  Evidence: the first-offset regression decoded normally before the fix. Local previous/current/next checks plus an
  exact decoded-block end check now reject first, middle, and terminal corruption before packed-lane access.
- Observation: incoming scans are already ordered by `(predicate, object)`, so only one predicate's inline writer per
  incoming plane needs to be live. The prior map-of-growable-arrays duplicated a sort guarantee the source already
  provided.
  Evidence: the old path buffered 700 keys in the focused regression; the replacement never reports more than 256,
  rejects a one-block/two-block pass mismatch, and passes 10 inline-index, 16 sequential-builder, and nine
  parallel-builder tests.
- Observation: the direct provider's final shape branch covered exactly two cases—neither endpoint bound and both
  endpoints bound—so distinct metrics can be recorded without changing arbitration or adding a heuristic.
  Evidence: both focused tests failed because their closed-enum reasons were absent; the 21-test query class passes
  after adding `ROOT_SCAN` and `DOUBLY_BOUND`.
- Observation: resolving and decoding every node-enumeration edge independently dominated traversal time.
  Evidence: the 4,096-edge/100-predicate benchmark moved from 181.2/183.0 microseconds in two baseline runs to
  23.2/24.2 microseconds in two candidate runs (7.6–7.8 times faster) after one reusable run cursor and 256-edge
  buffers were introduced.
- Observation: a resolved cursor plus one exact candidate is materially cheaper than reconstructing codec views for
  each fully-bound comparison.
  Evidence: exact count moved from 255.3 to 163.1 nanoseconds and exact has/probe from a noisy 322.3 to a stable
  158.3 nanoseconds on the identical 4,096-edge mixed-context fixture.
- Observation: the temporary direct `ByteBuffer` cost is small beside encoder scratch arrays but is deterministic per
  SMALL_VARINT run.
  Evidence: normalized allocation fell from 1,286,521 to 1,180,025 bytes per 1,024-run invocation—exactly 104 bytes
  per run—and the matching JFR allocation view no longer contains `Buffer$2.newDirectByteBuffer`; profile latency
  moved from 111.3 to 97.0 microseconds.
- Observation: repeated retained direct opens were allocation-bound by reconstructed cursors and one-shot chunk
  buffers, not merely the iterator object itself.
  Evidence: 256 opens moved from 124,934 to effectively 2 normalized bytes and from 19.18 to 13.32 microseconds. The
  matching JFR profile moved from 20.73 to 13.47 microseconds and no longer lists run-cursor, edge-count, iterator, or
  chunk-buffer allocation sites.
- Observation: the full LMDB verify has one persistent, unrelated integration failure in the sketch optimizer's
  MEDICAL_RECORDS q9 join-order convergence assertion.
  Evidence: the working-tree module run passed all 2,740 unit tests and 117 of 118 integration tests, then
  `LmdbThemeQueryRegressionIT.medicalPatientsWithMedsOrObservationsExcludingCodeAvoidsUnboundLeftGuards` failed after
  265 attempts. The isolated working-tree retry failed after 259 attempts, and the same isolated method compiled and
  run from a clean `git archive HEAD` failed after 258 attempts with the identical expected-order assertion. No
  direct-adjacency, transaction, codec, accounting, or lifetime test failed.

## Decision Log

- Decision: execute this as Routine D while still applying Routine A's red-before-production rule to every
  behavior-changing milestone.
  Rationale: the work is a multi-subsystem concurrency and data-layout refactor, but the repository explicitly
  requires a focused failing test before production edits that change behavior.
  Date/Author: 2026-07-30 / Codex.
- Decision: keep all new hooks and coordination types package-private and add no dependencies.
  Rationale: the failures are internal implementation invariants and do not justify expanding RDF4J's public API.
  Date/Author: 2026-07-30 / Codex.
- Decision: represent emergency gap state as `(fromRevision, sequence)` rather than only a minimum revision.
  Rationale: the minimum must continue to ratchet downward for safety, while the sequence must advance for every
  later event so an old gap cannot hide a new catch-up overflow.
  Date/Author: 2026-07-30 / Codex.
- Decision: replace inline key materialization with a single-pass streaming writer using the scanner's existing
  predicate/object ordering.
  Rationale: an object-per-key collector violates the memory contract and integer-bounds the build; sorted input
  already provides the grouping needed for radix and block emission.
  Date/Author: 2026-07-30 / Codex.
- Decision: stay on branch `optimize-lmdb`; use `GH-0000` for commits and do not push.
  Rationale: no issue number was supplied and branch changes or publishing were not requested.
  Date/Author: 2026-07-30 / Codex.

## Outcomes & Retrospective

Implementation is complete. Commit publication, dataset acquisition, native-reader lifetime, lease accounting,
arena growth serialization, memory-charge ownership, and rebuild generation tracking/coalescing are hardened with
focused passing regressions. Catalog, codec, bulk-read, scanner validation, streaming inline construction, fallback
metrics, and measured read-path improvements are complete. Header checks, formatting, `git diff --check`, the final
root install, the 145-test focused adjacency selection, and all 2,740 module unit tests pass.

The exact requested full-module command completed rather than being shortened. It passed 117 of 118 integration
tests; the only failure is the pre-existing MEDICAL_RECORDS q9 sketch-planner ordering assertion described above.
Running that method from an untouched `HEAD` archive proves the scoped changes did not introduce it. This plan
therefore treats the module verify as a fully triaged baseline blocker, not as an unexamined green claim.

## Context and Orientation

The affected Maven module is `core/sail/lmdb`. `TripleStore` owns the authoritative LMDB write transaction and the
monotonically increasing `dataRevision`. `TxnManager` opens and tracks LMDB read transactions and provides a shared
read/write lock: commit publication holds the write lock, while a read lock can atomically pair an LMDB snapshot with
its data revision. `LmdbSailStore.LmdbSailDataset` owns one LMDB read transaction and one optional
`LmdbAdjacencyReadView`.

`LmdbDirectAdjacencyStore` captures commit deltas, publishes immutable base/delta generations, and falls back to LMDB
when it cannot prove revision-exact coverage. An emergency gap is a marker saying that direct adjacency may be missing
one or more durable commits beginning at a revision. `LmdbPagedCsfBaseBuilder` creates a base generation by scanning
four statement-order planes with pinned read transactions. `LmdbAdjacencyMemoryAccount` enforces the configured
off-heap and heap-derived-state budget. `LmdbAdjacencyRunCodec`, `LmdbAdjacencyArena`, and
`ImmutablePagedQuadCsfIndex.KeyDomain` store compressed adjacency rows in native arenas.

Tests live beside the module in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb`. Reuse the existing
commit, snapshot, parallel-builder, memory-account, consolidation, arena, codec, inline-index, query, and benchmark
test classes where their fixtures already expose the required subsystem. Create a focused new test class only when
doing so makes a failure seam materially clearer.

## Plan of Work

### Milestone 1: make commit publication and dataset acquisition atomic

First add deterministic tests using package-private hooks. One test must pause a commit after tracked readers have
been reset but before the revision is published, open an unpinned tracked dataset in that interval, and prove that the
dataset cannot combine the post-commit B+tree with the old direct view. Commit-failure tests must inject failures
after the first physical commit, during record-cache replay, after reader reset, and from the direct-adjacency
listener. They must assert that a physical commit advances the logical revision exactly once and publishes an
emergency gap before new readers can use that revision.

In `TxnManager`, add a package-private `ReadTxnRegistration` record containing `Txn txn`, `long dataRevision`, and
`long initialVersion`. Add `createReadTxnTrackedAtRevision(LongSupplier)`; while holding the manager read lock, it
must open and register a resettable transaction, sample the revision, read the transaction version, and return the
record. On any failure, close an opened transaction before unlocking. Make unpinned tracked
`LmdbSailDataset` instances use this registration. Pinned and deliberately untracked readers retain their current
paths.

In `TripleStore`, introduce a private commit-progress carrier with the intended next revision, a
`physicalCommitSucceeded` flag, and the current live write handle. Centralize physical commit in a helper that clears
`writeTxn` before calling `mdb_txn_commit`, since LMDB consumes the handle regardless of return code, and marks
success only after `E(...)` returns. Centralize write-transaction begin in a helper that checks `mdb_txn_begin`
before assigning `writeTxn`. Thread the same progress object through `updateFromCache()` so internal resize commits
participate.

Catch `IOException`, `RuntimeException`, and `Error` across the complete commit critical section. If no physical
commit succeeded, abort only a still-live nonzero handle and leave the revision unchanged. If any physical commit
succeeded, synchronously call a package-private non-allocating listener method that marks a gap for the intended
revision, then set `dataRevision` to that intended revision before rethrowing. Close sealed deltas and record-cache
state exactly once, preserve the original exception, and never finish an LMDB handle twice.

### Milestone 2: make reader and builder lifetimes safe

Add a parallel-builder regression whose coordinator thread is interrupted while a worker remains inside a scanner
callback. Latches must prove `AdjacencySourceFamily.cancel()` is not invoked until the worker exits. Add iterator
tests that inject an initialization failure and then prove the view's active lease count returns to zero and store
shutdown completes. Add lease-state tests proving retain-after-close and over-release throw without changing the
closed count. Add a concurrent arena-allocation test that checks every returned reference is unique and still
contains the bytes written by its allocating thread.

Change `runPlaneTasks` to remember interruption without restoring the flag until cleanup finishes. Cancel futures,
call `shutdownNow`, and continue joining until every worker terminates. A one-minute interval may log a warning but
must not close the pinned source family. Only after termination may the method cancel/close the family, restore the
interrupt flag, and throw the original interruption as `IOException`.

Move adjacency-view lease acquisition into reusable iterator initialization/factory methods. If row resolution,
constructor work, or `init` throws, release the newly acquired lease in that method. Callers must never manually
retain a lease before creating an iterator. Implement lease retain and release as CAS loops so invalid transitions
leave the counter unchanged. Synchronize growth-mode `LmdbAdjacencyArena.allocateRef` using the arena's existing
allocation monitor; partition allocators remain unchanged.

### Milestone 3: make accounting and rebuild recovery exact

Add memory-account tests for reservation growth, shrinkage, reclassification, transfer, and closing before/after
transfer. Add failure injection after delta/base arena creation and generation construction; assert aggregate and
per-kind charges return exactly to baseline and the injected exception remains the reported exception.

Add an internal AutoCloseable memory charge. It owns an account, current `MemoryKind`, current byte count, and a
closed/transferred state. `adjustTo(long)` reserves or releases only the difference; `reclassify(MemoryKind)` moves
the current charge; `transfer()` returns an owner charge and disarms the temporary reservation; `close()` releases
the current kind and bytes exactly once. Use this abstraction in base construction, delta application, and generation
merge instead of local `reservedBytes` variables.

Replace `emergencyGapFromRevision` with an atomic immutable `GapMarker(long fromRevision, long sequence)`.
`markGap` CAS-updates the minimum revision and increments the sequence on every call. A build captures the marker
after pinning its base snapshot. Catch-up may clear only that captured marker, with CAS, after reaching the current
data revision while holding the transaction-manager read lock. If the sequence advances after capture and continuity
cannot be proven, abort publication.

Guard quiescent rebuild submission with an atomic queued/running flag. Clear the flag in task cleanup; if the store
remains degraded, enqueue exactly one retry. Regression tests must reproduce an old gap plus a new catch-up overflow
using the existing build-scan hook, assert bounded completion without a sleep loop, and assert a burst of queue holes
produces one queued/running rebuild plus at most one necessary retry.

### Milestone 4: harden arena, codec, scanner, and bulk-read validation

Reserve catalog slot `0xFF` for `CHUNK_SLOT_SELF` by limiting published arena slots to `0..254`. Test that slot 254
is valid and the next append fails before publication.

Centralize validation for materialized-run scalar and bulk access. Require a nonnegative run length and offset, an
offset no greater than row size, and a target range wholly inside the destination array. Extend the run cursor with
the BLOCK_FOR payload start and length. Before decoding an accessed block, read its current and next directory
offsets and require both to be in range and monotonic; verify the decoder consumes no bytes beyond the next offset.
Tests must corrupt first, middle, and final block offsets and observe deterministic `IllegalStateException` or
`IllegalArgumentException`, never decoded garbage.

Propagate the actual explicit/inferred plane into scanner construction and cover all four plane constants. Preserve
the existing unsigned ordering and on-disk/in-memory handle representation apart from the reserved sentinel slot.

### Milestone 5: stream the inline incoming index

Add streaming-builder tests across empty input, one key, 255/256/257-key block boundaries, high-16-bit radix prefix
changes, duplicate incoming groups, and checked long-count overflow. Add a pass-1/pass-3 mismatch test and memory
release assertions. Use long-valued counters and small deterministic fixtures rather than attempting a billion-row
test.

Implement `ImmutablePagedQuadCsfIndex.KeyDomain.StreamingBuilder`. It accepts sorted `(inlineObjectKey, runRef)` pairs for one
predicate, rejects out-of-order or duplicate keys, buffers at most one fixed-size block, emits a block on the
256-key boundary or radix-prefix change, and fills radix entries as prefixes advance. Pass 1 must size radix,
directory, and block payload allocations with long arithmetic. Pass 3 must allocate from the planned plane partition,
emit in the same order, and verify key, block, and byte totals on `seal()`.

Remove the base builder's `Map<Long, LongList[]>` collector. Stream each completed incoming group directly to the
builder and charge the bounded per-predicate Java metadata to `JAVA_METADATA`. No path may cast the total key count or
byte count to `int`; only the bounded 256-key scratch block uses integer indexes.

### Milestone 6: finish metrics and measured hot-path improvements

Add fallback reasons for root scans and doubly-bound patterns and assert their counters in query tests. Preserve all
existing fallback semantics and result parity.

Extend `DirectAdjacencyBenchmark` before changing hot paths. Add selectors for node enumeration, fully bound
count/probe, small-run encoding, and retained direct probes. Capture baseline results. Then batch node-iterator
decoding into reusable 256-edge primitive arrays using one reusable run cursor; use a lower-bound plus one candidate
for fully bound count; write small-run varints directly to the target memory segment without a per-run
`ByteBuffer`; and reuse the resettable direct iterator inside `RetainedNativeProbe`, releasing and reacquiring its
lease on each reset.

Run the same JMH selectors and JFR allocation recording after each change. Retain a performance edit only when the
target measurement moves repeatably in the intended direction and no neighboring adjacency benchmark shows a
meaningful regression. Record the exact result and confidence in `Surprises & Discoveries`; do not attribute a gain
to the JIT without compilation/profile evidence.

## Concrete Steps

Run all commands from the repository root. The initial clean install has completed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

For each new regression, run the smallest selector with retained logs:

    python3 .codex/skills/mvnf/scripts/mvnf.py TestClass#testMethod --module core/sail/lmdb --retain-logs

Immediately after the first expected red run, preserve compact Surefire evidence in top-level
`initial-evidence.txt` with `scripts/agent-evidence.py`, including the command, report path, and assertion/error
summary. Do not use `-am` or `-q` on any test command.

Before final verification, run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Use the supported benchmark wrapper for every performance selector:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.DirectAdjacencyBenchmark --method <method>

Add `--enable-jfr` for allocation evidence when the benchmark supports it. Keep baseline and candidate JVM, forks,
warmup, measurements, and data parameters identical.

## Validation and Acceptance

The transaction tests pass only when no durable LMDB mutation remains associated with an old published revision:
after a partial commit, new readers observe the incremented revision and direct adjacency reports a revision gap.
The dataset-acquisition race passes only when the dataset either has an exact matching pair or declines direct
adjacency.

The builder and iterator tests pass only when native transactions and arenas outlive every worker and iterator that
can access them. Accounting tests pass only when every successful and failed path restores exact totals by memory
kind. Rebuild tests pass only when catch-up publishes or schedules one coalesced retry without livelock.

Codec corruption tests must fail before out-of-range native reads. Inline-index tests must prove bounded buffering,
long-safe sizing, deterministic emission, and exact cleanup. Query tests must retain LMDB/direct result parity and
record complete fallback metrics. The full `core/sail/lmdb` verify must report zero failures.

Performance changes require exact benchmark commands, before/after results, allocation direction, profile/JIT
evidence or an explicit statement that it was not inspected, and a confidence level tied to the active JDK 25.

## Idempotence and Recovery

All tests and builds are safe to rerun. Preserve all untracked artifacts. Do not use destructive Git commands,
delete unexpected files, or overwrite unrelated work. Before each edit group, inspect `git status --short
--untracked-files=no`; if unrelated changes overlap a target file, stop and report the conflict.

If an offline Maven run fails only because an artifact is missing, rerun that exact command once without `-o`, then
return to offline mode. If a clean install fails for another reason, rerun it without `-T 1C`. If production code is
accidentally changed before its failing test is captured, revert only that known edit, update Progress, and restart
the milestone from the red test.

## Artifacts and Notes

Keep `maven-build.log`, retained `logs/mvnf` verification logs, top-level `initial-evidence.txt`, Surefire/Failsafe
reports, and benchmark/JFR output. Do not stage or remove artifacts unless explicitly requested.

The initial baseline transcript is:

    Reactor Summary for Eclipse RDF4J 6.1.0-SNAPSHOT
    RDF4J: LmdbStore ................................... SUCCESS [  7.615 s]
    BUILD SUCCESS
    Total time: 39.443 s (Wall Clock)

## Interfaces and Dependencies

No public API or dependency changes are permitted. The final internal interfaces are package-private
`TxnManager.ReadTxnRegistration`, package-private commit-phase test hooks, a private `TripleStore` commit-progress
carrier, an internal transferable `LmdbAdjacencyMemoryAccount` charge, immutable
`LmdbDirectAdjacencyStore.GapMarker`, and `ImmutablePagedQuadCsfIndex.KeyDomain.StreamingBuilder`.

Use existing JDK concurrency primitives, Foreign Function and Memory API segments already used by the module, LWJGL
LMDB bindings, AssertJ/JUnit fixtures, and existing RDF4J scanner/catalog abstractions. Preserve the current public
query interfaces, LMDB databases, and packed handle layout.

Revision note (2026-07-30): created the initial implementation-grade plan after the mandatory clean install and
source inspection. It resolves transaction, lock, lifetime, accounting, rebuild, codec, scaling, and performance
decisions so later milestones can be executed without rediscovering the original review.

Revision note (2026-07-30 18:02Z): completed Milestone 1's two headline regressions and implementation. The existing
commit listener proved sufficient as the deterministic publication-window hook, so the plan no longer requires a
separate production commit-phase hook for this window.

Revision note (2026-07-30 18:09Z): completed Milestone 2. Interruption is restored only after the executor has fully
terminated, iterator constructors/initializers own their lease acquisition and rollback, and the growth-mode arena
uses the same monitor as partition reservation.

Revision note (2026-07-30 19:24Z): completed Milestone 6's behavioral metrics portion. Root scans and doubly-bound
opens now contribute separate closed-enum fallback reasons, and the full query integration class preserves result
parity.

Revision note (2026-07-30 21:10Z): completed Milestone 6. Matched JMH JSON and JFR recordings under
`profiles/lmdb-hardening` justify retaining every candidate; the 22-test query class additionally covers exact-context
hits/misses, retained-probe ownership transitions, and node enumeration across 256-edge batch boundaries.

Revision note (2026-07-31 00:10Z): completed final verification and audit. Copyright/header checks, formatting,
`git diff --check`, the root clean install, focused regressions, and all module unit tests pass. The sole full-module
integration failure was reproduced from a clean `HEAD` archive and is unrelated baseline behavior.
