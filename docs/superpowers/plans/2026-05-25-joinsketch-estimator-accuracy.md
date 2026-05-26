# Add JoinSketch Accuracy Evidence to LMDB Planning

This ExecPlan is a living document. It follows `.agent/PLANS.md`; keep `Progress`, `Surprises & Discoveries`,
`Decision Log`, and `Outcomes & Retrospective` updated as work proceeds.

## Purpose / Big Picture

LMDB Cascades needs estimator evidence that is stronger than scalar row counts and more robust under RDF skew. This
change adds a bounded, heap-only JoinSketch adapter to RDF4J's query-algebra evaluation module and compares it against
Fast-AGMS and existing ArrayOfDoubles tuple sketches on real `StatementPattern` objects. A human can see the behavior by
running the new unit test: JoinSketch should have lower relative error than the Fast-AGMS and tuple-sketch baselines for
subject-star joins and composite-key joins over generated RDF statements.

## Progress

- [x] (2026-05-25 20:12+02:00) Completed required quick install before tests.
- [ ] Add failing accuracy tests that reference the production JoinSketch adapter.
- [ ] Implement heap-only Fast-AGMS and JoinSketch adapters in `core/queryalgebra/evaluation`.
- [ ] Wire JoinSketch evidence into `SketchBasedJoinEstimator` for direct single/pair join estimates when the side
  sketch exists, falling back to tuple sketches otherwise.
- [ ] Run focused unit tests, then the queryalgebra sketch test set.

## Surprises & Discoveries

- Observation: The contributed JoinSketch patch targets Apache DataSketches and requires `Family.JOINSKETCH`, which is
  not available in the current `datasketches-java-9.0.0.jar`.
  Evidence: `javap org.apache.datasketches.common.Family` shows families up to `BLOOMFILTER`, no `FASTAGMS` or
  `JOINSKETCH`.

## Decision Log

- Decision: Implement RDF4J-local, heap-only sketch adapters instead of adding classes to `org.apache.datasketches`.
  Rationale: The current DataSketches dependency lacks the contributed family IDs, and planner-side estimates only need
  insertion-time updates and inner-product queries. Heap-only adapters avoid serialization API churn while preserving the
  estimator math needed by LMDB Cascades.
  Date/Author: 2026-05-25 / Codex.

## Outcomes & Retrospective

Pending.

## Context and Orientation

The existing estimator is `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`.
It builds `ArrayOfDoublesSketch` summaries for statement-pattern bindings and estimates joins by intersecting tuple
sketches. Tuple sketches are useful but lose multiplicity accuracy when keys are sampled away. Fast-AGMS is a signed
CountSketch inner-product estimator. JoinSketch extends that idea with exact heavy and middle components plus a signed
light component, which should be better on skewed RDF keys.

The contributed source lives under `fast-agms/joinsketch-v2-contribution.zip`. It cannot be used directly because it
modifies Apache DataSketches' `Family` enum. This plan uses the update and inner-product logic locally with RDF4J
headers and a narrow API.

## Plan of Work

First add a failing unit test in `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`.
The test creates RDF `Statement` data and real `StatementPattern` objects, computes exact join counts from the generated
statements, and compares three estimator families using the same keys: tuple sketches, Fast-AGMS, and JoinSketch.

Then add package-private adapters in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`:
`FastAgmsFrequencySketch` and `JoinFrequencySketch`. Both expose `update(long key, long weight)` and
`innerProduct(...)`. The JoinSketch adapter keeps bounded exact heavy/middle components and a CountSketch-style light
component.

Finally, add optional JoinSketch side state in `SketchBasedJoinEstimator` keyed by the existing resident sketch entry id.
When direct single/pair join estimates are available, use JoinSketch inner products before tuple-sketch intersections.
If a JoinSketch side summary is absent, fall back to the existing tuple-sketch path.

## Concrete Steps

From repository root, run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

Then add the failing test and run:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=SketchJoinSketchAccuracyComparisonTest verify

Expected before implementation: compilation failure or assertion failure proving the JoinSketch adapter is missing.

After implementation, rerun the same command and expect the new test class to pass. Then run:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=SketchBasedJoinEstimatorTupleSketchJoinAccuracyTest,SketchBasedJoinEstimatorCrossComponentJoinRegressionTest,SketchJoinSketchAccuracyComparisonTest verify

## Validation and Acceptance

Acceptance is the new unit test proving that, for the generated star and composite-key statement-pattern joins,
JoinSketch has lower relative error than both Fast-AGMS and tuple sketches at the configured small memory budget, and
`SketchBasedJoinEstimator` can use JoinSketch evidence for direct join estimates without breaking existing tuple-sketch
accuracy tests.

## Idempotence and Recovery

The tests generate their own in-memory statements and do not mutate external stores. If the JoinSketch adapter performs
worse for a shape, keep the failing test and adjust the sketch parameters or integration logic rather than weakening the
assertion. Existing untracked artifacts such as `fast-agms/`, `logs/`, and `initial-evidence.txt` must remain in place.

## Artifacts and Notes

The initial required quick install completed successfully:

    [INFO] RDF4J: LmdbStore ................................... SUCCESS [  4.267 s]
    [INFO] RDF4J: Query algebra - evaluation .................. SUCCESS [  2.323 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  27.695 s (Wall Clock)

## Interfaces and Dependencies

Add package-private classes in `org.eclipse.rdf4j.query.algebra.evaluation.sketch`:

    interface FrequencySketch {
        void update(long key, long weight);
        double innerProduct(FrequencySketch other);
    }

    final class FastAgmsFrequencySketch implements FrequencySketch { ... }
    final class JoinFrequencySketch implements FrequencySketch { ... }

Use `org.apache.datasketches.hash.MurmurHash3`, already present through the existing DataSketches dependency. Do not add
a new Maven dependency for this slice.
