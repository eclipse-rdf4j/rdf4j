# Preserve the compiler join order on estimate-only tie-breaks in the LMDB native order planner

## Purpose

The LMDB native query engine (the code under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`) receives its join order from the compiler: the algebra-level optimizer chooses a cost-based order for the statement patterns of a join, and the physical planner is expected to keep that order unless deviating buys something concrete. One component deliberately deviates: the streaming order planner in `LmdbNativeSlotOrder.java` (class `LmdbNativeOrderPlanner`), which may promote a different pattern to the front of a join so that DISTINCT or GROUP BY can stream results instead of building hash tables. That promotion is justified when it lengthens the "exact ordered prefix" — the run of leading columns the cursor can prove arrive in sorted order — because a longer prefix eliminates hash-based deduplication work.

The problem this plan addresses: in three places the planner also swaps the join order when the structural benefit is *equal* and only a cardinality **estimate** prefers the alternative. Estimates in this engine are known to be biased (a code comment in `bestMultiJoin` says so explicitly: pseudo probe factors are structurally asymmetric, so estimate comparisons systematically prefer broad-scan-first orders). The compiler's order was chosen with better information. Swapping on an estimate-only preference therefore risks regressions — this exact class of decision contributed to the ELECTRICAL_GRID#2 ~700x regression investigated in July 2026 — while offering no structural gain. After this change, when a candidate order ties with the compiler order on every structural criterion, the compiler order must win; a candidate may still displace it on a strictly better *measured* cost, never on a pseudo-estimate alone.

This work was deliberately deferred from the 2026-07-21 change that made the factorized-sink reorder conditional on the factorized tail engaging (see `LmdbNativeFactorizedOrderSelectionTest` and `FactorizedTail.select` / `LmdbNativeFactorizedRows.selectFactorizedOrder`). That change fixed reordering with *no* payoff; this plan tightens reordering with *equal* payoff.

## Definitions

A few terms of art, in plain language:

- **Compiler order**: the child order of a `MultiJoinPlan` (in `LmdbNativeJoinPlans.java`) exactly as the native compiler built it from the optimized algebra. `MultiJoinPlan.derive` preserves it.
- **Exact ordered prefix**: for a candidate physical order and a requested key signature, the number of leading requested slots that provably arrive in globally sorted order from the index scan. Computed by `NativeSlotOrder.exactPrefixLength` in `LmdbNativeSlotOrder.java`.
- **Promotion**: rebuilding a `MultiJoinPlan` with one child moved to position 0 (`bestMultiJoin`, `LmdbNativeSlotOrder.java`), attempting to extend the exact ordered prefix.
- **Measured pattern work**: `cumulativePatternWork(children, prefixLength, boundMask, source)` — a cost figure derived from actual per-pattern fan-out measurements taken from the store (`row.source`), as opposed to the pseudo-cardinality constants in `LmdbNativePatternPlan.java` (predicate 64, context 256, subject/object 4096). `hasDirectPatternWork` reports whether every pattern in a prefix has a real measurement. `safeOrderedPromotion` in `LmdbNativeSlotOrder.java` already computes both figures for the promoted prefix and requires `candidateWork <= originalWork`.
- **Eliminated hash channels**: in the aggregate planner, DISTINCT aggregate arguments whose deduplication can be downgraded from a hash set to a constant-once or monotonic check because of the chosen scan order (`assess` in `LmdbNativeSlotOrder.java`).

## The three tie-break sites

All three are in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSlotOrder.java`. Line numbers are as of 2026-07-21; re-locate by method name if they have drifted.

First, `bestMultiJoin` (around lines 505–551). The incumbent `best` starts as the compiler order. A promoted candidate displaces it when `prefix > bestPrefix` (structural — keep this), or when `prefix == bestPrefix && hasDirectPatternWork(...) && betterEstimate(candidate, best, row)` (the tie-break to change). `betterEstimate` compares `plan.estimate(row)`, which blends measurements with pseudo-cardinalities. Note that `safeOrderedPromotion` has already vetted the candidate with `candidateWork <= originalWork`, so at this point the candidate is *no worse* on measured work — but "no worse plus a smaller estimate" is not evidence it is better.

Second, `betterAggregate` (around lines 422–438). It ranks aggregate-distinct plans by: more eliminated hash channels, then complete streaming group prefix, then longer group prefix — all structural, keep them — and finally `candidate.eliminatedHashChannels > 0 && candidate.arg.estimate(row) < current.arg.estimate(row)`, an estimate-only displacement at full structural equality. The dead line `// return current;` immediately below (line 437) is a leftover from a previous attempt to remove exactly this tie-break; delete it as part of this work.

Third, `tuple` (around lines 322–352). Across key-permutation signatures it keeps a candidate when `prefix.length > bestPrefix.length` (structural, keep) or `prefix.length == bestPrefix.length && betterEstimate(...)` (the tie-break to change). Because `permutations` emits the identity signature first and each signature's plan comes out of `bestMultiJoin`, fixing `bestMultiJoin` alone does not close this hole: two different signatures can produce two different promoted plans with equal prefix length, and the estimate then picks between two *reordered* plans.

## The change

Replace the estimate comparison in all three sites with a strictly-measured comparison. Introduce one helper next to `betterEstimate`:

    /**
     * True only when both prefixes are fully measured and the candidate's cumulative measured pattern work is
     * strictly smaller. Pseudo-cardinality estimates never break a structural tie: the compiler order wins ties.
     */
    private static boolean strictlyCheaperMeasured(NativeOrderedPlan candidate, NativeOrderedPlan current, RowState row)

Implementation sketch: both plans must be `MultiJoinPlan`s whose promoted prefix (use the full flattenable pattern prefix, mirroring how `safeOrderedPromotion` bounds its comparison) satisfies `hasDirectPatternWork`; then compare `cumulativePatternWork` with `<`. If either side is not fully measured, return false — an unmeasured claim of superiority is exactly the failure mode this plan removes. In `bestMultiJoin` the promoted prefix length (`outer + 1`) is already in hand; thread it through rather than recomputing. In `tuple` and `betterAggregate` the two plans being compared may have different shapes (one may be the untouched original), so the helper must tolerate non-`MultiJoinPlan` arguments by returning false.

In `betterAggregate`, additionally require the same structural equality that already holds at that point (equal channels, equal streaming, equal prefix length) and keep the `eliminatedHashChannels > 0` guard so a fully unspecialized candidate never displaces anything. Remove the commented-out `return current;`.

Do not touch: the prefix-extending promotion (structural), `safeOrderedPromotion` (its `<=` remains correct as an admission gate — the tie-break is where strictness belongs), the sink logic in `LmdbNativeJoinPlans.java`, or `reshapeLeftJoinForFactorization` in `LmdbNativeGroupStep.java`.

## Milestone 1 — pin current behavior, then flip it (test-first)

Write `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeOrderTieBreakTest.java` in the style of `LmdbNativeFactorizedOrderSelectionTest` (plan-level unit tests, no store: build `PatternPlan`s via `new PatternPlan(Term.slot(s), Term.constant(p), Term.slot(o), Term.unbound(), ContextConstraint.UNRESTRICTED, false, estimate)`, wrap in `MultiJoinPlan`, call the planner entry points directly). The planner entry points take a `RowState`; check first whether a `RowState` can be built without a store (`new RowState(null, layout, EmptyBindingSet.getInstance())` — see `LmdbNativeChunkHashBuildTest` for layout construction) or whether mask-taking overloads must be added, mirroring what was done for `derivedPlan(long)`.

The important subtlety: with `row.source == null` there are no measurements, so `hasDirectPatternWork` is false and the new tie-break must refuse to swap — which means the *simplest* red test is one where the current code swaps on estimates and the new code must not. Craft a plan where two orders yield the same exact prefix length but the non-compiler order has a smaller `estimate`. Assert the returned plan's children equal the compiler order. Run it, confirm it fails against current code (the estimate tie-break swaps), capture the Surefire snippet, then implement the change and show the same selection green. Add a companion test that a *prefix-extending* promotion still happens (guard against over-correcting), and one for `betterAggregate` (equal channels/prefix, differing estimates → compiler order wins; more channels → candidate still wins). For the strictly-cheaper-measured path a unit test needs measured fan-outs, which plan-level tests cannot fake without a source; cover that path with an integration-style test only if a cheap seam exists (`hasDirectPatternWork` consults `row.source` — consider a small fake `NativeLmdbQuerySource` if one already exists in the test tree; do not build new mocking infrastructure just for this).

Commands (from the repository root, one at a time):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeOrderTieBreakTest --retain-logs

Acceptance: new tests red before the change with the failure showing the swapped order, green after; `LmdbNativeFactorizedOrderSelectionTest`, `LmdbNativeFactorizedSinkTest`, `LmdbNativeLeftJoinReshapeOrderTest` still green.

## Milestone 2 — regression sweep and benchmarks

Run the lmdb module unit tests and compare failures against the pre-existing baseline (13 known failures as of 2026-07-21, including `LmdbNativeChunkHashBuildTest.serialTailLessChunkPrefixFeedsValuesSuffix`; verify by re-running the same classes at HEAD in a temporary worktree if in doubt):

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs verify

Then benchmark. This change only alters plans when a tie existed, so expect mostly-neutral results; the point is to catch cases where the estimate tie-break was accidentally load-bearing. Use the theme benchmark harness that exercises GROUP BY / DISTINCT heavy queries (`ThemeQueryBenchmark` in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/`, run via `scripts/run-single-benchmark.sh`), comparing before/after with the `jmh-benchmark-compare` skill. Pay particular attention to the ELECTRICAL_GRID and ANALYTICS themes. A regression on any theme query of more than ~5% needs investigation before landing: the likely cause is a query where the estimate-preferred order was genuinely better, and the fix is to make that superiority *measured* (extend `hasDirectPatternWork` coverage), not to reinstate the estimate comparison.

Acceptance: no new unit-test failures relative to the baseline; benchmark deltas neutral or positive, or each regression explained and resolved through measured-cost coverage.

## Progress

- [ ] Milestone 1: red tests for all three tie-break sites captured
- [ ] Milestone 1: `strictlyCheaperMeasured` implemented, three sites switched, dead `// return current;` removed
- [ ] Milestone 1: focused tests green, selection/sink/reshape suites green
- [ ] Milestone 2: module sweep matches pre-existing failure baseline
- [ ] Milestone 2: theme benchmarks compared before/after, regressions triaged
- [ ] Formatting (`mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`) and copyright check (`cd scripts && ./checkCopyrightPresent.sh`)

## Decision log

- 2026-07-21: Plan authored. Chosen criterion is "structural ties go to the compiler order; only strictly smaller fully-measured `cumulativePatternWork` may override", because `safeOrderedPromotion` already trusts measured work for admission and the code itself documents the estimate asymmetry. Rejected alternative: removing the tie-breaks outright (always keep compiler order on ties) — it forfeits real measured wins that the current machinery can prove; the strict-measured form keeps them.
