---
name: query-plan-snapshot-cli
description: Use QueryPlanSnapshotCli to capture and compare RDF4J query plans, then assess likely performance improvements/regressions from execution verification and semantic plan diffs. Trigger when users ask about optimizer impact, query-plan drift, join algorithm changes, or query performance regressions in testsuites/benchmark.
---

# query-plan-snapshot-cli

Use this skill to run reproducible query-plan captures, triage historical theme-query benchmark results, and classify likely regression/improvement signals.

## Fast workflow

1. Capture raw benchmark output into a normalized result file when needed.
2. Analyze the newest dated run against historical results.
3. Drill into the fastest known runs for a specific theme/query.
4. If needed, capture baseline/candidate plan snapshots and diff them semantically.

## History triage

Result files live in:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results`

Normalize raw JMH output into a new result file:

- `pbpaste | scripts/theme-query-benchmark-results.sh capture`
- `scripts/theme-query-benchmark-results.sh capture raw-jmh.txt`

Analyze only the queries that are more than 20% slower than history:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh`

Sort regressions from biggest to smallest:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --sort-regressions`

Only print the top N regressions:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --top 10`

Analyze every latest query, including current-run wins over previous best:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --all`

Drill into the three fastest known runs for one theme/query and print optimized plan/query when present:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --theme PHARMA --query-index 10`

Interpretation:

- Default mode: newest dated file only for the “latest” baseline; compares against all other `results-*.md`, including `results-develop.md` and `results-main-branch.md`, but prints only queries where latest is more than 20% slower than historical best.
- `--sort-regressions`: flat regression list, biggest slowdown first.
- `--top N`: top N regressions only; implies regression sorting.
- `--all`: prints every latest query; if latest is a new best it prints how much faster it is than the previous best.
- Query detail mode: top three runs sorted by score ascending; ties prefer richer files with plan/query content.
- `plan no | query yes`: optimized query rendered, no physical plan block in that result file.
- `plan no | query no`: summary-only run or no per-query capture in that file.

Use this path when the goal is optimizer-loop work: find the fastest known plan/query for a theme/query, then compare new runs back to that history before touching production logic.

## Fast regression test loop (persistent LMDB theme stores)

Theme regression/snapshot tests in `core/sail/lmdb` now support reusing a prepared LMDB store across runs.

- Enable persistent reuse:
  - `-Drdf4j.lmdb.themeRegression.persistentStore.enabled=true`
- Optional custom root directory:
  - `-Drdf4j.lmdb.themeRegression.persistentStore.root=persistent-lmdb-theme-store`
- Default root directory:
  - `persistent-lmdb-theme-store`

Behavior:

- If the store has expected `triples/data.mdb` and `values/data.mdb` sizes (from `expected-db-file-sizes.properties`), tests reuse it and skip rebuild/ingest.
- If sizes mismatch or the marker file is missing/invalid, tests rebuild the store, then refresh the expected-size file.

Example focused run:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbThemeQueryRegressionTest#socialMediaFiveCycleInterleavesValuesWithFollowsEdges -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true test`

## Install command standard

Use this root clean install when a clean build is needed:

```bash
mvn -B -ntp \
  -Dmaven.compiler.showWarnings=false \
  -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
  | tee maven-build.log \
  | awk '
     /\[WARNING\]/ { next }
      /\[ERROR\]/ { print; next }

      /Reactor Summary/ { summary=1 }
      summary { print }
    '
```

For install without clean, replace `clean install` with `install`. For a module install, add `-pl <module> -am` before `-Pquick`. Keep the `2>&1 | tee maven-build.log | awk ...` tail on every install variant; keep snapshot logs for CLI output only.

## Snapshot diff workflow

Use this when you need semantic plan diffs between two controlled captures of the same query.

1. Capture baseline run (main/reference commit).
2. Capture candidate run (changed commit) with same query selector + `--query-id`.
3. Produce semantic diff (`--compare-existing`).
4. Interpret runtime + diff together.

## Commands

Use wrapper (enforces pre-install and optional logging):

- Baseline:
  - `./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-baseline.log -- --store memory --theme MEDICAL_RECORDS --query-index 0 --query-id med-q0`
- Candidate:
  - `./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-candidate.log -- --store memory --theme MEDICAL_RECORDS --query-index 0 --query-id med-q0 --compare-latest --diff-mode structure+estimates`
- Compare existing snapshots explicitly:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot -Dexec.args="--compare-existing --query-id med-q0 --compare-indices 1,0 --no-interactive --diff-mode structure+estimates" | tee /tmp/qps-compare.log`
- Summarize improvement/regression signal:
  - `python3 ./.codex/skills/query-plan-snapshot-cli/scripts/interpret_query_plan_regression.py --baseline-log /tmp/qps-baseline.log --candidate-log /tmp/qps-candidate.log --comparison-log /tmp/qps-compare.log`

## Interpretation rule-of-thumb

- `averageMillis` down with stable `resultCount`: improvement signal.
- `averageMillis` up with stable `resultCount`: regression signal.
- `actualResultSizes=diff`: semantic/data-shape risk; perf conclusion low confidence.
- `joinAlgorithms=diff` or `structure=diff`: optimizer behavior changed; correlate with runtime delta.
- `estimates=diff` only: model/statistics shift; validate with repeated runs.

For more detailed reading patterns and triage prompts, use `references/workflow.md`.
