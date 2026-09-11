# Algebra equivalence checker certification dossier

## Status

**NOT CERTIFIED — every machine acceptance gate passed, but two independent reviews remain open.**

This is a live audit dossier. A passing individual test, bounded search, mutation score, or fuzz campaign never changes
the status by itself. The status may become certified only after every machine gate below is complete on one identified
source state and both independent reviewers approve that state.

## Claim and boundary

The intended certification claim is:

> Within the documented semantic domain, every `EQUIVALENT` result has a machine-checked soundness certificate, every
> `NOT_EQUIVALENT` result has a target-correct distinguishing witness, and unsupported cases return `UNKNOWN`.

This is a sound-but-incomplete claim. `UNKNOWN` is valid and completeness is not claimed. It is not a claim that the
software has no possible bug, that every pair terminates under every resource limit, or that every RDF4J extension,
backend, or physical operator is modeled.

The four targets are RDF4J runtime behavior at the recorded source revision, the dated SPARQL 1.1 Recommendation, the
dated 5 June 2026 SPARQL 1.2 Working Draft, and the conjunction of those two W3C targets. Observations are BAG, SET,
SEQUENCE, and ASK; incoming contexts are top-level empty and arbitrary permitted mappings.

## Source and specification identity

- Branch: `GH-5905-algebra-equivalence`.
- Base Git commit and runtime-oracle revision: `bf9edabb0006bca3b422b45f10c38723ed8863af`.
- Candidate transport commit: `3c0818fa99f9f1ad3921f8ef6644648c08a1e1f6`, published on
  `https://github.com/eclipse-rdf4j/rdf4j/pull/5948`.
- Candidate state: the tested semantic bundle is identified by the base commit plus the deterministic source digest
  below. A transport commit may also contain dossier and review files outside that digest; the base commit alone does
  not contain the implementation changes.
- Candidate source-bundle identifier:
  `bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`.
- Source digest: SHA-256 `a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c` over 155
  sorted files: the module `pom.xml`, `src/`, and `formal/`, excluding `formal/.lake` build outputs. Command:
  `find pom.xml src formal -path 'formal/.lake' -prune -o -type f -print | LC_ALL=C sort | xargs shasum -a 256 |
  shasum -a 256`.
- Reproducible path/hash inventory: `reviews/SOURCE_MANIFEST.sha256`. Its SHA-256 equals the source digest above, and
  `shasum -a 256 -c reviews/SOURCE_MANIFEST.sha256` verifies all 155 files.
- Reviewer transport archive: `/tmp/GH-5905-algebra-equivalence-candidate-a693a564.tar.gz` (238,643 bytes; SHA-256
  `7ad8b85292bebc85f0358d49f4dee44220954e3611a72b564b0a14f3c6985d22`). The manifest, not this transport hash, is
  authoritative for the semantic source bundle.
- SPARQL 1.1 Query Recommendation:
  `https://www.w3.org/TR/2013/REC-sparql11-query-20130321/`.
  Freshly downloaded HTML: 487,771 bytes; SHA-256
  `93c68d597452c329908cd48f7755baf8c89752b7c4c80070556889b7f5477363`.
- SPARQL 1.2 Query Working Draft, 5 June 2026:
  `https://www.w3.org/TR/2026/WD-sparql12-query-20260605/`.
  Freshly downloaded HTML: 1,019,152 bytes; SHA-256
  `5c49b76204782d11d580a31b17b4c7dbdebd9d2b0a6b3497862163079ecc5ffe`.
- Retained fetches: `/tmp/rdf4j-sparql11-query-20130321.html` and
  `/tmp/rdf4j-sparql12-query-20260605.html`.

## Toolchain

- Java: Azul OpenJDK 25.
- Maven: 3.9.15 with workspace-local repository `.m2_repo`.
- Lean: 4.32.0, release commit `8c9756b28d64dab099da31a4c09229a9e6a2ef35`.
- Lean dependencies: Lean's distributed standard library only; no Mathlib or other Lake package.
- PIT Maven plugin: 1.25.3.
- PIT JUnit 5 adapter: 1.2.3.
- Certificate format: version 2 JSON Lines.

## Positive-verdict kernel

`Rdf4jCanonicalizer` is an untrusted certificate producer. Every positive result, including exact structural
identity, carries an immutable `NormalizationCertificate`; structural identity is encoded as a zero-step trace.
Certificate-less compatibility constructors are deprecated and their evidence is rejected.

`ProofKernel` is mandatory for every `EQUIVALENT` return. It does not call the canonicalizer or reuse the producer's
binding, incoming-binding, or safety analyzers. It resolves stable child-index paths, recomputes side conditions,
applies one theorem-registered local rule at a time, compares full trees through the field-complete schema, and rejects
unconsumed, malformed, reordered, extra, or mismatched steps. The legacy `deepProofVerification` option controls only
an additional producer-parity diagnostic; it is not the soundness gate.

The independently built Lean executable decodes version-2 exact-tree JSON, validates target/observation/context
metadata, restricts accepted steps to the theorem-backed root-local fragment, constructs checked traces, and checks the
finite Java/Lean correspondence oracle. It rejects nested steps and unknown rules. Its executable acceptance theorem is
`accepted_executable_certificate_sound`.

## Formal model and theorem inventory

The Lean project models RDF terms, extensional solution mappings, bags, sets, sequences, ASK emptiness, collapsed
failures, datasets, incoming mappings, function purity/impurity, the supported expression fragment, RDF4J runtime,
SPARQL 1.1, dated SPARQL 1.2, and the conjunction target.

There are 78 explicit `#print axioms` entries. Every listed theorem is permitted to use only the standard Lean axiom
set `propext`, `Classical.choice`, and `Quot.sound`; no project-defined axiom is permitted. The build output records
the exact subset used by each theorem.

| Group | Theorems | Permitted standard Lean axioms |
| --- | --- | --- |
| Model and certificate composition | `equivalent_refl`, `equivalent_symm`, `equivalent_trans`, `equivalent_in_every_context`, `equivalent_in_context_refl`, `equivalent_in_context_symm`, `equivalent_in_context_trans`, `inventory_entry_sound`, `accepted_certificate_sound` | `propext`, `Classical.choice`, `Quot.sound` |
| Observation laws | `observe_append_associative`, `observe_append_empty_left`, `observe_append_empty_right`, `observe_bag_append_commutative`, `observe_set_append_commutative`, `observe_ask_append_commutative`, `observe_set_append_idempotent`, `observe_ask_append_idempotent`, `observe_set_eraseDups`, `observe_ask_eraseDups` | `propext`, `Classical.choice`, `Quot.sound` |
| Outcome laws | `unionOutcome_associative`, `observe_union_commutative`, `observe_union_empty_left`, `observe_union_empty_right`, `observe_union_set_idempotent`, `observe_union_ask_idempotent`, `deduplicateMappings_nodup`, `deduplicateMappings_eq_self`, `deduplicateMappings_idempotent`, `mem_deduplicateMappings`, `distinctOutcome_idempotent`, `filterOutcome_true_identity`, `filterOutcome_false_success`, `filterRowsOutcome_true`, `filterRowsOutcome_false_success`, `filterRowsOutcome_union`, `leftJoinRows_append`, `leftJoinOutcome_success`, `leftJoinOutcome_union_left`, `minusRows_append_left`, `minusRows_union_right`, `minusRows_blockers_commute`, `minusRows_blocker_idempotent`, `minusOutcome_success`, `minusOutcome_union_left`, `observe_distinct_set_hidden`, `observe_distinct_ask_hidden`, `observe_values_empty`, `observe_values_unit` | `propext`, `Classical.choice`, `Quot.sound` |
| Reference semantics | `referenceEval_join_empty_right`, `reference_union_associative`, `reference_filter_distributes_over_union`, `reference_join_unit_left`, `reference_join_unit_right`, `reference_join_empty_left`, `reference_join_empty_right_of_success`, `reference_leftJoin_empty_left`, `reference_leftJoin_empty_right`, `reference_leftJoin_unit_right`, `reference_minus_empty_left`, `reference_minus_empty_right`, `reference_union_commutative`, `reference_union_empty_left`, `reference_union_empty_right`, `reference_union_set_idempotent`, `reference_union_ask_idempotent`, `reference_distinct_idempotent`, `reference_distinct_set_hidden`, `reference_filter_true_identity`, `reference_filter_false_empty`, `reference_distinct_ask_hidden`, `reference_reduced_set_hidden`, `reference_reduced_ask_hidden`, `reference_values_empty`, `reference_runtime_values_unit_all_bindings`, `reference_values_unit_top_level` | `propext`, `Classical.choice`, `Quot.sound` |
| Executable certificate kernel | `checked_rewrite_sound`, `checked_trace_sound`, `accepted_executable_certificate_sound` | `propext`, `Classical.choice`, `Quot.sound` |

`formal/rule-coverage.csv` is the machine-readable 32-profile matrix for every `RuleId`:

- Total cells: 1,024.
- Theorem-backed cells: 347.
- Explicit implementation-inapplicability cells: 677.
- Pending cells: zero.

Inapplicable rules cannot be accepted by `CertifiedRuleRegistry` or the Lean executable. They conservatively force
`UNKNOWN`; they are not assumed sound.

## Structural equality and unsupported algebra

Positive structural comparison never calls RDF4J node `equals()`, `hashCode()`, or diagnostic fingerprints.
`NativeAlgebraSchema` classifies every concrete non-update native query-model class and every instance field as
semantic or explicitly non-semantic. The coverage test fails when RDF4J adds a native class or field without a
classification. Known opaque native nodes can prove only field-complete structural identity. Unknown extension-node
implementations return `UNKNOWN`.

## Counterexample correctness

Every `Counterexample` records its semantic target and oracle version. The old constructor remains source-compatible,
is deprecated, and labels its runtime oracle unversioned.

RDF4J runtime witnesses use cloned trees, a MemoryStore with an empty optimizer pipeline, the exact recorded RDF4J
revision, and two independent replays. Volatile, external, SERVICE, REDUCED, SAMPLE, blank-node-generating, and
slice-sensitive cases fail closed when a stable single-run witness cannot be guaranteed.

RDF4J evaluation is never used as W3C evidence. The current Java checker has no certified specification-witness bridge,
so specification-target searches that cannot produce formal evidence return `UNKNOWN`. Consequently every concrete
`NOT_EQUIVALENT` currently produced is an RDF4J-runtime witness. The conjunction target likewise returns `UNKNOWN`
unless a target-correct constituent witness producer exists.

## Deterministic bounded universe

`RuntimeBoundedUniverseTest` exhaustively compared a symmetry-reduced universe of 32 structurally distinct
expressions, five finite datasets, the empty incoming mapping plus three non-empty mappings where permitted, all four
observations, and both context modes. Expressions include empty/unit/VALUES leaves, default and named statement
patterns, JOIN, UNION, constant/variable/error FILTER, OPTIONAL, MINUS, DISTINCT, REDUCED, and projection. Terms include
IRIs, literals, named graphs, and RDF 1.2 triple terms.

- Profile/pair cells: 4,224.
- `EQUIVALENT` cells with accepted Java evidence and matching optimizer-free outcomes: 679.
- `NOT_EQUIVALENT` cells with twice-replayed target witnesses: 2,610.
- Violations, incomplete evaluations, and asymmetric verdict conflicts: zero.

Absence of a bounded counterexample is never treated as a proof.

## W3C and RDF4J corpus inventory

`CorpusCertificateAgreementTest` inventories the supported fragment in five repository corpora. Parse failures and
unsupported target algebra are classified explicitly, and an otherwise parseable tree that cannot be encoded for
independent replay is required to return `UNKNOWN`.

| Corpus | Query files | Parsed | Parse unsupported | Target algebra unsupported | Certificate encoding unsupported | Java/Lean certificates |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| W3C SPARQL 1.0 | 443 | 391 | 52 | 2 | 0 | 389 |
| W3C SPARQL 1.1 | 369 | 324 | 45 | 0 | 7 | 317 |
| RDF4J SPARQL 1.1 | 116 | 113 | 3 | 0 | 0 | 113 |
| W3C SPARQL 1.2 | 231 | 161 | 70 | 0 | 0 | 161 |
| RDF4J SPARQL 1.2 | 9 | 9 | 0 | 0 | 0 | 9 |
| **Total** | **1,168** | **998** | **170** | **2** | **7** | **989** |

The Java kernel accepted all 989 emitted certificates. Lean independently accepted all 989 over 47,472 finite-oracle
cases.

## Mutation analysis

The final PIT 1.25.3 profile includes `AlgebraEquivalenceChecker`, the Java proof kernel, certificate evidence,
theorem registry, local replay, exact-tree comparison, native schema, observation comparison, target validation, and
counterexample validation.

- Generated: 477.
- Killed: 477 (100%).
- Survived: zero.
- No coverage: zero.
- Equivalent/excluded waivers: zero.
- Mutated-line coverage: 756/768 (98%).
- Covered-mutant test strength: 100%.
- Machine report: `target/pit-reports/mutations.xml`.
- Review history: `MUTATION_REVIEW.md`.

## Release campaigns

Every campaign requires at least ten deterministic seeds, exact quotas, no time truncation, no soundness,
witness-validity, determinism, robustness, Java-replay, or Lean-replay violation.

| Campaign | Exact quota | Seeds | Java certificates | Lean oracle cases | Status |
| --- | ---: | ---: | ---: | ---: | --- |
| RDF4J runtime differential | 1,000,000 | 10 | 385,485 | 18,503,280 | PASS |
| RDF4J runtime rule-shaped | 100,000 | 10 | 26,347 | 1,264,656 | PASS |
| SPARQL 1.1 formal target | 100,000 | 10 | 100,000 | combined below | PASS |
| SPARQL 1.2 dated formal target | 100,000 | 10 | 100,000 | combined below | PASS |

The two W3C targets produced 200,000 certificates; Lean accepted all of them over 9,317,760 oracle cases. The first
post-fix runtime differential rerun was rejected after 11,668 seconds because its 10,800-second hard budget truncated
the workload. Its partial output is retained but contributes zero accepted pairs. The fresh-from-zero rerun with the
same inputs and a 43,200-second hard budget completed in 22,754.152 seconds. All ten seeds reached 100,000 pairs, Java
emitted 385,485 certificates, and Lean accepted all of them over 18,503,280 oracle cases.

The runtime seeds are 1493528833 through 1493528841 plus 1493528848. The W3C seeds are 1493504257 through 1493504265
plus 1493504272.

## Verification record

Retained Maven and Surefire evidence is indexed in the repository-root `initial-evidence.txt`. Important completed
commands include:

```sh
python3 .codex/skills/mvnf/scripts/mvnf.py +  JsonTupleExprSoundnessRegressionTest#runtimeReducedIsNotHiddenBySetObservation --retain-logs

python3 .codex/skills/mvnf/scripts/mvnf.py RuntimeBoundedUniverseTest --retain-logs -- +  -PslowTestsOnly,\!skipSlowTests

python3 .codex/skills/mvnf/scripts/mvnf.py CorpusCertificateAgreementTest --retain-logs -- +  -PslowTestsOnly,\!skipSlowTests -Dequivalence.certification.corpusTimeBudgetSeconds=1800

python3 .codex/skills/mvnf/scripts/mvnf.py SpecificationCertificateCampaignTest --retain-logs -- +  -PslowTestsOnly,\!skipSlowTests -Dequivalence.certification.specPairsPerTarget=100000 +  -Dequivalence.certification.specTimeBudgetSeconds=3600

mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/equivalence +  -Pequivalence-certification-mutation test-compile org.pitest:pitest-maven:mutationCoverage

cd core/queryalgebra/equivalence/formal
lake build
.lake/build/bin/check-certificates ../target/certification/specification/certificates.jsonl
```

The relevant W3C query suites passed through the compliance module's Failsafe path:

- `MemorySPARQL11QueryComplianceTest`: 176 tests, zero failures/errors/skips.
- `MemorySPARQL12QueryComplianceTest`: 66 tests, zero failures/errors/skips.

The final non-slow equivalence module passed 177 tests with zero failures, errors, or skips. `git diff --check` passed;
the final Lean source scan found no `sorry`, `admit`, or project `axiom`; all 78 `#print axioms` entries are present.
The final offline repository `-Pquick install` passed every reactor module in 2:01 wall-clock without cleaning retained
reports. The final 155-file source digest is recorded above.

## Independent review

Both approvals must identify candidate source-bundle identifier
`bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`:

- Reviewer runbook and evidence map: `reviews/REVIEW_PACKET.md`.
- Copy-ready independent-review requests: `reviews/REVIEW_REQUEST.md`.
- RDF4J evaluator review: `reviews/RDF4J_EVALUATOR_SIGNOFF.md` — **PENDING**.
- Formal-methods review: `reviews/FORMAL_METHODS_SIGNOFF.md` — **PENDING**.

An automated agent, campaign, or mutation tool is not a substitute for either independent reviewer.
The initial issue-number lookup established only that GH-5905 is not itself a pull request. After the candidate was
pushed, commit-based resolution identified open PR #5948 at exact head
`3c0818fa99f9f1ad3921f8ef6644648c08a1e1f6`. At 2026-07-20 09:30Z it had zero reviews and no requested reviewers, so
neither independent gate was satisfied.

## Trusted computing base

The trusted base includes the Lean kernel/compiler, correctness of the RDF4J and W3C specification transcription, the
JVM, the pinned RDF4J evaluator, and the dated specification documents. The Java certificate producer is untrusted.
The Java kernel is defended by separation, exact-tree replay, tests, and mutation analysis. Java/Lean correspondence is
independently and exhaustively tested over bounded/campaign domains, but this is not a full formal verification of the
JVM implementation or JSON encoder.

## Invalidation policy

After certification, any change to semantic fields, evaluator behavior, accepted rules, proof code, certificate
format, Java/Lean correspondence, toolchain, or pinned specifications invalidates the certificate and reruns every
gate. Comment-only documentation changes do not alter semantic evidence, but the final source identifier must still
match the reviewed bundle.
