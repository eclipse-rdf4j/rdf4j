# Add a persistent two-tier LMDB cardinality cache

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance
with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

LMDB query planning repeatedly estimates the number of statements matching a subject-predicate-object-context
pattern. Today those estimates are kept in a single process-wide map with 262,144 entries and are discarded after
every store commit by embedding the triple-store revision in the key. After this change, every `LmdbSailStore`
owns an exact-bounded, thread-safe cache. Ordinary estimates remain available for three hours and tolerate up to
one percent store churn. Frequently used patterns graduate into a 128-entry popularity-ranked tier that survives
clean restarts on stores with more than one million statements and is refreshed hourly with four times the normal
sampled-leaf budget.

The behavior is observable through focused tests: two statement patterns that differ only in variable names share
an entry; a cached value remains stable after less than one percent churn; the first mutation beyond one percent
invalidates it; popular entries survive a matching clean restart; and test B-trees show 16/32 sampled leaves for a
foreground estimate versus 64/128 for an asynchronous popular refresh. Public configuration round-trips through
Java, RDF, the LMDB repository template, and Workbench creation.

## Progress

- [x] (2026-09-01 06:05Z) Read the LMDB estimator, mutation, persistence, lifecycle, configuration, template, and test paths.
- [x] (2026-09-01 06:05Z) Completed the required offline root quick clean install successfully.
- [x] (2026-09-01 06:31Z) Added and ran the smallest below-one-percent churn regression; preserved its red Surefire evidence.
- [x] (2026-09-01 07:19Z) Implemented semantic-key ordinary/popular tiers and deterministic capacity, expiry, promotion, decay, activation, refresh-race, and shutdown-drain tests.
- [x] (2026-09-01 07:21Z) Added estimator profiles, exact statistics, committed mutation accounting, strict-ratio invalidation, and lifecycle integration coverage.
- [x] (2026-09-01 07:11Z) Added defensive popular-tier persistence and an actual clean store close/reopen test.
- [x] (2026-09-01 07:24Z) Added all Java/RDF/template/documentation options and LMDB/Workbench round-trip tests.
- [x] (2026-09-01 07:55Z) Ran focused, affected Workbench, and complete tracked LMDB verification; formatted, checked copyright, and completed the final quick build and file audit.
- [x] (2026-09-01 07:58Z) Ran the four cache benchmarks, accounted for every rich-refresh page read, and found no regression requiring JFR.

## Surprises & Discoveries

- Observation: `LmdbStatementPatternCardinalitySource` is already created once per `LmdbSailStore`, but its map is
  static and its key contains both `System.identityHashCode(tripleStore)` and `TripleStore.getDataRevision()`.
  Evidence: the current class declares `SHARED_CARDINALITY_CACHE` and constructs `SharedCardinalityKey` with those
  values, so a commit leaves old entries resident but unreachable.
- Observation: the store already has a single-threaded estimator-maintenance executor and drains its work before
  closing `ValueStore` and `TripleStore`.
  Evidence: `LmdbSailStore` owns `estimatorPersistExec`; this is the correct sequential executor for popular refresh
  and avoids a second maintenance thread.
- Observation: an unrelated top-level `initial-evidence.txt` already records a GH-6006 Theme accuracy failure.
  Evidence: it names `PageCardinalityEstimatorThemeAccuracyIT` and log `20260831-212643-verify.log`. This task must
  preserve that artifact, so its first red report will be written to
  `lmdb-cardinality-cache-initial-evidence.txt` instead of overwriting it.
- Observation: `Future.cancel(false)` can mark a running refresh future cancelled and return before its estimator
  callback exits, so cancellation alone is not a shutdown drain.
  Evidence: the focused shutdown test returned from `stopRefreshAndDrain` while its rich estimator callback was
  blocked; waiting for the running future's completion made the identical selector pass.
- Observation: the Workbench LMDB form is generated from repository-template metadata.
  Evidence: adding the eleven placeholders to `lmdb.ttl` made the existing generic form pipeline round-trip all
  values without adding field-specific XSL code.
- Observation: guarding persistence with ordinary-cache enablement still wrote an empty sidecar when the popular
  tier was explicitly disabled.
  Evidence: `disabledPopularTierDoesNotCreateASidecar` failed before the guard used `popularEnabled()` and passed
  afterward; loading and persistence now both honor popular size zero.

## Decision Log

- Decision: use Routine D for the feature while still creating and observing a focused failing test before any
  production edit.
  Rationale: concurrency, time, LMDB I/O, persistence, configuration, and cross-module template changes make this a
  significant feature; repository instructions additionally require test-first evidence for behavior changes.
  Date/Author: 2026-09-01 / Codex.
- Decision: represent a statement-pattern key with its constant RDF `Value` objects, using `null` for unbound
  positions, and ignore variable names.
  Rationale: values express semantic identity across LMDB transactions and avoid recycled numeric IDs matching an
  unrelated value. Numeric IDs are resolved only while computing or refreshing an estimate.
  Date/Author: 2026-09-01 / Codex.
- Decision: use JDK concurrency primitives and a small package-private cache component with an injectable time
  source and estimator callback.
  Rationale: lock-free `ConcurrentHashMap.get` is appropriate for hits, one admission lock can enforce exact
  capacity on misses, deterministic time injection avoids sleeping tests, and no dependency is needed.
  Date/Author: 2026-09-01 / Codex.
- Decision: use a richer estimator profile only for asynchronous popular-tier activation, restart, hourly refresh,
  and post-invalidation rewarm.
  Rationale: foreground misses and stale recovery must preserve the current bounded latency. The multiplier affects
  only sampled leaves, independently for explicit and inferred databases; it must not alter branch reads, the two
  boundary-leaf reads, metadata counts, or the 32-leaf exact-range cutoff.
  Date/Author: 2026-09-01 / Codex.

## Outcomes & Retrospective

The implementation is complete. Each store now owns an exactly bounded semantic-key cache with lock-free hits,
three-hour ordinary expiry, mutation-threshold invalidation, decaying popularity, permanent large-store activation,
sequential rich refresh, generation-fenced publication, and defensive popular-only persistence. The normal estimator
continues to sample 16/32 leaves while every asynchronous popular refresh samples 64/128 leaves at the default
multiplier, independently for explicit and inferred indexes. All eleven public options round-trip through Java and
RDF and appear in the LMDB repository template, Workbench form, and documentation.

The final cache class passes 19 tests. The complete tracked LMDB module passes 1,190 tests with no failures or
errors and 101 skips; only the preserved untracked GH-6006 `PageCardinalityEstimatorThemeAccuracyIT` experiment was
excluded. Affected Workbench classes pass 21, 13, and 41 tests. Formatting, copyright/SPDX validation, and the
123-project root quick clean install pass. JMH measured warmed ordinary and popular hits at 26.370 ns/op and
23.760 ns/op, eight-thread same-key hits at 2.142 microseconds/op, and a complete 128-entry rich refresh at
22.436 ms. That refresh reads exactly 33,536 pages: 32,768 sampled leaves, 512 boundary leaves, and 256 branch
pages. These measurements did not reveal a repeatable foreground regression or maintenance monopolization, so the
conditional JFR investigation was not triggered. Detailed red and green evidence is preserved in
`lmdb-cardinality-cache-initial-evidence.txt` and `lmdb-cardinality-cache-final-evidence.txt`.

## Context and Orientation

The work is in Maven module `core/sail/lmdb`. `LmdbSailStore` is the store-level lifecycle and transaction owner.
It constructs `ValueStore`, which maps RDF values to LMDB numeric IDs, and `TripleStore`, which stores explicit and
inferred statement indexes. It creates one `LmdbStatementPatternCardinalitySource`, which
`LmdbEvaluationStatistics` calls when the query optimizer asks for a statement-pattern cardinality.

An ordinary cache entry is a computed estimate and computation timestamp plus the three newest foreground access
times. Approximate FIFO means entries are admitted in a queue and the oldest still-present queue keys are evicted
when the exact capacity would otherwise be exceeded; access does not reorder the queue. A popular entry keeps the
semantic key, optional current cardinality, computation timestamp, decaying score, score timestamp, latest access
timestamp, and admission age. Exponential decay means a score of `s` observed after elapsed time `d` becomes
`s * 2^(-d/halfLife)`. Each real foreground access first decays the score and then adds one point. When full, a
candidate replaces the entry with the lowest effective score; an equal-score candidate wins against the older
entry so new patterns can compete.

`LmdbBtreeRangeCounter` performs page-based range counting. Its normal sampled-leaf budgets are 16 for a contiguous
range and 32 when a residual component matcher must inspect decoded leaf entries. It may also read branch pages and
two exact boundary leaves, and ranges no larger than 32 leaves are read exactly. The richer asynchronous profile
uses configurable multiplication, default four, giving 64 and 128 sampled leaves without multiplying the other
reads or exact cutoff. `LmdbPageCardinalityEstimator` and `TripleStore.cardinality` are the call chain through which
that profile must travel. If page estimation fails, `TripleStore` must continue to use its existing fallback
estimator unchanged.

`LmdbSailStore` queues add and bulk-add operations and counts removals while executing an active transaction. Actual
successful changes, not requested operations, must accumulate in transaction-local counters. Publish the sum only
after both LMDB stores commit; discard it on rollback. Every cache reset records an exact baseline statement count
from `mdb_stat` across explicit plus inferred index databases. Cumulative additions plus removals strictly greater
than `mutationRatio * baseline` clears ordinary entries and marks popular values stale while retaining keys and
scores. Exactly one percent remains valid. Baseline zero is treated naturally: any successful mutation exceeds the
zero threshold. Reset the baseline and mutation debt from a fresh LMDB statistic after invalidation.

Popularity activates only after an exact statement count is strictly greater than the configured threshold. While
dormant, every tenth valid normalized lookup (hit or miss) runs the statistic check: attempts 10, 20, and so on.
Once activated, the gate is permanent for that `LmdbSailStore` lifetime. Ordinary access history and popularity
scores may accumulate before activation, but popular values are neither served nor refreshed before activation.

The sidecar stores only popular entries. It follows the existing LMDB versioned-sidecar convention: write a bounded
binary snapshot to a temporary sibling, flush it, and atomically rename it, falling back to a non-atomic replace only
where atomic moves are unavailable. The header records a magic value and version, LMDB transaction ID, mutation
baseline and debt, then a bounded entry count. Each entry records four numeric value IDs, cardinality and computation
time, score and score timestamp, access time, and admission age. A snapshot is accepted only when its version,
lengths, count, configured capacities, and transaction ID are valid. Reconstruct semantic values through
`ValueStore` only after the current transaction ID matches. Malformed, oversized, expired, incompatible, or
transaction-mismatched snapshots are ignored without preventing startup. Matching unexpired values become
servable only after activation and are then queued for richer asynchronous refresh.

## Plan of Work

First add one focused regression to `LmdbEvaluationStatisticsMemoizationTest`. It loads 200 statements, obtains the
100-row estimate for a fixed predicate, commits one matching statement, and asks with new variable names. The
existing revision-keyed cache returns 101 and the test expects the bounded stale value 100. Run that one method and
copy the failing Surefire summary into `lmdb-cardinality-cache-initial-evidence.txt` before editing production code.

Next add `LmdbStatementPatternCardinalityCache` under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`.
It owns exact-bounded ordinary and popular maps, approximate-FIFO admission queues, the single miss lock, activation
attempt counter, mutation baseline/debt, generation number for stale-publication protection, coalesced refresh
state, deterministic time source, and persistence model. Its foreground API returns a value by semantic key and
invokes a normal-profile estimator only on a miss or expired/stale recovery. Hits remain map lookups plus atomic
access metadata updates and do not resolve LMDB IDs. Its refresh API snapshots popular keys, estimates them
sequentially with the richer profile, and atomically publishes only when the captured generation still matches.

Replace the static map in `LmdbStatementPatternCardinalitySource` with the store-local cache. Normalize constants
from `StatementPattern` before lookup. Keep the existing `-1` failure contract and zero for constants absent from
the value store. Add package-private diagnostics and deterministic constructors only where tests need them; do not
expose cache internals publicly.

Thread an immutable sample profile from `TripleStore.cardinality` through `LmdbPageCardinalityEstimator` to
`LmdbBtreeRangeCounter`. A normal profile has multiplier one. A rich profile clamps the configured multiplier to
1 through 64 and multiplies only the 16/32 sample counts with saturating arithmetic. Add test B-trees large enough
that exact counting does not mask sampling, and account for boundary and branch reads separately.

Add package-private `TripleStore` methods for exact explicit-plus-inferred statement count using `mdb_stat` and the
current LMDB transaction ID using a read transaction. Wire actual add, bulk-add, and remove results into
transaction-local mutation counters in `LmdbSailStore`; publish to the cache only after successful commit and clear
on rollback. Give the source the store's sequential estimator-maintenance executor, schedule hourly refresh only
when the interval is positive, coalesce immediate rewarms, and prevent stale tasks from publishing after
invalidation. During shutdown stop new refresh work, cancel and drain scheduled/queued work, persist the popular
snapshot, log persistence failures as warnings, and then close `ValueStore` and `TripleStore`.

Extend `LmdbStoreConfig` and `LmdbStoreSchema` with eleven options and defaults:
`statementPatternCardinalityCacheSize=8192`, `statementPatternCardinalityCacheExpiryMillis=10800000`,
`statementPatternCardinalityCacheMutationRatio=0.01`, `popularStatementPatternCardinalityCacheSize=128`,
`popularStatementPatternCardinalityCacheActivationThreshold=1000000`,
`popularStatementPatternCardinalityCacheActivationCheckInterval=10`,
`popularStatementPatternCardinalityCachePromotionAccesses=3`,
`popularStatementPatternCardinalityCachePromotionWindowMillis=10800000`,
`popularStatementPatternCardinalityCacheRefreshMillis=3600000`,
`popularStatementPatternCardinalityCacheDecayHalfLifeMillis=10800000`, and
`popularStatementPatternCardinalityCacheRefreshSampleMultiplier=4`. Ordinary size zero disables the whole feature;
popular size zero disables only popular behavior; refresh interval zero disables the scheduled refresh but not
foreground recovery. Reject non-finite or negative mutation ratios. Normalize other sizes and durations to
non-negative values and clamp the multiplier to 1 through 64. Add getters/setters, RDF export/parse, IRIs, LMDB
repository-template fields, Workbench create assertions, and documentation in
`site/content/documentation/programming/lmdb-store.md`.

Build tests from the behavior outward. Unit tests use a fake monotonic clock and estimator to cover exact capacity
under concurrency, expiration boundaries, semantic normalization, third in-window promotion, out-of-window access,
decay, equal-score replacement, estimator failure, dormant activation attempts, permanent activation, and store
isolation. Integration tests cover commit/rollback mutation accounting, exact one-percent and first-excess
invalidation, every async refresh path, no artificial access during refresh, serving valid values during routine
refresh, and generation fencing. Persistence tests cover close/reopen, downtime decay, matching and mismatched
transaction IDs, expired values, truncation, incompatible version, changed capacity, and write failure. Configuration
tests round-trip all eleven options and Workbench template fields.

Finally add JMH benchmarks for warmed ordinary hit, warmed popular hit, concurrent same-key hit, and complete
128-entry rich refresh. Record page counts, duration, and foreground latency. Do not claim improvement without the
measurement; if a repeatable regression or monopolizing refresh appears, run the repository's supported JFR-backed
single-benchmark workflow before completion.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j7`. The required initial build has passed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run the first red regression and later repeat the identical selector:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEvaluationStatisticsMemoizationTest#retainsCachedCardinalityBelowOnePercentStoreChurn --module core/sail/lmdb --retain-logs

Run new focused classes and then the LMDB module without `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStatementPatternCardinalityCacheTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBtreeRangeCounterTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreConfigTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Find the affected Workbench test selector after editing `CreateServletTest`, then run it through `mvnf` with module
`tools/workbench`. Before final handoff run copyright and formatting, followed by the required root quick install:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use `git diff --check`, `git status --short`, and file-scoped diffs for the final audit. Never remove or overwrite the
existing GH-6006 patch, Theme integration test, benchmark text, `initial-evidence.txt`, or `final-evidence.txt`.

## Validation and Acceptance

The focused churn regression must fail before production edits with expected 100 and actual 101, then pass without
changing its assertion. Cache unit tests must prove no tier exceeds its configured exact capacity even under
concurrent miss admission, an entry computed exactly three hours ago is expired, only three accesses within the
window promote, decayed old scores yield to competitive new patterns, and independent stores never share entries.

Activation tests must observe exact LMDB size checks only on attempts 10, 20, and so forth, reject exactly one
million, accept 1,000,001, and stay active after the store shrinks. Mutation tests must retain values at exactly one
percent churn and invalidate on the first strict excess. Asynchronous tests must show all four rewarm paths use the
rich profile, do not increment popularity, preserve a valid value during routine refresh, reject stale generation
publication, and use normal sampling for foreground recovery.

Synthetic B-tree tests must report 16 and 32 foreground sampled leaves versus 64 and 128 rich sampled leaves, plus
the unchanged boundary and branch reads. Small ranges at or below the exact cutoff must remain exact. Explicit and
inferred databases each receive the profile independently. A forced page-estimator failure must still reach the
unchanged fallback estimator.

Persistence acceptance requires a clean close and reopen with matching transaction ID to retain eligible semantic
keys and unexpired values, while mismatched IDs, expired values, truncation, version mismatch, oversized counts,
and capacity changes are ignored safely. A forced sidecar write failure must log a warning and still close the store.
Every Java option must export to RDF and parse back, appear in the repository template, and reach Workbench store
creation. Focused tests, the complete `core/sail/lmdb` module, and affected Workbench tests must pass.

Benchmark acceptance records warmed ordinary and popular hit latency, concurrent same-key behavior, 128-entry rich
refresh duration, and total sampled pages. Performance work is complete only after repeatable regressions are
investigated and excessive maintenance-executor monopolization is either fixed or explicitly documented with JFR
evidence.

## Idempotence and Recovery

All build and test commands are repeatable. `mvnf --retain-logs` preserves timestamped logs and rebuilds the local
module dependencies before tests. Do not use `git clean`, broad restore, reset, or manual stash because the checkout
contains unrelated untracked evidence. If an offline build lacks an artifact, rerun that exact build once without
`-o`, then return to offline operation. If production code is ever changed before the first red Surefire report is
captured, revert only that task-owned hunk with `apply_patch`, restore the plan's first test step as active, and
repeat the red selector.

Refresh tasks carry a generation token, so retrying after a failed or invalidated refresh cannot publish obsolete
data. Persistence always writes a temporary sibling and replaces the sidecar only after a complete write, leaving
the previous valid snapshot recoverable if the write fails.

## Revision Note

This plan was updated after implementation to record the final persistence-disabled edge case, complete verification
totals, benchmark results and page accounting, and the explicit boundary around the preserved untracked Theme
accuracy experiment.

## Artifacts and Notes

The initial root clean install ended with:

    [INFO] RDF4J: LmdbStore ................................... SUCCESS
    [INFO] RDF4J: Workbench ................................... SUCCESS
    [INFO] BUILD SUCCESS

The focused pre-production regression produced:

    Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    expected: <100.0> but was: <101.0>

The report is `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationStatisticsMemoizationTest.txt`
and the retained Maven log is `logs/mvnf/20260901-062557-verify.log`.

The first failing test evidence belongs in `lmdb-cardinality-cache-initial-evidence.txt`; full Maven output belongs
under `logs/mvnf/`. Final focused and broad evidence should use a distinct
`lmdb-cardinality-cache-final-evidence.txt` so existing task artifacts remain untouched.

## Interfaces and Dependencies

Use only the JDK and existing RDF4J/LMDB APIs. No dependency is added. `LmdbStatementPatternCardinalityCache` is
package-private and is owned by exactly one `LmdbStatementPatternCardinalitySource`. Its semantic key contains
`Resource subject`, `IRI predicate`, `Value object`, and `Resource context`, each nullable. Its estimator callback
accepts that key and an immutable profile identifying normal or rich sampled-leaf budgets. Its size-statistics
callback returns the exact explicit-plus-inferred entry count and current LMDB transaction ID. Its persistence API
loads during store initialization and writes only after refresh work has been canceled and drained during close.

`TripleStore.cardinality(long,long,long,long)` remains the normal-profile entry point for existing callers. Add an
internal overload accepting the estimator profile. `LmdbPageCardinalityEstimator` and `LmdbBtreeRangeCounter` gain
matching internal overloads, while existing signatures delegate to multiplier one. The cache's mutation callback
accepts committed successful additions and removals; rollback never invokes it.

Public configuration stays on `LmdbStoreConfig` with fluent setters matching existing style, RDF predicates in
`LmdbStoreSchema`, repository-template bindings under `core/repository/api/src/main/resources`, Workbench field
mapping under `tools/workbench`, and user documentation under `site/content/documentation/programming`.

Revision note (2026-09-01 06:05Z): created the initial self-contained implementation plan after repository
orientation and the required clean install; recorded the pre-existing `initial-evidence.txt` collision so the
GH-6006 artifact remains unchanged.

Revision note (2026-09-01 06:31Z): recorded the successful test-first milestone and its exact Surefire failure before
the first production edit.

Revision note (2026-09-01 07:24Z): recorded completed implementation/configuration milestones, the discovered
shutdown-drain race and its red-to-green evidence, and the template-driven Workbench integration decision.
