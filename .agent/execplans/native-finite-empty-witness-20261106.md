# Prove Empty Native Finite Witnesses

This ExecPlan is a living document and follows `.agent/PLANS.md`. Keep its `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` sections current as the implementation proceeds.

## Purpose / Big Picture

LMDB's native planner can currently retain a nested `EXISTS` even when a finite set of stable RDF terms is known to have no matching statement in the current read snapshot. The query still returns the right rows, but it performs needless work and can repeat that work inside UNION arms. This change teaches native compilation to prove only a whole pure inner join empty when every value in a stable finite binding assignment is absent from the same authenticated source and an ordinary statement pattern consumes that binding. A test will show the operation disappears from selected native IR while query results remain equal to the witness-free control.

## Progress

- [x] (2026-11-06 02:50Z) Preserve initial red and snapshot controls.
- [x] (2026-11-06 02:50Z) Inspect source authority and proof seam.
- [x] Guard native child callback capture.
- [x] (2026-11-06 03:02Z) Add positive and conservative controls.
- [x] Run callback and cache regression suites.
- [x] Capture current finite-proof red.
- [x] Replace duplicate snapshot with Capture.
- [x] Bound join inspection to linear work.
- [x] (2026-11-06 04:54Z) Fix mutable literal control fixture.
- [x] Restore pre-existing EXISTS diagnostics.
- [x] Capture unauthenticated-source admission red.
- [x] Implement conservative retention capability.
- [x] Require lookup and active planes.
- [in_progress] Verify capability and reset tests.
- [ ] Verify finite proof and neighbors.
- [ ] Record final evidence and limits.

## Surprises & Discoveries

- The first implementation reached the empty subplan in the scoped fallback, but the selected plan still retained direct single-pattern EXISTS operators. The filter compiler's projected-EXISTS fast path bypassed tuple compilation; the same pure-join proof now runs before both direct and scoped EXISTS paths.
  Evidence: benchmark-evidence/finite-domain-postfix-focused-20261106T0303Z/LmdbFiniteDomainEmptyWitnessTest.surefire.txt; it failed only at the intended no-nx assertion after result and native-execution checks.

- The three added pre-fix controls pass: mixed finite values retain the anti-witness, positive EXISTS distinguishes absent from present values, and STRUUID remains in the optimized effect barrier.
  Evidence: post-evidence.finite-domain-control-expansion-20261106T0251Z.txt, tests=3, failures=0.

- The first red is a real packed finite-filter rewrite, not a manually authored `VALUES` anchor. The selected native aggregate IR still contained two `nx{}` nested negated-existence operators although count results matched the witness-free control.
  Evidence: `initial-evidence.finite-domain-empty-witness-20261106.txt`; full reports and logs are preserved under `benchmark-evidence/finite-domain-empty-witness-test-first-20261106T020446Z/`.
- The selected path exposes an exact mutable `LmdbLiteral` in the generated finite assignment. The first candidate proof safely declined it before dictionary lookup; admitting the wrapper solely because it is internal or not marked externally owned would be unsound. The proof now needs to detach only exact known built-in/native values with recursively safe children and verify native semantic versions around the copy, then query the current source with the detached immutable value.
  Evidence: `benchmark-evidence/finite-domain-proof-trace-20261106T0322Z/trace.20261106T032355.surefire.txt` and `trace.verify.stdout.log`.
- A version-checked detached copy still did not eliminate the selected witness, and does not by itself prove that the original finite row stays unchanged after proof and before execution. The focused regression still fails only at the intended no-`nx{}` assertion after the native/adjacency/result checks passed. Parent review requested a pause until a private-row ownership or checked-fallback design is approved.
  Evidence: `benchmark-evidence/finite-domain-postfix-focused-v4-20261106T0345Z/` and `post-evidence.finite-domain-empty-witness-20261106.v4.txt`.
- The cache identity helper has a separate nested-native callback boundary: a parent `LmdbLiteral.getSemanticVersion()` can inspect its datatype child through `ChildVersion` before the capture traversal has rejected a custom native subclass. The next focused regression constructs an exact native literal with a child whose observation is enabled only after construction, then asserts capture rejects it without any callback. A separate native evaluation test will show that a later evaluation must recapture a mutated prepared literal.
- The new callback regression fails as intended: capture rejects the native literal but observes its custom native datatype once, because the parent version check runs before child class validation. A successive-evaluation recapture control is now added to the finite-domain test class and has compiled, but is not executed in this focused callback-fix step.
- The callback fix now visits children from the exact loaded native implementation and rejects unsupported child classes before the parent semantic-version check. The focused regression, full value-admission class, and five related ownership/identity classes pass. The finite-domain proof patch is preserved separately and was not changed during this task.
  Evidence: `benchmark-evidence/nested-native-literal-callback-postfix-20261106T0405Z/`, `benchmark-evidence/builtin-value-cache-admission-postfix-20261106T0407Z/`, and `benchmark-evidence/cache-ownership-identity-postfix-20261106T0410Z/`.
- The finite-domain focused red still reaches the intended selected-IR assertion after results, native interpreter selection, and direct-adjacency engagement pass. The current in-progress snapshot/type registry is the path to replace with the approved shared capture/materializer.
  Evidence: `initial-evidence.finite-domain-empty-witness-current-red-20261106.txt` and `benchmark-evidence/finite-domain-empty-witness-shared-capture-red-20261106T0418Z/`.
- The shared Capture/materializer now passes the focused finite-witness red-to-green. Full class verification stops at the mutable-value control's fixture assertion: `findLmdbLiteral` does not find an `LmdbLiteral` in the prepared parsed query, so the test fails before mutating or reevaluating it. Twelve other methods pass. Broad verification is paused until this ownership boundary is reviewed.
  Evidence: `benchmark-evidence/finite-domain-empty-witness-shared-capture-green-20261106T0431Z/` and `benchmark-evidence/finite-domain-empty-witness-full-class-blocked-20261106T0434Z/`.
- The mutable-value fixture now explicitly installs one store-created `LmdbLiteral` into the parsed matching `ValueConstant`; the focused repeated-evaluation control passes 1/1 with its original count and semantic-version assertions intact.
  Evidence: `benchmark-evidence/finite-domain-mutable-control-green-20261106T045407Z/` and `post-evidence.finite-domain-mutable-control-20261106.txt`.
- The `[exists-compile]` diagnostics removed during this branch's filter-compiler edit were restored verbatim from `HEAD`; the remaining compiler diff contains the current proof entry points only.
- A stable, non-null lookup token from a mutable custom `NativeLmdbQuerySource` does not retain its dictionary mapping. With keyable `ListBindingSet` rows and all pure-join/value-safety gates satisfied, the current proof returns `EmptyPlan`; after source and AST mutation, `resetValuesFallbackCompilation()` does not invalidate that memoized proof. No capability implementation was changed during this test-first step.
  Evidence: `initial-evidence.dictionary-snapshot-reset-20261106.txt`, `initial-evidence.dictionary-snapshot-capability-20261106.txt`, `initial-evidence.dictionary-snapshot-capability-v2-20261106.txt`, and `benchmark-evidence/finite-domain-lookup-scope-and-reset-red-20261106T051443Z/` (2 tests, 2 intended failures).
- The retained real dataset control previously passed in the full finite-domain class: it showed a pinned native source retains its dictionary miss and old adjacency/data view after insertion. That run's only failure was the now-corrected mutable-literal fixture.
  Evidence: `benchmark-evidence/finite-domain-empty-witness-full-class-blocked-20261106T0434Z/` (`retainedNativeDatasetKeepsItsDictionaryAndAdjacencyProofAcrossInsertion` passed; 12/13 overall).
- The reviewed capability now defaults to decline, and LMDB read views attempt membership retention under the ValueStore native read barrier only after an all-absent finite candidate is found. The first focused run compiled successfully; both admission/reset assertions passed, while an added post-insertion row execution control failed and is being isolated before broad validation.
  Evidence: `benchmark-evidence/dictionary-snapshot-capability-source-reset-first-run-20261106T0527Z/` (2 tests, 1 failure), plus the earlier first compile diagnostic at `benchmark-evidence/dictionary-snapshot-capability-compile-blocker-20261106T0525Z/`.
- Snapshot semantics matter to this optimization. `SNAPSHOT_READ` is consistent for each query but may acquire a fresh source for a later query; only `SNAPSHOT` pins one source across transaction queries. The regression now checks both contracts, and a retained native dataset control proves its old dictionary and adjacency view remain bound to its old revision.
  Evidence: `benchmark-evidence/finite-domain-snapshot-boundary-20261106T0232Z/` and `benchmark-evidence/main-snapshot-read-control-20261106T0238Z/`.

## Decision Log

- Decision: run the proof at both Join compilation and before the projected/direct EXISTS fast paths.
  Rationale: a fast path for a single statement pattern can bypass Join compilation even when an earlier optimizer has exposed a pure finite assignment in the witness; the same source proof must guard both entry paths.
  Date/Author: 2026-11-06 / Codex.

- Decision: perform the proof inside native query compilation, using the current `NativeLmdbQuerySource` and the source's position-aware dictionary lookup; do not cache an empty result in a logical plan.
  Rationale: dictionary absence and statement data are snapshot-dependent, while cached logical templates may outlive a source revision.
  Date/Author: 2026-11-06 / Codex.
- Decision: decline unless the candidate is a pure inner-join tree of supported statement-pattern and stable finite-binding leaves, and every row supplies a term-safe value for a variable read by a real statement pattern.
  Rationale: an empty leaf proves the whole join empty only when no effectful or unsupported operation can change evaluation behavior, and UNDEF or a mixed present/absent domain is not empty.
  Date/Author: 2026-11-06 / Codex.
- Decision: require an authoritative value lookup scope and current-source proof; use neither planner metrics nor estimates as absence evidence.
  Rationale: UNKNOWN from an untrusted or synthetic source is not a proof of absence, and the proof must reflect the data source used for execution.
  Date/Author: 2026-11-06 / Codex.
- Decision: read the value-lookup scope before and after checking the finite values and require the same non-null token by identity.
  Rationale: a changed lookup mapping invalidates negative evidence; the source contract defines identity as the authority for absence.
  Date/Author: 2026-11-06 / Codex.
- Decision: snapshot finite assignment values into exact immutable RDF4J built-ins before checking the dictionary; native wrappers must be initialized/versioned and have only recursively inspectable children, and their semantic version must remain unchanged through detachment.
  Rationale: packed finite assignments can hold mutable LMDB wrappers. An owner flag alone does not prove that the wrapper or its nested datatype/term children cannot change, and arbitrary getters must not be invoked before the exact-class boundary.
  Date/Author: 2026-11-06 / Codex.
- Decision: replace the duplicate finite-value registry and copier with the now-reviewed shared `LmdbQueryValueIdentity.Capture(true)` materializer after the focused finite red is recaptured.
  Rationale: one audited ownership boundary must handle immutable built-ins and initialized private native values consistently; the finite compiler must not maintain a second type registry or retain original caller values for dictionary lookup.
  Date/Author: 2026-11-06 / Codex.
- Decision: after exact native class/readiness checks, visit raw loaded children and reject unsupported child classes before calling the parent's semantic-version method.
  Rationale: `LmdbLiteral.getSemanticVersion()` may consult its datatype's version via a stored child witness. Only the exact native value classes and built-in ordinary classes can be inspected safely; copy/version and graph traversal still use the existing Capture path after this preflight.
  Date/Author: 2026-11-06 / Codex.
- Decision: memoize pure-join inspection nodes per native compiler and prove only maximal pure join regions.
  Rationale: this keeps a left-deep query at linear algebra-node inspection work while still collecting all statement variables and finite assignments for the maximal region; relevant finite cells are then checked once for the region.
  Date/Author: 2026-11-06 / Codex.
- Decision: remove a standalone filter only when its condition is exactly `NOT EXISTS` and that witness was proven empty; leave positive `EXISTS` on its existing EmptyPlan path.
  Rationale: the proof makes this two-valued filter true for every input row, while no effects in the filter input are skipped.
  Date/Author: 2026-11-06 / Codex.
- Decision: build finite-join inspection summaries once per compiler and share child summaries across left-deep joins; prove only maximal pure join regions.
  Rationale: re-walking every pure subtree at each recursive join is quadratic, while memoized summaries preserve candidate completeness at pure-region roots and bound query-algebra inspection to nodes plus the finite binding cells checked.
  Date/Author: 2026-11-06 / Codex.
- Decision: require an explicit source capability to retain dictionary membership after all candidate values are observed absent; a lookup-scope token alone is insufficient.
  Rationale: tokens identify a mapping but retain no native dictionary transaction. The proof must be tied to the same paired source and remain protected against dictionary resize; custom sources decline by default, composites require the lookup source plus every execution-active plane, and synthetic sources delegate.
  Date/Author: 2026-11-06 / Codex.

## Outcomes & Retrospective

The shared capture/materializer now supplies detached lookup values, and the focused regression passes with the negative witness absent from executed native IR. The full class is not yet accepted: its mutable-prepared-value control fails before exercising mutation because its fixture cannot locate the expected `LmdbLiteral` in the parsed query. Fix or replace that fixture without weakening its ownership contract, then rerun the full class and neighboring kernel/interpreter/cache ownership suites. The full class already ran 12 of 13 tests successfully, including the source-revision, snapshot, numeric/language, UNDEF/duplicate, graph, explicit/inferred and positive/mixed-witness controls.

## Context and Orientation

The relevant planner is `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregatePlanner.java`. Its `compileTupleUncached` method compiles RDF4J tuple algebra into native slot plans. `LmdbNativeAggregateValuesCompiler.java` handles finite binding assignments and existing optimized VALUES folds. `LmdbNativeAggregatePlannerBase.java` provides `idOf`, `termSafeAbsenceProof`, source access, and the generic-engine effect safety check `canDiscardWithoutEvaluation`.

`NativeLmdbQuerySource` is the compiler bridge to dictionary and statement reads. A position-aware `idOf(value, mask)` returns a stored identifier or `UNKNOWN_ID`. A dictionary miss is a usable absence fact only for a safe RDF spelling and a source that certifies its visible value-to-id mapping. The `valueLookupScope()` contract is one such optional capability; a null token declines authority. `BindingSetAssignmentKeyability.canInspect` is the existing gate for repeatable, inspectable finite rows. A `SlotPlan.empty()` is the native relational empty result. The compiler must recompute the proof for every newly bound query source so a later data revision cannot reuse a stale empty result.

The test is `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbFiniteDomainEmptyWitnessTest.java`. It forces the native interpreter tier and inspects the actual selected aggregate IR through the existing debug test hook. Existing red evidence and the eight non-red snapshot controls are already saved; never overwrite those artifacts.

## Plan of Work

First inspect the `NativeLmdbQuerySource.valueLookupScope()` implementations, stable-row inspection helper, and the compiler's existing pure-node and discard-safety rules. Identify one compiler boundary where a supported pure join can be scanned for a complete finite assignment and an actual statement pattern reading its variable. The scan must stop conservatively on any other node, nested/effectful expression, unstable binding source, undeclared binding, UNDEF row, unsafe term representation, or absent authority token.

At that boundary, resolve every finite value against the same current `source` using `idOf(value, layout.positionMask(variable))`. Return `SlotPlan.empty()` only if every row binds the candidate variable and every value yields `UNKNOWN_ID` with `termSafeAbsenceProof(value) == true`. If any value is present or spelling is not term-safe, decline the proof. Do not rewrite the finite assignment by itself, and do not infer absence from estimates, sketches, or counters. If the compiler's existing empty propagation cannot eliminate the nested negative witness, add a narrowly scoped lowering rule that treats `NOT EXISTS` over a proven `EmptyPlan` as true while preserving positive EXISTS and all non-pure barriers.

Expand the focused test only where a permanent control is missing: positive EXISTS with a present witness; mixed present/absent finite rows; unstable or effectful nodes as barriers; UNDEF and duplicates; inserted data under a new source revision; and retained old snapshot behavior. Assert results and selected operations, not a fixed latency or whole-plan serialization.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Use the existing named Maven workspace `adjacency-merge-copy-20261105`, retain logs, and never add `-am` or `-q` to test commands. The focused pre-fix red is already captured, so do not rerun or overwrite it.

After implementation, first run the red method with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace adjacency-merge-copy-20261105 'LmdbFiniteDomainEmptyWitnessTest#packedEmptyFiniteWitnessIsEliminatedFromTheSelectedNativeKernel' --module core/sail/lmdb --retain-logs

If green, archive its Surefire report, Maven logs, and compact summary in a new uniquely named `benchmark-evidence` subdirectory and write a matching top-level `post-evidence.*.txt`. Next run the full `LmdbFiniteDomainEmptyWitnessTest` class, then the related native interpreter, kernel, and optimizer regression classes identified during implementation, each with retained logs. Keep each run's reports in a distinct evidence copy before another run can replace them. Finish with `git diff --check` and inspect `git status --short` without deleting existing artifacts.

## Validation and Acceptance

The first focused test must pass with the packed finite-filter assignment still present in the optimized query, the forced native aggregate path still selected, actual adjacency engagement observed, result count still equal to the control, and the selected aggregate IR no longer containing the nested `nx{}` operation for the proven-empty witness. The present witness must remain represented. Mixed finite domains, UNDEF, unsafe terms, unsupported/effectful barriers, and non-authoritative sources must conservatively retain the original plan and correct results. A newly bound source after an insertion must see the new witness; a retained `SNAPSHOT` source must continue to see the old source and old result.

All full-class and neighboring native tests must pass. Final handoff must include exact commands, Surefire summaries and report paths, compilation/log paths, changed source/test paths, and a `git diff --check` result. If any fixture or semantic failure appears, stop at that failure with the report instead of weakening a guard or assertion.

## Idempotence and Recovery

The proof is recomputed when the current source and native compiler are constructed. No persistent empty-plan cache is introduced. Re-running tests uses the named workspace and fresh reports; archive each result before another run overwrites a workspace report. If implementation or validation fails, leave the red and control evidence intact and record the exact failure in this plan. Do not delete benchmark stores, untracked files, logs, reports, or archives.

## Artifacts and Notes

The first red transcript is in `initial-evidence.finite-domain-empty-witness-20261106.txt`. Its full report and verify log are in `benchmark-evidence/finite-domain-empty-witness-test-first-20261106T020446Z/`. Corrected snapshot controls are in `benchmark-evidence/finite-domain-snapshot-boundary-20261106T0232Z/`. Current-main snapshot-read contract control is in `benchmark-evidence/main-snapshot-read-control-20261106T0238Z/`. Do not modify or replace these artifacts.

## Interfaces and Dependencies

The implementation should use existing interfaces: `NativeLmdbQuerySource.idOf(Value, int)`, `NativeLmdbQuerySource.valueLookupScope()`, `BindingSetAssignmentKeyability.canInspect(BindingSetAssignment)`, `LmdbNativeAggregatePlannerBase.termSafeAbsenceProof(Value)`, `LmdbNativeAggregatePlannerBase.canDiscardWithoutEvaluation(TupleExpr)`, and `SlotPlan.empty()`. Keep source authority and `layout.positionMask(variable)` aligned with the exact statement-pattern position mask that the variable may occupy. Do not add public APIs or dependencies.

Plan update note (2026-11-06 02:50Z): created this plan from the parent-reviewed finite-domain proof requirements and already-preserved red/snapshot evidence before editing production code.
Plan update note (2026-11-06 03:14Z): focused post-fix run showed the projected EXISTS fast path bypassed the new Join proof; the proof is now checked before direct/projected EXISTS compilation as well.
Plan update note (2026-11-06 03:02Z): the three new controls passed before implementation; the primary red and snapshot evidence remain untouched.
Plan update note (2026-11-06 02:51Z): source review confirmed transaction-bound authority tokens; added explicit mixed-domain, positive EXISTS, and function-barrier controls before implementation.
Plan update note (2026-11-06 03:48Z): the traced rejection was the exact mutable `LmdbLiteral` generated in the packed finite assignment. A version-checked detached probe still failed to remove the selected `nx{}` and did not establish that the original row cannot change after proof; stop pending ownership/fallback review.
Plan update note (2026-11-06 04:55Z): parent confirmed native compilation is repeated per evaluation on the query-owned source and asked for a separate red on `LmdbQueryValueIdentity.Capture(true)` invoking a custom native datatype's version callback before child rejection; also add a successive-evaluation recapture control. Preserve the finite-domain production draft patch and leave production untouched until the new red is captured.
Plan update note (2026-11-06 05:02Z): the isolated callback red is saved at `initial-evidence.nested-native-literal-callback-20261106.txt`. A loaded native `LmdbLiteral` with an observed `LmdbIRI` subclass datatype triggers one getter callback before Capture rejects it; implementing child preflight before the parent version check.
Plan update note (2026-11-06 05:22Z): exact native children are now collected and class-checked before the parent version read. The focused red passed (1/1), the admission suite passed (53/53), and five existing identity/ownership classes passed (37/37). The separate finite-domain implementation remains an unaccepted draft pending ownership review.
Plan update note (2026-11-06): the finite-domain helper now delegates detached values to the shared Capture/Materializer; memoized pure-join inspection avoids repeated left-deep scans, and standalone proven-empty NOT EXISTS filters return their unchanged input. The focused test passed (1/1); the full class stopped at the mutable-prepared-value control fixture before its behavioral assertions (12 passed, 1 failed).
