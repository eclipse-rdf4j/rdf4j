# Role-Neutral Vertex Cohort Rescue for Omni Bridge Chains

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root. Keep this file self-contained enough that a new
agent can resume from it without prior chat context.

## Purpose / Big Picture

LMDB's Omni join estimator keeps compact witness samples for query cardinality estimation. Existing subject and object
cohort witnesses improve local overlap, but a value can switch role across a bridge: in `?a bridgeAB ?b . ?b p ?x`,
`?b` is first an object and then a subject. Role-salted subject/object cohorts do not naturally preserve that value
across the transition. This work adds a role-neutral vertex cohort so a sampled RDF value remains sampled regardless of
subject or object role, and it makes bridge traversal carry source-aware weighted witness frontiers instead of
unconditionally coercing them to base witnesses.

The existing `BASE` witness path is valid and already role-neutral by term hash. This change does not replace or
discard it. `VERTEX_COHORT` is an additional rescue source for cases where bottom-k/base witnesses lose the bridge
frontier and role-specific cohorts cannot help because their subject/object salts disagree across role changes.

The observable outcome is better Omni estimates for subject-star chains connected by predicate bridges. A focused
three-star bridge fixture must show no additional `SAMPLE_LOSS` diagnostic steps and lower q-error / absolute log error
with default cohorts than with `omniWitnessCohortBucketCount=0`.

## Progress

- [x] (2026-07-09 08:13+02:00) Ran mandatory root quick install before implementation edits.
- [x] (2026-07-09 08:16+02:00) Created this ExecPlan.
- [x] (2026-07-09 08:38+02:00) Added failing low-level `VERTEX_COHORT` tests.
- [x] (2026-07-09 08:42+02:00) Added failing three-star bridge accuracy regression.
- [x] (2026-07-09 10:10+02:00) Implemented vertex cohort storage, probing, intersections, and persistence.
- [x] (2026-07-09 10:36+02:00) Made bridge traversal and subject-star filtering source-aware while preserving `BASE`.
- [x] (2026-07-09 10:51+02:00) Preserved weighted star/bridge frontiers for the ordered connected path.
- [x] (2026-07-09 11:44+02:00) Ran focused red/green selectors, formatting, copyright check, diff check, and LMDB module verification.

## Surprises & Discoveries

- Observation: The branch already contains modified Omni cohort/config files before this implementation pass.
  Evidence: `git status --short --branch --untracked-files=no` showed modifications under
  `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch` and related config/tests. Treat these as existing
  user or prior-agent work; do not revert them.
- Observation: The existing ordered-connected diagnostic can report severe bridge-chain underestimation with retained
  witness count one and fallback `NONE`, not `SAMPLE_LOSS`.
  Evidence: the first red run of
  `SketchBasedJoinEstimatorOmniAccuracyRegressionTest#omniVertexCohortImprovesThreeStarBridgeAccuracy` showed
  disabled and enabled `connectedRows=40.0` for an expected 12000 rows, with `connectedQError=300.0` and
  `disabledSampleLoss=0, enabledSampleLoss=0`.
- Observation: The implementation must preserve `BASE` as a valid role-neutral candidate; the defect is unconditional
  base-only coercion when a compatible vertex-cohort alternative exists, not the existence of the base path itself.
  Evidence: user review on 2026-07-09 corrected the framing and explicitly requested candidate-preserving,
  source-aware bridge traversal.
- Observation: The three-star regression improved through a compatible vertex-cohort filter late in the chain rather
  than by replacing every bridge step with vertex cohorts.
  Evidence: the green diagnostic showed disabled `connectedRows=40.0` and enabled `connectedRows=3759.0` for a true
  12000 rows. Enabled steps kept `BASE` for ordinary bridge/fanout work and switched to `VERTEX_COHORT` where it
  preserved the sparse carried endpoint.
- Observation: Full `core/sail/lmdb` verification is red on broader planner assertion tests outside the focused vertex
  cohort surface.
  Evidence: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` ran 2025 tests and failed 8
  assertions, including `LmdbSparsePrefixCostTest`, `QueryBenchmarkTest`, and other planner-metric/shape checks.
  The focused new selectors, config tests, and Omni accuracy class passed.

## Decision Log

- Decision: Use the existing public `omniWitnessCohortBucketCount` and `omniWitnessCohortBucketIndex` settings for
  subject, object, and vertex cohorts.
  Rationale: The requested interface has one bucket pair, and one coordinated vertex sample is the key overlap
  improvement for bridge chains.
  Date/Author: 2026-07-09 / Codex.

- Decision: Defer a physical CSR/counting adjacency store.
  Rationale: The current `EDGE_FORWARD` and `EDGE_REVERSE` Omni relations already provide the adjacency-like witness
  lookup surface needed to prove the estimator invariant. A separate CSR layout should be benchmarked after the
  correctness model is stable.
  Date/Author: 2026-07-09 / Codex.

- Decision: Determine `VERTEX_COHORT` membership from the emitted/carried vertex witness key for each posting.
  Rationale: This makes an edge-forward object output and the next subject-star center use the same value hash, source,
  and bucket decision. A per-statement OR would mix populations and could carry values that are not actually selected
  for the next frontier.
  Date/Author: 2026-07-09 / Codex.

## Outcomes & Retrospective

Implemented. `VERTEX_COHORT` is appended to `OmniWitnessSet.SourceKind` and persists through byte-array and mapped
snapshots. Vertex cohort membership is computed from the emitted/carried witness key: subject for subject-star records,
object for forward-edge outputs, and subject for reverse-edge outputs. Subject/object cohorts remain separate and
`BASE` remains a valid candidate.

Bridge traversal now uses source-aware predicate following for future subject centers, while ordinary leaf fanout stays
on the base follow path. Subject-star filtering uses source-aware probes/intersections and preserves a compatible
non-base carried source when it remains clean and nonempty. Weighted frontiers remain weighted bags in the ordered
connected bridge path.

The focused low-level and three-star tests prove the intended rescue case. Broad LMDB verification still has unrelated
planner assertion failures in the current branch; those were not folded into this change.

## Context and Orientation

The relevant production code is in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch`.
`OmniWitnessSet` stores retained witness hashes, weights, sampling probability, estimated rows, confidence, fallback
reason, source kind, and alternative candidates. `OmniJoinEstimator` owns the in-memory and persisted witness indexes.
`SketchBasedJoinEstimator` builds those indexes during ingestion and consumes witness sets while estimating joins.
`PredicateSketchFamily` wraps predicate/context-specific edge lookups.

A witness frontier is a weighted bag of sampled keys. The same key may have weight greater than one because several
paths or statement multiplicities contribute to the same binding. For ordinary join cardinality, the estimator must not
collapse this to a distinct-key set unless it is deliberately estimating a distinct count.

Source kind means the sampling source that produced a witness alternative. Existing sources are `BASE`,
`SUBJECT_COHORT`, and `OBJECT_COHORT`. This plan appends `VERTEX_COHORT` and keeps source-specific alternatives
separate so each alternative has a valid sampling probability. Source kind alone is not enough for arbitrary
proportional math: a frontier also needs compatible witness population, sampled key kind, effective sampling
probability, and bag-versus-distinct semantics. This implementation only combines alternatives centrally when they
represent the same probed operation over the same carried key population.

## Plan of Work

First, add red tests in `OmniJoinEstimatorTest` for vertex cohort static probes, same-source intersection, bridge
following, byte-array round trip, mapped snapshot round trip, and configuration mismatch rejection. Include one
deterministic low-level fixture where base witnesses and role-specific cohorts lose an object-to-subject bridge, while
`VERTEX_COHORT` preserves it.

Second, add a red regression in `SketchBasedJoinEstimatorOmniAccuracyRegressionTest` for three subject-centered stars
connected by two bound-predicate bridges. The test must run the same query with cohorts disabled and enabled. It should
assert that enabled diagnostics do not increase `SAMPLE_LOSS` steps and that enabled q-error plus absolute log error
are lower. The explicit `SAMPLE_LOSS` recovery requirement is covered by a deterministic low-level vertex sparse-overlap
test because the full-query diagnostic can expose the current failure as a trusted but badly under-retained estimate.

Third, implement `VERTEX_COHORT`. Append it to `OmniWitnessSet.SourceKind`, update `isCohort()`, update candidate
selection to iterate all source kinds, and update `OmniWitnessAccumulator` to preserve vertex alternatives. In
`SketchBasedJoinEstimator`, compute vertex selection from the stable subject/object theta hashes using a new vertex seed
and the existing bucket count/index. Extend `OmniWitnessCohortSelection` and the `updateOmniStatic`,
`updateOmniPredicate`, and `updateOmniPredicateContext` helpers to carry a vertex flag chosen per relation update from
the emitted/carried witness key:

- subject-star witness: subject hash
- `EDGE_FORWARD` output witness: object hash
- `EDGE_REVERSE` output witness: subject hash

Do not use a broad per-statement vertex OR for these carried-key postings.

Fourth, store and load vertex cohort postings. Add a third cohort map in `OmniJoinEstimator.AttributeIndex`; update
write, snapshot, mapped load, byte-array load, and clear paths. Bump `OmniJoinEstimator.SERIAL_VERSION` and
`OmniWitnessPersistenceStore.MANIFEST_VERSION` so older snapshots rebuild instead of silently missing vertex postings.
Do not change the per-attribute mapped posting layout unless the existing layout cannot represent the new source.

Fifth, remove unconditional base-only bridge/filter coercion from the star/bridge paths while keeping `BASE` as a valid
candidate and fallback. `probeOmniBridgeWitnesses(...)` in `SketchBasedJoinEstimator` should call source-aware
`PredicateSketchFamily.follow(...)`. Subject-star filtering should probe source-aware witnesses and call
`OmniJoinEstimator.intersect(...)` instead of the base-only variants. Preserve weights in the bridge frontier path; do
not call `unitWeightOmniWitnesses(...)` there unless a separate distinct-key path requires it.

Finally, if the three-star bridge regression still depends on caller order, add an internal linear star-chain estimator
for subject-centered stars connected by bound bridges. Rank possible anchor stars by no fallback, retained witness count,
lower estimated rows, lower bridge fanout estimate, and original order. Traverse outward along the bridge chain with
source-aware weighted frontiers and ratio updates only between compatible frontier masses.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Initial build evidence already captured:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use focused red/green selectors:

    python3 .codex/skills/mvnf/scripts/mvnf.py OmniJoinEstimatorTest#vertexCohortBridgeFollowPreservesRoleNeutralSource --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py OmniJoinEstimatorTest#vertexCohortWitnessesSurviveByteArrayRoundTrip --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py OmniJoinEstimatorTest#mappedSnapshotKeepsVertexCohortWitnesses --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorOmniAccuracyRegressionTest#omniVertexCohortImprovesThreeStarBridgeAccuracy --retain-logs

After focused tests pass, run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    git diff --check
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Do not use `-am` or `-q` for tests.

## Validation and Acceptance

The low-level vertex tests must fail before production changes because `VERTEX_COHORT` does not exist and source-aware
bridge following cannot preserve it. They must pass after implementation and show a sampling probability of
`1 / bucketCount` for vertex alternatives.

The three-star bridge regression must fail before the bridge/filter implementation because default cohorts do not
improve the sparse bridge-chain fixture. It must pass after implementation by showing no additional `SAMPLE_LOSS`
diagnostics and lower q-error plus absolute log error with default cohorts than with `bucketCount=0`.

Existing subject/object cohort tests must continue to pass. Existing byte-array and mapped-snapshot tests must continue
to reject mismatched bucket settings.

## Idempotence and Recovery

The implementation is additive and can be retried. If a focused red test fails for the wrong reason, fix the test before
editing production code. If broad LMDB verification is red, compare failures against focused selectors and current
branch baseline notes before attributing them to this work. Do not delete or revert unrelated dirty files.

## Artifacts and Notes

Initial evidence is appended to `initial-evidence.txt`; full initial build output is in `maven-build.log`.
Retained focused verification logs are under `logs/mvnf/`. The broad LMDB run log is
`logs/mvnf/20260709-092500-verify.log`.

## Interfaces and Dependencies

No external dependencies are added. No public config keys are added. Existing public config controls vertex cohorts:
`withOmniWitnessCohortBucketCount(int)`, `withOmniWitnessCohortBucketIndex(int)`, and
`withOmniWitnessCohortBucket(int, int)`, plus the matching LMDB config/system-property/RDF fields already present in
this branch.
