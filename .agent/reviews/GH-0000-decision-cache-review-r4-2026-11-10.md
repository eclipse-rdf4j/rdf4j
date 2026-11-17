# Fourth review of the decision-cache change (round-5 diff, 2026-11-10) and decisions

Read-only review by an independent agent; nothing was run. Paths are in the round-5 private copy;
`lmdb/` = `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. "Decision" lines are the coordinator's.

**F1. Medium, confirmed — "newer evidence" means only a higher data generation; the cache's evidence epoch never
moves in production.** `lmdb/LmdbPlanDecisionCache.java` `newerThan` 1719-1721, `mayReplace` 1692-1709, `dead`
2591-2597, `MissDoorkeeper.newer` 2756-2757; `globalEvidenceEpoch` is bumped only by `markEvidenceChanged` (no
main-code caller) and reverse-index overflow (unreachable at default size).
(a) A family (ranked with refresh work, or replay-only) published at data generation D is lifecycle-blocked; the
same feedback moves LEO. Every later re-plan at (D, new LEO) is BLOCKED, and after reclamation the tombstone blocks
the same way. Only a commit clears it: on a read-only store the key is never cached again. Round 4 allowed any
evidence difference; the review-3 decision said "not older in any monotone component", and equal D is not older.
(b) A ranked family without refresh work published at D+1 while Frontier statistics lag: when they catch up at the
same D+1 it serves nobody, cannot be replaced and is not dead until the next commit.
The new tests change only the data revision, so they miss both.
Decision: FIX before merge. A build is newer than a retired or unservable variant when its data generation is not
lower and its evidence snapshot differs from that variant's build evidence. Same snapshot → blocked (the re-plan
would reproduce the retired decision). Lower data generation → blocked. Tombstones store the data generation and a
digest of the retired variant's build evidence. The epoch is not an evidence clock and leaves these comparisons.
Residual accepted: a flight that planned under stale evidence at the same data generation and publishes after the
retirement is admitted (it is a different decision under different evidence, served only where its own rules allow).

**F2. Low-medium, confirmed, pre-existing — a live replay-only family is displaced by a build from an older pinned
snapshot and flips back** (`mayReplace` 1684-1690, 1705-1710).
Decision: FIX. A build with a lower data generation never replaces a servable replay-only family.

**F3. Low, confirmed — a tombstone keeps a key unprovable after its evidence became newer** (`retire()` sets
`unpublishableMisses=capacity`, 2729; cleared only in `publishFlight`, which a bypassed request never reaches).
Decision: FIX.

**F4. Low, confirmed — the per-appended-alias charge (128 B, 1806-1807) is below what is retained** (≥144 B since
`ExactAlias` grew). Decision: FIX with an itemized constant; include the odd-capacity `int[]` padding nit.

**F5. Low, confirmed — a waiter's re-entry in `LmdbHeuristicFilterPassCache.getOrCompute` (123-124) is unbounded
recursion** under repeated cuts. Decision: FIX. On a null share the waiter computes directly.

**F6. Low, coverage — no test now checks the production shape at newer evidence** (a ranked family with refresh work
blocks a newer replay-only or different build and such a key evicts at most once), nor evidence moves at an
unchanged data generation. Decision: ADD the tests; they also pin F1.

**F7. Informational** — stale javadoc at `LmdbPipelinePlanCache.java:444-446` ("kept no provider runtime state");
epoch described as an evidence clock; the refresh's cut check (`LmdbDetachedPlanRefresh.java:254`) runs before the
last planning work. Decision: FIX all three.

**Fixes without a failing test (E3, E6a).** The reviewer verified both arguments and gave recipes: for the refresh,
a sampling view that blocks only on `rdf4j-lmdb-plan-refresh-*` threads (or calling the refresh work's `execute()`
directly); for the uncached path, a context-less request whose request-bound optimizer sample is cut before the
packed session opens. Decision: WRITE both tests, with red evidence taken by reverting the fix.

**Hit-rate notes.** Nothing new on the ranked exact-hit path. Under sampling contention a cut ranked planning is
neither retained nor shared, so N concurrent identical requests behind a cut owner become N plannings; by design,
unmeasured (0 cuts on the theme run). Recorded as an open item (a counter).

## No defect found
`reproducible()` forwarding by every main-code wrapper, the read point, decline/waiter behaviour, checkpoint replay,
no nested planner publishing on its own; ranked families with refresh work never churn and use-and-refresh still
serves after feedback; older-pinned-snapshot block; doorkeeper accounting; refresh exception containment and
reservation release; the uncached-scope argument; nothing cut is shared by the heuristic cache; tombstoned keys
never evict; all E10 charge/release pairs; no timing config opts in to diagnostics; digest equality; lock order.

## Flaky test measured separately
`LmdbDetachedPlanningOwnershipTest#actualPackedPipelineKeepsARealWarmHitAndSchemaTwoDiagnostics` fails 3 of 10 runs
before and 3 of 10 after round 5 (same assertion, line 363: third explain is not a pipeline hit). It evaluates and
then expects a hit without awaiting background maintenance. Decision: settle the store in the test and re-measure.
Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/flaky-ownership/`.
