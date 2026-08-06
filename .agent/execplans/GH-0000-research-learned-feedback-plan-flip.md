> **Maintainer note:** the hand-off artifact is `research-task-learned-feedback-plan-flip.zip` (repo root).
> The researcher has NO repository access — everything they need is inside the zip (this TASK.md,
> BACKGROUND-RCA.md, evidence/, and 28 bundled PDFs). Keep this file in sync with the zip's TASK.md.
>
> Zip contents are staged from the session scratchpad `bundle/` dir; papers come from `papers2/papers/`.

# Research task: how should a learned query optimizer avoid feedback-driven plan flips?

**You are handed this zip and nothing else — you do not have access to the codebase.** Everything
you need is in this bundle plus the open web:

```
TASK.md               — this document
BACKGROUND-RCA.md     — full root-cause analysis of the failure, self-contained
evidence/             — query text, plan dumps, executed telemetry, feedback-event traces
papers/               — 28 PDFs we already own that look relevant (start here, expand freely)
```

**Goal:** survey how existing learned/adaptive query optimizers solve the four defects below, and
recommend the best-understood, most correct fix for each — with citations — so the engineering team
implements proven designs instead of reinventing them. There is substantial literature on exactly
these failure modes; we want the state of the art, not novelty.

## 1. The system (30-second version)

An embedded RDF/SPARQL store (LMDB-backed) with a Cascades-style cost-based planner plus a learned
feedback layer modeled loosely on DB2's LEO: after each query execution, a completed-tree pass
records per-operator `(predicted, actual)` observations into a hierarchical Normal-Inverse-Gamma
model of *log-error*, keyed by a `FrontierLearningKey` ≈ (operator family, topology fingerprint of
the sub-plan, access kernel, physical algorithm). Posteriors are applied at planning time as
multiplicative corrections to the raw cost model, clamped to ln(4) per unit of evidence weight.
Learned state persists on disk per store and is shared by all queries against that store.

See `BACKGROUND-RCA.md` for the full diagnosed failure narrative with evidence pointers.

## 2. The observed failure (fully diagnosed)

A benchmark query "q9" (`SELECT (COUNT(DISTINCT ?enc)...)` with VALUES anchors, an OPTIONAL, and a
`FILTER NOT EXISTS` — text in `evidence/q9-query.sparql`) has two near-tied anti-join
implementations:

- **Set difference** (hash anti-join, OPTIONAL pruned): true cost ~220 ms, planner-priced 547K work units.
- **Correlated NOT-EXISTS probe** (re-evaluates the probe per outer row): true cost ~1.6 s, but
  planner-priced **537K** — 2% *cheaper*.

Repeated identical runs starting from empty learned state:

```
run:   1     2      3     4      5      6     7
plan:  Diff  PROBE  Diff  Diff   Diff   PROBE PROBE
ms:    220   1644   667   1061   1060   471   471
```

A limit cycle settling ~2× worse than optimal, never recovering the clean plan. No cross-query
pollution is required — one execution of the query flips its own next planning — though a sibling
query of the same shape (`evidence/q10-query.sparql`) flips it too.

## 3. The four root defects (each verified empirically — details in BACKGROUND-RCA.md)

**D1 — Incommensurate observations.** Re-invoked operators (inner side of a nested-loop join,
VALUES anchors, EXISTS bodies) defer feedback to the completed-tree pass, which then pairs the
planner's *per-invocation* prediction with the *cumulative* actual across all invocations. Proven
instance (`evidence/run1-executed-telemetry.txt`): a constant 2-row `VALUES` relation recorded
`predicted=2, actual=99 600` (2 rows × 49.8K invocations) → posterior logMean ≈ 9.9 → a clamped
×4.86 inflation applied on the next planning.

**D2 — Asymmetric correction across alternatives ("grass is greener").** Corrections attach to
topology-exact keys of the plan that *executed*. The executed alternative's estimated cost drifts
(mostly upward); the never-executed alternative competes with raw, optimistic model estimates. Any
near-tie therefore flips toward the unobserved plan — and after the flip, the same asymmetry
operates in reverse, producing the oscillation above. Kill-switch bisection confirmed the flip
needs only exact-key corrections (hierarchical family pooling disabled → still flips).

**D3 — Mispriced correlated probe.** The correlated NOT-EXISTS alternative charges the probe
sub-plan roughly once (68.6K work; filter output estimated at 2.38 rows) while the runtime
re-executes it per outer row (~97K probes; real filter output 65 346 rows — a ×27 000 cardinality
error visible in `evidence/run2-feedback-record-events.txt`). This is why the flip is catastrophic
instead of neutral.

**D4 — Correction latency and scope.** The only channel that observes D3's error (a "semi-anti
surface" observation on the filter) records *after* the bad plan has been paid for; its learning
key embeds the executed algorithm (`implementation=streaming-correlated`), so evidence cannot warn
the planner off an algorithm it hasn't run; physical-dimension replacement is gated behind ≥3
observations; and the ln(4)-per-observation clamp needs ~7 observations to express a ×27 000 error.
Individually reasonable safeguards compose into "pay full price several times before learning".

## 4. Research questions

**Q1 (for D1):** How do feedback optimizers make observations commensurate with predictions for
re-invoked/nested operators? How does DB2 LEO normalize adjustments for nested-loop inner sides,
EXISTS short-circuits (hasNext-once semantics), and partially-consumed iterators? Is "divide
cumulative actual by invocation count" the accepted answer, or do systems record per-invocation
distributions? What happens when invocations see correlated (non-i.i.d.) outer bindings?

**Q2 (for D2):** What is the state of the art for keeping *competing alternatives* comparably
calibrated when evidence exists only for executed plans? Framings to evaluate:
- Bandit/regret formulations (Bao's Thompson sampling over hint sets; SkinnerDB's regret-bounded
  intra-query learning; LEON's ranking-plus-exploration; Vaidya et al.'s contextual bandits).
- Consistency repair: Markl's maximum-entropy consistent selectivity estimation — can corrections
  learned on one plan's keys be propagated as constraints on the *logical sub-expression's*
  cardinality, so every physical alternative over the same sub-expression re-prices consistently?
- Plan-transition damping: hysteresis before switching near-tied plans; plan pinning/baselines
  (Oracle SQL Plan Management "verify before use", SQL Server Query Store forced plans, Postgres
  AQO) — what do production systems require before accepting a learned plan change?
- Validity ranges / check conditions (DB2 Progressive Optimization): re-optimize only when an
  observed cardinality leaves the range within which the current plan remains optimal — does this
  subsume the oscillation problem?

**Q3 (for D3):** What is the correct cost model for correlated sub-plans (dependent joins, EXISTS
probes)? Charging per-invocation work × invocation count; first-row vs all-rows costs;
memoization/cache-hit modeling; "probe N times with cache" vs "materialize once" crossover. Classic
System R rescan costing through HyPer/Umbra dependent-join unnesting (Neumann & Kemper, "Unnesting
Arbitrary Queries") and anything newer.

**Q4 (for D4):** How should learned corrections be scoped and trusted? Per-algorithm vs
per-logical-expression keys; evidence thresholds vs magnitude-aware fast paths (should a ×27 000
observed error clear the clamp faster than a ×2 one?); asymmetric risk (underestimating a
correlated probe is catastrophic, overestimating is mild) — do any systems use risk/penalty-aware
plan selection (PARQO, Babcock's robust intervals, plan bouquets/SpillBound) to bias near-ties
toward the lower-variance plan?

**Q5 (synthesis):** Which minimal combination of fixes has the best published evidence:
(a) commensurate observations only, (b) + correlated-probe repricing, (c) + logical-key consistency
propagation, (d) + risk-aware tie-breaking or switch hysteresis? Has any published system
documented and fixed this exact observe→replan oscillation? (LEO follow-up literature is a likely
place; so are the industrial "plan regression" mechanisms.)

## 5. Bundled papers (`papers/`) — starting points, expand freely

Feedback/learning lineage: `11-stillger-et-al-2001-leo-db2-learning-optimizer.pdf` (read for both
its normalization design and its known failure modes), `09-chen-roussopoulos-1994-...query-feedback.pdf`,
`10-aboulnaga-chaudhuri-1999-self-tuning-histograms.pdf`,
`markl-et-al-2007-consistent-selectivity-estimation-maximum-entropy.pdf` (Q2 consistency repair).

Robustness / re-optimization / oscillation:
`markl-et-al-2004-robust-query-processing-progressive-optimization.pdf` (validity ranges),
`adaptive_query_processing_survey.pdf` (Deshpande/Ives/Raman — see validity-range and plan-switching
sections), `babcock-et-al-2005-towards-robust-query-optimizer.pdf`,
`haritsa-2020-robust-query-processing-mission-possible.pdf` (plan bouquets/SpillBound pointers),
`xiu-agarwal-yang-2024-parqo-penalty-aware-robust-plan-selection.pdf`,
`negi-et-al-2023-robust-query-driven-cardinality-estimation-under-drift.pdf`,
`kamali-et-al-2025-robust-plan-evaluation-approximate-ml.pdf`,
`raychaudhury-et-al-2026-coresets-robust-query-optimization.pdf`,
`zhang-et-al-2025-simple-adaptive-vs-learned-query-optimizers.pdf`,
`zhao-et-al-2025-debunking-myth-join-ordering-robust-sql-analytics.pdf`,
`moerkotte-neumann-steidl-2009-preventing-bad-plans-q-error.pdf` (q-error → optimality bounds; our
near-tie sensitivity in one lens), `14-leis-et-al-2017-...index-based-join-sampling.pdf`.

Learned optimizers with exploration/regression control:
`marcus-et-al-2021-bao-...pdf`, `trummer-et-al-2018-skinnerdb-...pdf`,
`chen-et-al-2023-leon-...pdf` (claims less regression than Bao — verify how),
`lehmann-et-al-2024-learned-query-optimizer-behaving-as-expected.pdf`,
`reload-robust-efficient-learned-query-optimizer.pdf`, `limao-lifelong-modular-...pdf`,
`yang-et-al-2022-balsa-...pdf` (safe exploration), `marcus-et-al-2019-neo-...pdf`,
`vaidya-et-al-2022-query-logs-machine-learning-parametric-query-optimization.pdf`.

Costing / architecture context: `selinger-et-al-1979-access-path-selection-system-r.pdf` (rescan
costs), `01-graefe-1995-cascades-framework.pdf`,
`05-ding-narasayya-chaudhuri-2024-extensible-query-optimizers-in-practice.pdf` (industry survey —
POP and feedback in production).

External leads to chase on the web (not bundled): Postgres AQO extension and its oscillation
discussions; Oracle SQL Plan Management / adaptive plans; SQL Server 2017+ CE feedback + Query
Store regression detection; Lero (learning-to-rank optimizer); Kepler (Google, parametric plan
selection with regression control); SpillBound; Neumann & Kemper "Unnesting Arbitrary Queries";
DB2 POP production experience reports.

## 6. Constraints a recommended fix must respect

- Planning is deterministic and synchronous (no pilot executions at plan time); budget ~milliseconds.
- Learned state must remain a per-store disk artifact shared by all queries on that store.
- No shadow/duplicate executions of full queries in production. Limited intra-query adaptivity
  already exists (an adaptive materialized-EXISTS probe with a probe-count budget).
- Every behavior change ships behind a kill switch, with a regression test that fails pre-fix.
- The planner enumerates alternatives inside one memo — a fix that prices the *logical*
  sub-expression consistently across physical alternatives is strongly preferred over per-plan
  patches.

## 7. Deliverables

1. **Comparison matrix**: for each surveyed system (LEO, POP, max-entropy, Bao, LEON, SkinnerDB,
   plan bouquets/SpillBound, PARQO, SPM/Query Store, AQO, Kepler, Lero, …): how it addresses D1–D4,
   its trigger/trust mechanism, oscillation behavior, and production evidence.
2. **Recommended fix per defect** (D1–D4) with citations, ranked by evidence strength, plus the
   minimal combination expected to break the q9 limit cycle (Q5).
3. **Design sketch** for the two highest-value fixes adapted to the architecture in §1/§6:
   what to observe, what key to store it under, how to apply it symmetrically across memo
   alternatives, and what invariants/regression tests should pin it.
4. **Reading shortlist** (≤10 papers/docs) for the implementing engineer, in order.
