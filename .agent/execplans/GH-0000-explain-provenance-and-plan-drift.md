# Fix LMDB benchmark explain provenance and plan drift

This ExecPlan is a living document and follows `.agent/PLANS.md`. It records the implementation of the user-requested fix for the LMDB theme benchmark explanation output. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current as work proceeds.

## Purpose / Big Picture

The LMDB theme benchmark currently prepares one optimized tuple expression for the timed query, then renders a different plan by calling `explain(Optimized)` again after the trial. Adaptive estimator feedback can make those two plans differ, so telemetry, rendered SPARQL, and benchmark timings appear inconsistent. After this change, the benchmark artifacts will identify whether they came from the prepared plan or a telemetry explanation, and the rendered prepared query will be generated from the exact tuple expression used to build the evaluation step.

The benchmark will also support a fixed plan lifecycle for repeatable timing experiments. The existing per-invocation replanning behavior remains available as an explicit adaptive mode. No public `Query.explain()` API or optimizer selection rule is changed.

## Progress

- [x] (2026-08-03) Inspected the benchmark, prepared-plan helper, explanation API, and LMDB optimizer diagnostics.
- [x] (2026-08-03) Add failing tests for prepared/telemetry provenance and lifecycle selection.
- [x] (2026-08-03) Add immutable prepared-plan snapshots and stable structural fingerprints.
- [x] (2026-08-03) Separate prepared rendering and telemetry rendering in benchmark teardown.
- [x] (2026-08-03) Add fixed/adaptive lifecycle controls and plan-drift logging.
- [x] (2026-08-03) Classify predicate-range diagnostic outcomes without changing costing.
- [x] (2026-08-03) Run focused tests, benchmark repetitions, formatting, and final audit.

## Surprises & Discoveries

- `SailQuery.explain(Level)` clones the parsed tuple expression on every call, and `SailSourceConnection` optimizes that clone again. Therefore a teardown call to `explain(Optimized)` cannot be the provenance of the tuple expression prepared earlier by `LmdbBenchmarkQueryPlan`.
- The current benchmark uses `@Setup(Level.Invocation)` for `ExecutionState.prepareQuery()`, so plan selection can change between warmup and measurement as LEO and operator feedback revisions advance.
- The current logging hashes `Explanation.toString()`, which includes phase-dependent telemetry and actual metrics. A stable plan identity must hash structural rendering instead of the full explanation text.
- Predicate-range alternatives are generated as costed alternatives and the original filter remains legal. A selected-plan-only diagnostic cannot distinguish candidate generation from candidate selection, so the diagnostic reason needs explicit candidate state.
- The first current-source JMH run initially failed before launch in the restricted sandbox because forked JMH needs a local ServerSocket; the supported benchmark command succeeded once local fork permission was granted.
- Independent benchmark invocations can begin with different learned-estimator state. The earlier pair produced `fce1cb7a...` and `595c4054...`; the final pair started from the same state and kept `595c4054...` stable in both repetitions. Fixed mode controls within-trial drift; it does not reset persisted estimator learning between processes.

## Decision Log

- Decision: Keep the public explanation API and optimizer selection semantics unchanged. Rationale: the reported mismatch is in the benchmark's comparison and rendering lifecycle, not in the contract for an independent `explain()` call. Date/Author: 2026-08-03, Codex.
- Decision: Use a package-private immutable snapshot in the test-side `LmdbBenchmarkQueryPlan`. Rationale: the benchmark needs exact prepared-plan provenance without adding a public RDF4J API. Date/Author: 2026-08-03, Codex.
- Decision: Use the prepared snapshot for the existing rendered-query artifact and write telemetry to a separate artifact. Rationale: one artifact must represent the timed preparation, while telemetry is a separate explanatory execution. Date/Author: 2026-08-03, Codex.
- Decision: Make fixed plan reuse the default benchmark lifecycle and retain per-invocation replanning as adaptive mode. Rationale: timing comparisons should not silently combine changing plan selection with execution timing, while experiments that require feedback adaptation must remain possible. Date/Author: 2026-08-03, Codex.

## Outcomes & Retrospective

The existing rendered-query artifact now contains the immutable prepared snapshot that was captured from the tuple expression passed to `precompile`; it includes the rendered SPARQL, generic plan, diagnostics, SHA-256 structural fingerprint, estimator revision, and tuple-expression identity. Telemetry is rendered separately from `telemetry.tupleExpr()` and receives its own source, fingerprint, revision, identity, and prepared-shape comparison. The normal teardown path no longer calls `explain(Optimized)`.

`FIXED` is the default lifecycle and reuses one prepared evaluation step through warmup and measurement. `ADAPTIVE` retains per-invocation preparation and reports fingerprint/revision changes. Final fixed-mode repetitions used JDK 25, JMH 1.37, one 4-second warmup, two 2-second measurements, and one fork: 0.567 and 0.571 ms/op, both with fingerprint `595c4054...`, zero plan changes, and telemetry shape matches. The remaining 0.004 ms/op (~0.7%) spread is timing variance after plan identity was stable.

Predicate-range diagnostics now distinguish selected candidates, cost domination, absent finite rewrite domains, value-limit rejection, unsupported bound shapes, and Cascades fallback. The focused range test class (11 tests), q2 medical regression (1 test), benchmark-plan unit class (13 tests), and smoke suite (20 tests, 18 skipped) pass. The full medical suite still has the existing q4 non-finite-random-seek errors and q9 finite-condition-code-anchor failure; these were reported without weakening their assertions.

## Context and Orientation

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbBenchmarkQueryPlan.java` parses a benchmark query, runs the LMDB optimizer, precompiles the optimized tuple expression, and evaluates that precompiled step. It already stores a generic optimized-plan string and diagnostics, but it does not retain a rendered SPARQL snapshot or a stable plan identity.

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryPlanRunBenchmark.java` owns the JMH benchmark. Its execution state currently prepares a plan for every invocation, prints the first optimized generic plan, and at trial teardown calls fresh telemetry and optimized explanations. The fresh optimized explanation is currently rendered as if it were the prepared benchmark plan.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizer.java` and its packed logical rule program annotate predicate-range alternatives. The implementation work will only add truthful diagnostic categories; it will not change the cost model or whether a range alternative wins.

The existing integration tests in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/` exercise MEDICAL_RECORDS q2 and related optimizer behavior. Maven tests must run through `.codex/skills/mvnf/scripts/mvnf.py` with the checkout-local `.m2_repo`; test commands must not use `-am` or `-q`.

## Plan of Work

First add lifecycle assertions that fail against the current implementation. The tests will require a prepared snapshot with a rendered query and a stable fingerprint, require the benchmark renderer to use that snapshot, and require telemetry rendering to use the telemetry tuple expression rather than a fresh optimized explanation. A fixed/adaptive lifecycle test will also establish the intended default and explicit adaptive behavior.

Next extend `LmdbBenchmarkQueryPlan` with an immutable package-private snapshot. Build the generic plan text, diagnostics, rendered SPARQL, and structural SHA-256 fingerprint from the same optimized tuple expression that is passed to `precompile`. Extract the frontier/LEO revision from existing planned metrics when available. Keep the current capture flag behavior so ordinary benchmark execution does not pay for unnecessary explanation materialization.

Then rework `ThemeQueryPlanRunBenchmark` teardown and logging. Store the first and last prepared snapshots in `ExecutionState`. Render the existing prepared artifact from the last completed prepared snapshot, and write a separate telemetry SPARQL artifact by rendering `telemetry.tupleExpr()` directly. Remove the fresh optimized explanation from the normal path. Log the source, fingerprint, estimator revision, tuple-expression identity for debugging, and a structural match result when both shapes are available. Use stable structural fingerprints instead of `String.hashCode()` of explanation text.

Add a `FIXED`/`ADAPTIVE` benchmark lifecycle control. In fixed mode, prepare once per trial, reuse the prepared evaluation step for warmup and measurement, and close it before trial resources are closed. In adaptive mode, preserve the existing prepare-and-close-per-invocation behavior and report the last prepared snapshot. Ensure teardown ordering is explicit so a fixed plan is closed before the store and connection.

Finally improve the LMDB guarantee diagnostics. Record whether a predicate-range candidate was generated, whether it was selected, and a specific rejection category for non-finite domains, value-limit rejection, cost domination, or optimizer fallback. Keep the existing selected plan and proof annotations intact. Add tests for selected q2, a non-selected candidate, and a fallback/non-finite case.

## Concrete Steps

Work from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Before behavior changes, run the new focused test selection and save its Failsafe report snippet in `initial-evidence.txt`. Implement the smallest test first, observe its failure, then patch the production or benchmark source. Use commands of this form:

    python3 .codex/skills/mvnf/scripts/mvnf.py --it <Class#method> --module core/sail/lmdb --retain-logs

After each implementation milestone, rerun the same selector. Then run the full focused classes, module formatting, copyright checks, and `git diff --check`.

For performance verification, use the repository benchmark helper with identical JDK, store, warmup, measurement, and fork settings for two fixed-mode runs. Record plan fingerprints and estimator revisions from the logs before comparing timing means. Timing variance that remains after plan identity is stable should be attributed to JVM warmup, filesystem cache, and hardware rather than hidden plan drift.

## Validation and Acceptance

The new q2 provenance test passes and shows `packed-predicate-range-anchor`, `VALUES ?date`, and one stable prepared fingerprint. The benchmark's existing rendered SPARQL file contains the prepared snapshot, not a plan produced by a fresh teardown optimization. A separate telemetry file contains a rendering of the tuple expression returned by the telemetry explanation.

With explanation logging enabled, output includes entries equivalent to `source=prepared`, `source=telemetry`, `planFingerprint=<sha256>`, `estimatorRevision=<value>`, and `planShapeMatchesPrepared=<true|false>`. A mismatch is visible and attributable rather than silently presented as one plan.

Fixed mode uses one prepared fingerprint throughout a trial. Adaptive mode may report fingerprint or estimator-revision changes, but those changes are explicitly labeled. Existing query result assertions remain unchanged.

Focused tests must pass. Known unrelated medical regression failures, if they remain, must be reported with their existing Failsafe evidence rather than weakened. Formatting and copyright checks must pass, and no production `SailQuery.explain()` behavior is modified.

## Idempotence and Recovery

All edits are additive or localized. Re-running `mvnf` is safe; it may recreate `target/` reports and benchmark artifacts, so preserve the existing untracked artifacts and append evidence rather than overwriting `initial-evidence.txt`. Do not delete or reset unrelated files. If a benchmark process leaves an LMDB store locked, stop only the process started for this task and rerun the focused test; do not remove broad directories.

## Artifacts and Notes

The important artifacts are the prepared generic plan, prepared diagnostics, prepared rendered SPARQL, telemetry generic explanation, telemetry rendered SPARQL, and retained Maven/Failsafe reports. The final handoff must link the changed source files and include compact passing evidence plus any unrelated failures.

## Interfaces and Dependencies

Add an immutable test-side record in `LmdbBenchmarkQueryPlan` with accessors for the prepared generic plan, diagnostics, rendered SPARQL, structural fingerprint, estimator revision, and tuple-expression identity. Because the benchmark class lives in a subpackage, the nested record is exposed to that test-side consumer; it remains outside the shipped RDF4J API.

Use the existing `TupleExprIRRenderer`, `QueryModelTreeToGenericPlanNode`, and LMDB planned metric names. Use the JDK `MessageDigest` SHA-256 implementation for stable fingerprints; do not add a dependency. Use the existing JMH `@Setup`/`@TearDown` lifecycle annotations and make fixed versus adaptive behavior explicit in the benchmark state.
