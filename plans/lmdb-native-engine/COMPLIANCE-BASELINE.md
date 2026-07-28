# LMDB SPARQL compliance baseline

Plan 13 §1.5 freezes the 24 LMDB-only failures observed before Phase I. The machine-readable source of
truth is `compliance/sparql/lmdb-compliance-baseline.json`; this document records its provenance and
operating contract. The baseline may shrink in an observed run, but no failure or error outside these
24 identities is accepted.

## Provenance

- Module: `compliance/sparql` (`rdf4j-sparql-compliance`).
- Frozen run: `logs/mvnf/20260718-144033-verify.log`.
- Git HEAD during the run: `42e6ab6f2e`, reconstructed from the local reflog.
- Runtime: JDK 25.0.2 and Maven 3.9.15 on macOS arm64.
- Result: 2,648 tests, 24 failures, 0 errors, 3 skipped.
- Control suites in the same run: MemoryStore, NativeStore, ExtensibleStore, FedX, and parser
  compliance produced none of these failures. Every frozen identity belongs to an LMDB suite.

The dynamic-test report identity (`fully.qualified.Class#method()[index]`) is the comparison key because
it is what Failsafe writes to `TEST-*.xml`. The description is retained beside it so index drift can be
reviewed rather than silently accepted.

## Frozen failures

| Suite | Report identity | Description |
| --- | --- | --- |
| SPARQL 1.1 query | `LmdbSPARQL11QueryComplianceTest#tests()[31]` | bind10 — BIND scoping, filter variable not in scope |
| SPARQL 1.1 query | `LmdbSPARQL11QueryComplianceTest#tests()[35]` | Post-query VALUES, two object variables and one row |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[13]` | INSERT 01 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[35]` | DELETE INSERT 1 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[36]` | DELETE INSERT 1b |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[37]` | DELETE INSERT 1c |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[38]` | DELETE INSERT 2 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[39]` | DELETE INSERT 4 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[40]` | DELETE INSERT 4b |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[41]` | DELETE INSERT 5b |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[43]` | Simple DELETE WHERE 1 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[47]` | Graph-specific DELETE WHERE 1 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[49]` | Simple DELETE 1 |
| SPARQL 1.1 update | `LmdbPARQL11UpdateComplianceTest#getTestData()[53]` | Graph-specific DELETE 1 |
| Repository SPARQL | `LmdbSPARQLComplianceTest#defaultGraph()[1]` | `DefaultGraphTest.testSesameNilAsGraph` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#filterScopeTests()[1]` | `FilterScopeTest.testScope1` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#filterScopeTests()[5]` | `FilterScopeTest.testScope3` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#bind()[8]` | `BindTest.testBindScope` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#minus()[1]` | `MinusTest.testScopingOfFilterInMinus` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#aggregate()[21]` | `AggregateTest.testSES2361UndefCountWildcard` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#builtinFunction()[20]` | `BuiltinFunctionTest.testSES869ValueOfNow` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#minusScope()[12]` | `SparqlMinusScopingTests.T15_rhs_filter_referencing_outer_var_is_unbound_and_ignored` |
| Repository SPARQL | `LmdbSPARQLComplianceTest#minusScope()[13]` | `SparqlMinusScopingTests.T16_rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset` |
| SPARQL 1.2 query | `LmbdSPARQL12QueryComplianceTest#tests()[46]` | Reified triples — embedded triple value-equality |

The historical class-name spellings `LmdbPARQL...` and `LmbdSPARQL...` are intentional and match the
checked-in suites and Failsafe reports.

## Automated comparison gate

Run the compliance module in an isolated workspace. `maven.test.failure.ignore` lets Maven distinguish a
compile/lifecycle failure from the expected test failures; the checker then owns the test-result verdict.

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py \
  --workspace agent-compliance-baseline \
  compliance/sparql --retain-logs -- -Dmaven.test.failure.ignore=true

python3 scripts/check-lmdb-compliance-baseline.py \
  .mvnf/workspaces/agent-compliance-baseline/build/org.eclipse.rdf4j/\
rdf4j-sparql-compliance/6.1.0-SNAPSHOT/failsafe-reports
```

The checker fails closed when no `TEST-*.xml` files are present, any of the five LMDB suite reports is
missing, XML is malformed, or any failure/error is not in the frozen set. A strict subset passes and the
resolved identities are printed. The executable's focused tests are
`scripts/test_lmdb_compliance_baseline.py`.

## Current observation

The isolated 2026-07-20 validation produced 17 failures, all in the frozen set, so the gate passed with
seven resolved entries and zero new failures. Evidence is retained in
`initial-evidence.agent-compliance-baseline.txt` and
`post-evidence.agent-compliance-baseline-gate.txt`; Maven logs and XML reports are under workspace
`agent-compliance-baseline`.
