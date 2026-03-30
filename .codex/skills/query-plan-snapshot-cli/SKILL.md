---
name: query-plan-snapshot-cli
description: Use QueryPlanSnapshotCli to capture and compare RDF4J query plans, then assess likely performance improvements/regressions from execution verification and semantic plan diffs. Trigger when users ask about optimizer impact, query-plan drift, join algorithm changes, or query performance regressions in testsuites/benchmark.
---

# query-plan-snapshot-cli

Use this skill to run reproducible query-plan captures and classify likely regression/improvement signals.

## Fast workflow

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
