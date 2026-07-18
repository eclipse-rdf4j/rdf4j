# Add live adaptive filter placement to the LMDB native row engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while work proceeds. Maintain it in accordance with
`.agent/PLANS.md` at the repository root.


## Purpose / Big Picture

The LMDB native evaluator currently fixes every filter at one join depth before a query starts. That can be
expensive when the estimates used for placement do not match the rows seen by one execution. After this work, one
safe filter in an ordinary sequential `MultiJoinPlan` can begin at its deepest legal position and move between
pre-installed gates while rows stream. The query is never sampled, restarted, or speculatively executed before the
first result. A movement becomes visible only after the complete descendant subtree of the current earliest-legal
prefix has drained, preserving the exact result sequence and multiplicity.

The feature is observable through executed-query telemetry and through deterministic tests comparing the adaptive
and static cursor chains. Batch, ordered, factorized, parallel, aggregate-specialized, correlated, native-`EXISTS`,
hash, and merge execution remain static. The final release state enables
`rdf4j.lmdb.adaptiveFilterPlacement.enabled` by default; the property can be set to `false` for an identity fallback.


## Progress

- [x] (2026-07-18 16:42Z) Read repository, performance, and test-runner instructions; inspect dispatch, cursor,
  recording-filter, lease, and ordered-plan code.
- [x] (2026-07-18 16:42Z) Confirm the existing dirty tree does not overlap the adaptive implementation files and
  complete the required root quick install successfully.
- [x] (2026-07-18 16:55Z) Add and observe focused failures for envelope ordering and metadata classification;
  preserve both reports in `initial-evidence.txt`.
- [x] (2026-07-18 17:00Z) Implement relocation metadata, cost classification, compiler-only propagation, and
  bound-mask-specific non-crossing envelopes; verify the legacy constructor and candidate cap.
- [x] (2026-07-18 17:22Z) Add and observe focused gate, boundary, admitted-chain, outward/inward policy,
  cooldown, setup-failure, and feedback-suppression failures before their production slices.
- [x] (2026-07-18 17:22Z) Implement the adaptive session, cursor chain, controller, ownership, telemetry, and final
  fallback dispatch without modifying `MultiJoinPlan.open()` or the ordinary `JoinCursor`.
- [x] (2026-07-18 17:53Z) Add an allocation-free metadata-ineligible admission decline before order derivation;
  preserve the focused failure and verify the complete 15-test adaptive class.
- [x] (2026-07-18 17:53Z) Add matched JMH admission and phased-policy workloads with setup-time checksum and policy
  assertions; complete one-fork discovery smokes for every state.
- [x] (2026-07-18 19:08Z) Run the default-on LMDB module verify: 1,921 tests, zero failures/errors,
  three skips; validate formatting/copyright and complete the final root quick install.
- [x] (2026-07-18 19:09Z) Pass five-fork JMH throughput/allocation gates and a 100-second JFR profile, then
  add a failing unset-property test and enable the feature by default.


## Surprises & Discoveries

- Observation: The requested dispatch seam already exists in `NativeRowsIteration.initialize()`: after ordered,
  batch, parallel, and factorized choices decline, the code calls `step.arg.open(row)` and records `nestedLoop`.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java`.
- Observation: Uncorrelated `NativeBareRowsStep` evaluations delegate to their bulk `NativeRowsStep`; non-empty
  input bindings use the bare cursor. Therefore the final bulk fallback covers the requested uncorrelated scope
  without changing native `EXISTS` or correlated bare evaluation.
  Evidence: `NativeBareRowsStep.evaluate()` in `LmdbNativeRowStep.java`.
- Observation: `JoinCursor` requests a new left row only after the current right cursor or replay has exhausted.
  A wrapper around the earliest legal left-prefix cursor is therefore a drained-subtree movement boundary.
  Evidence: `JoinCursor.next()` and `JoinCursor.replayNext()` in `LmdbNativeJoinPlans.java`.
- Observation: `Function.Determinism.DETERMINISTIC` already promises stable values, errors, and observable effects.
  The existing preparation-scoped repeatability classifier is suitable for relocation once `EXISTS` is separately
  rejected, and the result does not depend on STANDARD versus STRICT comparison mode.
  Evidence: `Function.java` and `QueryEvaluationUtility.isRepeatableWithinPreparation()`.
- Observation: `MaskedFilter` is created from a `RecordingNativeBooleanFilter` at the flattening seam and is cloned
  by `NativeFilterLease`; copying metadata at both sites retains plan-local identity without changing the ordinary
  filter-call interface.
  Evidence: focused `filterMetadataClassifiesCostSafetyAndEvidence` and `envelopePreservesFilterOrder` tests pass.
- Observation: Default-on ineligible admission must reject from metadata before deriving a bound-mask order or
  allocating prefix estimates. A focused estimate tripwire failed on the old ordering and now passes.
  Evidence: `metadataIneligiblePlanDeclinesBeforeEstimation` and its preserved failure in `initial-evidence.txt`.
- Observation: One-fork discovery smokes exercised all matched JMH states. The three adaptive policy states retained
  exact checksums and completed the required outward commit, later inward commit, and inward rollback transitions;
  their smoke timings were 67--89% below their workload-specific fixed controls. These are setup validation only,
  not the final five-fork performance evidence.
- Observation: Five-fork measurements retained the no-filter disabled hot path: the enabled median was 0.25% lower,
  and the enabled 99.9% score-confidence upper bound was 0.53% above the disabled mean. Normalized allocation was
  1,120.208 B/op enabled versus 1,120.209 B/op disabled.
  Evidence: `profiles/lmdb-adaptive/no-op-5fork.json` and `no-op-gc-5fork.json`.
- Observation: Five-fork adaptive medians improved 86.44% for outward commit, 67.53% for later inward commit, and
  88.76% for the distribution-shift rollback workload. Setup asserts exact checksums and the intended final depth,
  commits, and rollbacks for every state; stable commit workloads have no rollback or oscillation and the explicit
  shift workload performs one intended rollback.
  Evidence: `profiles/lmdb-adaptive/phased-5fork.json`.
- Observation: The macOS JFR profile contains 8,743 execution samples and allocation events over 100 seconds. The
  adaptive controller uses only invocation-local primitive fields/arrays; no shared atomic appears in its code.
  Evidence: `profiles/lmdb-adaptive/adaptive-later-inward.jfr`.


## Decision Log

- Decision: Keep `MultiJoinPlan.open()` and `JoinCursor` byte-for-byte on the static path; implement a separate
  `AdaptiveJoinCursor` for observed adaptive joins.
  Rationale: Disabled and ineligible queries must not acquire a per-row observer branch or wrapper allocation.
  Date/Author: 2026-07-18 / Codex.
- Decision: Double the decision interval only when the active depth changes. Candidate installation and rollback
  double it; commit does not.
  Rationale: This follows the explicit active-depth invariant and resolves the source design's contradictory commit
  wording.
  Date/Author: 2026-07-18 / Codex.
- Decision: Filters with finite `offset + limit < 256` decline admission.
  Rationale: Such invocations cannot provide the minimum trial evidence and deepest-first execution could add
  latency without an opportunity to learn.
  Date/Author: 2026-07-18 / Codex.
- Decision: Attach adaptive metadata to `RecordingNativeBooleanFilter`, then copy it into `MaskedFilter` when a
  flattenable filter enters a `MultiJoinPlan`; preserve the two-argument `MaskedFilter` constructor as ineligible.
  Rationale: `recordFilterOutcomes(Filter, NativeBooleanFilter)` is the one compiler seam retaining the algebra
  `Filter`, planned selectivity, evidence, and condition tree. This avoids another predicate wrapper in the hot loop.
  Date/Author: 2026-07-18 / Codex.
- Decision: Adaptive execution borrows the complete direct bag through `NativeFilterLease` in suppress-feedback
  mode and never falls back after adaptive setup begins.
  Rationale: The final fallback is not speculative. One lease gives close-once ownership and prevents placement-
  contaminated pass/fail statistics without reopening or duplicating filter state.
  Date/Author: 2026-07-18 / Codex.
- Decision: Enable `rdf4j.lmdb.adaptiveFilterPlacement.enabled` when the property is unset after the focused,
  module, five-fork no-op/allocation, phased-policy, and JFR gates passed.
  Rationale: Default-on admission is allocation-free for disabled/ineligible paths, the no-filter regression gate
  passed, adaptive workloads exceeded the required improvement, and callers retain an explicit `false` escape hatch.
  Date/Author: 2026-07-18 / Codex.


## Outcomes & Retrospective

Runtime admission, adaptive execution, policy, ownership, feedback suppression, fallback dispatch, and bounded
telemetry are implemented. The complete focused class passes 16 tests, including randomized exact sequence and
multiplicity checks for two-to-eight-child plans, lifecycle failures, and the default-on contract. The full LMDB
module passes 1,921 tests with zero failures/errors and three skips. Five-fork no-op throughput/allocation gates,
three checksum-guarded adaptive workloads, and a 100-second JFR profile passed. The feature is default-on and can be
disabled with `-Drdf4j.lmdb.adaptiveFilterPlacement.enabled=false`. No public API or dependency was added.


## Context and Orientation

The work is confined to `core/sail/lmdb`. `SlotPlan` is the internal physical-plan interface. A direct
`MultiJoinPlan` contains flattened inner-join children and `MaskedFilter` objects. `MultiJoinPlan.derive()` caches an
`OrderedPlan` for each set of slots bound at cursor-open time; today that plan stores the child order and one static
depth per filter. `MultiJoinPlan.openChain()` creates the static nested-loop chain by alternating `JoinCursor` and
`FilterCursor` wrappers.

A `RowCursor` mutates one execution-local `RowState` and returns `true` for each current mapping. `JoinCursor` drains
all matches below one left row before asking its left cursor for another row. A “prefix” in this plan means one row
returned by the cursor immediately after the target filter's earliest legal child depth and after same-depth filters
that precede the target. A “gate” is a non-owning row wrapper installed at one candidate depth. Passive gates count
arrivals and pass rows through; the single active gate evaluates the target filter.

`RecordingNativeBooleanFilter` aggregates outcomes for persistent optimizer feedback and publishes them on close.
`NativeFilterLease` already creates attempt-local facades and owns close/commit/discard for speculative native
strategies. Adaptive execution must add a feedback-suppression policy: used filters are still closed, but every
recording wrapper discards outcomes before close.

The relevant production files are:

    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregatePlannerBase.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFilters.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowPlans.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeJoinPlans.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeFilterLease.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeExplain.java

Put the adaptive implementation in a new package-private
`LmdbNativeAdaptiveFilterPlacement.java` file in the same package. New Java files use the exact 2026 Eclipse header
and `// Some portions generated by Codex` immediately below it.


## Plan of Work

First add metadata and envelope tests. Introduce `AdaptiveFilterMetadata` with plan-local ID, required mask, cost
units, planned pass ratio, evidence count, eligibility, and decline reason. The compiler increments an ID counter
when it wraps an algebra filter for feedback. Cost units are `CHEAP=1`, `MEDIUM=4`, `EXPENSIVE=16`, with composite
costs summed and capped at 64. Raw-ID, `BOUND`, and simple constant-membership shapes are cheap; comparisons,
arithmetic, casts, and logical compositions are medium; regex, generic predicates, and deterministic extension
functions are expensive. Relocation requires a nonnegative complete mask, no `EXISTS`, and
`QueryEvaluationUtility.isRepeatableWithinPreparation(condition)`.

Copy metadata from the recording filter into each `MaskedFilter`. The existing constructor creates explicitly
ineligible metadata so hand-built plans and non-algebra conditions stay static. Extend `OrderedPlan` with one
`FilterPlacementEnvelope` per filter. Its candidate depths start at the existing earliest static depth. For each
later depth, compare the target `(candidateDepth, targetIndex)` with every other filter's
`(staticDepth, otherIndex)`; include it only while every comparison has the same sign as at the target's static
placement. This preserves the complete filter order, including filters sharing a depth. More than eight candidates
declines admission.

Next test and implement adaptive chain construction. `LmdbNativeAdaptiveFilterPlacement.tryOpen(NativeRowsStep,
MultiJoinPlan, RowState)` performs a bounded allocation-free disabled check, rejects correlation, ordered scans,
short finite limits, small work, cheap filters, insufficient estimated evaluations, unsafe fan-out, and invalid
envelopes, then chooses one target. Estimate conditional pattern fan-out with `PatternPlan.estimateForBoundMask()`
and other child estimates without LMDB I/O. Minimums are 100,000 work units, 4,096 static evaluations, cost 4, and
two candidates; maximum initial envelope fan-out is 16,384. Score is
`costUnits * staticEvaluations * alternativeCount * uncertainty`, where uncertainty is 2 without evidence and
`1 + 64/(evidence + 64)` otherwise. Lowest filter ID wins ties.

Borrow the admitted plan through a suppressing `NativeFilterLease`. Add `AdaptiveFilterSession`, immutable
`PlacementState`, `AdaptiveFilterGateCursor`, `AdaptivePrefixBoundaryCursor`, `AdaptiveJoinCursor`, and an owning
top-level cursor. Build filters in array-index order. At the earliest depth, insert the boundary immediately before
the target slot, after preceding same-depth filters. Insert a target gate at every candidate depth. Use the separate
observed join cursor only for joins strictly after the earliest depth through the deepest depth. Gates never close
the target; the session closes its facade once and the lease closes the underlying filter once.

Implement the controller with primitive arrays and longs. Prefix completions drive windows: initial 1,024 and
maximum 65,536. Work costs are 8 per right open, 1 per produced join row, replay, and materialization, and the
metadata cost per target evaluation. Outward proposals require at least 20% rejection or 25% filter-work share and
choose the greatest positive projected pruning saving after extra evaluations. Inward proposals require at least
90% pass and a later passive population no greater than 50% of current evaluations; choose the largest predicted
evaluation saving. One move is pending at a time and one immutable state assignment installs it at the boundary.

Every move is a trial against the previous proven window. A trial can request rollback after 128 prefixes when
normalized work is over 1.25 times baseline and absolute excess exceeds 2,048. A prefix exceeding 16,384 work units
requests rollback, or an emergency earliest-depth trial when there is no previous placement. At the trial interval,
commit only for at least 15% normalized and 2,048 absolute savings; otherwise roll back. Active-depth changes double
the interval. Commit does not. Commit and rollback start one cooldown window; rollback blacklists the candidate for
four current intervals.

Extend explain helpers to publish bounded successful-attempt metrics. Booleans are long 0/1 values; counters and
depths are long metrics; the decision reason and the maximum-eight candidate arrays are strings. A `next()` failure
marks the owner failed, so close suppresses adaptive telemetry as well as feedback. Natural exhaustion, LIMIT, and
early caller close publish telemetry while still suppressing persistent filter outcomes.

Finally insert `tryOpen` only before the last nested-loop fallback in `NativeRowsIteration.initialize()`. A null
result executes the existing `step.arg.open(row)` and existing `nestedLoop` path unchanged. Keep the feature default
off while developing and flip the property default to true only after all correctness and performance gates below
pass.


## Concrete Steps

Work from the repository root. Before every behavior-changing production slice, add and run the smallest failing
test with `mvnf`; never use `-am` or `-q` for tests. Preserve the first failing report immediately:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAdaptiveFilterPlacementTest#envelopePreservesFilterOrder --retain-logs
    python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAdaptiveFilterPlacementTest#envelopePreservesFilterOrder" core/sail/lmdb/target/surefire-reports > initial-evidence.txt

Proceed through focused methods for metadata/envelopes, gates/boundaries, policy, lifecycle/feedback, dispatch, and
telemetry. After focused green runs:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAdaptiveFilterPlacementTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAdaptiveFilterPlacementIntegrationTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before final verification, run the copyright check and formatter, then the root quick install. The repository's
formatter command intentionally uses `-q`; the prohibition applies to tests, not formatting:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Add a JMH class with paired `enabled` and `disabled` parameters and checksum validation. Use the supported wrapper:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAdaptiveFilterPlacementBenchmark --method noOp --forks 5 --warmup-iterations 4 --measurement-iterations 10
    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAdaptiveFilterPlacementBenchmark --method adaptiveWorkload --forks 5 --warmup-iterations 4 --measurement-iterations 10 --enable-jfr --enable-jfr-cpu-times


## Validation and Acceptance

Focused tests must prove mode-independent legality; independent envelopes for different entry masks; no filter
crossing; maximum candidate decline; deepest-first start; a passive gate at every candidate; exactly one evaluator;
same-depth order; moves only after drained prefixes; exact sequence and multiplicity; outward/inward commit and
rollback; interval, cooldown, blacklist, and hard-prefix behavior; one selected filter; feedback suppression; and
close-once ownership under exhaustion, errors, cancellation, setup failure, and early close.

Integration tests must prove disabled, ineligible, correlated, ordered, batch, parallel, factorized, aggregation,
and native-`EXISTS` identity; admitted execution and explain metrics; and exact results against both static native
execution and generic evaluation. Seeded differential tests cover two to eight children, duplicates, empty inputs,
one-to-many and many-to-many joins, several fixed filters, and varying masks/costs.

No-op benchmark median regression must be at most 1%, with p95 confidence upper bound below 3%, no per-row
allocation increase, and no shared atomic operation. Three adaptive workloads—outward win, later inward win,
and distribution-shift inward rollback—must preserve checksums and improve median time by at least 10% where a move
commits. A deterministic representative policy corpus must show rollback below 1%, false commits below 2%, zero
oscillation, and zero cleanup failures. Do not enable the default until these gates pass.


## Idempotence and Recovery

All tests, builds, and benchmarks are safe to rerun. Never delete untracked artifacts. If a focused test uncovers a
design mismatch, update this ExecPlan and its Decision Log before changing direction. If adaptive setup throws, close
the partial chain, session, and lease while preserving suppressed failures, then propagate the original error; do not
restart the query. If a performance gate fails, keep the property development-default false, profile the exact
workload, fix the root cause, and rerun the paired measurement.


## Artifacts and Notes

The starting root quick install completed successfully on 2026-07-18. Its output is in `maven-build.log`. The first
focused failing report and later compact report summaries belong in `initial-evidence.txt` and this section. Do not
replace or remove the existing unrelated modified benchmark/slot-order files or untracked zip artifact.

Final verification artifacts:

    logs/mvnf/20260718-184346-verify.log
    profiles/lmdb-adaptive/no-op-5fork.json
    profiles/lmdb-adaptive/ineligible-5fork.json
    profiles/lmdb-adaptive/no-op-gc-5fork.json
    profiles/lmdb-adaptive/phased-5fork.json
    profiles/lmdb-adaptive/adaptive-later-inward.jfr

The final root quick install completed in 34.708 seconds. The initial failure for the unset-property default contract
and all earlier TDD failures remain preserved in `initial-evidence.txt`.


## Interfaces and Dependencies

No public Java API and no dependency are added. The sole external control is the existing-style system property:

    rdf4j.lmdb.adaptiveFilterPlacement.enabled=true|false

The package-private implementation must provide these final shapes:

    final class LmdbNativeAdaptiveFilterPlacement {
        static RowCursor tryOpen(NativeRowsStep step, MultiJoinPlan plan, RowState row) throws IOException;
    }

    record PlacementState(long generation, int activeDepth) {}


Revision note (2026-07-18 16:55Z): Recorded the completed metadata TDD evidence and the remaining envelope coverage
after the first production slice.

Revision note (2026-07-18 17:00Z): Closed the metadata/envelope milestone after the four-method focused class passed;
the active milestone is now gate, boundary, and controller construction.

Revision note (2026-07-18 17:22Z): Recorded working deepest-first gates, drained movement, outward/inward trials,
cooldown/blacklist policy, and complete-bag suppressing lease ownership. Final fallback dispatch and telemetry are the
active slice.

    final class FilterPlacementEnvelope {
        final int filterId;
        final int earliestLegalDepth;
        final int deepestLegalDepth;
        final int[] candidateDepths;
    }

`OrderedPlan` retains its existing `order` and `filterDepth` fields and adds the envelope array. Static callers do
not read it. `NativeFilterLease` retains its existing default publishing behavior for all callers and adds an
explicit suppress-feedback construction mode used only by adaptive execution.


Revision note (2026-07-18): Created the initial self-contained ExecPlan from the approved implementation plan and
repository inspection. No production code has been changed.

Revision note (2026-07-18 19:09Z): Recorded final default-on TDD, module, root-build, five-fork throughput/allocation,
and JFR evidence; closed the implementation plan.
