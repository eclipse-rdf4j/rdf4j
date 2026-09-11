# Equivalence certification mutation review

## Status

**PASSED — all generated mutants killed.**

This is the review record for the latest PIT 1.25.3 run on 2026-07-19. No mutant was declared equivalent, excluded, or
removed from the certification profile to reach the threshold.

## Run summary

- Command: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/equivalence
  -Pequivalence-certification-mutation test-compile org.pitest:pitest-maven:mutationCoverage`
- Mutated classes: certificate kernel and evidence types, independent replay normalizer, semantic-field comparison,
  native schema and validator, observation outcomes, target metadata, and counterexample target/oracle validation.
- Generated: 477.
- Killed: 477 (100%).
- Survived: 0.
- No coverage: 0.
- Mutated-line coverage: 756/768 (98%).
- Covered-mutant test strength: 100%.
- PIT classification: zero `EQUIVALENT`, zero excluded.
- Machine report: `target/pit-reports/mutations.xml`.

## Resolution history

The first expanded run generated 535 mutants, killed 442 (83%), left 37 survivors, and left 56 uncovered. Focused
tests then exercised certificate-envelope mutation, target validation, field-complete comparison, replay side
conditions, counterexample counts, repeat validation, and semantic rejection. Two unreachable replay identity returns
were replaced by explicit caller invariants, and generated/configured evaluation cases were routed through one tested
target-validation path. The penultimate run killed 516/522 with six uncovered sites; the final replay and scripted
oracle tests closed all six. Subsequent path-local replay and field-complete hashing changes expanded the target to 639
mutants. The first new run killed 606 with 24 uncovered and 9 survivors; explicit semantic-field tests and kernel
rejection tests raised that to 638/639 with no uncovered mutants, and the unknown-extension fail-closed test killed the
last survivor. Incremental replay validation briefly expanded the profile to 643/643. The final source simplification
and explicit inclusion of `AlgebraEquivalenceChecker` generated 477 mutants; the first run killed 472 with three
survivors and two uncovered sites. Focused structural-proof, Boolean-facade, and skipped-case tests killed all 477 in
the final run.

The authoritative per-mutant identity, mutator, bytecode index, description, and killing test remain in
`target/pit-reports/mutations.xml`. Passing this gate does not by itself certify semantic soundness; the remaining
campaign, repository, and independent-review gates are listed in `CERTIFICATION.md`.
