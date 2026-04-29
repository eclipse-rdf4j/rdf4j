# QueryPlanSnapshotCli workflow

## Goal

Read optimizer/query-plan changes as performance signals without mixing in unrelated variables.

## Guardrails

- Same store, theme, and query selector between baseline/candidate.
- Same `--query-id` to simplify lookup.
- Keep JVM/system-property flags identical unless intentionally testing a flag.
- Always refresh build artifacts first:
  - `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`

## Minimal run pair

1. Baseline:

`./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-baseline.log -- --store memory --theme MEDICAL_RECORDS --query-index 0 --query-id med-q0`

2. Candidate:

`./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-candidate.log -- --store memory --theme MEDICAL_RECORDS --query-index 0 --query-id med-q0 --compare-latest --diff-mode structure+estimates`

3. Explicit compare-existing (stable reproducible diff text):

`mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot -Dexec.args="--compare-existing --query-id med-q0 --compare-indices 1,0 --no-interactive --diff-mode structure+estimates" | tee /tmp/qps-compare.log`

4. Regression/improvement summary:

`python3 ./.codex/skills/query-plan-snapshot-cli/scripts/interpret_query_plan_regression.py --baseline-log /tmp/qps-baseline.log --candidate-log /tmp/qps-candidate.log --comparison-log /tmp/qps-compare.log`

## Reading semantic diff fields

- `structure=diff`: operator tree changed.
- `joinAlgorithms=diff`: join strategy changed; usually high-impact for runtime.
- `actualResultSizes=diff`: result-size flow changed; can indicate data-shape or semantic shifts.
- `estimates=diff`: cost model changed. In isolation, not enough to claim runtime regression.

## Confidence ladder

- High confidence regression:
  - `averageMillis` up >= 10% and `structure`/`joinAlgorithms` diff.
- Medium confidence regression:
  - `averageMillis` up >= 10% and no semantic diff file available.
- Low confidence / inconclusive:
  - Runtime neutral but semantic diff exists, or result counts changed.

## Common mistakes

- Comparing different query IDs or different query text.
- Forgetting pre-install (`-Pquick clean install`) before CLI run.
- Treating estimate-only diffs as hard regressions.
- Ignoring `resultCount` mismatch in execution verification.
