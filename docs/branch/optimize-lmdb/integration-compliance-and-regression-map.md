# Compliance harness, regression suites, and frozen baselines

This guide distinguishes changes to test-discovery/evidence infrastructure
from changes to production query behavior. The historical range is merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` through inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. Compliance tests and captured
baselines are not runtime features; source links below do not report a current
test result.

## Stable SPARQL compliance identities

The shared manifest suite now derives dynamic-test display names from the
manifest directory, test name, and canonical test URI. Local `file:`/`jar:`
test URIs under the suite's classpath root are rewritten to a
`classpath:`-relative identity. Public W3C HTTP URIs and URNs remain canonical
as-is, as do local resources outside the classpath root. This removes checkout
and build-output paths from identities while preserving distinct manifest
cases; it also makes generated test names suitable for stable reports and
baselines. The suite checks that the resolved manifest URL ends in its
classpath resource so an accidental resource mismatch does not silently
create misleading identities.

`BasicTest` also corrects a duplicate dynamic display name for the
subject/context identical-variable case. `ComplianceTestcaseIdentityTest`
checks uniqueness and demonstrates local, jar, public, and URN forms without
executing the generated query test bodies. This identity machinery changes
test reporting/discovery, not SPARQL result semantics.

Sources: [`SPARQLComplianceTest`](../../../testsuites/sparql/src/main/java/org/eclipse/rdf4j/testsuite/query/parser/sparql/manifest/SPARQLComplianceTest.java),
[`BasicTest`](../../../testsuites/sparql/src/main/java/org/eclipse/rdf4j/testsuite/sparql/tests/BasicTest.java),
and [`ComplianceTestcaseIdentityTest`](../../../testsuites/sparql/src/test/java/org/eclipse/rdf4j/testsuite/query/parser/sparql/manifest/ComplianceTestcaseIdentityTest.java).

## LMDB SPARQL compliance harness

The LMDB compliance module has five required suites spanning general RDF4J
SPARQL, SPARQL 1.1 query/update, and SPARQL 1.2 query/update manifests. The
shared test support applies one configured index layout and contains a
test-only switch, `rdf4j.lmdb.compliance.standardFactory`, for selecting the
ordinary `DefaultEvaluationStrategyFactory` in the compliance arm. This is a
test diagnostic; it is not a production runtime property or a feature that
Workbench operators can toggle. LMDB storage and native query behavior remain
documented in the [storage](storage-overview.md) and
[native evaluation](query-routing-and-hosts.md) guides.

The new checked-in
[`lmdb-compliance-baseline.json`](../../../compliance/sparql/lmdb-compliance-baseline.json)
stores a complete per-suite testcase-identity manifest plus explicitly
described allowed failure/skip entries. At its recorded freeze time,
`2026-09-17`, it identifies five suites and 504 test cases; its source report
metadata records 504 observations with four failures and no errors/skips, and
also states that the captured run used working-tree review changes. The
`source.head` field is provenance for that historical report, not a result
from the current source review revision. Read the baseline's provenance fields
before interpreting its allowlist as present-day behavior.

The standalone
[`check-lmdb-compliance-baseline.py`](../../../scripts/check-lmdb-compliance-baseline.py)
compares Failsafe XML against the required suite and testcase inventory. Its
static checks reject incomplete schema/suite manifests, missing or unknown
test IDs, duplicate or contradictory outcomes, unexpected failures, errors,
or skips, and outcome/count inconsistencies; it also distinguishes resolved
allowlisted failures from those still failing. The script is part of the
developer evidence surface, not proof that CI currently invokes this gate.
The pull-request workflow change instead invokes the SPARQL AST maintenance
check, described in [developer tooling](integration-developer-tooling.md).

Sources: [`LmdbComplianceTestSupport`](../../../compliance/sparql/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbComplianceTestSupport.java),
[`LmdbSPARQLComplianceTest`](../../../compliance/sparql/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSPARQLComplianceTest.java),
[`SPARQL 1.1 query wrapper`](../../../compliance/sparql/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSPARQL11QueryComplianceTest.java),
and [pull-request verification workflow](../../../.github/workflows/pr-verify.yml).

## Federation semantics and multi-store regressions

`RepositoryFederatedServiceSemanticsTest` is a new repository-level regression
suite for the logical unit of a constant-IRI SPARQL `SERVICE` invocation. It
pins cases where chunking 16 incoming bindings across the default block size
would otherwise reveal more than one `UUID()` call; cases where a remote
failure occurs after an initial row; SILENT versus non-SILENT behavior; and
safe correlation through a synthetic row-index variable even if user data
already binds a lookalike name. The test doubles can fail after a configured
number of rows to distinguish whole-invocation results from returned prefixes.

These tests support the current semantic contract: a constant endpoint call
is one logical invocation and yields one result multiset; a non-SILENT failure
is a query error, while a failed `SERVICE SILENT` yields the singleton empty
mapping instead of a partial prefix. The implementation's partitioning,
buffering, and merge tradeoffs are described in
[federation semantics](integration-federation-semantics.md). This test's
declared behavior is more general than the branch's service-specific source
edit: it is a conformance/regression boundary, not a promise to call a remote
endpoint once per input row.

Changed MemoryStore and NativeStore paths in this range are cross-store
regressions for shared query-model/evaluator behavior, not new MemoryStore or
NativeStore implementations. Their role is to ensure that shared changes such
as binding compatibility, ordering, parser semantics, and transaction-aware
add dispatch are exercised outside LMDB. The primary changed shared test
anchors are the API/factory binding-index suites, the `testsuites/sparql`
manifest/dynamic tests, and `RepositoryFederatedServiceSemanticsTest`.

Relevant sources: [`RepositoryFederatedServiceSemanticsTest`](../../../compliance/repository/src/test/java/org/eclipse/rdf4j/repository/sparql/federation/RepositoryFederatedServiceSemanticsTest.java),
the existing [federated service integration suite](../../../compliance/repository/src/test/java/org/eclipse/rdf4j/repository/sparql/federation/RepositoryFederatedServiceIntegrationTest.java),
and [federation implementation guide](integration-federation-semantics.md).

## Evidence and maintenance boundary

Read source tests to learn which inputs and contracts they intend to protect;
read the baseline and archived report to learn which outcomes were historically
observed. Neither source presence nor a stored green/red summary establishes
that the current source passes; the source map is not a current test result.
If a test is added or renamed, update the stable manifest identity/required
testcase inventory and its review evidence coherently; do not broaden the allowed-failure list just
to make a comparison quiet. When interpreting an allowed failure, follow the
per-case description and source behavior rather than treating the allowlist
as the expected result for every implementation.

Source tests, frozen baseline reports, and static/build verification have
different evidence roles. This guide treats source links as coverage pointers
and the frozen report as historical evidence only.
