# Restore Theme Query May Parity

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root. A future worker must be able to continue from this file alone.

## Purpose / Big Picture

The LMDB theme query benchmark currently has many queries slower than the average score from the first three weeks of May. The goal is to make every current theme query run at least as fast as its May baseline average without monkey-patching, hard-coded query heuristics, or hiding regressions. The work is complete when a fresh `ThemeQueryPlanRunBenchmark.runQuery` measurement for the current code is less than or equal to the per-query average of `ThemeQueryBenchmark.executeQuery` scores from dated result files `2026-05-01` through `2026-05-21`, inclusive.

The user-visible behavior is benchmark output: the analyzer should show zero current queries slower than the May baseline target, and focused query-plan snapshots should show the planner choosing cheaper physical join shapes for the formerly slow queries.

## Progress

- [x] (2026-06-03T02:58Z) Defined the May baseline as the arithmetic mean of all `ThemeQueryBenchmark.executeQuery` rows in `results-2026-05-01*.md` through `results-2026-05-21*.md`.
- [x] (2026-06-03T02:58Z) Identified `results-2026-06-03.md` as the current latest file and `ThemeQueryPlanRunBenchmark.runQuery` as its benchmark row source.
- [x] (2026-06-03T02:58Z) Computed the initial regression set: 56 current queries are slower than their May-average target.
- [ ] Rank the current regressions by ratio and absolute delta, then choose the smallest set of query-plan root causes that explains the largest regressions.
- [ ] Capture current and historical plans for the highest-ratio queries, starting with `SOCIAL_MEDIA q7`, `ENGINEERING q7`, `PHARMA q7`, `PHARMA q2`, and `HIGHLY_CONNECTED q10`.
- [ ] Add the smallest failing in-repo regression test for the first confirmed planner root cause before production changes.
- [ ] Implement a durable planner fix in the responsible LMDB planning code, avoiding benchmark-specific or query-specific conditionals.
- [ ] Run focused tests and fresh benchmarks, then repeat analysis until every query is at or below the May-average target.

## Surprises & Discoveries

- Observation: The current latest result file is a plan-run benchmark, not the older query benchmark. The history analyzer was updated before this plan so it can treat `ThemeQueryPlanRunBenchmark.runQuery` as the latest run while comparing historical scores from `ThemeQueryBenchmark.executeQuery`.
  Evidence: `latest=results-2026-06-03.md bench=ThemeQueryPlanRunBenchmark.runQuery`.
- Observation: The first May baseline pass found 56 current queries slower than target. The top ratios are `SOCIAL_MEDIA q7` at 33.11x, `ENGINEERING q7` at 20.78x, `PHARMA q7` at 17.57x, `PHARMA q2` at 6.65x, and `HIGHLY_CONNECTED q10` at 6.17x.
  Evidence: local parser output over `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-*.md`.

## Decision Log

- Decision: Use the arithmetic mean of first-three-weeks-of-May query benchmark rows as the acceptance target for each `(themeName, z_queryIndex)` pair.
  Rationale: The user requested "as fast as the average of the queries in the first 3 weeks of may"; the dated historical result files contain repeated query benchmark scores in that exact window, and a mean gives a direct per-query numeric target.
  Date/Author: 2026-06-03 / Codex.
- Decision: Treat the newest dated result file as current and prefer `ThemeQueryBenchmark.executeQuery` rows when present, otherwise `ThemeQueryPlanRunBenchmark.runQuery`.
  Rationale: The newest file for today only has plan-run rows; this preserves compatibility with older query benchmark files while allowing today’s output to be assessed.
  Date/Author: 2026-06-03 / Codex.
- Decision: Require a focused in-repo failing test before production planner changes.
  Rationale: This is behavior-changing optimizer work. The repository rules require reproducing the issue in a test before patching production code, and durable fixes need a small semantic/performance proxy that exercises the planner root cause.
  Date/Author: 2026-06-03 / Codex.

## Outcomes & Retrospective

Not complete. The target table exists and the first regression ranking exists. The next outcome is a query-plan explanation that ties multiple slow queries to one root planner cause.

## Context and Orientation

Theme query benchmark result files live under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results`. Each result file is Markdown containing JMH rows and, for some query runs, captured optimized query text and optimized query plans. A theme is a generated dataset family such as `SOCIAL_MEDIA` or `PHARMA`. A query index is a numbered benchmark query within that theme. The pair `(themeName, z_queryIndex)` uniquely identifies one benchmark query.

The relevant analyzer script is `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh`. It parses result files and can print the latest run, historical faster runs, and captured plans for one query. It is useful for triage, but the final acceptance target in this plan is stricter than the script’s default "historical best" view because the user asked for parity with the May-average baseline.

The snapshot CLI is run through `.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh`. It captures a controlled current query plan for one theme/query index. Use this when a historical file does not contain enough plan detail, or when validating that a production change altered the planner in the intended way.

This repository requires Java 25 or newer and Maven commands must use the workspace-local repository `.m2_repo`. Tests must not use Maven `-am` or `-q`. Before focused Maven tests, use the project’s root quick install or the `mvnf` wrapper, which performs the required install.

## Plan of Work

First, compute a reproducible target table from the historical result files. The table must include current score, May-average target, sample count, ratio, and absolute delta for each current query. Store generated reports under `/tmp` unless a script or checked-in test is intentionally added.

Second, inspect plan details for the highest-ratio queries. Start with query index 7 across `SOCIAL_MEDIA`, `ENGINEERING`, and `PHARMA` because the same query index appears in the top three regressions. Compare current plans from `results-2026-06-03.md` with faster May or historical plans. Look for structural causes: join order changes, a fallback to broad scans, missing direct lookup operations, changed cardinality estimates, or a physical operator that materializes too much before applying selective edges.

Third, once a shared planner root cause is identified, add the smallest focused regression test in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb`. Prefer an existing theme-query regression test class if it already builds the same datasets and exposes plan assertions. The test should assert a planner invariant or bounded-work property that fails on the current code and is directly tied to the observed slow plans. Do not assert exact benchmark timings in a unit test.

Fourth, patch the responsible planner or cost model code. The fix must be general and data-driven: for example, improve cost estimation, preserve a selective lookup edge, or choose a physical join order based on existing statistics. Do not special-case theme names, query indexes, benchmark names, or string fragments.

Fifth, run focused tests and a benchmark loop for the changed query set. Recompute the target table against the fresh current result. Repeat the plan-inspect-fix-measure loop until all current queries are at or below their May-average targets.

## Concrete Steps

From the repository root, compute the May target ranking with a Python parser over the Markdown files. The current exploratory command is:

    python3 - <<'PY'
    from pathlib import Path
    import re
    root=Path('core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results')
    row=re.compile(r'^(?P<bench>ThemeQuery(?:Benchmark\.executeQuery|PlanRunBenchmark\.runQuery))\s+(?:(?:true|false)\s+)?(?P<theme>[A-Z_]+)\s+(?P<q>\d+)\s+avgt\s+(?:(?:\d+)\s+)?(?P<score>[0-9.]+)')
    date_re=re.compile(r'results-(\d{4}-\d{2}-\d{2})(?:-(\d+))?\.md$')
    QUERY='ThemeQueryBenchmark.executeQuery'
    PLAN='ThemeQueryPlanRunBenchmark.runQuery'
    def suffix(name):
        m=date_re.match(name)
        return int(m.group(2) or 0)
    files=sorted((date_re.match(p.name).group(1), p.name, p) for p in root.glob('results-*.md') if date_re.match(p.name))
    latest=max(files, key=lambda x:(x[0], suffix(x[1])))
    base={}
    for date,name,p in files:
        if '2026-05-01' <= date <= '2026-05-21':
            for line in p.read_text().splitlines():
                m=row.match(line)
                if m and m.group('bench') == QUERY:
                    base.setdefault((m.group('theme'), int(m.group('q'))), []).append(float(m.group('score')))
    bybench={QUERY:{}, PLAN:{}}
    for line in latest[2].read_text().splitlines():
        m=row.match(line)
        if m:
            bybench[m.group('bench')][(m.group('theme'), int(m.group('q')))] = float(m.group('score'))
    latest_rows=bybench[QUERY] or bybench[PLAN]
    rows=[]
    for key, score in latest_rows.items():
        vals=base.get(key)
        if vals:
            avg=sum(vals)/len(vals)
            if score > avg:
                rows.append((score/avg, score-avg, score, avg, len(vals), key))
    rows.sort(reverse=True)
    print(f'latest={latest[1]} bench={QUERY if bybench[QUERY] else PLAN}')
    print(f'slower_than_may_avg={len(rows)}')
    for ratio, delta, score, avg, n, (theme,q) in rows[:30]:
        print(f'{theme} q{q}: latest={score:.3f} avg_may={avg:.3f} n={n} ratio={ratio:.2f} delta={delta:.3f}')
    PY

Expected current output begins with:

    latest=results-2026-06-03.md bench=ThemeQueryPlanRunBenchmark.runQuery
    slower_than_may_avg=56
    SOCIAL_MEDIA q7: latest=9.940 avg_may=0.300 n=18 ratio=33.11 delta=9.640

For each top query, print the current and fastest known historical detail:

    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --theme SOCIAL_MEDIA --query-index 7
    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --theme ENGINEERING --query-index 7
    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh --theme PHARMA --query-index 7

When a current plan needs a fresh controlled capture, run:

    ./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-social-q7-current.log -- --store lmdb --theme SOCIAL_MEDIA --query-index 7 --query-id social-q7

If comparing two snapshots already captured by the CLI, run:

    mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot -Dexec.args="--compare-existing --query-id social-q7 --compare-indices 1,0 --no-interactive --diff-mode structure+estimates"

Before any Maven test command, run the required root quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

For focused regression tests after a failing test exists, prefer:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbThemeQueryRegressionTest#theNewFocusedMethod --retain-logs

## Validation and Acceptance

The final acceptance check is a fresh current benchmark result parsed against the May target table. Acceptance requires `slower_than_may_avg=0`. If a fresh full benchmark is too expensive to run in one pass, run focused single-query benchmarks for every previously failing query and update the report with those fresh scores; the full pass remains the final confirmation when practical.

Focused planner tests must fail before the production fix and pass after. They should prove the planner invariant behind the performance win, not merely assert a hard-coded plan string unrelated to cost or semantics.

Benchmark evidence must include the exact command, result file path, and a compact table showing each formerly slow query with `latest`, `avg_may`, `ratio`, and status.

## Idempotence and Recovery

The analysis commands only read benchmark Markdown files and can be repeated safely. Generated logs should go under `/tmp` unless they are intentionally part of the final evidence. Do not delete existing untracked or modified benchmark artifacts; this repository may contain user or other-agent work. If a Maven offline command fails because a dependency is missing, rerun the exact command once without `-o`, then return to offline mode. If benchmark output is noisy, keep full logs under `/tmp` and cite compact snippets in the handoff.

If a suspected fix does not improve the slow queries, revert only the candidate change made for that attempt or supersede it with a documented plan update. Do not weaken tests, alter benchmark thresholds, or exclude queries from the target set.

## Artifacts and Notes

Initial target computation:

    latest=results-2026-06-03.md bench=ThemeQueryPlanRunBenchmark.runQuery
    may_files=18 may_rows=1579 queries_with_baseline=88 current_queries=95 slower_than_may_avg=56
    SOCIAL_MEDIA q7: latest=9.940 avg_may=0.300 n=18 ratio=33.11 delta=9.640
    ENGINEERING q7: latest=5.310 avg_may=0.256 n=18 ratio=20.78 delta=5.054
    PHARMA q7: latest=487.603 avg_may=27.757 n=18 ratio=17.57 delta=459.846
    PHARMA q2: latest=255.959 avg_may=38.514 n=17 ratio=6.65 delta=217.445
    HIGHLY_CONNECTED q10: latest=753.625 avg_may=122.153 n=18 ratio=6.17 delta=631.472

## Interfaces and Dependencies

No new external dependency is planned. The work should use existing benchmark result files, existing LMDB theme-query regression tests, the existing query-plan snapshot CLI, and the existing benchmark runner scripts.

The eventual production change is expected to live under `core/sail/lmdb/src/main/java` or a directly related planner/cost-model package already used by LMDB theme query planning. The test change is expected under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb`.

Revision note 2026-06-03: Initial ExecPlan created from the user request to restore all current theme queries to May-average parity and from the first local ranking pass over benchmark history.
