# Scope-safety fixture validation

Validation date: 2026-07-13.

The 50 expected result files were authored as fixture data, not captured from the scope-safety implementation. Exact comparison includes each row's binding domain, RDF value, and duplicate multiplicity; q49 additionally compares row order.

## Agreeing engines

- RDF4J 6.0.0-SNAPSHOT: all 50 cases pass in `OFF`, `AUDIT`, `ENFORCE`, and sampled `SHADOW` on MemoryStore, NativeStore, and LMDB (600 store/mode/query evaluations). Telemetry assertions require every one of the 150 sampled `SHADOW` evaluations to reach `SHADOW_MATCH`; a skip does not satisfy this matrix.
- Apache Jena 6.1.0: all 50 cases match. The official binary archive SHA-512 was `fb5647f95d7a5a685c07ace3e691001f2bbe3335fd1b4e0a3953e91aae71eaf89a669af41786c605058e7c55a05131851af19492dbe65c6398ef7a709dc41650`.
- RDF4J's independent algebra-equivalence module: all 56 proof-kernel, finite-model, differential, and native-node coverage tests pass. Production optimizer modules do not depend on this module.

Commands:

```text
python3 scripts/generate-scope-safety-fixtures.py --check
python3 scripts/validate-scope-safety-fixtures.py --engine jena --jena-command <apache-jena-6.1.0>/bin/arq
python3 .codex/skills/mvnf/scripts/mvnf.py MemoryScopeSafetyTest#allFiftyQueriesMatchInEveryMode --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py NativeScopeSafetyTest#allFiftyQueriesMatchInEveryMode --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbScopeSafetyTest#allFiftyQueriesMatchInEveryMode --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/equivalence --retain-logs
```

## RDFLib cross-check

RDFLib 7.6.0 agrees exactly on 30 cases. It disagrees on q01, q04, q17, q19, q21, q23, q24, q27–q33, q35–q37, q40–q41, and q44. The differences are concentrated in `OPTIONAL` fallback behavior and `SELECT *` visibility for variables local to `MINUS`/`EXISTS`. Because RDF4J and Jena agree on those cases, the RDFLib output is retained as an independent engine-disagreement report rather than used to rewrite the expected files.

Command:

```text
python3 scripts/validate-scope-safety-fixtures.py --engine rdflib
```

Tool versions were checked against the official Apache Jena release page and the RDFLib 7.6.0 PyPI release before validation.

## Adversarial variable-scope addendum

Ten additional cases under `scope-safety/adversarial` were authored independently of the original generated 50-case catalog. They combine three-level subquery shadowing, declared-but-unbound `VALUES` exports, hidden variables in `OPTIONAL` and `MINUS`, alias-created `MINUS` overlap, nested `EXISTS`, disjoint `UNION` exports, aggregate input shadowing, graph-variable shadowing, and `LATERAL` correlation. Exact comparison includes result-header names and order as well as every row's binding domain, values, and multiplicity; a02 keeps an all-`UNDEF` variable in the header while leaving it absent from the row.

- Apache Jena 5.2.0 matches all ten exact SRX results.
- RDF4J passes all ten in `OFF`, `AUDIT`, `ENFORCE`, and sampled `SHADOW` on MemoryStore, NativeStore, and LMDB. Together with the original catalog, this is 720 exact store/mode/query evaluations and 180 telemetry-proven `SHADOW_MATCH` comparisons.
- The first run exposed a `BindingSetAssignmentInlinerOptimizer` state leak: a one-row `VALUES` environment inside a nested subquery or `EXISTS` remained active while visiting the parent expression. The focused optimizer regressions preserve that failure mode, and the repaired visitor now restores the parent binding environment on every variable-scope and subquery-expression exit.
