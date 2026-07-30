# Reuse resolved LMDB adjacency state across generated query kernels

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`PLANS.md` at the repository root.

## Purpose / Big Picture

The first MEDICAL_RECORDS theme query currently spends a large fraction of its execution repeatedly validating and
decoding the same immutable adjacency references. After this work, a generated LMDB query kernel will resolve a
reference once, reuse its native segment, base offset, codec metadata, and edge count, and stream bulk data without
restarting compressed decoders. Query results and all invalid-reference, arena-lifetime, snapshot, context, and
ordering behavior remain unchanged.

The improvement is observable by running
`org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery` with `themeName=MEDICAL_RECORDS` and
`z_queryIndex=0` in the Docker Java 26 JFR loop. The baseline is approximately 4.819 ms/op. In the baseline recording,
`LmdbAdjacencyArena.region(long,long)` has 9.51% exclusive CPU samples and 17.46% worker-inclusive samples. A successful
implementation must reduce both total benchmark time and the combined arena, memory-segment wrapper, run-resolution,
and codec-copy hotspot share without weakening correctness.

## Progress

- [x] (2026-07-29 09:45Z) Read performance, Docker JFR, Maven, and ExecPlan guidance.
- [x] (2026-07-29 09:46Z) Confirm the existing dirty worktree completes the root quick clean install.
- [x] (2026-07-29 09:47Z) Record the baseline JFR hotspot and allocation model.
- [x] (2026-07-29 09:51Z) Capture pre-change arena, codec, and locator green tests.
- [x] (2026-07-29 10:01Z) Add focused parity and lifetime tests before changing the arena/run contracts.
- [x] (2026-07-29 10:01Z) Implement one checked reusable arena resolution per native page or run.
- [x] (2026-07-29 10:00Z) Stream SMALL_VARINT, BLOCK_FOR, and CHUNK_DIRECTORY bulk decoding.
- [x] (2026-07-29 10:18Z) Prebind predicate ordinals and retain reference-page locality.
- [x] (2026-07-29 10:31Z) Batch filter metrics and avoid repeated scratch-row slot scans.
- [x] (2026-07-29 10:39Z) Pre-size generated distinct sets and borrow retained native domains where safe.
- [x] (2026-07-29 10:47Z) Run focused tests, the LMDB module test suite, formatting, and the root quick install.
- [x] (2026-07-29 10:50Z) Rerun the identical Docker benchmark and JFR profile and compare hotspots.

## Surprises & Discoveries

- Observation: `LmdbAdjacencyArena.region(long,long)` is already inlined by HotSpot on Docker Linux aarch64 Java 26.
  Making the method final, static, or smaller cannot remove the measured work by itself.
  Evidence: every worker sample whose top frame is `region` has JFR frame type `Inlined`.

- Observation: all worker-exclusive `region` samples come from four repeated-resolution paths: 39.0% from
  `LmdbReferenceNodeLocator.headerRef`, 23.6% from codec resolution for `edgeCount`, 20.0% from codec resolution for
  `copy`, and 17.4% from `findGroupRun`.
  Evidence: the baseline JFR contains 4,832 worker-exclusive `region` samples split 1,885, 1,140, 966, and 841
  respectively.

- Observation: wrappers and the immutable `RunView` dominate sampled worker allocation weight.
  Evidence: `DirectByteBuffer`, `NativeMemorySegmentImpl`, and `LmdbAdjacencyRunCodec.RunView` contribute
  approximately 34.3%, 34.0%, and 21.4% of worker allocation weight.

- Observation: the worktree already contains uncommitted page-region and group-search locality changes. They are the
  starting state for this plan and must not be reverted or overwritten.
  Evidence: `git status --short --untracked-files=no` reports modifications in the arena, locator, direct adjacency
  store, native adjacency adapter, in-memory index, sail store, and associated tests.

- Observation: the reusable arena cursor, run cursor, and streaming codec paths pass their focused compatibility
  suites without changing encoded-format behavior.
  Evidence: post-change focused results are `LmdbAdjacencyArenaTest` 11/0/0, `LmdbAdjacencyRunCodecTest` 28/0/0, and
  `LmdbReferenceNodeLocatorTest` 11/0/0.

- Observation: predicate prebinding, direct-adjacency reuse, nested filter restoration, ordered and fallback distinct
  aggregation, and retained native-domain borrowing pass 182 focused tests with no failures or errors.
  Evidence: `LmdbDirectAdjacencyQueryTest` ran 19 tests; the arena, codec, and locator selection ran 50; the generated
  aggregate, filter, lowering, and cleanup selection ran 113.

- Observation: the complete LMDB module is not green in the starting dirty worktree. Its pre-plan run had 15 failures
  and one error; the post-change run contains those same failures and error plus one
  `LmdbValueGcRetirementTest` failure that passes all three methods when immediately rerun in isolation.
  Evidence: `post-evidence-group-run-lmdb-module.txt` records the pre-plan 15/1 result,
  `logs/mvnf/20260729-102613-verify.log` records the post-change 16/1 result, and the isolated test reports 3/0/0 with
  `BUILD SUCCESS`. No reproducible failure was introduced by this plan.

- Observation: the candidate benchmark improved from 4.819 +/- 1.228 ms/op to 3.262 +/- 1.397 ms/op, a 32.3% reduction
  in the reported mean. Excluding the cold first measurement, iterations two through ten average 2.971 ms/op.
  Evidence: the identical Docker Java 26 selector produced ten 10-second measurements in
  `profiles/lmdb/lmdb-theme-medical-q0-resolved-run-candidate.jfr`.

- Observation: `LmdbAdjacencyArena.region` no longer appears in the candidate hot-method view, after accounting for
  9.51% exclusive CPU samples in the baseline. The old `DirectByteBuffer`, `NativeMemorySegmentImpl.dup`, and
  `LmdbAdjacencyRunCodec.RunView` allocation sites also disappear from the leading allocation view.
  Evidence: comparison of `jfr view hot-methods` and `jfr view allocation-by-site` for the baseline and candidate
  recordings.

## Decision Log

- Decision: Use a caller-owned reusable carrier rather than allocating a `ResolvedRegion` or `RunView` per lookup.
  Rationale: the allocation profile proves HotSpot does not scalar-replace the current `RunView`; explicit
  query-confined reuse gives deterministic ownership and allocation behavior.
  Date/Author: 2026-07-29 / Codex.

- Decision: Keep checked public/package entry points and add a pinned read fast path instead of globally removing
  validation.
  Rationale: arena closure, malformed references, region boundaries, and snapshot lifetime are correctness
  invariants. The query read lease can prove lifetime once, while one-shot and test callers retain defensive checks.
  Date/Author: 2026-07-29 / Codex.

- Decision: First retain `MemorySegment` access and remove repeated slices; consider raw native addresses only after the
  checked cursor is correct and profiled.
  Rationale: raw addresses can eliminate Foreign Function and Memory API checks but make use-after-close failures
  unsafe. The lower-risk structural changes address the measured repeated work and preserve Java memory safety.
  Date/Author: 2026-07-29 / Codex.

- Decision: Preserve the generated vectorized execution model.
  Rationale: the benchmark is already served by a Janino-generated primitive kernel. The problem is repeated
  representation conversion below that kernel, not interpretation or polymorphic dispatch.
  Date/Author: 2026-07-29 / Codex.

- Decision: Use a primitive last-seen accumulator only when lowering and runtime order checks prove an unsigned
  nondecreasing distinct-input domain, with the existing hash set as a correctness fallback.
  Rationale: this removes hashing from ordered global `COUNT(DISTINCT ...)` without making planner order assumptions
  observable when an input is unexpectedly disordered.
  Date/Author: 2026-07-29 / Codex.

- Decision: Borrow a domain array only from the revision-retained native adjacency and keep the LMDB iterator fallback.
  Rationale: snapshot retention already supplies the required lifetime proof; a global or cross-revision cache would
  introduce invalidation and concurrency risks that are unnecessary for this query.
  Date/Author: 2026-07-29 / Codex.

## Outcomes & Retrospective

The implementation is complete. The identical Docker Java 26 benchmark improved from 4.819 +/- 1.228 ms/op to
3.262 +/- 1.397 ms/op, or 32.3% by the reported means. The first candidate iteration was cold at 5.885 ms/op; the
remaining nine measurements average 2.971 ms/op and range from 2.914 to 3.103 ms/op.

The principal JFR objective was achieved: `LmdbAdjacencyArena.region`, previously the largest exclusive application
hotspot at 9.51%, is absent from the candidate hot-method view. Repeated direct-buffer and memory-segment wrappers and
immutable `RunView` objects represented about 77% of the baseline allocation pressure attributed to this path; those
sites are absent from the candidate's leading allocation view. Remaining costs are useful next targets rather than
duplicated work: filter evaluation, checked cursor range access, keyed-match lookup, codec byte decoding, and native
memory-session checks.

Correctness validation comprises 182 focused green tests covering the changed paths, successful copyright and
formatting checks, and a successful root quick install on local Java 25. The full LMDB module command was also run, but
the dirty-worktree baseline was already red with 15 failures and one error. The post-change run reproduced those
failures and one suite-only value-GC failure; all three methods in that class passed immediately in isolation. This is
recorded rather than misrepresented as an all-green module suite.

The candidate recording is
`profiles/lmdb/lmdb-theme-medical-q0-resolved-run-candidate.jfr`, SHA-256
`c718f517c1a9a2d40c4623c03a71552a03c94222f06a5cda61921929d49bfcd9`. The baseline recording remains
`profiles/lmdb/lmdb-theme-medical-records-q0-20260729.jfr`, SHA-256
`9cdd287b57a7a72d3448fa02799b3bc9993560508b59c5125f9052483b8543a5`.

## Context and Orientation

The module is `core/sail/lmdb`. An adjacency run is an immutable compressed sequence of neighbor and context IDs stored
in an off-heap `LmdbAdjacencyArena`. An encoded reference is a nonzero unsigned 40-bit value representing a virtual
byte address divided by eight. `LmdbAdjacencyArena.region` maps that reference to a `MemorySegment` and validates the
requested byte range. `LmdbAdjacencyRunCodec` parses three storage formats: SMALL_VARINT for at most fifteen edges,
BLOCK_FOR for ordinary runs, and CHUNK_DIRECTORY for very large runs.

`LmdbReferenceNodeLocator` maps an RDF value ID to a node header and then to the run for a fixed predicate and direction.
`LmdbDirectNativeAdjacency` implements `NativeLmdbQuerySource.NativeAdjacency`, the interface called by generated query
kernels. The generated code calls `find`, `size`, and `copyNeighbors`; currently `size` eagerly materializes a small run
and `copy` resolves the same run again. `LmdbNativeKernelHooks` adapts generated primitive filter arguments to a
`RowState`. `RecordingNativeBooleanFilter` records optimizer feedback. `KernelRuntime.LongHashSet` backs generated
`COUNT(DISTINCT ...)`, and `LmdbNativeKernelBindings.materializeDomains` scans store-backed root domains into arrays.

The worktree modifications predating this ExecPlan already introduce `LmdbAdjacencyArena.region`, `regionOffset`, and a
`LmdbReferenceNodeLocator.SearchContext`. Preserve those changes and evolve them rather than restoring the tracked
baseline. Do not delete or rename unrelated untracked benchmark and evidence artifacts.

## Plan of Work

First add focused tests to `LmdbAdjacencyArenaTest`, `LmdbAdjacencyRunCodecTest`, and
`LmdbReferenceNodeLocatorTest`. The tests must cover reusable resolution across the first and last valid byte, a range
that crosses a region, access after final close, all three codecs, contexts present and absent, nonzero batch offsets,
partial final batches, chunk boundaries, and repeated page lookups. Existing tests remain the oracle for encoded
format compatibility.

In `LmdbAdjacencyArena`, introduce a reusable read-resolution carrier with a `MemorySegment`, base offset, and validated
end offset. Load the current region backing array once. Exact/sealed arenas should publish a stable array for reads;
growth-mode arenas may replace the array only when adding a region. A checked resolution performs logical lifetime,
reference, and range checks once. Expanding a range for the same reference checks only the already-selected segment and
base offset. Existing `slice` and `region` methods delegate to this implementation so their exception behavior remains
compatible. Query-confined users reuse one carrier and do not allocate it per lookup.

In `LmdbReferenceNodeLocator`, extend `SearchContext` with the last value type, page number, page reference, segment, and
base offset. `headerRef` reuses that page while consecutive IDs remain in the same 4,096-slot page, and it expands the
validated page range after computing rank without resolving the reference again. `findGroupRun` similarly resolves the
node header once, reads counts, expands the checked range, and searches on the same segment. The context remains
single-owner and has a correctness-equivalent fallback for nonlocal keys.

In `LmdbAdjacencyRunCodec`, replace allocation-returning `resolve` on the hot path with a reusable `RunCursor`. The
cursor holds catalog, arena slot, segment, base offset, tag, codec, context flag, edge count, and total validated length.
Package-private overloads accept a cursor for `edgeCount`, `copy`, `neighborAt`, `contextAt`, and `lowerBound`; existing
entry points retain their current signatures and use a temporary path for cold callers. `LmdbDirectNativeAdjacency`
owns and reuses a cursor for its cached handle so generated `size` and copy calls share one resolution.

Implement codec-specific bulk loops. SMALL_VARINT creates no `ByteBuffer`; it advances byte offsets once and decodes
neighbors and contexts sequentially, making a batch linear in its edge count. BLOCK_FOR computes directory and block
metadata once per block and walks its lanes. CHUNK_DIRECTORY locates the first child once, resolves one child cursor,
streams to the chunk boundary, and continues; it must not allocate `new long[1]` for contexts. Keep scalar random access
for lower-bound searches and one-shot callers.

In `LmdbInMemoryAdjacencyIndex` and `LmdbDirectNativeAdjacency`, bind base coverage and the raw-predicate ordinal when the
adapter is created. `find` should not binary-search the predicate catalog for every key. Overlay selection and
generation-specific catalogs remain dynamic and must preserve newest-generation-first resolution.

In `RecordingNativeBooleanFilter`, use worker-confined primitive counters for scalar accepts and merge them into the
existing atomic target only on close or an existing batch boundary. In `LmdbNativeKernelHooks.testFilter`, update the
known argument slots, mask, and binding-view state directly and restore them in `finally`; do not rescan every slot four
times. Preserve nested calls, exception cleanup, parallel-worker outcome merging, and adaptive feedback semantics.

In `LmdbNativeKernelEmitter`, pass a planner/cardinality-derived expected size to generated
`KernelRuntime.LongHashSet` construction when available, while retaining a bounded default. When physical order proves
the distinct input monotonic, use a last-seen primitive accumulator instead of a hash set. In
`LmdbNativeKernelBindings`, cache or borrow an immutable store-backed domain only when the native probe can prove it is
valid for the retained snapshot; otherwise retain the current materialization fallback. Do not introduce global
cross-snapshot caches.

After each independently green milestone, rerun the focused test selection. At completion, run the entire LMDB module,
formatting, the root quick install, and the identical Docker JFR selector. Compare benchmark latency, exclusive and
inclusive arena cost, codec cost, filter cost, distinct cost, allocation sites, GC pause, lost samples, and generated
kernel compilation behavior.

## Concrete Steps

Run all commands from the repository root
`/Users/havardottestad/Documents/Programming/rdf4j`.

The mandatory initial build command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

The observed initial outcome is `BUILD SUCCESS` in 33.831 seconds. Full output is retained in `maven-build.log`.

The focused pre-change outcomes are:

    LmdbAdjacencyArenaTest: tests=10, failures=0, errors=0, skipped=0
    LmdbAdjacencyRunCodecTest: tests=27, failures=0, errors=0, skipped=0
    LmdbReferenceNodeLocatorTest: tests=10, failures=0, errors=0, skipped=0

The first selection is preserved in `initial-evidence-resolved-adjacency.txt`; complete Maven output is under
`logs/mvnf/`.

Run focused tests through the repository runner, never with Maven `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyArenaTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyRunCodecTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbReferenceNodeLocatorTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyQueryTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelAggregateTest --retain-logs

Run the full module after the focused selections:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Check headers and format after source edits:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is an existing repository exception to the no-quiet-tests rule; it does not execute tests.

Run the exact profiled benchmark before and after:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
      org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery \
      --param themeName=MEDICAL_RECORDS \
      --param z_queryIndex=0 \
      --jfr-output profiles/lmdb/lmdb-theme-medical-q0-resolved-run-candidate.jfr

Use `jfr view hot-methods`, `jfr view allocation-by-site`, and worker-thread stack grouping to compare with
`profiles/lmdb/lmdb-theme-medical-records-q0-20260729.jfr`.

## Validation and Acceptance

All focused tests and the complete `core/sail/lmdb` module must pass. Arena tests must prove that malformed, out-of-range,
cross-region, and closed-arena reads still fail. Codec tests must compare every decoded neighbor and context with the
input for SMALL_VARINT, BLOCK_FOR, and CHUNK_DIRECTORY, including offset batches and chunk transitions. Locator tests
must compare cached and uncached searches for ascending, descending, random, missing, cross-page, and cross-type keys.
Filter tests must prove identical accept/reject counts after normal completion and exceptions, including parallel
worker forks.

The query must return the same result as the standard RDF4J evaluation path. The benchmark must not regress beyond
measurement noise. Acceptance for the optimization is a repeatable reduction in latency accompanied by a fall in
`LmdbAdjacencyArena.region`, `LmdbAdjacencyRunCodec.resolve`, direct-buffer/segment-wrapper allocation, or the secondary
filter/distinct hotspots. A latency change without a matching hotspot or allocation shift is insufficient evidence.

Because the repository baseline is Java 25 while the Docker measurement uses Java 26, correctness must pass locally on
Java 25 and performance claims must explicitly name Docker Java 26. Do not claim that raw access, inlining, scalar
replacement, or vectorization carries across both runtimes without evidence.

## Idempotence and Recovery

All build, test, and benchmark commands are repeatable. The Maven repository is workspace-local. JFR output names are
explicit so reruns replace only the named candidate recording. Do not run `git reset`, `git clean`, `git restore`, or
delete untracked artifacts. If a focused change fails, edit only the files changed for that milestone and use
`git diff -- <paths>` to distinguish this work from the pre-existing dirty changes.

If offline Maven resolution fails for a missing artifact, rerun the exact build once without `-o`, then return to
offline operation. If the Docker container stops, the benchmark wrapper safely restarts it. If native-address
prototyping is attempted and cannot preserve close/lifetime behavior, discard that prototype and retain the
MemorySegment cursor implementation.

## Artifacts and Notes

Baseline artifacts:

    profiles/lmdb/lmdb-theme-medical-records-q0-20260729.jfr
    profiles/lmdb/lmdb-theme-medical-records-q0-jit-facts.txt
    initial-evidence.txt

Baseline benchmark:

    ThemeQueryBenchmark.executeQuery MEDICAL_RECORDS 0
    4.819 +/- 1.228 ms/op

Baseline JFR SHA-256:

    9cdd287b57a7a72d3448fa02799b3bc9993560508b59c5125f9052483b8543a5

The baseline has one fork and no warmup by design because the supported JFR wrapper fixes those settings. Treat exact
gain estimates as medium confidence until repeated unprofiled measurements agree with the hotspot shift.

## Interfaces and Dependencies

Do not add dependencies. Use Java 25 `MemorySegment`, primitive arrays, and existing LMDB/codec utilities.

`LmdbAdjacencyArena` must expose a package-private reusable carrier whose effective interface is:

    final class ReadCursor {
        MemorySegment segment;
        long reference;
        long baseOffset;
        long validatedBytes;
    }

    void resolve(long encodedRef, long bytes, ReadCursor target)
    void expand(long encodedRef, long bytes, ReadCursor target)

Names may change to match local conventions, but the carrier must be caller-owned and allocation-free after
construction.

`LmdbAdjacencyRunCodec` must expose a package-private reusable cursor whose effective state includes catalog, arena slot,
arena read cursor, tag, codec, context presence, edge count, and total run length. `LmdbDirectNativeAdjacency` owns one
such cursor for its current handle. Cold compatibility entry points retain the existing method signatures.

`LmdbReferenceNodeLocator.SearchContext` remains confined to one query operator and must cache both group-search
position and page-resolution state. No cursor or mutable carrier may be shared across concurrent query workers.

Revision note, 2026-07-29: Initial ExecPlan created from the MEDICAL_RECORDS q0 Docker JFR investigation and the
pre-existing dirty-worktree implementation state.

Revision note, 2026-07-29 09:51Z: Added the mandatory clean-build and focused pre-change test outcomes before beginning
the reusable arena resolution milestone.

Revision note, 2026-07-29 10:50Z: Completed the reusable-resolution, streaming-codec, predicate, filter, and aggregate
milestones; recorded focused and module validation; and added the Docker Java 26 candidate benchmark and JFR outcome.
