# Certify semantic soundness of the GH-5905 algebra equivalence checker

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while implementation proceeds. Maintain this document according to
`.agent/PLANS.md`. This plan extends `.agent/GH_5905_ALGEBRA_EQUIVALENCE_EXEC_PLAN.md`; that earlier plan explains the
module's origin and public API, while this document is self-contained about the certification work.

## Purpose / Big Picture

The experimental checker currently returns `EQUIVALENT`, `NOT_EQUIVALENT`, or `UNKNOWN` for two RDF4J query-algebra
trees. Certification must establish the narrower and defensible claim that an `EQUIVALENT` verdict always carries a
machine-checked soundness certificate, a `NOT_EQUIVALENT` verdict always carries a target-correct finite witness, and
anything outside the proved semantic domain returns `UNKNOWN`. The checker deliberately remains incomplete.

The result is visible in three ways. Every positive verdict is rejected unless an independent Java kernel replays its
certificate. A Lean 4 model proves the rule calculus sound for RDF4J runtime semantics, SPARQL 1.1, the dated 5 June
2026 SPARQL 1.2 Working Draft, and the shared target. Finally, deterministic exhaustive, mutation, corpus, and fuzz
campaigns compare the Java producer, Java kernel, Lean kernel, and target evaluator.

## Progress

- [x] (2026-07-18 19:09Z) Captured the mandatory offline root clean-install baseline; all reactor modules succeeded in
  32.343 seconds and the full output is in `maven-build.log`.
- [x] (2026-07-18 19:12Z) Created this certification ExecPlan and recorded the pre-existing dirty working tree without
  modifying or deleting any user-owned artifact.
- [x] (2026-07-18 19:14Z) Tested and rejected an invalid three-row `REDUCED` minimization: it produced the same SET
  support on both sides. Reverted the attempted production guard and returned to the original complex artifact.
- [x] (2026-07-18 19:21Z) Preserved the actual serialized runtime-SET artifact and observed
  `JsonTupleExprSoundnessRegressionTest#runtimeReducedIsNotHiddenBySetObservation` fail because a verified
  error-vs-success witness was incorrectly reported `EQUIVALENT`.
- [x] (2026-07-18 19:23Z) Restricted SET-level `REDUCED` elimination to non-runtime targets; the focused preserved
  witness now returns `NOT_EQUIVALENT` with a verified counterexample.
- [x] (2026-07-18 19:26Z) Passed the complete 11-test soundness class, 26-test algebra regression class, and corrected
  slow three-row replay after the runtime `REDUCED` fix.
- [x] (2026-07-18 19:38Z) Added immutable version-1 normalization certificate evidence, deprecated legacy
  certificate-less proof construction, made kernel verification unconditional, and separated replay into kernel-owned
  normalization, binding, incoming-binding, and safety implementations.
- [x] (2026-07-18 19:44Z) Added dated target IRIs and target/oracle witness metadata; disabled RDF4J-evaluated W3C
  counterexamples so specification targets fail closed pending the formal evaluator.
- [x] (2026-07-18 20:05Z) Added the Lean 4.32.0 standard-library-only project, semantic-domain types, four target
  profiles, generic certificate theorem scaffold, axiom inventory, rejecting JSON-lines executable, and a 1,024-cell
  rule/profile manifest. Only 32 structural-identity cells are proved; 992 cells remain explicitly pending.
- [x] (2026-07-18 20:09Z) Added a production closed native-node registry and coverage guard. A focused synthetic native
  subclass now fails closed, and the guard covers every concrete non-update RDF4J `QueryModelNode`.
- [x] (2026-07-18 20:14Z) Ran the complete equivalence module after correcting omitted helper-node classifications;
  all 113 tests passed. The preceding full run's 16 failures are retained as evidence of the guard gap.
- [x] (2026-07-18 20:19Z) Ran the expanded PIT 1.25.3 profile. It failed the required 100% threshold with 442/535
  mutants killed (83%), 37 surviving, and 56 uncovered; `MUTATION_REVIEW.md` inventories the blockers.
- [x] (2026-07-18 20:24Z) Applied repository formatting and completed the final offline root `-Pquick clean install`;
  every reactor module succeeded in 37.588 seconds, including query-algebra equivalence.
- [x] (2026-07-18 21:13Z) Closed the expanded PIT 1.25.3 gate without exclusions: all 519 generated mutants were
  killed, no mutants lacked coverage, mutated-line coverage was 896/911 (98%), and test strength was 100%.
- [x] (2026-07-18 21:20Z) Completed a symmetry-reduced RDF4J-runtime bounded universe across 32 expressions, five
  datasets, four incoming mappings, four observations, and both contexts. All 4,224 profile/pair cells agreed with
  optimizer-free evaluation; 679 positive certificates replayed and 2,610 witnesses replayed twice.
- [x] (2026-07-18 21:23Z) Corrected the Lean SET observation from an order-sensitive list to an extensional support
  predicate and proved ten foundational BAG/SET/ASK append and deduplication laws. The 15-job build is green with no
  unfinished proofs or project axioms.
- [x] (2026-07-18 21:29Z) Applied formatting (no changes required) and passed the complete 142-test non-slow
  equivalence module in 3.454 seconds.
- [x] (2026-07-18 21:31Z) Passed the final offline repository `-Pquick install` checkpoint across the entire reactor in
  18.049 seconds without cleaning away retained PIT and bounded-universe reports.
- [x] (2026-07-19 07:56Z) Replaced root-only certificate summaries with stable child-index paths and local subtree
  results, removed `ProofKernel`'s dependency on the whole-tree replay API, and recorded distinct JOIN association and
  commutation intermediate states. Focused architecture and malformed-certificate tests are green.
- [x] (2026-07-19 08:09Z) Removed RDF4J node `equals()` from structural proof acceptance, added an explicit comparator
  inventory for all 76 native node classes, and classified every non-update query-model instance field as semantic or
  non-semantic. The expanded 145-test module is green.
- [x] (2026-07-19 08:16Z) Added ten Lean outcome-rule laws for UNION, DISTINCT observation, and VALUES foundations.
  The 17-job Lean 4.32.0 build is green with no `sorry`, `admit`, project axiom, or `sorryAx` dependency.
- [x] (2026-07-19 09:03Z) Added distinct immediate results for JOIN/UNION/MINUS association, deduplication,
  commutation, filter distribution and pushdown, LEFT JOIN distribution, native-survivor identities, and projection
  child paths. Removed native `hashCode()` from opaque canonical hashing and passed the expanded 156-test module.
- [x] (2026-07-19 11:03Z) Reclosed the expanded PIT 1.25.3 gate after the replay/schema changes: all 639 generated
  mutants were killed, no mutant lacked coverage, mutated-line coverage was 1034/1051 (98%), and test strength was
  100%. No mutant was excluded or classified equivalent.
- [x] (2026-07-19 09:35Z) Made the independent Java replayer consume and validate each local step at its active node
  frame, reject immediately on the first malformed step, and use structured rule parameters instead of diagnostic
  prose. The 12-test replay class and expanded 48-test regression class are green.
- [x] (2026-07-19 09:35Z) Split `VALUES_UNIT` by semantic target and incoming context after the formal model exposed a
  runtime/specification mismatch: RDF4J runtime retains its evaluator-verified all-binding identity, while W3C targets
  now apply the rule only at empty top-level context.
- [x] (2026-07-19 09:35Z) Expanded the Lean reference semantics with expression-level UNION, DISTINCT, VALUES, and
  constant-true FILTER theorems. The 1,024-cell matrix now has 239 theorem cells, 285 explicit inapplicability cells,
  and 500 cells still pending.
- [x] (2026-07-19 12:24Z) Replaced order-sensitive list-backed solution mappings with executable extensional standard-
  library tree maps, derived FILTER distribution from primitive row filtering, and expanded the matrix to 475 theorem
  cells, 285 explicit inapplicability cells, and 264 pending cells. The 19-job Lean build and unfinished-proof scan pass.
- [x] Narrow the executable certificate fragment to theorem-backed rules. `CertifiedRuleRegistry` now rejects
  error/totality-sensitive reorder, distribution, projection, and FILTER(false) steps; the 1,024-cell matrix records
  347 theorem cells and 677 reviewed inapplicability cells with no pending cells. The Java module is green at 172 tests.
- [x] (2026-07-19 12:24Z) Re-ran the complete 163-test module and expanded PIT profile after incremental replay and
  target-aware VALUES changes. One `requireAccepted` deletion initially survived; a trailing-step regression now kills
  it, and all 643/643 mutants are killed with no uncovered or excluded mutants.
- [x] (2026-07-19 15:23Z) Completed the version-2 JSON certificate interchange. The Java kernel and the independent
  Lean executable decode exact source trees, enforce a root-local theorem boundary, reject malformed and nested proof
  steps, and agree on all generated valid certificate batches.
- [x] (2026-07-19 16:14Z) Closed the formal rule matrix at 347 theorem-backed and 677 explicit inapplicability cells,
  with no pending cell. The standard-library-only Lean build is green and the exported theorem inventory uses only
  `propext`, `Classical.choice`, and `Quot.sound`.
- [x] (2026-07-18 21:20Z) Completed the 4,224-cell runtime bounded universe. Every concrete positive certificate and
  target-tagged negative witness agreed with cached optimizer-free RDF4J evaluation, with zero incomplete or
  asymmetric cells.
- [x] (2026-07-19 21:52Z) Inventoried 1,168 W3C/RDF4J corpus queries: 998 parsed, 989 produced independently replayed
  certificates, and every unsupported parse, target algebra, or formal encoding was required to return `UNKNOWN`.
- [x] (2026-07-20 00:03Z) Reclosed the expanded PIT 1.25.3 gate after the final production robustness fix: all 477
  generated mutants were killed, no mutant survived or lacked coverage, and no exclusion/equivalent-mutant waiver was
  used.
- [x] (2026-07-20 00:04Z) Reran the final W3C release campaign after the last production change: exactly 100,000
  SPARQL 1.1 and 100,000 pinned SPARQL 1.2 pairs completed across ten seeds per target, and Lean accepted all 200,000
  certificates over 9,317,760 oracle cases.
- [x] (2026-07-20 00:15Z) Reran the final runtime rule-shaped release campaign: ten seeds completed exactly 100,000
  pairs with zero harness violations; Java emitted 26,347 certificates and Lean accepted all of them over 1,264,656
  oracle cases.
- [x] (2026-07-20 07:55Z) Completed the final runtime differential release campaign from zero after one correctly
  rejected truncated attempt: ten seeds reached exactly 1,000,000 pairs with zero violations; Java emitted 385,485
  certificates and Lean accepted all of them over 18,503,280 oracle cases.
- [x] (2026-07-20 08:27Z) Passed the relevant W3C MemoryStore evaluation suites through Failsafe: all 176 SPARQL 1.1
  and 66 SPARQL 1.2 dynamic tests completed with zero failures, errors, or skips.
- [x] (2026-07-20 09:06Z) Passed final formatting, whitespace, 78-theorem axiom inventory, unfinished-proof scan,
  rebuilt Lean replays, the 177-test module, and the complete offline reactor quick install. Identified the 155-file
  candidate bundle as base commit `bf9edabb...` plus SHA-256 `a693a564...7142d7c`.
- [x] (2026-07-20 09:12Z) Added a 155-file path/hash manifest whose own SHA-256 equals the candidate digest, verified
  every entry with `shasum -c`, and packaged the claim boundary, machine gates, evidence map, and reproduction commands
  in `reviews/REVIEW_PACKET.md` for independent reviewers.
- [x] (2026-07-20 09:15Z) Fetched both dated specification documents from their official W3C URLs, reproduced the
  recorded SHA-256 hashes byte-for-byte (487,771-byte SPARQL 1.1 and 1,019,152-byte SPARQL 1.2), and retained the
  downloads under `/tmp/` for reviewers.
- [x] (2026-07-20 09:19Z) Audited GH-5905 read-only and confirmed that the issue has no comments, assignees, or
  independent review decision. The issue-number PR lookup showed only that #5905 is not itself a PR; a later
  commit-based lookup corrected the initial inference about related PR state. Added copy-ready role-specific requests
  and strict returned-review acceptance criteria in `reviews/REVIEW_REQUEST.md` without altering the frozen semantic
  source bundle.
- [x] (2026-07-20 09:22Z) Packaged the verified 155-file source bundle and ten review/evidence records into the
  238,643-byte `/tmp/GH-5905-algebra-equivalence-candidate-a693a564.tar.gz` transport archive and recorded SHA-256
  `7ad8b85292bebc85f0358d49f4dee44220954e3611a72b564b0a14f3c6985d22` for reviewer handoff. Extracted it into a fresh
  directory and revalidated the authoritative manifest and all 155 files.
- [x] (2026-07-20 09:30Z) Committed the 101-file certification change as
  `3c0818fa99f9f1ad3921f8ef6644648c08a1e1f6`, excluding user-owned `AGENTS.md` and ignored evidence artifacts, and
  pushed `GH-5905-algebra-equivalence`. Commit-based GitHub resolution found existing open PR #5948 at that exact head;
  it had zero reviews and no requested reviewers.
- [ ] Obtain independent RDF4J evaluator and formal-methods approvals for the identified candidate bundle before
  changing `CERTIFICATION.md` from `NOT CERTIFIED`.

## Surprises & Discoveries

- Observation: GH-5905 itself cannot supply either independent sign-off: it is an open issue with no comments or
  assignees. The initial issue-number PR lookup proved only that #5905 was not itself a PR; after publishing the
  candidate, commit-based resolution identified existing open PR #5948. That PR still supplies no sign-off because it
  has zero reviews and no requested reviewers.
  Evidence: the read-only `gh-read-inspector` issue result at 2026-07-20 09:19Z and commit result at 09:30Z; PR #5948
  names head `3c0818fa99f9f1ad3921f8ef6644648c08a1e1f6` and contains empty reviewer/review collections.

- Observation: Local history offers credible routing candidates for the RDF4J evaluator gate but not a substitute for
  a decision: excluding the implementation author, Jeen Broekstra and Jerven Bolleman have 195 and 116 authored commits
  respectively across the evaluator/model modules, while issue author Ken Wenzel can route GH-5905. No local evidence
  establishes a qualified Lean reviewer, so the formal-methods gate still requires external human coordination.
  Evidence: author-counted `git log` over `core/queryalgebra/evaluation` and `core/queryalgebra/model`; names are recorded
  as routing suggestions only in `reviews/REVIEW_REQUEST.md`.

- Observation: A deep rule-law campaign already found a live false-`EQUIVALENT` result for removing `REDUCED` under
  RDF4J runtime SET observation.
  Evidence: `initial-evidence.txt` records `SOUNDNESS [runtime-set] law-189-reduced-hidden-by-set`; the minimized source
  artifacts are under `/tmp/rdf4j-equivalence-rule-deep-20260727/seed-20260727/001-soundness`.

- Observation: The original public `ProofKernel` was circular: normalization proofs were verified by constructing
  another `Rdf4jCanonicalizer`. The current kernel no longer references that producer, uses separately compiled
  analyzers, and consumes submitted steps incrementally at stable node frames instead of comparing only after a whole
  diagnostic normalization.
  Evidence: the exploding-later-node regression proves that a malformed first step is rejected before unrelated tree
  traversal. The remaining cross-language blocker is independent Lean AST decoding and step replay.

- Observation: RDF4J runtime treats one empty `BindingSetAssignment` row as the incoming-binding identity, whereas
  that runtime behavior cannot establish an arbitrary-incoming-binding W3C rule.
  Evidence: optimizer-free runtime evaluation found no distinction with a non-empty input mapping; the SPARQL 1.1
  regression previously returned false `EQUIVALENT` and now returns `UNKNOWN` outside `TOP_LEVEL_EMPTY`.

- Observation: The current differential oracle attacks only runtime-target positive verdicts with execution.
  Evidence: `FuzzProfile.runtimeRefutable()` excludes specification targets because RDF4J correlation and evaluation
  order are not W3C algebra semantics.

- Observation: The first hand-minimized `REDUCED` replay was not the original soundness defect. It changed only
  multiplicity, not SET support, so the focused `NOT_EQUIVALENT` expectation was invalid.
  Evidence: direct evaluation printed original support `{empty, v0=s1}` and candidate support `{empty, v0=s1}`; the
  regression became `UNKNOWN` after guarding the proof because its explicit case did not distinguish the trees.

- Observation: The preserved deep artifact differs through an observable runtime failure: evaluating the REDUCED
  tree triggers a binding-comparison `NullPointerException`, while removing REDUCED produces a different completed
  outcome. SET observation collapses equal failures but still distinguishes failure from success.
  Evidence: the focused test first verifies direct outcome inequality and the bounded finder accepts the empty dataset
  before the checker incorrectly returns `EQUIVALENT`.

- Observation: The previous specification-target counterexample path was also unsoundly sourced: simple W3C-target
  differences were labelled `NOT_EQUIVALENT` after executing RDF4J's runtime evaluator.
  Evidence: `AlgebraEquivalenceCheckerTest#specificationTargetDoesNotUseRdf4jEvaluationAsACounterexampleOracle`
  returned `NOT_EQUIVALENT` before the fail-closed guard; it now returns `UNKNOWN` with a formal-oracle reason.

- Observation: Stable paths alone did not make certificates local: composite rewrites repeatedly claimed the same
  final state for two or three different rules, and opaque semantic equality initially retained native hashing.
  Evidence: focused red tests caught collapsed JOIN, UNION, MINUS, filter, LEFT JOIN, DISTINCT, identity, and projection
  steps plus unequal hashes for explicitly equal opaque nodes. Each now has a distinct immediate state/path, and
  opaque hashing no longer calls native `hashCode()`.

- Observation: A coverage guard limited to `TupleExpr` and `ValueExpr` misses concrete helper nodes that participate in
  evaluation, including `ProjectionElemList` and `ExtensionElem`.
  Evidence: the first full module run after closing the registry returned 16 `UNKNOWN` verdicts and failed 16/113 tests;
  widening the inventory to every concrete non-update `QueryModelNode` made the same 113-test selection green.

- Observation: The expanded mutation target remains materially under-tested even though ordinary tests are green.
  Evidence: PIT generated 535 mutants, killed 442 (83%), left 37 surviving and 56 uncovered, and failed the 100%
  threshold. `core/queryalgebra/equivalence/MUTATION_REVIEW.md` records every affected method and line.

- Observation: Direct side-condition tests plus deterministic evaluator injection closed the mutation gap without
  exclusions or weakening the target set.
  Evidence: the final PIT run generated 519 mutants, killed all 519, reported zero uncovered mutants and 100% test
  strength, and completed with `BUILD SUCCESS`.

- Observation: A cached direct-evaluation universe makes an exhaustive runtime gate inexpensive enough for a focused
  test while still checking every positive certificate and every concrete witness.
  Evidence: `RuntimeBoundedUniverseTest` completed 4,224 profile/pair cells in 1.960 seconds with zero violations.

- Observation: The initial Lean scaffold's `ObservedOutcome.set` representation was accidentally order-sensitive,
  which would have made equal supports with different enumeration orders unequal in the formal model.
  Evidence: it stored `rows.eraseDups` as a `List Mapping`; it now stores `Mapping → Bool`, and Lean proves append
  commutativity for SET observation.

- Observation: Merely adding stable paths was insufficient for multi-rule nodes: JOIN association and commutation both
  initially claimed the same final `JOIN_AC` subtree.
  Evidence: the focused intermediate-result regression failed before `CertificateTrace.record` captured the ordered
  `JOIN_FLAT` state separately from the subsequent unordered `JOIN_AC` state.

- Observation: RDF4J's order-capability API is not a persisted field schema; `BindingSetAssignment`, `TripleRef`, and
  `TupleFunctionCall` deliberately throw `UnsupportedOperationException` from `getOrder()`.
  Evidence: the first full module run after explicit structural comparison had 18 errors at
  `BindingSetAssignment.getOrder`; removing unsupported capability calls while retaining stored-field comparisons made
  all 145 tests green.

- Observation: Treating a solution mapping as an ordinary entry list made binding order part of formal equality,
  contradicting SPARQL's extensional solution-mapping semantics and blocking a sound projection-order theorem.
  Evidence: `Mapping` is now `Std.ExtTreeMap String RdfTerm`; all exported theorems build against its extensional
  equality, with the added standard Lean dependencies `Classical.choice` and `Quot.sound` recorded by `#print axioms`.

- Observation: Reorder safety currently establishes determinism and lack of side effects, but does not separately
  establish total evaluation. MINUS blocker chaining can change whether a later failing operand is evaluated after an
  earlier blocker eliminates every left row.
  Evidence: the primitive `minusRows` laws are proved, but no expression-level MINUS chain cell was promoted; the
  Java/Lean totality and collapsed-failure precondition remains an explicit certification obligation.

- Observation: The first post-fix one-million-pair runtime rerun was correctly rejected by its hard time budget under
  unusually high host load, even though the same workload had previously completed much faster.
  Evidence: `logs/mvnf/20260719-221631-verify.log` failed after 11,668 seconds at 36,000/100,000 on the first reported
  seed with `time budget truncated`; no partial pair or certificate count is accepted.

## Decision Log

- Decision: Use a certificate-producing Java normalizer, a separate mandatory Java replay kernel, and a Lean model of
  the certificate calculus.
  Rationale: This keeps the runtime API Java-only while separating the untrusted proof search from the accepting
  kernel. Lean supplies the rule-soundness theorem; exhaustive and mutation tests police the Java/Lean translation.
  Date/Author: 2026-07-18 / Codex

- Decision: Pin Lean to 4.32.0 and use its standard library only.
  Rationale: A pinned toolchain makes proofs reproducible and avoids adding a second, fast-moving theorem library to
  the trusted dependency set.
  Date/Author: 2026-07-18 / Codex

- Decision: Keep `SPARQL_1_2_DRAFT` source-compatible but bind it to the dated 2026-06-05 Working Draft through public
  version metadata and the certification manifest.
  Rationale: An undated moving specification cannot support a stable correctness statement.
  Date/Author: 2026-07-18 / Codex

- Decision: Preserve the existing checker/result entry points. Extend proof and counterexample evidence compatibly,
  deprecate certificate-less proof construction, and require certificate replay regardless of `deepProofVerification`.
  Rationale: Callers keep the tri-valued API while accepted evidence gains an enforceable semantic contract.
  Date/Author: 2026-07-18 / Codex

- Decision: Publish `CERTIFICATION.md` immediately as a negative audit record headed `NOT CERTIFIED`, rather than wait
  to document gaps or mislabel partial infrastructure as a certificate.
  Rationale: The requested claim is meaningful only if every positive proof, target witness, theorem, mutation, bounded
  enumeration, release campaign, and independent review gate has actually passed.
  Date/Author: 2026-07-18 / Codex

- Decision: Do not encode a desired rewrite as a special reference-evaluator equation merely to make its theorem
  reflexive. Derive each law from primitive row/outcome semantics and leave its matrix cells pending when the required
  totality, purity, binding, or error-scheduling premise has not yet been mechanized.
  Rationale: A circular semantic transcription would compile while adding no evidence that the Java rule is sound.
  Date/Author: 2026-07-19 / Codex

- Decision: Rerun the truncated runtime campaign from zero with the same ten seeds, pair generator, oracle revision,
  chunk size, and worker count, changing only the hard resource budget from 10,800 to 43,200 seconds.
  Rationale: Time truncation is a failed gate, while a larger limit accommodates external host-load variance without
  changing the semantic workload or accepting partial artifacts.
  Date/Author: 2026-07-20 / Codex

## Outcomes & Retrospective

All machine acceptance gates have completed on candidate bundle
`bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.
The preserved runtime SET/`REDUCED` soundness defect is fixed; Java and Lean independently accept the final certificate
batches; the 4,224-cell bounded universe, 1,168-query corpus inventory, 477/477 mutation gate, 1,000,000-pair runtime,
100,000-pair runtime-rule, two 100,000-pair W3C campaigns, W3C compliance suites, 177-test module, and reactor install
are green. Certification is still not claimed because the required independent RDF4J evaluator and formal-methods
review records are unsigned. Any semantic change invalidates these machine results.

## Context and Orientation

Production code is under `core/queryalgebra/equivalence/src/main/java`. `AlgebraEquivalenceChecker` validates both
trees, accepts exact structural equality, otherwise asks `Rdf4jCanonicalizer` for canonical forms, and finally runs an
optional bounded counterexample search. `Rdf4jCanonicalizer` implements the rewrite laws named by `RuleId` and emits a
human-readable `RuleApplication` trace. `ProofKernel` independently consumes machine-readable local steps and rejects
any mismatch before returning `EQUIVALENT`.

The checker has four semantic targets, four root observation modes, and two incoming-context modes. A proof rule may
be valid only for a subset of those 32 profiles. `UNKNOWN` is always the conservative fallback. RDF4J runtime semantics
include incoming bindings, evaluation order, and the distinction between successful and failing evaluation. The W3C
targets instead use their abstract algebra semantics. `BOTH_1_1_AND_1_2` means that an equivalence proof must be valid
under both specification semantics; a counterexample may name either constituent target that refutes the conjunction.

## Plan of Work

First convert the existing `REDUCED` fuzz artifact into a focused checker regression. The test must construct the
smallest VALUES input that produces different SET outcomes with and without RDF4J `REDUCED`, add the empty evaluation
case, and assert that the checker does not return `EQUIVALENT`. Run it through `mvnf`, retain the Surefire failure, and
only then change the production rule. Fix the root semantic precondition; do not special-case the test shape.

Next introduce immutable normalization certificates. A certificate records the selected semantic target, observation
and context modes, the final canonical tree, and ordered local steps for each input. A step records a `RuleId`, a stable
child-index path, and typed rule parameters such as direction or permutation. `Rdf4jCanonicalizer` becomes an untrusted
producer. The independent kernel starts from a clone, resolves every path, checks the exact node shapes, recomputes the
rule's side conditions using kernel-owned declarative code, applies one local rewrite, and compares the final trees with
field-complete structural equality. It must never call the canonicalizer or its analyzers, and it must never accept a
hash or diagnostic string as semantic equality. `AlgebraEquivalenceChecker` invokes this kernel before returning every
positive result.

Replace native-node `equals()` assumptions with an explicit semantic schema. Each concrete native tuple/value node is
classified and all evaluation-relevant properties are encoded. Coverage tests fail when a new node or semantic property
appears. Unrecognized extension implementations remain opaque and yield `UNKNOWN` rather than positive evidence.

Add `core/queryalgebra/equivalence/formal` as a Lean 4.32.0 Lake project. Define RDF terms, mappings, bags, sets,
sequences, ASK observation, collapsed failure outcomes, data, incoming contexts, functions, and the supported algebra.
Define the four target contracts, then state and prove one theorem for every rule/profile entry and a composite theorem
that accepted certificate replay implies observational equivalence. The Lean certificate executable consumes a stable,
versioned JSON-lines format emitted in batches by the Java tests. The build rejects `sorry`, `admit`, and project-defined
axioms and records `#print axioms` output for exported soundness theorems.

Make finite witnesses target-aware. Runtime witnesses retain optimizer-free RDF4J evaluation, record the tested source
revision, and replay twice. Specification witnesses are accepted only through the Lean reference evaluator. Add target
and oracle metadata to immutable `Counterexample` evidence. Runtime-only correlation or evaluation order can never be
used as a W3C witness.

Finish with a machine-readable rule/profile manifest, symmetry-reduced exhaustive enumeration, W3C/RDF4J corpus
agreement, PIT mutation tests for the accepting kernel and semantic schema, and deterministic release fuzz campaigns.
Generate `CERTIFICATION.md` from measured artifacts only; do not pre-fill a success claim. Changes to rules, semantic
fields, target evaluators, proof sources, toolchain, or pinned specifications invalidate the report.

## Concrete Steps

Run commands from `/Users/havardottestad/Documents/Programming/rdf4j-stf`. Use `mvnf` for every test selection and never
use `-am` or `-q` while tests are enabled. Retain focused and release logs.

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/equivalence --retain-logs
    cd core/queryalgebra/equivalence/formal && lake build

The first and final reactor checks use the mandatory offline root clean-install command from `AGENTS.md`, with full
output in `maven-build.log`. Validate source headers before formatting, then run the repository formatter. Preserve
`initial-evidence.txt`, all `logs/mvnf` evidence, all fuzz artifacts, and every unrelated untracked file.

## Validation and Acceptance

Every `RuleId` must have a 32-cell target/observation/context row. Each cell names a Lean theorem or an explicit
inapplicability reason. Tests cover every applicable rule, every independent side condition negatively, nesting and
composition, certificate mutation, evidence immutability, structural semantic fields, and unsupported-node fallback.

The Lean build succeeds with no unfinished proofs or project axioms. Java and Lean kernels accept the same valid
certificate corpus and reject every single-field or single-step mutation. Exhaustive bounded enumeration finds no
disagreement among direct evaluation, Java verdicts, Java replay, and Lean replay. PIT 1.25.3 kills every non-equivalent
mutant in the kernel, observation comparison, target validation, and semantic schema; exclusions and equivalent mutants
are reviewed individually.

Release campaigns complete without time truncation: 1,000,000 runtime differential pairs, 100,000 runtime rule-law
pairs, and 100,000 pairs for each W3C target, with at least ten independent seeds per campaign. There are zero
soundness, witness, determinism, robustness, or cross-kernel violations. The focused regression, module suite, relevant
W3C suites, Lean build, mutation profile, final root build, and API compatibility check all pass. One RDF4J evaluator
reviewer and one formal-methods reviewer approve the final claim.

## Idempotence and Recovery

All builds, proof checks, and deterministic campaigns are safe to rerun. Persist generated reports under `target` or
the existing evidence locations, never by overwriting source manifests automatically. A newly found counterexample is
first minimized and preserved as a failing repository test. If a production behavior change was made before that
failure was observed, revert only that task-owned patch and restart from the regression step. Never reset, clean,
restore, stash, delete, or rename user-owned working-tree content.

## Artifacts and Notes

The known reduced artifact is `/tmp/rdf4j-equivalence-rule-deep-20260727/seed-20260727/001-soundness`. The baseline and
prior TDD evidence are in `initial-evidence.txt` and `logs/mvnf`. The final generated certification document belongs at
`core/queryalgebra/equivalence/CERTIFICATION.md` and must state the source revision, dated specification IRIs and hashes,
tool versions, theorem and rule matrices, bounded universe, mutation results, campaign seeds/counts, review sign-offs,
and trusted computing base.

## Interfaces and Dependencies

Keep `AlgebraEquivalenceChecker`, `EquivalenceResult`, and status meanings source-compatible. Add immutable public
`NormalizationCertificate` and `CertificateStep` types and `NormalizationProof.getCertificate()`. Deprecate the old
certificate-less `NormalizationProof` constructors; the kernel rejects those objects. Keep `RuleApplication` as
diagnostic evidence only. Add `Counterexample.getSemanticsTarget()` and `getOracleVersion()`, retaining a deprecated
runtime-defaulting constructor. Add `SemanticsTarget.getVersionIri()`. Keep `deepProofVerification` source-compatible,
but document that mandatory certificate replay always occurs and the flag only requests an additional expensive parity
check.

Pin Lean to 4.32.0 in `formal/lean-toolchain` and use no Mathlib dependency. Use PIT 1.25.3 plus the JUnit Platform
adapter in a certification-only Maven profile; it is not a runtime dependency. The trusted base is the pinned Lean
kernel/compiler, the specification transcription, the JVM, and the pinned RDF4J evaluator. The certification does not
claim completeness, resource-bounded termination, performance, third-party extension semantics, or unmodelled backend
behavior.

Revision note (2026-07-18): Created the certification ExecPlan from the approved implementation plan, recorded the
mandatory baseline and live reduced soundness violation, and fixed the formal/tool/API decisions before implementation.

Revision note (2026-07-18 19:14Z): Recorded the minimized failing runtime-SET `REDUCED` regression and the mutable
binding aliasing behavior that invalidates the runtime support-preservation rule.

Revision note (2026-07-18 19:17Z): Corrected the invalid first minimization, recorded the direct SET-equivalence
evidence, reverted the premature production guard, and resumed reproduction from the original complex artifact.

Revision note (2026-07-18 19:21Z): Promoted the original serialized pair into repository resources and recorded the
valid focused false-equivalence failure, including its error-vs-success runtime semantics.

Revision note (2026-07-18 19:23Z): Restricted the support-preservation rule to specification targets and recorded the
focused post-fix green evidence.

Revision note (2026-07-18 19:26Z): Recorded the broader post-fix regression evidence and corrected the invalid slow
replay expectation without deleting its artifact.

Revision note (2026-07-18 19:44Z): Added the interim independent mandatory Java replay boundary and immutable
certificate envelope, documented its remaining local-step limitation, and made specification witnesses fail closed
instead of using the RDF4J runtime evaluator.
