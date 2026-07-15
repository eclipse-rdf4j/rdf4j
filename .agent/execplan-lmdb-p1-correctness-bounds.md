# Complete LMDB correctness, bounded memory, and optimized-path coverage

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while work proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

After this work, LMDB native queries preserve SPARQL filter scope, evaluate non-repeatable functions at their original
row boundary, reproduce sequential SUM/AVG and MIN/MAX results when arithmetic or representative choice depends on
encounter order, and keep chunk/hash/top-K memory within physically retained capacity. Every eligible serial,
factorized, parallel, and composite-source path uses the batched substrate; unsupported or unsafe shapes deliberately
fall back to ordinary sequential evaluation.

Calendar ordering in `ValueComparator` becomes one genuine total order across valid, invalid, timezone-bearing, and
timezone-free XML Schema calendar literals. Hash replay becomes zero-copy, count-only replay remains correct above
`Integer.MAX_VALUE`, and ordered factorized top-K retains payload only for live heap slots. Each observable behavior is
demonstrated by a focused automated regression before its production change.

## Progress

- [x] (2026-07-14) Reviewed the committed P1 findings and identified the affected modules and methods.
- [x] (2026-07-14) Audited the worktree and reserved non-overlapping files; existing query-mode edits remain untouched.
- [x] (2026-07-14) Ran the mandatory root clean install; all reactor modules succeeded in 36.202 seconds.
- [x] (2026-07-14) Added and observed the OPTIONAL filter-scope regression: native count 10, generic count 0.
- [x] (2026-07-14) Added and observed the volatile factorized-filter regression: native count 4, generic count 5.
- [x] (2026-07-14) Added and observed the adaptive hash-budget regression: 2048 rows consumed instead of 1024.
- [x] (2026-07-14) Added and observed the calendar comparator-cycle regression: one test, one failure.
- [x] (2026-07-14) Applied all four root-cause fixes without adding dependencies or public API.
- [x] (2026-07-14 20:54Z) Adopted committed starting point `70183af857`; mandatory root install passed in 30.211s.
- [x] (2026-07-14 21:00Z) Captured focused reds for timezone-free `xsd:time`, invalid calendar lexicals,
  unmarked custom functions, and direct 513-row hash replay; appended each report to `initial-evidence.txt`.
- [x] (2026-07-14 21:52Z) Captured the complete remaining regression matrix at `70183af857`: all-calendar ordering,
  query-mode ORDER BY/extrema, aggregate encounter order, long/direct hash replay, physical hash/arena accounting,
  top-K churn, retained composite probes, and missing worker chunk engagement. Appended every focused Surefire result
  to `initial-evidence.txt` without replacing earlier evidence.
- [x] (2026-07-14 22:18Z) Implemented the global calendar key, conservative repeatability, encounter-order fallback,
  physical chunk/hash accounting, retained composite probes, external-root/serial chunk activation, and live-slot top-K
  payload foundations. The 14-method calendar total-order class is green.
- [x] (2026-07-14 22:18Z) Captured and fixed a post-review STRICT precedence regression: DATE once again precedes
  GYEAR according to `CoreDatatype.XSD`; the cache now uses direct bounded keys behind weak literal identities.
- [x] (2026-07-14 23:45Z) Captured and fixed physical-capacity accounting for legacy chunked-prefix collections and
  factorized-tail branch memos, including refusal rollback, scan-once release, and idempotent close.
- [x] (2026-07-14 23:48Z) Captured and fixed parallel aggregation/source cleanup: all sibling sources close, real
  worker failures outrank encounter-order fallback, and later cleanup failures remain suppressed on the primary.
- [x] (2026-07-15 00:36Z) Centralized expression repeatability across core and LMDB optimizers. Unknown extension
  functions and blank-node generators now remain at their original row boundary; built-in query-constant `NOW()`
  remains eligible. Focused and containing optimizer selections are green, including the 40-test factorized aggregate
  class.
- [x] (2026-07-15 00:51Z) Captured and fixed fallback cleanup arbitration. Fresh suppression-enabled stackless
  signals preserve real cursor/tail/source failures; focused methods, the seven-test cleanup class, and the 23-test
  parallel aggregation class are green.
- [x] (2026-07-15 00:55Z) Captured composite active-source, parallel partial-open, and ordered cleanup regressions.
  Production cleanup is now being changed against those three reds.
- [x] (2026-07-15 01:27Z) Closed Parallel SELECT lifecycle gaps: EOF waits for cleanup, body failures outrank
  cleanup regardless of arrival order, cleanup-only failures surface, and rejected startup transfers source ownership
  exactly once. The 11-test cleanup and 11-test pipeline classes are green.
- [x] (2026-07-15 01:49Z) Made scan-once count tables physically budgeted and transactional. Initial/growth
  capacity is charged before allocation, old memos remain live until complete publication, refusal rolls back and
  latches, and exact capacity is released once. Focused regressions and the scan-once query class are green.
- [x] (2026-07-15 01:58Z) Made chunk `SEEN_ONCE` publication rollback-safe across key copy, memo insertion,
  retained-probe creation, and probe open while preserving the original failure.
- [x] (2026-07-15 02:13Z) Made factorized ENUM materialization physically budgeted and transactional. Initial and
  growth capacity is reserved before allocation/probe work, retained arrays are not compacted, serial refusal streams
  the ordinary suffix, and ordered refusal discards and restarts. Five focused methods plus the 22-test chunk/hash and
  13-test ordered-factorized classes are green.
- [x] (2026-07-15 02:21Z) Captured the remaining attempt-commit and external-root reds. Floating fallback leaks
  chunk/hash/SIP/scan-once counters, successful parallel work leaks counters before sibling-source close succeeds,
  and fully disconnected plus zero-produced-slot roots bypass factorized external-root chunk execution.
- [x] (2026-07-15 02:36Z) Closed attempt-commit and external-root coverage gaps. Speculative aggregation now records
  worker and nested-path telemetry in hierarchical attempt-local metrics, publishes only after all result and source
  cleanup succeeds, and abandons every delta on encounter-order replay. Parallel SELECT/aggregation retain positional
  external roots across disconnected, constant-root, and multi-batch refill shapes. The focused methods and the
  12-test cleanup, 25-test aggregation, and 13-test SELECT pipeline classes are green.
- [x] (2026-07-15 02:55Z) Captured seven replay-ledger reds covering initial/growth capacity, refusal rollback,
  multi-stage ownership, high-fanout and arena refusal latches, and exceptional-close release.
- [x] (2026-07-15 03:10Z) Implemented physical classic-replay accounting, exact normal/exceptional release,
  zero-copy arena replay, and charged uncacheable latches. All seven focused methods and the 28-test class are green.
- [x] (2026-07-15 03:30Z) Captured the mutable-label regression, five LMDB barrier reds, fifteen core
  repeatability reds, and the safe scoped-OPTIONAL engagement control. Calendar keys now parse their validated label.
- [x] (2026-07-15 03:30Z) Parse cached calendar keys only from immutable RDF labels.
- [x] (2026-07-15 05:00Z) Gated core and LMDB rewrites at volatile tuple-expression barriers. Fourteen core and
  six LMDB focused regressions, followed by six containing classes, are green with evidence appended.
- [x] (2026-07-15 05:31Z) Charged ordered sort/top-K storage, made enum retention adaptive, and deferred every
  optimized-attempt metric until successful cleanup. Nine focused methods and three containing/neighbor classes pass.
- [x] (2026-07-15 07:00Z) Closed optimized filter ownership and direct external-root SELECT gaps. Deterministic
  compiled comparisons now fork worker-confined memo state; unsafe filters reject before opening snapshot sources;
  every submitted and unsubmitted worker filter has one cleanup owner with stable failure ordering.
- [x] (2026-07-15 06:35Z) Captured final ordered-memory and SIP regressions: speculative sort counters,
  eager top-K metadata, retained SIP capacity, and atomic SIP refusal are all red with retained logs and appended
  `initial-evidence.txt` entries.
- [x] (2026-07-15 06:46Z) Implemented final physical accounting: ordered counters are transactional, top-K
  metadata grows lazily and is charged atomically, sorted expansion reads retained payload in place, SIP masks own
  exact reservations, and unsigned mask rotation is allocation-free.
- [x] (2026-07-15 07:10Z) Verified every focused post-fix regression, including transactional ordered metrics,
  live-slot top-K metadata, SIP physical accounting, parallel filter lifecycle, strict calendar precedence, and all
  five startup-isolated feature-flag fallbacks. Every selection is green and appended to `initial-evidence.txt`.
- [x] (2026-07-15 07:38Z) Captured and fixed ordered composite worker delegation. Parallel composite snapshots now
  preserve ordered index signatures and merge ordered child scans instead of inheriting the unsupported default.
- [x] (2026-07-15 07:57Z) Closed final static-review and benchmark validity findings. Ordered composite scans now
  delegate ordered operations and seek across merged prefetched heads; function constant folding classifies the
  already-resolved implementation; comparator setup is trial-scoped; and composite probes have a same-build matched
  retained-versus-per-open control. Focused red/green evidence, the 14-test composite class, and both JMH jars are green.
- [x] (2026-07-15 09:19Z) Fixed adaptive hash activation at the completed-probe boundary and made streaming attempt
  metrics forward observations made after first-row engagement. The 10-test chunk pipeline, four-test row-step, and
  32-test hash/accounting classes are green.
- [x] (2026-07-15 09:47Z) Ran the full LMDB gate. All 1,776 unit tests are green; the 113 integration tests exposed two
  plan-shape performance regressions: learned filter evidence never replaced sampled evidence for ENGINEERING:2, and
  a VALUES-redundant local filter remained on ELECTRICAL_GRID:7.
- [x] (2026-07-15 09:56Z) Captured focused reds for deferred hash activation and parallel SELECT attempt metrics.
  The early-return case swept all 10,000 rows; failed and cancelled workers each leaked one chunk engagement.
- [x] (2026-07-15 10:39Z) Deferred hash construction until continued demand and made parallel SELECT metrics
  attempt-local. Focused regressions and the hash, cleanup, and parallel-pipeline containing classes are green.
- [x] (2026-07-15 10:41Z) Closed both plan-shape regressions with learned-filter costing and segment-local VALUES
  coverage, and made folded constant filters over UNION all-or-nothing. Focused regressions, both ITs, and the
  optimizer/compiler containing classes are green.
- [x] (2026-07-15 10:58Z) Captured replay-safety regressions proving an unmarked function ran 11 times for six source
  rows before late floating fallback and four times for two extrema inputs before alias-tie fallback.
- [x] (2026-07-15 11:06Z) Gated encounter-order-changing execution before speculation when the input plan is not
  replay-safe. Both regressions now evaluate the function once per original row, report ordinary sequential
  telemetry, and publish no optimized-attempt counters; deterministic factorized and parallel controls remain active.
- [x] (2026-07-15 11:38Z) Completed the post-replay LMDB module gate: all 1,781 unit and integration tests passed,
  with zero failures or errors and three skips. The complete retained log is
  `logs/mvnf/20260715-111525-verify.log`.
- [x] (2026-07-15 11:49Z) Added all-calendar MIN/MAX parity across evaluation, LMDB native/generic,
  MemoryStore, and NativeStore. All four focused methods and all four containing classes are green.
- [x] (2026-07-15 12:32Z) Benchmarked and rejected deferred coordinator-tail materialization. Two matched JFR
  protocols were 21.4% and 17.1% slower than the pre-change profile, while the removed allocations accounted for
  only 0.10% of sampled pressure. The warmed three-fork candidate remained at 5.315 +/- 0.418 ms/op. Reverted only
  the candidate experiment and retained the correctness/accounting work.
- [x] (2026-07-15 12:41Z) Rebuilt the restored eager path and ran the identical warmed three-fork protocol. It measured
  4.871 +/- 0.418 ms/op versus the candidate's 5.315 +/- 0.418 ms/op; all four restored-path containing classes and
  the chunk/hash accounting class are green.
- [x] (2026-07-15 13:04Z) Fixed JDK 25 discovery for the query-evaluation JMH jar and ran the calendar workload against
  the current branch and a detached `28293f90` archive with byte-identical benchmark source. Baseline calendar sort
  fails TimSort's comparator-contract check in every fork; current STANDARD/STRICT complete at
  5.743 +/- 0.138 and 5.820 +/- 0.145 ms/op. Pure-IRI controls show no meaningful regression.
- [x] (2026-07-15 13:27Z) Completed the matched latency matrix. Exact decimal SUM/AVG remain 5.9-6.7x faster with
  parallel aggregation; floating fallback exposed a 39-42% speculative-attempt tax; slot-owned top-K is 28x faster;
  retained composite probes are 30% faster; hash replay and external-root chunk confidence intervals overlap their
  broader row-chain controls. JSON results are retained under `profiles/lmdb-opt/jmh/`.
- [x] (2026-07-15 14:08Z) Captured and fixed the early contributing-root floating fallback before task reservation.
  The first bulk-filled root morsel is inspected and then transferred unchanged to ordinary production.
  Exact decimal confidence intervals are unchanged, while all four immediate floating variants improve 29.8-31.2%
  and converge with the parallel-disabled sequential controls.
- [x] (2026-07-15 14:23Z) Captured and fixed two preflight review regressions. The prepared cursor now comes from
  the producer sibling in the workers' pinned snapshot set, and every row in the buffered first morsel is inspected
  with rollback between probes. Both focused regressions are green with retained red/green evidence.
- [x] (2026-07-15 14:35Z) Added and passed a 1,041-row successful prepared-root handoff control, then replaced
  per-exact-row bind/rollback with behavior-neutral raw inline type-tag inspection. Matched focused pre/post selections
  retain both once-only exact transfer and late-in-morsel floating detection.
- [x] (2026-07-15 15:09Z) Pinned every parallel sibling family to one LMDB commit epoch. Task admission now precedes
  reader allocation; real siblings retain one transaction-manager read-lock lease through final close and validate
  their `mdb_txn_id`; composite families reject differing known tokens and close every partial child. All five focused
  red regressions and the admitted floating/orphan controls are green with appended evidence.
- [x] (2026-07-15 15:57Z) Made floating-root branch preflight fully lazy and binding-correct. Exact morsels allocate no
  producer probes, rejected branches allocate no later probes, and `constantSlot` memo keys include the bound-slot
  discriminator. Partial creation and close failures unwind every probe, root cursor, sibling, and task reservation;
  six focused red/green and pre/post characterization selections are appended to `initial-evidence.txt`.
- [x] (2026-07-15 16:11Z) Closed final physical setup gaps. Top-K object-reference metadata is conservatively charged
  at eight bytes per live slot, and serial/external chunk construction now unwinds partial stage chains, replay
  reservations, retained filters, roots, and arenas with deterministic nested failure precedence. Four focused
  red/green selections and the two-test setup-failure class are green with retained evidence.
- [x] (2026-07-15 16:41Z) Made hash-replay and top-K-replacement work counters transactional, proved 513 direct
  published-bucket rows and 511 committed live-slot replacements, and isolated the parallel cleanup fixture from an
  ambient disabled feature flag. Focused commit/discard gates plus the 34-test hash, 20-test ordered-factorized,
  35-test cleanup, two-test setup-failure, and four-test preflight-failure classes are green.
- [x] (2026-07-15 18:12Z) Captured benchmark-led external-root hash and single-active composite delegation regressions.
- [x] (2026-07-15 18:16Z) Disabled duplicate worker hashes and delegated one active composite probe; all focused and
  containing-class selections are green.
- [x] (2026-07-15 21:10Z) Completed the post-rollout LMDB module gate: 1,808 tests passed with zero failures or
  errors and three skips. The retained log is `logs/mvnf/20260715-184425-verify.log`.
- [x] (2026-07-15 21:21Z) Closed final read-only audit findings. Multi-source probes reuse one wrapper, one-active
  composites directly delegate statement/probe/sibling families, external-root stages skip hash-only setup arrays,
  and test/benchmark properties restore caller state even when teardown fails. Three focused reds and four containing
  classes are recorded in `initial-evidence.txt`.
- [x] (2026-07-15 22:09+02:00) Reran affected matched benchmarks and JFR profiles, formatted the worktree,
  completed the final 1,810-test LMDB gate, validated copyright/SPDX headers, completed the 30.898-second root clean
  install, and audited the final diff against `28293f90`. No actionable correctness or resource-lifetime issue remains
  from the final independent reviews; external-root chunk execution remains experimental and default-off because it
  did not satisfy the rollout performance gate.

## Surprises & Discoveries

- Observation: The worktree already contains uncommitted query-evaluation-mode changes in
  `DefaultEvaluationStrategy.java`, `LmdbNativeDifferentialFuzzTest.java`, and new ORDER BY tests in several Sail
  modules.
  Evidence: `git status --short` and the file-scoped diff show these changes do not overlap the four production methods
  in this plan. They must be preserved and excluded from this task's ownership.

- Observation: The exact timezone/no-timezone `xsd:dateTime` triple violates transitivity in strict mode before the
  fix.
  Evidence: `ValueComparatorTransitivityTest#testMixedTimezoneDateTimesFormTotalOrder` failed with
  `Tests run: 1, Failures: 1` and reported
  `2000-01-01T00:00:00+14:00 < 1999-12-31T23:00:00Z < 2000-01-01T00:00:00`, while the first value compared greater
  than the third. Full log: `logs/mvnf/20260714-200431-verify.log`.

- Observation: Moving the left bag's filters above the reshaped OPTIONAL changes their variable scope in a real LMDB
  query, not merely in a synthetic plan.
  Evidence: `leftFilterCannotObserveOptionalBindingAfterReshape` returned native `COUNT(?z)=10` versus generic `0`.
  Full log: `logs/mvnf/20260714-200930-verify.log`.

- Observation: A symmetric 3-by-3 join with two registered per-call functions is deterministic across join order and
  exposes factorized memo reuse.
  Evidence: `volatileBranchFilterIsEvaluatedPerJoinedSolution` returned native `COUNT(*)=4` versus generic `5`.
  Full log: `logs/mvnf/20260714-201114-verify.log`.

- Observation: The hash builder reads and retains the complete sweep before its single budget reservation.
  Evidence: with only two memo entries left, `hashBuildStopsAtBudgetAndReleasesPartialReservations` consumed all 2048
  source rows rather than stopping after the first 1024-row batch. Full log:
  `logs/mvnf/20260714-201358-verify.log`.

- Observation: Between planning and implementation, the formerly dirty tracked P1 work was committed and pushed as
  `70183af857` (`more bug fixes`).
  Evidence: `git status --short` now lists only the pre-existing untracked ExecPlans/evidence, and
  `git log -1 --oneline` reports `70183af857 more bug fixes`. This plan treats that immutable commit as its baseline
  and does not reapply already committed hunks.

- Observation: The new baseline builds cleanly before further edits.
  Evidence: `maven-build.log` reports query evaluation and LMDB `SUCCESS`, `BUILD SUCCESS`, and total time 30.211s.

- Observation: Published hash buckets are copied into the reusable classic replay array, and hash reservations track
  used values rather than geometrically allocated capacity.
  Evidence: `hashReplayStreamsPublishedBucketAndAccountsForAllocatedCapacity` expected the replay array to remain at
  256 entries after a 513-value bucket, but it grew to 513. The current bucket grows to 1024 slots while reserving only
  513 values. Full log: `logs/mvnf/20260714-210003-verify.log`.

- Observation: Deferring the ordered-DISTINCT coordinator's physical factorized tail did not pay for its additional
  structural analysis and plumbing.
  Evidence: The pre-change JFR protocol measured 4.632 +/- 0.544 ms/op and attributed only 0.09% of sampled allocation
  pressure to `Branch.<init>` plus 0.01% to `FactorizedTail.create`. Candidate protocols measured
  5.624 +/- 0.254 ms/op with worker re-analysis and 5.424 +/- 0.137 ms/op with a shared immutable candidate. The warmed
  three-fork candidate measured 5.315 +/- 0.418 ms/op. Artifacts are under `profiles/lmdb-opt/jfr` and
  `profiles/lmdb-opt/jmh/ordered-distinct-after-shared.json`.

- Observation: Rebuilding the restored source recovered the warmed ordered-DISTINCT throughput regime.
  Evidence: The identical four-warmup/four-measurement, three-fork protocol measured 4.871 +/- 0.418 ms/op after the
  revert versus 5.315 +/- 0.418 ms/op for the shared candidate. The 8.3% point-estimate improvement agrees with both
  matched JFR comparisons; the warmed confidence intervals overlap, so no stronger significance claim is made.

- Observation: The query-evaluation benchmark jar initially omitted every generated JMH entry on JDK 25.
  Evidence: Packaging succeeded, `ValueComparatorBenchmark.class` existed in `target/test-classes`, but the assembled
  jar had no `META-INF/BenchmarkList` and failed with `Unable to find the resource: /META-INF/BenchmarkList`. Unlike
  LMDB, this module had not declared `jmh-generator-annprocess` as an explicit test compiler annotation processor.

- Observation: The exact pre-change calendar comparator cannot be timed on the acceptance workload because it violates
  TimSort's comparator contract before the first warmup completes.
  Evidence: A `git archive 28293f90` tree with the byte-identical benchmark source threw
  `IllegalArgumentException: Comparison method violates its general contract!` in every STANDARD and STRICT fork.
  Current STANDARD/STRICT runs completed at 5.743 +/- 0.138 and 5.820 +/- 0.145 ms/op. Pure-IRI subject comparison was
  5.633 +/- 0.027 current versus 5.573 +/- 0.072 baseline (overlapping intervals); predicate intervals also overlap.

- Observation: Immediate floating roots paid for a complete doomed parallel setup before encounter-order replay, but
  a second row-at-a-time preflight scan was too expensive for exact decimal aggregation.
  Evidence: parallel-enabled floating variants measured 225.689-230.759 ms/op versus 160.726-163.613 with parallelism
  disabled. The first preflight design reduced the float tax but regressed SUM(decimal) from 2.962 +/- 0.515 to
  6.131 +/- 0.810 ms/op. Inspecting and transferring the producer's first bulk morsel restored SUM/AVG(decimal) to
  2.939 +/- 0.107 and 3.229 +/- 0.137 while reducing floating variants to 155.270-159.990 ms/op.

- Observation: Opening worker transactions back-to-back protects only their initial creation, not the complete
  parallel query lifetime or multi-member composite opening.
  Evidence: `CompositeNativeLmdbQuerySource.openParallelSources` opens member groups separately; Txn registration
  follows LMDB reader creation; and map-resize commits deactivate/reactivate untracked readers. A commit can therefore
  split composite members or renew a nominally pinned worker transaction. Retaining the TxnManager read stamp across
  every sibling lifetime and verifying `mdb_txn_id` against the original closes all three races.

- Observation: Every native aggregation specialization that changes row association currently shares the same
  `AggState` mutation rules as the sequential path.
  Evidence: weighted SUM/AVG multiplies a value by its multiplicity, parallel SUM/AVG merges worker partials, and
  extrema ties preserve a specialization-local first term. None has a runtime encounter-order guard or a coordinator
  restart path.

- Observation: Composite sources inherit the generic probe implementation and therefore reopen concatenated child
  statement iterators for each correlated key.
  Evidence: `CompositeNativeLmdbQuerySource` has no `newProbe()` override, while the default implementation delegates
  each open to `statements()` and bypasses child retained probes.

- Observation: The external-root chunk control does not justify adaptive hash construction inside each independently
  scheduled worker. Its unique-key workload measured 0.929 +/- 0.072 ms/op and 2,435,769 +/- 330,776 B/op versus
  0.869 +/- 0.072 ms/op and 2,112,663 +/- 257 B/op for retained worker row chains. Fork-level allocation is bimodal
  by roughly one complete worker-local hash table. External-root batching must therefore disable adaptive hash builds
  while retaining probes, adjacent replay, and memoization.

- Observation: A composite with exactly one active source still wraps its one retained child probe and allocates a
  concatenating iterator on every open. The matched explicit-only workload is already 25% faster and 82% lower
  allocation than per-open legacy scans, but direct child-probe delegation can remove the remaining wrapper without
  changing query results or lifecycle ownership.

- Observation: The current calendar defect is shared by every query engine rather than being LMDB-specific.
  Evidence: generic evaluation, MemoryStore, NativeStore, and LMDB all produced the same seven-position STANDARD
  ORDER BY mismatch and selected the same wrong MAX representative. The corresponding STRICT datatype-precedence
  selections all remained green. Logs: `20260714-214132`, `214437`, `214701`, `215030`, and their paired controls.

- Observation: All four row-reordering aggregation families require the same runtime fallback boundary.
  Evidence: factorized weighted double/float, parallel aggregation, ordered DISTINCT, and ordered single-pattern
  grouping each stayed on its optimized strategy in the focused red tests. Parallel catastrophic cancellation also
  returned `0.0` instead of sequential `1.0`.

- Observation: Current hash lifecycle cleanup is sound for completion, early close, and child failure, but ownership
  and the charged quantity are wrong.
  Evidence: those three lifecycle controls passed, while long multiplicity, 8-to-16 growth, retained arena charge,
  hash-switch charge, and cursor-owned reset each failed independently.

- Observation: An external-root chunk stage cannot copy the whole shared `RowState` after a downstream batch has
  populated it. The morsel cursor rolls back only root bindings, so a second refill can inherit stale downstream slots
  and reject otherwise valid probe matches.

- Observation: JDK 25's `XMLGregorianCalendar.compare` is itself non-antisymmetric for recurring values at timezone
  wrap boundaries: both `---02Z.compare(---01)` and `---01.compare(---02Z)` return `LESSER`.
  Evidence: the exhaustive calendar regression first failed on that pair, and a direct JDK probe returned `-1` in
  both directions. Such a pair is not a determinate order that any antisymmetric comparator can preserve; the
  preservation assertion therefore accepts only reciprocal `LESSER`/`GREATER` XML comparisons.

- Observation: Reciprocal recurring comparisons still cannot all be embedded in a transitive order. JDK 25 reports
  the cycle `00:00:00 < 14:00:00 < 05:00:00Z < 00:00:00` (with reciprocal `GREATER` results).
  Evidence: direct JDK probes reproduce every edge. Exhaustive XML-order preservation is therefore limited to the
  year-bearing family; recurring datatypes use the plan's explicit leap-safe anchored interval semantics and retain
  dedicated simple determinate controls.
  Evidence: static resource/row-state review traced the state transition across
  `ExternalRootBatchStage.fill`, `ChunkPrefixRowCursor.next`, and `MorselCursor.next`. Focused >1024-root regressions
  are being captured before the seed-plus-root-slot fix.

- Observation: Retained composite probes need one failure funnel for runtime open/seek/iterator-close errors, and
  their final close must attempt every child even when the current iterator close fails.
  Evidence: static close-path audit found early throws before the all-child cleanup loop and unchecked operations that
  bypass `CompositeProbe.fail`.

- Observation: `offset + limit` can overflow before top-K selection, and live-slot payload reservations need an
  explicit release lifecycle even though their current budget is method-local.
  Evidence: static arithmetic and ownership audit of `NativeRowsStep.evaluateAll`, `slice`, and `TopKPayloadStore`.

- Observation: A repeated-variable statement is represented outside the flat native `MultiJoin` bag, even when it is
  one leg of a three-pattern query. It therefore cannot enter parallel morsel scheduling or the worker chunk pipeline.
  Evidence: the focused query reported `LAST_REJECTION=not-multi-join` in three progressively narrowed shapes. The
  final regression asserts deliberate ordinary-path parity instead of forcing an unsupported optimized shape.

- Observation: The legacy chunked-prefix fallback allocated and grew collection arrays before consulting any shared
  budget, while factorized-tail branch memos trimmed a 32-slot array to 17 and charged only the logical length.
  Evidence: focused reds `20260714-233920` and `20260714-234008`; matching post-fix greens `234553` and `234502`.

- Observation: Parallel cleanup discarded secondary real failures and stopped closing sources at the first exception.
  Evidence: `LmdbNativeParallelAggregationCleanupTest` failed both methods in `20260714-234645` and passed both after
  first-failure/suppression cleanup in `20260714-234757`.

- Observation: Extension-function repeatability was independently guessed by constant folding, generic filter
  optimizers, LMDB filter simplification, sketch join rewrites, and union distribution.
  Evidence: ten focused regressions failed before the shared policy, including zero-argument function folding,
  join/OPTIONAL relocation, union duplication, nested-filter merging, and per-row blank-node BIND movement. The
  matching core and LMDB optimizer classes are now green; logs `20260715-002636` through `20260715-003653`.

- Observation: A stackless fallback singleton with suppression disabled can replace a real close failure raised while
  unwinding speculative aggregation, and parallel/composite cleanup still contains independent first-throw leaks.
  Evidence: post-implementation static audit of `EncounterOrderFallback`, `ParallelRowCursor`, and composite source
  close/open funnels. Focused lifecycle regressions are being added before those production paths change.

- Observation: Factorized scan-once tables and pre-budget enum materialization still retain memory outside the shared
  physical ledger.
  Evidence: static ownership audit found uncharged `LongCountMap` backing arrays and full enum arrays constructed
  before the budget refusal point. Focused capacity/refusal regressions are pending in the current implementation step.

- Observation: The final physical-memory audit found that every chunk probe retained an uncharged classic replay
  array and that refusal left the `SEEN_ONCE` marker eligible for repeated doomed collection.
  Evidence: focused reds `20260715-025008` through `025322` independently expose initial allocation, growth,
  rollback, multi-stage ownership, and refusal-latch failures.

- Observation: Repeatability barriers are still incomplete outside the previously patched filter-local gates.
  Evidence: static traversal of the core normalizer, iterative, join, disjunctive, and filter optimizers found tuple
  cloning/reassociation paths that inspect only the moved scalar condition; the LMDB VALUES-anchor, union-prefix, and
  OPTIONAL-anchor rewrites inherit the same gap. A registry replacement can also make the string URI `NOW` volatile.

- Observation: Optimized native filter owners omit filter cleanup, and one parallel aggregation worker `finally`
  block can replace its operational failure with a tail-close failure.
  Evidence: the ordinary row cursor closes its filter, while chunk, factorized-tail, and factorized-row close paths
  close only their upstream/probes; `LmdbNativeParallelAggregation` closes the tail from an unguarded `finally`.

- Observation: Parallel SELECT only attempts the external-root chunk pipeline through successful row factorization.
  Evidence: a repeated-variable suffix can be chunk-bound safely but makes factorized-row planning return null, after
  which the worker opens the ordinary row chain directly.

- Observation: Publishing adaptive hash construction at classic-probe exhaustion fixes activation but can sweep and
  allocate a complete table before returning an already-produced final batch. A LIMIT or early-close consumer can pay
  that full cost without ever requesting another row. Activation must be latched and performed only on continued
  demand before the next upstream refill.

- Observation: Parallel SELECT external-root workers still hard-wire direct attempt metrics. Failed and cancelled
  workers can publish counters, and successful workers contend on global atomics in the hot path. Each worker needs a
  local child metric scope that the coordinator commits only after successful ownership/cleanup arbitration.

- Observation: The first complete LMDB gate after the function-pinning and hash-boundary fixes has no unit failures,
  but two performance-plan ITs retry for 30 seconds and time out. ENGINEERING:2 remains on sampled local-filter
  evidence instead of `learned_filter`; ELECTRICAL_GRID:7 retains a filter exactly implied by its VALUES domain.
  Evidence: `logs/mvnf/20260715-091941-verify.log` and the matching Failsafe reports.

- Observation: Repeatability is a tuple-expression property as well as a scalar-function property. `SERVICE`, tuple
  functions, `SAMPLE`, and arbitrary custom aggregates have no affirmative repeatability contract, yet the first
  centralized finder treated them as repeatable. This false negative reaches every new core and LMDB rewrite guard.
  Evidence: static traversal of `QueryEvaluationUtility.NonRepeatableFinder`, `GroupIterator`, and the tuple-function
  and aggregate-function SPIs; focused classifier and optimizer regressions are required before extending the finder.

- Observation: Three optimizer families still move deterministic operators across volatile tuple subtrees:
  `OrderLimitOptimizer` moves ORDER across Projection/Distinct, `FilterInValuesOptimizer` clones or narrows the input,
  and `LmdbSketchJoinOptimizer` clones/re-nests OPTIONAL, MINUS, and EXISTS inputs. LMDB Union distribution also has a
  `TupleFunctionCall` hole in its hand-written branch visitor.
  Evidence: static method-level audit of those transformations; each path is now part of the current repeatability
  barrier step and will receive a smallest red before production changes.

- Observation: Exact-class allowlisting covers every deterministic function in the default evaluation-module service
  file, but deliberately disables optional GeoSPARQL, SPIN/SPIF, and SHACL extension functions because the public
  function SPI has no affirmative purity contract and core cannot depend on those downstream modules. Seven
  deprecated compatibility cast subclasses are known deterministic but not service-loaded by default.
  Evidence: compared each module's `META-INF/services/...Function` file with the exact-class allowlist. Correctness
  takes precedence; a future additive purity contract is the safe way to recover downstream optimization.

- Observation: Closing the newly owned optimized filters directly from chunk/factorized stages is insufficient because
  ordered/factorized aggregation can abandon a speculative attempt and re-run the original row plan. Closing or
  publishing feedback from those filters before the encounter-order/budget decision would make fallback reuse a
  closed delegate and contaminate fallback telemetry.
  Evidence: lifecycle audit of `FilterStage`, `FactorizedTail`, `LmdbNativeFactorizedRows`, and
  `NativeGroupIteration.evaluateSequentialFallback`. The implementation needs an attempt-local borrowed filter lease:
  stage closes are idempotent no-ops, success flushes/closes once, and abandonment discards attempt counters while
  leaving the original plan filters reusable.

- Observation: Parallel filter cloning leaks already-created recording copies when a later filter is not forkable, and
  worker plans that were built but never submitted have no filter cleanup owner.
  Evidence: `LmdbNativeParallelPipelines.forkFilters` returns null immediately and startup rejection only closes query
  sources/task reservations. Atomic fork cleanup and unsubmitted-plan cleanup are included in the lifecycle step.

- Observation: Encounter-order fallback was value-sensitive but not plan-replay-sensitive. A negative-mask
  `FilterPlan` could pass through slot-order rebuilding and factorized prefix peeling, execute an unmarked function
  during speculation, then execute it again when floating arithmetic or an extrema alias tie reopened the original
  plan sequentially.
  Evidence: `LmdbNativeParallelAggregationTest#unmarkedFunctionRunsOnceWhenLateFloatingSumFallsBack` observed 11 calls
  for six original rows, and `#unmarkedFunctionRunsOnceWhenExtremaTieFallsBack` observed four calls for two original
  rows. Logs: `logs/mvnf/20260715-105634-verify.log` and `logs/mvnf/20260715-105733-verify.log`.

- Observation: VALUES coverage is valid only inside the same reorderable segment. A matching assignment beyond an
  explicitly scoped OPTIONAL cannot justify removing an earlier filter.
  Evidence: red `logs/mvnf/20260715-102332-verify.log`; green
  `logs/mvnf/20260715-102535-verify.log`.

- Observation: Folding a constant filter into UNION is an all-or-nothing transformation. Pushing into one compilable
  child while compiling the other child without the constraint leaks rows.
  Evidence: red `logs/mvnf/20260715-103049-verify.log`; green
  `logs/mvnf/20260715-103149-verify.log`.

## Decision Log

- Decision (superseded by the user-expanded plan): Treat “every P1” as the four issues explicitly labeled P1 in the completed review: OPTIONAL filter scope,
  volatile factorized filters, adaptive hash-build memory bounding, and calendar comparator transitivity.
  Rationale: Numeric SUM/AVG and match-count truncation were labeled P2 and P3 respectively; the remaining performance
  opportunities were not labeled P1 findings.
  Date/Author: 2026-07-14 / Codex

- Decision: Use focused regression-first fixes even though this multi-module effort is managed by an ExecPlan.
  Rationale: Repository instructions prohibit production changes for observable behavior until an in-repo failing test
  has been captured.
  Date/Author: 2026-07-14 / Codex

- Decision: Keep the adaptive hash fix on the existing cursor and memo-budget abstractions, without adding a dependency.
  Rationale: The desired property is bounded materialization and correct fallback, not a new hash-table library. A
  query-local incremental reservation path is lower-risk and reversible.
  Date/Author: 2026-07-14 / Codex

- Decision (superseded by the affirmative-purity policy below): Treat registered functions reporting `mustReturnDifferentResult()`, unknown functions, and blank-node
  generators as non-placeable filters, while keeping query-constant `NOW()` placeable.
  Rationale: A negative placement mask preserves the algebraic filter boundary and prevents every native relocation,
  memoization, and parallel-copy path, rather than patching only one factorized cache.
  Date/Author: 2026-07-14 / Codex

- Decision: Reuse the hash builder's incrementally reserved `HashMap<GroupKey, HashBucket>` as the published lookup
  table instead of copying every bucket into a second map and the shared arena.
  Rationale: This both enforces refusal before retention and removes the avoidable peak duplication identified by the
  performance review, while preserving each bucket's sweep order and duplicate multiplicity.
  Date/Author: 2026-07-14 / Codex

- Decision: Totalize same-datatype calendar values by lexicographically comparing their earliest and latest possible
  UTC instants, using the XML Schema legal timezone range of -14:00 through +14:00.
  Rationale: Interval ordering extends determinate timeline comparisons and gives overlapping timezone-free values one
  global transitive relation, without changing strict versus extended cross-datatype behavior.
  Date/Author: 2026-07-14 / Codex

- Decision: Supersede the same-datatype interval decision with one calendar-family sort key used for every
  calendar-versus-calendar comparison.
  Rationale: Mixing pairwise XML comparison with datatype/lexical fallback still creates cross-datatype cycles,
  equality-substitution failures, invalid-lexical cycles, and incorrect recurring-time order. A single cached key is
  the only comparator-local design that is globally transitive.
  Date/Author: 2026-07-14 / Codex

- Decision: Abort and restart an optimized aggregation only when runtime values prove encounter order matters.
  Rationale: Floating addition and comparator-equal RDF-term aliases are non-mergeable, while integer/decimal
  arithmetic, COUNT, and strictly ordered extrema remain safe and should retain factorized/parallel speedups.
  Date/Author: 2026-07-14 / Codex

- Decision: Charge array/block capacity and reuse immutable payloads directly.
  Rationale: Logical length does not bound retained Java arrays. A pipeline-owned capacity ledger, direct bucket
  replay, and heap-slot top-K arrays align accounting with actual memory while removing redundant copies.
  Date/Author: 2026-07-14 / Codex

- Decision: Treat arbitrary extension `FunctionCall` nodes as non-repeatable; only query-stable `NOW()` is exempt.
  Rationale: `mustReturnDifferentResult() == false` is not affirmative purity metadata. Conservative placement keeps
  correctness without adding a public API; a future opt-in purity contract can restore custom-function factorization.
  Date/Author: 2026-07-14 / Codex

- Decision: Require plan-level replay safety before entering any aggregation specialization that can abandon partial
  state and reopen the original input.
  Rationale: Negative-mask filters and non-repeatable computed extensions cannot be evaluated speculatively and then
  replayed without changing observable call count or results. A coordinator-level gate covers prefix-run, ordered,
  ordered-DISTINCT, factorized, and parallel attempts while leaving repeatable inputs optimized.
  Date/Author: 2026-07-15 / Codex

- Decision: Treat folded constant-filter pushdown over UNION as atomic.
  Rationale: If every child cannot accept the constraint, compiling the original UNION and applying one residual
  filter is the only equivalent transformation.
  Date/Author: 2026-07-15 / Codex

- Decision: Reject and revert the deferred coordinator-tail candidate refactor.
  Rationale: Its target allocations were already negligible, both matched JFR protocols regressed latency, and a
  warmed three-fork run confirmed the slower candidate state. The user requires optional path changes to remain only
  when allocation or latency improves without a meaningful control regression.
  Date/Author: 2026-07-15 / Codex

- Decision: Preflight only an inline floating aggregate produced by the root of a filter-free independent-branch plan,
  after bounded task admission but before worker submission. Open that scan from the producer sibling in the same
  pinned snapshot set as every worker, and transfer its already-filled first morsel and cursor to ordinary parallel
  production when the complete sample is exact or inconclusive.
  Rationale: Admission before sibling allocation prevents task-budget losers from exhausting LMDB readers. The
  producer scan still proves whether a sampled floating value contributes before any worker runs, without mixing the
  query's tracked source with newly opened worker snapshots or guessing from orphaned rows. Reusing the bulk morsel
  preserves the exact numeric fast path; filtered, shared-tail, late, and pointer-backed values retain dynamic fallback.
  Date/Author: 2026-07-15 / Codex

- Decision: Hold one transaction-manager read-lock lease for the lifetime of each parallel sibling family and compare
  opaque LMDB transaction IDs before publishing children or composite slices.
  Rationale: Opening siblings back-to-back only prevents a commit during construction. The lifetime lease also blocks
  later commit/reset and map-resize renewal, while ID equality makes a stale untracked original source refuse fresh
  siblings. Composite token validation prevents explicit and inferred member families from spanning commit epochs.
  Date/Author: 2026-07-15 / Codex

- Decision: Keep external-root chunk execution behind an internal experimental gate, disabled by default, while
  retaining its implementation, direct regressions, and benchmark opt-in.
  Rationale: At 66,561 production-eligible unique roots the candidate measured 11.228 +/- 0.571 ms/op versus
  11.256 +/- 0.849 for the worker row chain, with higher mean allocation. At 16-way repeated keys it measured
  93.511 +/- 1.876 versus 95.475 +/- 3.362 ms/op with overlapping confidence intervals and 148 KB/op more allocation.
  The plan explicitly forbids rolling out optional path expansion without a repeatable allocation or latency win.
  Date/Author: 2026-07-15 / Codex

- Decision: Delegate `newProbe()` directly when a composite snapshot has exactly one active source.
  Rationale: The child already owns the complete retained-probe lifecycle. Removing the one-element composite wrapper
  preserves behavior and reduced the explicit-only control from 49,496 to 292 B/op and from 0.123 to 0.116 ms/op.
  Date/Author: 2026-07-15 / Codex

- Decision: Reuse one concatenating iterator for multi-source retained probes and directly transfer validated
  one-active statement and parallel-source handles.
  Rationale: `NativeProbe.open()` explicitly invalidates its prior iterator, so reset-in-place preserves the lifetime
  contract and exact child-close ownership. The final matched explicit-plus-inferred A/B measured 0.155 +/- 0.002
  ms/op and 696 +/- 37 B/op versus 0.224 +/- 0.002 ms/op and 516,141 +/- 54 B/op for legacy per-open scans.
  Date/Author: 2026-07-15 / Codex

- Decision: Allocate adaptive-hash position arrays only when hash construction is possible.
  Rationale: External-root and merge-walk probe stages permanently refuse hash construction, so the arrays were dead
  per-stage setup payload. The final external-root rerun still allocated 138.7 KB/op more than the row chain and had
  materially worse/unstable latency, reinforcing the existing default-off decision.
  Date/Author: 2026-07-15 / Codex

## Outcomes & Retrospective

The plan is complete. Regression-first work replaced the non-transitive pairwise calendar comparator, preserved
OPTIONAL/filter row boundaries for non-repeatable expressions, added encounter-order fallback for floating SUM/AVG and
distinct-term extrema ties, made chunk/hash/memo/SIP/sort/top-K accounting physical and transactional, pinned parallel
LMDB snapshots, reused retained composite probes, and bounded top-K payload retention by live heap slots. The final
read-only audits found no remaining actionable P1 correctness, cleanup, snapshot, or accounting defect.

Every supported LMDB query shape now either selects its safe specialized strategy or deliberately falls back to the
ordinary equivalent strategy. Serial factorized aggregation uses the chunk prefix where eligible. Exact
integer/decimal aggregation remains parallel/factorized; floating and representative-sensitive extrema replay
sequentially. Unknown extension functions do not relocate or memoize. Single-pattern, ordered-DISTINCT, projected
two-way joins, VALUES-only, disabled-feature, and unsupported shapes retain their prior safe paths. The external-root
worker chunk substrate is implemented and directly tested but remains behind an internal experimental default-off gate:
matched measurements showed no repeatable latency win and higher allocation, so enabling it would violate this plan's
rollout criterion.

Final verification is green:

- Query-evaluation module: 830 tests, zero failures/errors.
- LMDB module, post-format and post-fix: 1,810 tests, zero failures/errors, three skips; retained log
  `logs/mvnf/20260715-194001-verify.log`.
- MemoryStore and NativeStore query-mode selections: six tests each, zero failures/errors.
- Copyright/SPDX validation and `git diff --check`: green.
- Final offline root `-Pquick clean install`: all reactor modules succeeded in 30.898 seconds.

Matched JMH/JFR evidence supports the enabled performance paths. Exact decimal parallel SUM/AVG is 5.9-6.7x faster
than sequential controls. Live-slot top-K payload storage reduced isolated allocation from 6,817,457 to 33,176 B/op
and improved latency from 0.190 to 0.108 ms/op; the end-to-end top-K query was about 28x faster than generic execution.
The final retained multi-source composite path measured 0.155 +/- 0.002 ms/op and 696 +/- 37 B/op versus
0.224 +/- 0.002 ms/op and 516,141 +/- 54 B/op for legacy per-open scans. Direct hash replay reduced matched query
latency from 5.096 to 4.003 ms/op, while its broader-control allocation result was intentionally not treated as an
allocation win. Calendar sorting completes deterministically at 5.743 +/- 0.138 ms/op (STANDARD) and
5.820 +/- 0.145 ms/op (STRICT); the `28293f90` baseline fails TimSort's comparator-contract check, while non-calendar
controls show no meaningful regression.

All red/green snippets are preserved in the root `initial-evidence.txt`; full Maven logs and JMH/JFR artifacts remain
under `logs/mvnf/` and `profiles/lmdb-opt/`. The worktree remains intentionally dirty and unstaged, with unrelated and
pre-existing untracked artifacts preserved. No public API, dependency, configuration property, or persistent format was
added.

Original and current baseline install evidence:

    [INFO] RDF4J: Query algebra - evaluation .................. SUCCESS [  2.790 s]
    [INFO] RDF4J: LmdbStore ................................... SUCCESS [  4.970 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  36.202 s (Wall Clock)

    [INFO] RDF4J: Query algebra - evaluation .................. SUCCESS [  2.422 s]
    [INFO] RDF4J: LmdbStore ................................... SUCCESS [  4.088 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  30.211 s (Wall Clock)

## Context and Orientation

The repository root is `/Users/havardottestad/Documents/Programming/rdf4j`.

The general RDF value comparator is in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/ValueComparator.java`.
Its strict mode only compares compatible types semantically; standard mode extends comparison across calendar
datatypes. A comparator used by Java and native sorts must be antisymmetric and transitive even when XML
`XMLGregorianCalendar.compare` returns `INDETERMINATE`. The current pairwise interval patch does not provide that global
property.

The LMDB native engine is under `core/sail/lmdb`. `LmdbNativeGroupStep` selects prefix-run, ordered, parallel,
factorized, and ordinary aggregation. `LmdbNativeAggregateState` holds SUM/AVG/MIN/MAX state. Factorized multiplication
and parallel partial-state merging can reorder IEEE floating addition; factorized, parallel, or ordered execution can
also choose a different RDF term when the comparator says two distinct terms are equal. An encounter-order fallback is
an internal signal that discards such speculative state and reruns the original ordinary row chain.

`LmdbNativeChunkPipeline` batches flat join prefixes and may replace repeated probes with a scan-once hash table.
`FactorizedTail.MemoBudget` limits cached entries and primitive values, while `LmdbNativeLongArena` physically owns
block arrays. Accounting must follow array/block capacity, not logical payload length. A published `HashBucket` is
immutable and can therefore be streamed directly. `LmdbNativeRowStep.tryEvaluateOrderedFactorized` uses a bounded heap
for ORDER BY plus LIMIT, but its current append-only arena retains payload for evicted candidates.

`CompositeNativeLmdbQuerySource` combines explicit and inferred snapshots. Its default probe recreates iterators for
every correlated key; a composite probe must instead own one retained child probe per active source. Parallel workers
receive root scan rows as morsels, so their chunk pipeline begins at depth one and must not assume globally ordered
morsels for merge-walk or side-information passing.

## Plan of Work

Milestone one captures all remaining regressions at `70183af857`. Extend `ValueComparatorTransitivityTest` with valid
and invalid values from every calendar datatype and assert antisymmetry, transitivity, equality substitution, and
preservation of determinate XML less/greater relations. Run the already committed timezone-free time,
dateTime/dateTimeStamp, invalid lexical, unmarked custom-function, hash capacity, and direct-replay selections before
changing production. Add focused reds for weighted and parallel floating SUM/AVG, comparator-equal MIN/MAX aliases,
long count-only hash replay, terminal memo/arena release, top-K replacement churn, serial/parallel chunk engagement,
and retained composite probes.

Milestone two replaces all calendar pairwise logic with one cached `CalendarSortKey`. Valid year-bearing values use a
chronological interval; recurring values use datatype-specific intervals anchored to leap-safe year 2000; invalid
values use a fixed datatype/lexical block. Strict mode ranks datatype before interval. Standard mode shares one
chronological domain for gYear, gYearMonth, date, dateTime, and dateTimeStamp, while recurring datatypes remain separate.
The key compares earliest boundary, latest/exclusive boundary, datatype, timezone metadata, label, and base direction,
returning zero only for equal RDF terms. A 32-entry thread-local identity ring caches keys with weak literal references.
In the same milestone, keep OPTIONAL left filters below the left join and classify every arbitrary `FunctionCall` as
non-repeatable except query-stable NOW.

Milestone three introduces a stackless `EncounterOrderFallback`. Every aggregate specialization that can reorder or
reassociate rows marks its `AggContext` accordingly. Floating SUM/AVG signals before mutation; MIN/MAX signals when a
comparison ties two RDF-term-distinct values. The coordinator closes all speculative resources, discards partial group
tables, initializes a fresh row, and evaluates `arg.open(row)` with sequential DISTINCT channels. It commits explain
strategies and counters only after optimized success and gives independent worker failures precedence over fallback.
Integer/decimal SUM/AVG, COUNT, and strict extrema remain optimized.

Milestone four adds a pipeline-owned memory ledger around the shared long arena and memo budget. The ledger reserves
actual block-capacity deltas before allocation and releases them only after cursor close resets the arena. Hash buckets
precompute array capacity, reserve entry plus capacity delta, and roll back on refusal. Published buckets replay from
their immutable value arrays without a second copy. Replay counts become long so zero-width buckets can emit beyond the
integer range. Hash capacity and arena capacity are released exactly once on every close or failure path.

Milestone five reuses the chunk substrate wherever its proof holds. Serial factorized aggregation calls
`tryOpenPrefix` with the tail budget. Parallel SELECT and aggregation use an external-root batch stage over morsel rows,
starting probe stages at depth one with merge/SIP disabled. Composite sources cache active children and return a probe
that retains and closes one child probe each. Existing feature flags and row-chain fallbacks remain authoritative.

Milestone six replaces ordered factorized top-K arena retention with a `TopKPayloadStore` indexed by live heap slot.
Admission returns the accepted slot; new slots reserve entry/capacity, replacements reuse the evicted slot and array,
and final expansion reads relative branch offsets from that slot. Full factorized sort continues using the arena.

Finally rerun each red unchanged, broaden to containing classes and modules, format, run the required root install,
benchmark the exact controls and new hot paths with matched JDK settings, and inspect the final diff against
`28293f90` without modifying or staging unrelated worktree files.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`.

Run the mandatory baseline install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
      | tee maven-build.log \
      | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Use the repository test runner for focused tests, never `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs

After a failing selection, capture its Surefire report into the repository-root `initial-evidence.txt` using
`scripts/agent-evidence.py` before a later run overwrites it. Existing untracked evidence belongs to another task, so
append this task's compact evidence only if ownership is confirmed; otherwise preserve the reports and record paths in
this plan.

Before finalization run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is the repository-mandated exception to the general ban on quiet test execution; it does not run
tests.

## Validation and Acceptance

Acceptance requires every new behavior-changing test to fail at `70183af857` and pass unchanged after its fix. Calendar
property tests cover every XML Schema calendar datatype, timezone absence, Z, +/-14:00, leap/fractional values, invalid
lexicals, Java/native sort agreement, and strict/standard queries in evaluation, LMDB, MemoryStore, and NativeStore.

OPTIONAL filters produce generic-equivalent rows while safe deterministic shapes still report factorized engagement.
Unmarked custom functions evaluate per joined solution and never enter a memoized placement. Weighted double 0.1 with
fanout six yields SUM 0.6 and AVG 0.09999999999999999; weighted float 0.1 with fanout seven yields SUM 0.70000005 and AVG
0.10000001. Parallel and ordered floating cases equal ordinary sequential execution. MIN/MAX over semantic aliases
return the same RDF term as the sequential first encounter. An exact query after fallback proves worker/source cleanup.

Hash tests prove early refusal, exact capacity charging, direct ordered replay, long count-only emission, and balanced
entry/arena/hash reservations on normal, early-close, switch, and failure paths. Top-K tests prove O(K) live payload
capacity through hundreds of replacements without losing engagement. Telemetry tests prove serial factorized and
parallel morsel prefixes use chunk execution only when eligible, and composite probe tests prove child-handle reuse and
single close ownership.

Focused tests, containing classes, `core/queryalgebra/evaluation`, and `core/sail/lmdb` finish with zero
Surefire/Failsafe failures; query-mode selections in `core/sail/memory` and `core/sail/nativerdf` also pass. JMH results
must use identical JDK, heap, warmup, measurement, and fork settings. Correctness fixes land regardless of speed;
optional path expansion remains enabled only with repeatable lower allocation or latency and no meaningful exact-path
regression. JFR supplies allocation evidence for replay, arena, probe, and top-K claims; no JIT claim is made without
compiler/profile evidence.

## Idempotence and Recovery

The install and test commands are safe to repeat. Do not use `git reset`, `git restore`, `git clean`, or manual stashing.
If another process changes an overlapping file, stop that fix, inspect the file-scoped diff, and preserve both authors'
work. If a production edit is accidentally made before its failing test is captured, revert only that task-owned hunk
with `apply_patch`, update this plan, and restart that milestone from the test.

## Artifacts and Notes

Keep full install output in `maven-build.log` and retained focused-test logs under `logs/mvnf`. Record compact failing and
passing Surefire snippets in this living plan as milestones complete. Do not delete any untracked artifact.

## Interfaces and Dependencies

No supported public API, configuration property, external dependency, or persistent format changes are permitted.
Reuse `MaskedFilter`, `NativeBooleanFilter`, `FactorizedTail.MemoBudget`, `GroupKey`, `NativeBatch`, and the existing
query expression tree. `CalendarSortKey` stays private to `ValueComparator`. `EncounterOrderFallback`, the pipeline
memory ledger, external-root stage, composite probe, and `TopKPayloadStore` remain package-private. Existing feature
flags govern new chunk/factorized/parallel use. Unknown custom functions intentionally remain unoptimized until a
separately reviewed affirmative public purity contract exists.

Revision note (2026-07-14): Initial plan created to implement the four P1 findings from the static review while
preserving concurrently developed query-evaluation-mode changes.

Revision note (2026-07-14): Recorded the successful mandatory root clean install before beginning regression tests.

Revision note (2026-07-14): Captured the pre-fix calendar comparator transitivity failure and appended it to the shared
root `initial-evidence.txt` without disturbing the concurrent query-mode evidence.

Revision note (2026-07-14): Captured all three LMDB pre-fix failures, refined the volatile test to be independent of
join order, and recorded the focused hash-budget harness that uses the existing package-visible abstractions.

Revision note (2026-07-14): Implemented the four fixes and recorded matching focused green runs: comparator
`logs/mvnf/20260714-201902-verify.log`, hash budget `logs/mvnf/20260714-201951-verify.log`, OPTIONAL scope
`logs/mvnf/20260714-202040-verify.log`, and volatile filters `logs/mvnf/20260714-202124-verify.log`.

Revision note (2026-07-14): Adversarial post-fix review exposed three remaining P1 gaps: pairwise calendar
totalization violates equality substitution across dateTime/dateTimeStamp, published hash buckets are copied into an
unbudgeted replay array and bucket growth reserves fewer values than it allocates, and custom functions without
affirmative purity metadata are still assumed repeatable. Captured the dateTimeStamp focused red in
`logs/mvnf/20260714-202805-verify.log`; focused regressions for the hash and function cases precede their refinements.

Revision note (2026-07-14): Expanded the ExecPlan after the user authorized the complete correctness, bounded-memory,
aggregate fallback, optimized-path coverage, and top-K work. Adopted committed baseline `70183af857`, recorded its green
30.211-second root install, superseded the incomplete pairwise calendar design, and specified all remaining milestones.

Revision note (2026-07-15): Rejected and surgically reverted the deferred coordinator-tail candidate experiment after
two matched JFR protocols regressed by 21.4% and 17.1%; the warmed three-fork candidate remained slower. Preserved all
prior correctness/accounting work and the zero-capacity memo retry fix.

Revision note (2026-07-15): Captured and fixed admission-before-reader and full-lifetime parallel snapshot regressions.
The producer preflight now runs only after task admission and before submission; real sibling families retain one
read-lock lease and validate `mdb_txn_id`, while composites reject differing known member epochs.

Revision note (2026-07-15): Closed the plan after final retained-probe reuse, one-active direct delegation,
hash-impossible setup trimming, and property-restoration fixes. Recorded the authoritative 1,810-test LMDB gate,
30.898-second final reactor install, matched benchmark/JFR outcomes, clean diff audit, and the evidence-backed decision
to keep external-root worker chunk execution experimental and default-off.
