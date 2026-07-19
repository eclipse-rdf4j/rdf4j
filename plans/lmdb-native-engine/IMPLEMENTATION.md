# Complete the LMDB native query engine program

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`,
and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance
with `.agent/PLANS.md` and implements the checked-in workstream specifications
`plans/lmdb-native-engine/00-overview.md` through `plans/lmdb-native-engine/13-verification.md`.

## Purpose / Big Picture

The LMDB query evaluator will choose compatible execution mechanisms by measured cost, compose filtering,
batching, parallelism, factorization, ordering, joins, aggregation, and paths where semantics allow, and
bound every query-side allocation through one memory authority. Completion is observable through the
fixed equivalence, dispatch, snapshot, lifecycle, and benchmark gates in `13-verification.md`, with every
program-level target recorded in `ACCEPTANCE.md` and every behavior-risk switch recorded in `ROLLOUT.md`.

## Progress

- [x] (2026-07-19 20:41Z) Read `.agent/PLANS.md` and all fourteen workstream documents completely.
- [x] (2026-07-19 20:41Z) Ran the mandatory root `-Pquick clean install`; all reactor modules succeeded.
- [x] (2026-07-19 20:48Z) Audited plan-defining symbols against the branch; the workstreams remain open.
- [ ] Restore the pre-existing repeated-probe factorized-admission regression found by the initial module verify.
- [ ] Build the continuous verification corpus and freeze the LMDB compliance baseline.
- [ ] Complete Phase I: 01§1-2, 02§1-3, 03§1-2, 04§1, 07§1-3.
- [ ] Complete Phase II: 01§3-4, 02§4, 04§2-3, 05§1-3, 06§1-2, 07§4-5, 10§1-2.
- [ ] Complete Phase III: 04§4-5, 05§4-5, 06§3-5, 08, 09§1-3, 10§3-5, 11, 12.
- [ ] Complete Phase IV: 09§4-5 and finish wiring the global memory authority.
- [ ] Run the program exit review and close every acceptance and rollout ledger entry.

## Surprises & Discoveries

- Observation: The initial repository-wide quick build succeeds on the active JDK 25 runtime.
  Evidence: `maven-build.log` ends with `BUILD SUCCESS` after all reactor modules, including LmdbStore.
- Observation: The workstream documents describe the current branch accurately; marker audits found the
  old all-or-nothing task reservation, global slot-count-only specialization cache, factorized-only
  multiplicity, dead CSR zone-map fields, and no acceptance or rollout ledgers.
  Evidence: `rg` matches remain at `LmdbNativeParallelPipelines.tryReserveTasks(int)`,
  `LmdbNativeSpecialization.CACHE`, and `LmdbNativeFactorizedRows.ordinarySuffixOnly`, while searches for
  `StrategyProposal`, `LmdbFanOutStats`, and `LmdbQueryMemoryManager` return no production definitions.
- Observation: The initial full `core/sail/lmdb` verify has one ordinary suite failure in the in-flight
  runtime-admission work: 1,024 completed paid probes do not admit the expected bounded factorized trial.
  Evidence: `initial-evidence.txt` preserves the failure from
  `LmdbNativeChunkHashBuildTest.factorizedAdmissionCountsRepeatedCompletedProbesTowardTheObservationFloor`.

## Decision Log

- Decision: Execute the checked-in phase graph exactly, while constructing plan 13's harness before the
  first behavior-changing production milestone.
  Rationale: The harness is explicitly a Phase I prerequisite and is the only reliable way to detect
  semantic or dispatch drift across this unusually broad engine program.
  Date/Author: 2026-07-19 / Codex.
- Decision: Keep the sketch estimator and cascades-style planner code untouched.
  Rationale: `00-overview.md` makes this a binding scope exclusion; all costing and physical decisions
  are runtime-side.
  Date/Author: 2026-07-19 / Codex.
- Decision: Preserve the correctness-required exclusions enumerated in `00-overview.md`.
  Rationale: Worker merge/SIP restrictions, ordered DISTINCT adjacency, factorized key stability, GRAPH
  path exclusions, and related gates have verified semantic foundations and are not optimization gaps.
  Date/Author: 2026-07-19 / Codex.

## Outcomes & Retrospective

The program is in progress. The repository starts from a clean successful quick build, and the living
tracking artifacts now exist. Outcomes will be updated at every completed phase and at program exit.

## Context and Orientation

The implementation lives primarily in `core/sail/lmdb`. Native query planning and execution are under
`src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`; LMDB storage, CSR adjacency, value encoding, and
dictionary access are under `src/main/java/org/eclipse/rdf4j/sail/lmdb`. Tests and JMH benchmarks are
under the corresponding `src/test/java` tree. `00-overview.md` defines the immutable scope and dependency
graph. Each numbered workstream defines exact class and method anchors, soundness constraints, focused
tests, and measurable acceptance criteria. `13-verification.md` defines the cross-cutting gates.

An execution path is the physical strategy that produces a query fragment, such as nested-loop, batch
hash join, parallel pipeline, or factorized rows. A proposal is an admissible strategy paired with an
estimated cost and a deferred cursor opener. A CSR adjacency entry stores predicate-specific keys,
neighbors, and prefix offsets in flat arrays. A query memory reservation is a byte-accounted claim that
must spill, shed, or fall back immediately when refused.

## Plan of Work

First create the equivalence, dispatch, snapshot, lifecycle, and benchmark harnesses and record the
pre-existing compliance baseline. Then complete Phase I in work-item order: execution telemetry and
fan-out statistics; filter unification, batch-join filters, and decode-free comparisons; tuple-table
counter cleanup and batched probes; elastic task admission; CSR zone-map, degree, seek, ordered access,
and SIP sourcing. Each behavior-changing item begins with its smallest focused repository test and is
validated before the next dependent item.

Phase II introduces proposal-based cost arbitration, slice and top-K changes, adaptive filter
decoration, worker batching, ordered producer reuse, live sort layouts, base-cursor multiplicity, cache
policy, and budgeted primitive group tables. Phase III adds parallel sort/merge/build, radix and
order-aware execution, deeper factorization, range and encoding work, joins, aggregation coverage,
paths, and memory/dictionary batching. Phase IV adds leapfrog intersection, outer accumulation, and
completes global budget wiring. The final pass reruns every gate and audits the interaction matrix.

## Concrete Steps

Work from the repository root. Before test work, refresh the workspace-local Maven repository with the
mandatory root quick install already recorded above. Run focused and module tests through:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run a single benchmark through:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class <fqcn> --method <method>

Run plan snapshots with the repository's query-plan snapshot CLI workflow and compare JMH results with
the checked-in JMH comparison tool. Never use Maven `-am` or `-q` for tests. All Maven invocations use
`-Dmaven.repo.local=.m2_repo`, and offline mode is preferred.

## Validation and Acceptance

Every focused correctness test and the full `core/sail/lmdb` module suite must pass, with the frozen
LMDB-only compliance baseline not growing. Equivalence corpora compare bags, and ORDER BY corpora compare
sequences modulo equal-key ties. Dispatch tests assert winning strategy and decline reasons. Snapshot
diffs are restricted to declared target shapes. Lifecycle tests prove cancellation, snapshot isolation,
and zero memory-ledger leakage. Each performance claim requires paired JMH runs with a three-percent
noise floor; profile claims require JFR or equivalent evidence on the active JDK. Program completion
requires every row in `ACCEPTANCE.md` to be closed and every switch in `ROLLOUT.md` to have a measured
decision.

## Idempotence and Recovery

All builds, tests, snapshots, and benchmarks are repeatable. Test artifacts and logs are retained; no
untracked artifact is deleted. Each feature has a correct fallback on budget refusal, unsupported shape,
or disabled rollout switch. If a focused test exposes a regression, keep the failing test, revert only
the current surgical production edit, update this plan's discovery and decision logs, and resume from
the root cause. Persist benchmark inputs and compare runs rather than replacing them.

## Artifacts and Notes

The initial build transcript is `maven-build.log`. Compact verification evidence is persisted in
`initial-evidence.txt` when the first verify selection runs. Performance results, JFR recordings, plan
snapshots, and the closing interaction-matrix review remain checked-in or linked from `ACCEPTANCE.md`.

## Interfaces and Dependencies

The required end-state interfaces and types are specified in the numbered workstreams. Central additions
include `LmdbFanOutStats`, proposal-based dispatch carrying opener/cost/tag, vectorized
`NativeBooleanFilter.selectBatch`, elastic task reservations that report granted workers, base
`RowCursor.multiplicity`, range-aware pattern/source APIs, and `LmdbQueryMemoryManager` reservations with
reclaimable spill or shed behavior. No sketch-estimator dependency is introduced. New third-party
dependencies are not expected; existing primitive arrays, LMDB APIs, and repository utilities are used.

Revision note (2026-07-19, Codex): Created the program-level living ExecPlan because the numbered
workstreams are immutable implementation specifications and lacked the mandatory progress, discovery,
decision, and retrospective sections needed for restartable execution.
