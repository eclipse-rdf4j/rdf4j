# Algebra equivalence certification review packet

## Candidate identity

Review exactly this candidate:

`bf9edabb0006bca3b422b45f10c38723ed8863af+a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`

The first component is the RDF4J base/runtime-oracle commit. The second is the SHA-256 of
`SOURCE_MANIFEST.sha256`, which contains the path and SHA-256 of every file in the 155-file certification source
bundle.

From `core/queryalgebra/equivalence`, verify the candidate before reviewing:

```sh
shasum -a 256 reviews/SOURCE_MANIFEST.sha256
shasum -a 256 -c reviews/SOURCE_MANIFEST.sha256
```

The first command must print
`a693a564067c036baaa91b7eb1ea4d9f645879678bad8f74d9cd6e34c7142d7c`; the second must report every file `OK`.
Any mismatch means the review does not apply to this candidate.

The candidate is published as transport commit `3c0818fa99f9f1ad3921f8ef6644648c08a1e1f6` on
`https://github.com/eclipse-rdf4j/rdf4j/pull/5948`. The PR commit makes the files reviewable; the base-plus-digest
identifier above remains the authoritative semantic identity.

A 238,643-byte transport archive containing those 155 files plus the dossier, mutation review, review records,
ExecPlan, evidence ledger, and final reactor log is retained at
`/tmp/GH-5905-algebra-equivalence-candidate-a693a564.tar.gz`. Its SHA-256 is
`7ad8b85292bebc85f0358d49f4dee44220954e3611a72b564b0a14f3c6985d22`. The archive hash identifies the transport
artifact; `SOURCE_MANIFEST.sha256` remains the authoritative semantic-source identity. An independent extraction into
`/tmp/GH-5905-review-verify.2CvMxf` revalidated the manifest digest and all 155 source entries.

## Claim under review

Within the documented semantic domain, every `EQUIVALENT` result has a machine-checked soundness certificate, every
`NOT_EQUIVALENT` result has a target-correct distinguishing witness, and unsupported cases return `UNKNOWN`.

This is deliberately sound but incomplete. It does not claim that every pair receives a concrete verdict, that the
JVM implementation is formally verified, or that external extension nodes and unmodelled backends are supported.
The full boundary and trusted computing base are in `../CERTIFICATION.md`.

## Pinned semantics and tools

- RDF4J runtime: commit `bf9edabb0006bca3b422b45f10c38723ed8863af`.
- SPARQL 1.1: `https://www.w3.org/TR/2013/REC-sparql11-query-20130321/`, recorded HTML SHA-256
  `93c68d597452c329908cd48f7755baf8c89752b7c4c80070556889b7f5477363` (487,771 bytes).
- SPARQL 1.2: 5 June 2026 Working Draft,
  `https://www.w3.org/TR/2026/WD-sparql12-query-20260605/`, recorded HTML SHA-256
  `5c49b76204782d11d580a31b17b4c7dbdebd9d2b0a6b3497862163079ecc5ffe` (1,019,152 bytes).
- Fresh official downloads reproducing those hashes are retained at
  `/tmp/rdf4j-sparql11-query-20130321.html` and `/tmp/rdf4j-sparql12-query-20260605.html`.
- Lean 4.32.0, standard library only.
- PIT 1.25.3 with JUnit 5 adapter 1.2.3.
- Java 25 and Maven 3.9.15.

## Machine-gate summary

| Gate | Accepted result |
| --- | --- |
| Rule/profile matrix | 1,024 cells: 347 theorem-backed, 677 explicitly inapplicable, 0 pending |
| Lean theorem inventory | 78 theorems; only `propext`, `Classical.choice`, and `Quot.sound`; no project axiom |
| Runtime bounded universe | 4,224 cells; 679 certified equivalent, 2,610 twice-replayed non-equivalent, 0 violations |
| Corpus inventory | 1,168 queries; 989 Java/Lean certificates; every unsupported case required `UNKNOWN` |
| PIT mutation | 477/477 killed; 0 survived/uncovered/excluded/equivalent waivers |
| Runtime differential release | 1,000,000 pairs over 10 seeds; 385,485 certificates; 18,503,280 Lean oracle cases |
| Runtime rule-shaped release | 100,000 pairs over 10 seeds; 26,347 certificates; 1,264,656 Lean oracle cases |
| W3C releases | 100,000 pairs for each target; 200,000 certificates; 9,317,760 Lean oracle cases |
| W3C evaluation suites | SPARQL 1.1: 176/176; SPARQL 1.2: 66/66 |
| Final module | 177/177 tests |
| Final reactor | Offline `-Pquick install`, all modules, 2:01 wall-clock |

The rejected 10,800-second runtime campaign is retained as negative evidence: time-budget truncation correctly failed
the gate and contributed no accepted pairs. The fresh 43,200-second rerun started from zero and completed the exact
quota.

## Evidence map

- Complete dossier: `../CERTIFICATION.md`.
- Living execution record: `../../../../.agent/GH_5905_ALGEBRA_EQUIVALENCE_CERTIFICATION_EXEC_PLAN.md`.
- Command/report ledger: repository-root `initial-evidence.txt`.
- Lean model and executable kernel: `../formal/`.
- Rule/profile matrix: `../formal/rule-coverage.csv`.
- Mutation review: `../MUTATION_REVIEW.md`.
- PIT machine report: `../target/pit-reports/mutations.xml`.
- Runtime random release: `../target/certification/runtime-random-release-final-2/`.
- Runtime rule release: `../target/certification/runtime-rule-release-final/`.
- W3C formal release: `../target/certification/specification/`.
- Corpus certificate batch: `../target/certification/corpus/`.
- Preserved original soundness artifact:
  `/tmp/rdf4j-equivalence-rule-deep-20260727/seed-20260727/001-soundness`.

Generated `target/` and `/tmp/` evidence is intentionally outside the source digest. A reviewer should validate the
source identity first, then confirm that the retained logs/manifests name that same base/runtime revision and that no
semantic source changed after the final runs.

## Reproduction commands

Run from the repository root unless a directory is specified:

```sh
python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/equivalence --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py --it MemorySPARQL11QueryComplianceTest \
  --module compliance/sparql --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py --it MemorySPARQL12QueryComplianceTest \
  --module compliance/sparql --retain-logs

cd core/queryalgebra/equivalence/formal
lake build
rg -n '\b(sorry|admit|axiom)\b' --glob '*.lean' .
.lake/build/bin/check-certificates ../target/certification/runtime-random-release-final-2/certificates.jsonl
.lake/build/bin/check-certificates ../target/certification/runtime-rule-release-final/certificates.jsonl
.lake/build/bin/check-certificates ../target/certification/specification/certificates.jsonl
.lake/build/bin/check-certificates ../target/certification/corpus/certificates.jsonl
```

The release-generation and PIT commands, fixed seeds, exact expected counts, logs, and reports are recorded in the
dossier and `initial-evidence.txt`. Full release regeneration is required if any semantic source changes.

## Required decisions

The RDF4J evaluator reviewer completes `RDF4J_EVALUATOR_SIGNOFF.md`. The formal-methods reviewer completes
`FORMAL_METHODS_SIGNOFF.md`. Reviewers must be independent of the implementation and must either approve this exact
candidate identifier or request changes with concrete findings. Approval of a different digest does not satisfy the
gate.

Copy-ready requests and the acceptance criteria for returned reviews are in `REVIEW_REQUEST.md`. A read-only audit of
PR #5948 at 2026-07-20 09:30Z found the exact transport commit at its head, zero reviews, and no requested reviewers.
The earlier issue-number lookup established only that issue GH-5905 is not itself a PR; commit-based resolution is the
authoritative PR association. Implementation-commit references are provenance only and do not constitute either
required decision.

Until both records contain verifiable approvals, `../CERTIFICATION.md` must remain `NOT CERTIFIED`.
