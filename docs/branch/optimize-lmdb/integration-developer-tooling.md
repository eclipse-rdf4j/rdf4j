# Developer tooling and reproducible source maintenance

This guide covers changed repository workflows, generated-source maintenance,
and test/report helpers. It does not treat editor/agent configuration as
runtime behavior. The historical comparison is merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` through inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`.

## SPARQL AST patch workflow

The committed parser tree has a checked-in JavaCC grammar plus two ordered,
file-scoped patches applied to generated `SyntaxTreeBuilder` and
`SyntaxTreeBuilderTokenManager` source. The authoritative workflow is
[`manage-sparql-ast.py`](../../../scripts/manage-sparql-ast.py) and its
[maintenance guide](../../../core/queryparser/sparql/JavaCC/README.md).
The grammar is
[`sparql.jjt`](../../../core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt);
the checked-in Java sources remain customized generated output.

The manager pins JavaCC 7.0.12 by SHA-256, builds a temporary generation
baseline, applies formatting before patches, and checks exact replay against
the tracked generated files. `check` is a read-only drift check in the sense
that it restores its snapshots after replay; it refuses to proceed if it
cannot safely own a temporary `sparql.jj`. `regenerate` protects locally
modified outputs, and `record` rewrites patch files only when the replay
matches the current custom Java sources exactly. These safeguards keep the
grammar, checked-in generated files, and patches from drifting independently.
The pull-request workflow invokes `python3 scripts/manage-sparql-ast.py
check`. Parser semantics themselves are in
[SPARQL language changes](integration-sparql-language.md).

Changed workflow sources include
[`01-optimize-SyntaxTreeBuilder.diff`](../../../core/queryparser/sparql/JavaCC/patches/01-optimize-SyntaxTreeBuilder.diff),
[`02-optimize-SyntaxTreeBuilderTokenManager.diff`](../../../core/queryparser/sparql/JavaCC/patches/02-optimize-SyntaxTreeBuilderTokenManager.diff),
[`test_manage_sparql_ast.py`](../../../scripts/test_manage_sparql_ast.py),
and [PR verification](../../../.github/workflows/pr-verify.yml).

## Maven workspace isolation

The new [`mvn-agent.py`](../../../scripts/mvn-agent.py) uses the shared
[`maven_workspace.py`](../../../.codex/skills/mvnf/scripts/maven_workspace.py)
helpers also used by the Maven test-runner skill. It requires a workspace ID
(`--workspace` or `MVNF_WORKSPACE`) and a literal `--` separating runner
options from Maven arguments; it validates inherited and forwarded Maven
arguments, checks the Maven version and project config, creates workspace
output/log paths, records the run, and writes Maven output there. This is a
cooperative isolation scheme for multiple local worker runs, not a separate
Maven implementation or a promise that all external tools honor the same
workspace. It is integrated with the root
[`workspace-build-root` profile](../../../pom.xml), whose output roots are
covered in [build and packaging](integration-build-and-packaging.md).

The source-level `test_maven_workspace_pom.py` protects the POM/profile
contract and the existence of the runner; `test_token_efficient_output.py`
protects output summarization behavior. These tests document regression
coverage; their presence is not a current test result.

## Compliance and report helpers

[`check-lmdb-compliance-baseline.py`](../../../scripts/check-lmdb-compliance-baseline.py)
is a static report-to-baseline comparator. It requires complete expected
suite/case inventories and compares semantic test identities, outcomes,
duplicates, contradictions, and counts. It is distinct from the
[`manage-sparql-ast.py check`](../../../core/queryparser/sparql/JavaCC/README.md)
step wired in pull-request CI. Its test suite is
[`test_lmdb_compliance_baseline.py`](../../../scripts/test_lmdb_compliance_baseline.py);
the evidence meaning and historical-state caveat are documented in
[compliance and regression mapping](integration-compliance-and-regression-map.md).

[`three-tier-report.py`](../../../scripts/three-tier-report.py) reads JMH
tables and normalizes time/op units to milliseconds. For each query/parameter
cell and regime it builds a conservative low/high envelope across supplied
runs, and only classifies one regime as faster/slower when intervals do not
overlap; overlapping intervals stay inconclusive. It can flag wide-error
results as noisy. The paired test source is
[`test_three_tier_report.py`](../../../scripts/test_three_tier_report.py).
This report utility does not run a benchmark, determine why a planner chose a
strategy, or make an overlap equivalent to proof of parity. The associated
results belong to the [performance evidence guide](integration-performance-evidence.md).

[`lmdb-regret-scorer.py`](../../../scripts/lmdb-regret-scorer.py) and its
[`lmdb-regret-queries.txt`](../../../scripts/lmdb-regret-queries.txt) define a
developer scoring workflow comparing the default LMDB dispatch arm against
named flag-disabled alternatives on selected theme queries. Its conclusions
are tied to its configured arms, paired measurements, source fingerprint and
query corpus; the existence of a scorer does not prove a current default is
optimal. See the query guide on [dispatch and strategy selection](query-arbitration-and-explanation.md)
and the [benchmark methodology](integration-performance-evidence.md).

## Runner and agent skill surfaces

The `scripts/` changes also update
[`run-single-benchmark.sh`](../../../scripts/run-single-benchmark.sh), its
Docker wrapper, and their token-efficient output test. These wrappers select
one declared JMH benchmark and can capture a JFR recording; they are
operational entry points whose measurement limits are described in
[performance evidence](integration-performance-evidence.md). The standalone
bulk loader's launcher and arguments are covered in
[bulk loading](integration-bulk-loading.md).

The changed `.agent/` and `.codex/` files package repository-specific skills,
references, and helper scripts for Maven tests, performance evidence, plan
snapshots, debugging, and source review. Their intended workflows appear in
the relevant guides above, rather than duplicating every skill body here.
The `.claude/` portion of the changed-path inventory is classified by
pathname only; its instruction content is outside this developer-tooling
guide. Agent instructions and experimental plans are developer aids, not
production implementation evidence.

## Safe maintenance sequence

For parser work, read the JavaCC guide before touching grammar or generated
files; preserve the ordered patch replay contract and let `record` serialize
the patch instead of changing offsets by hand. For a benchmark change, make
the workload, timed region, baseline and evidence output explicit before
comparing numbers. For a compliance report change, preserve a complete
identity inventory and inspect provenance before updating any accepted
outcome. For concurrent Maven work, choose a distinct workspace ID and keep
its captured logs with its output root. This doc was authored without running
any of those commands.
