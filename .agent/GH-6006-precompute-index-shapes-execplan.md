# Precompute LMDB binding shapes and stabilize timeout verification

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current while the work proceeds. This document follows `.agent/PLANS.md`.

## Purpose / Big Picture

LMDB cardinality estimation currently reinterprets an index's four-field order every time a statement pattern is estimated. After this change, each index will prepare the answers for all sixteen possible subject/predicate/object/context bound-state combinations during store initialization. The invocation path will perform primitive lookups instead of walking `indexMap` or `fieldSeq`, and residual matchers will consume a four-bit mask instead of allocating a `boolean[4]`. Query answers, index bytes, estimator sampling, and the retained page caches must remain unchanged.

The server-boot timeout stress test will also stop deciding whether a timeout occurred from JDK-specific client exception text. It will submit deterministic one-second queries and assert the server's `QueryInterruptedException` log event, while retaining its LMDB reader-handle and eventual-health assertions.

## Progress

- [x] (2026-09-02 21:11Z) Inspect current matcher, index-selection, timeout-test, and response-streaming paths.
- [x] (2026-09-02 21:25Z) Capture pre-change focused test and benchmark evidence.
- [x] (2026-09-02 21:48Z) Add initialization-time index shape tables and direct matcher masks.
- [x] (2026-09-02 21:55Z) Replace timeout-text classification with server-side timeout evidence.
- [x] (2026-09-02 22:50Z) Format, inspect, commit, and push before post-change tests.
- [x] (2026-09-02 23:49Z) Run focused and full LMDB/server-boot module verification.
- [x] (2026-09-03 00:08Z) Compare the benchmark against both the original baseline and a same-condition old-code control.

## Surprises & Discoveries

- Observation: The timeout test can pass while its diagnostic samples contain only `IOException: Premature EOF`.
  Evidence: Two focused runs reported three and seven recognized timeouts respectively, both with five sampled premature-EOF failures. Server logs showed `QueryInterruptedException: Query evaluation took too long` followed by `Cannot call sendError() after the response has been committed`.

- Observation: `TripleStore` selects estimator indexes using only which of the four fields are bound, never the actual IDs.
  Evidence: `getBestPageEstimatorIndex`, `getSecondaryNoPrefixEstimatorIndex`, and `residualLayoutScore` inspect only boundness and configured field order, so sixteen initialization-time selections preserve the existing decisions.

- Observation: The original pre-change benchmark and post-change benchmark ran under materially different machine conditions.
  Evidence: The original pre-change result was `76.193 +/- 10.785 ms/op`; the post-change result was `176.897 +/- 9.105 ms/op` and an immediate repeat was `172.557 +/- 4.414 ms/op`. A detached build of the old code at `2ef8c5484c`, run immediately afterward with the same JDK and JMH parameters, measured `188.150 +/- 6.149 ms/op`. The optimized plan, estimates, chosen indexes, and row counts were unchanged. The same-condition control therefore rules out the lookup-table change as the cause of the earlier cross-time shift and places the new code about 8.3 percent below the old-code control mean.

## Decision Log

- Decision: Use flat primitive lookup arrays rather than per-invocation objects or runtime code generation.
  Rationale: The domain has exactly sixteen shapes. Primitive tables remove allocation and repeated interpretation while remaining simpler than generated classes or lambdas.
  Date/Author: 2026-09-02, Codex.

- Decision: Preserve the public `GroupMatcher(byte[], boolean[])` constructor and `IndexKeyWriters.MatcherFactory` surface.
  Rationale: The optimized LMDB path can use an additive integer-mask constructor without creating avoidable source or binary compatibility risk.
  Date/Author: 2026-09-02, Codex.

- Decision: Verify timeouts from `TupleQueryResultView` server log events and leave production streaming behavior unchanged.
  Rationale: Once an HTTP 200 response is committed, no general streaming implementation can replace it with a 503. Server-side `QueryInterruptedException` is authoritative and independent of client/JDK wording.
  Date/Author: 2026-09-02, Codex.

- Decision: Commit and push before every post-change test run.
  Rationale: The user explicitly wants CI to start as soon as changes are published. Pre-change evidence and the baseline benchmark are collected before edits; post-change verification begins only after publication.
  Date/Author: 2026-09-02, Codex.

## Outcomes & Retrospective

The implementation uses per-index primitive tables for all sixteen binding shapes, plus store-level primary and secondary selection arrays. The invocation path computes one logical binding mask, performs table lookups, and constructs residual matchers from an integer mask. The shared snapshot page cache, invocation-local page reuse, and leaf-measurement memoization remain intact.

The implementation and tests were pushed before post-change verification in commits `40e07e4de2` (`GH-6006 Precompute LMDB index pattern layouts`) and `41a54f0b61` (`GH-6006 Stabilize LMDB timeout verification`). Focused matcher/cardinality/layout verification passed 8 tests. Full LMDB verification passed 1,190 unit tests with 3 skipped and 103 integration tests with 99 skipped. Full server-boot verification passed 25 unit tests and 2 integration tests. The timeout stress test observed 117 server-side interruptions, zero reader-handle failures, and a successful health query while retaining five representative client-side premature-EOF samples.

The original baseline-to-candidate comparison was invalidated by a large environmental shift. A controlled old-code run under the current conditions measured `188.150 +/- 6.149 ms/op`, while the immediately repeated new code measured `172.557 +/- 4.414 ms/op`; their 99.9 percent intervals do not overlap. This supports the intended execution improvement, but the narrow conclusion is that the precomputed design did not cause the cross-time slowdown and was faster in the same-condition control. A randomized interleaved benchmark would be needed for a stronger percentage claim.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleIndex.java` owns one configured four-field index order. It currently stores `indexMap`, a generated key writer, and a generated matcher factory. Its prefix range helper walks the suffix of `indexMap`, and its residual matcher allocates and edits a four-element boolean array.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` invokes those helpers during cardinality estimation. It scans configured indexes to choose primary and secondary layouts. A binding shape is a four-bit integer: one bit each indicates whether subject, predicate, object, and context are bound. Actual IDs do not influence layout selection.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/util/GroupMatcher.java` encodes up to four equality checks against LMDB varint keys. It already dispatches to one of sixteen unrolled match methods, but first reconstructs the dispatch mask from a `boolean[4]`.

`tools/server-boot/src/test/java/org/eclipse/rdf4j/tools/serverboot/LmdbTimedOutQueryReadHandleTest.java` submits many slow HTTP queries and checks that the repository remains usable without exhausting LMDB reader handles. Its timeout counter currently recognizes several client exception strings but not `Premature EOF`, even though the server logs the actual timeout before the committed streaming response is aborted.

## Plan of Work

First capture the behavior-neutral refactor baseline with `GroupMatcherTest` and `CardinalityTest`, retaining Surefire evidence and showing that these tests directly exercise matcher construction and non-contiguous cardinality constraints. Capture the unchanged `ThemeQueryBenchmark.executeQuery` result for Medical Records query zero using three warmups, five measurements, and two forks.

In `TripleIndex`, define logical field bits and create flat tables during construction: sixteen prefix lengths, five logical prefix masks, sixteen index-ordered matcher masks, and sixteen residual-layout scores. Build them by walking the index order only during construction. Keep the existing public pattern-score method, but make it compute a four-bit binding mask and perform a lookup. Replace the prefix-key suffix walk with a logical prefix-mask lookup. Replace residual boolean-array editing with an integer residual mask derived by clearing the guaranteed leading index-position bits. Add package-visible lookup methods needed by `TripleStore` without exposing mutable arrays.

In `GroupMatcher`, add `public GroupMatcher(byte[] valueArray, int matchMask)`. The existing boolean-array constructor converts once and delegates. Comparator setup and matcher selection use the integer bits directly. `TripleIndex.createMatcher` and `createResidualMatcher` use the integer constructor, while `IndexKeyWriters.MatcherFactory` remains available but leaves the optimized path.

In `TripleStore`, compute a binding mask once at the start of cardinality estimation. Use `Integer.bitCount` for the bound-field count. After constructor-time index creation or reindexing has established the final list, fill primary and secondary index arrays for all sixteen masks using the existing prefix, residual-layout, index-order, and different-leading-field tie breakers. Cardinality lookup reads those arrays directly. Quality-based secondary execution remains conditional and unchanged.

In the timeout test, remove `Random` and submit every stress request with one second. Attach the existing in-memory appender to `TupleQueryResultView`, count events whose throwable is `QueryInterruptedException`, and assert that count is positive. Remove client exception-text recognition as the timeout authority, but retain failure samples, reader-handle checks, and the final health query.

Add exhaustive tests for all sixteen matcher masks and all supported index orders. Verify prefix length, range-fill semantics, residual masks, and layout scores against simple test-side reference calculations. Exercise both GroupMatcher constructors over the existing varint-length matrix. Preserve the non-contiguous and mixed-pattern cardinality tests as end-to-end coverage.

Run copyright and formatting checks, inspect the exact diff, stage only intended files, and create two commits: `GH-6006 Precompute LMDB index pattern layouts` and `GH-6006 Stabilize LMDB timeout verification`. Push both commits. Only then run post-change focused tests, full LMDB and server-boot module tests, and the candidate benchmark. If a correction is needed, commit and push it before rerunning tests.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j7`. Maven commands must use `.m2_repo`, never use `-q` for tests, and never use `-am` when tests are enabled.

Capture pre-change tests with:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=GroupMatcherTest,CardinalityTest verify

Capture the baseline benchmark with:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --param themeName=MEDICAL_RECORDS --param z_queryIndex=0 --warmup-iterations 3 --measurement-iterations 5 --forks 2

After editing, run copyright validation from `scripts/`, format with the repository `process-resources` invocation, inspect `git diff --check`, commit the two coherent changes, and push the current branch. Do not run post-change tests before the push.

After pushing, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py GroupMatcherTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CardinalityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbTimedOutQueryReadHandleTest#lmdbRepositoryStillAcceptsQueriesAfterManyTimedOutServerQueries --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-boot --retain-logs

Then rerun the exact baseline benchmark command. Compare JMH score and error intervals. Do not claim a speedup if they overlap; report code-shape/allocation intent and the absence of a measurable macro difference. If the candidate moves repeatably and attribution is unclear, capture JFR or method-scoped JIT evidence before explaining the cause.

## Validation and Acceptance

All twenty-four index permutations and sixteen binding shapes must produce the same prefix, range, matcher, and layout decisions as the reference implementation. Existing cardinality tests must return the same estimates and exact small-range results. The timeout stress test must observe at least one server-side query interruption, zero reader-handle exhaustion events, and a successful health query after the workload. Full `core/sail/lmdb` and `tools/server-boot` module verification must have no new failures.

The benchmark is an evidence gate, not a correctness gate. A measurable speedup requires separated JMH uncertainty intervals under identical JDK, parameters, warmup, measurement, and fork counts.

## Idempotence and Recovery

All lookup construction is deterministic and local to store initialization. Re-running tests and benchmarks only replaces build output under `target/` and retained logs. Preserve all pre-existing and newly created untracked artifacts. Stage files explicitly; never use `git add .`. If post-push verification fails, make a narrow corrective commit, push it, and only then rerun the affected selector.

## Artifacts and Notes

The initial timeout evidence is in `server-boot-timeout-initial-evidence.txt`. Earlier unrelated evidence and benchmark files are untracked and must remain untouched.

## Interfaces and Dependencies

Add the public overload:

    GroupMatcher(byte[] valueArray, int matchMask)

The mask uses index-position bits zero through three. Values outside the low four bits are invalid. Preserve:

    GroupMatcher(byte[] valueArray, boolean[] shouldMatch)

No dependency, persisted format, configuration, page-cache lifecycle, or query-result contract changes are permitted.
