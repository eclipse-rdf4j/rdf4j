# A planning deadline must not hand back the written-order plan while a priced plan is within reach (2026-11-13)

Status: specified, not implemented. Routine A (behaviour change: planning may run past its wall-clock deadline).

## Observation

`triage-planner39/round5/report.txt`, item 1. On the AAS query2 fixture a search stopped before about 65 candidate
evaluations returns the written-order plan: a Cartesian pair first, planned Cartesian work 211.2M, root price
UNAVAILABLE. Executed, it does not finish in 120 s (one diagnostic evaluation ran for more than six minutes).
Stopped between 69 and 91 evaluations the search returns a priced plan that runs in 1.7–3.0 s; from 97 evaluations
on, 0.7–2.2 s. The same on the build without the 2026-11-13 planner rules, so this predates them.

The stop that matters in production is the wall-clock deadline (`rdf4j.optimizer.lmdb.cascades.timeoutMillis`,
500 ms, read from `System.nanoTime()`): this search costs about 3.6 ms per evaluation warm (88–91 % of it in
Frontier statistics evaluation), 800 ms of planning cold, and 8 of 30 warm plannings lost 0.3–3.9 s off-CPU on a
loaded machine. So a cold start, a GC pause or a busy host can cut the search before the first priced complete plan
exists, and the query then runs for minutes instead of seconds. The anytime invariant ("return the cheapest
complete priced plan retained so far") holds, but is empty when nothing priced has been retained yet.

## Required behaviour

1. In the budgeted and auto search modes, reaching the wall-clock deadline stops the search only if a complete
   priced plan for the root is already retained. Otherwise the search continues, and stops as soon as the first
   complete priced root plan is retained (then it returns that plan, completeness DEADLINE_EXPIRED as today), or
   when the work limit, the resource limit or cancellation stops it, whichever comes first. The work limit stays
   the hard bound on planning effort; nothing about it changes.
2. Cancellation and interruption of the query (connection shutdown, query timeout, thread interrupt) still stop
   planning at once. If planning has no cancellation check today, that is a finding to report, not something to
   add silently.
3. The overrun is visible: a planned metric with the nanoseconds spent past the deadline and the reason the
   search finally stopped (`first-priced-plan`, `work-limit`, `resource-limit`, `cancelled`).
4. Nothing else moves: exact mode, detached refresh planning (which has no deadline), the provider-work limit,
   the meaning of the completeness values, and what is returned when a priced root is already retained at the
   deadline.
5. A query whose root can never be priced (17 of 182 theme queries today) runs to its work limit as it does
   whenever the deadline does not intervene. Measure what that costs (below) before this is merged; if it makes
   planning of such queries materially longer, report the numbers and stop — the alternative (a bounded grace
   multiple of the deadline) is an owner decision.

## Determinism

The deadline is read from `System.nanoTime()` with no seam. Add the smallest package-private time source to the
budget object (inert by default) so the tests can place the deadline at an exact evaluation count. No sleeps.

## Tests (red first)

- Core packed planner tests, with the injected clock: deadline before any priced root → the search continues and
  returns the first priced root, with the overrun metric and reason `first-priced-plan`; deadline after a priced
  root is retained → stops at once as today; deadline with no priced root reachable → stops at the work limit,
  reason `work-limit`, returns what today's work-limit stop returns; cancellation during the overrun → stops.
- LMDB, AAS query2 fixture: with the clock forcing the deadline at the evaluation count where today's plan is the
  written-order Cartesian plan, the returned plan is priced and its execution finishes within the test's limit
  with the expected count (6679).
- One mutation per rule (stop at the deadline regardless; ignore the work limit during the overrun; drop the
  cancellation check).

## Measurements before merge (plan-only corpus probe, 182 theme queries, both builds, two runs each)

Per query: whether the deadline was reached, whether a priced root existed at that moment, overrun time, final
stop reason, and total planning time before/after. Summaries: number of queries whose plan changes, number that
overrun, the overrun distribution, and planning time for the never-priced queries. Run it warm and once with a
cold JVM per query batch, since cold planning is where the deadline bites.

## First implementation hand-back and owner decisions (2026-11-13, `triage-planner39/round6/report.txt`)

Facts established:
- Scope is narrower than the observation suggested: in the default auto mode the main LMDB planning path has no
  wall-clock deadline; `timeoutMillis` applies in `planPreparedInput` / `auditPreparedInput`, i.e. budgeted mode.
  All 182 theme plans are identical in auto mode with 0 overruns.
- query2 with the deadline forced at 16 work units: today the written-order plan (64 evaluations, unpriced,
  Cartesian 211.2M); with the change the first priced root at 79 evaluations, running in 1.2–1.3 s, c=6679.
- Budgeted corpus (4096 units, 500 ms): one priceable query that today gets an unpriced plan at the deadline
  (DATA_TRANSFORMATION:12) now gets its priced plan at +138 to +472 ms.
- Cost 1 (requirement 5): the never-priced SOCIAL_MEDIA:10 plans for 919–1,020 ms, up from ~522 ms, same plan.
- Cost 2: three priceable queries overrun although today's deadline stop already returns a priced plan
  (DATA_TRANSFORMATION:1, LIBRARY:8, PHARMA:1): the probe looks only at the memo's root winner and does not see
  the priced plan the stop path assembles (the join region's seeded incumbent).
- Planning already checks cancellation (phase boundaries, every provider-work admission, query-timeout interrupt).
- Boundary chosen by the implementer: the overrun applies only inside the search window. A deadline already
  expired before the search starts still returns the mandatory written-order fallback; applying the overrun from
  planning entry turned nine existing tests red that assert an expired deadline refuses optional work and
  evidence reads. Accepted.

Decisions:
1. The probe answers "would the plan the deadline stop assembles right now be priced?", not "is the memo's root
   winner priced?". The three needless overruns must disappear and those queries return today's plans.
2. The overrun is bounded: the search stops unconditionally when the time past the deadline reaches the deadline's
   own length (a hard stop at twice the configured timeout), stop reason `grace-expired`, returning what the stop
   path returns today. The deadline stays a wall-clock promise within a factor of two; no new configuration.
3. Open, separate item (reproduce before any change): a plan-cache flight waiter that waits until the owner's
   deadline and then plans for itself starts with an already expired deadline and so gets the written-order
   fallback. Read from code only.

## Second hand-back (2026-11-13 late, `triage-planner39/round7/report.txt`); merged into the working tree, uncommitted

- Probe: a dry run of the whole stop path is impossible (after the deadline latches, the stop prices the levels
  above the join region through provider calls — 85 for LIBRARY:8). Implemented instead: the dense join search
  registers its region with the budget; the probe reads the same retained complete candidate and the same
  publication condition (`publishesDenseTerminal`, shared with the real stop), then walks the relations outside
  the region: everything outside must already have a priced winner and the levels above must not have been
  offered yet. Where it can still disagree it errs towards today's stop. Only the dense kernel registers; other
  kernels fall back to "root winner priced" (a needless overrun there is bounded by the grace; none in the corpus).
- Grace: the overrun ends at the first deadline check at or after deadline + (deadline − budget creation time),
  stop reason `grace-expired`.
- Budgeted corpus (5 base/fix pairs): the needless overruns are gone (LIBRARY:8 and DATA_TRANSFORMATION:1 stop
  at the deadline with base's plan; PHARMA:1 was misreported — today's stop returns it unpriced);
  DATA_TRANSFORMATION:12 fixed 5 of 5 (+115 to +399 ms); SOCIAL_MEDIA:10 +297 to +507 ms, capped; planning of the
  177 queries that never reach the deadline +0.3 %. query2 forced-deadline test green (1.2–1.3 s, c=6679).
  Five mutations caught; nothing green→red in the core (1,890) and LMDB (236) selections.
- Flight waiter reproduced on both builds: it waits only to the owner's deadline, then plans alone with that
  deadline expired and returns the unpriced written-order fallback. Decision: the waiter waits for the owner's
  result until the owner's hard stop (deadline + grace) and gets no fresh deadline of its own; implementation in
  flight (`round8/`).

## Not in this change

Making Frontier statistics evaluation cheaper during planning (the larger lever: about 90 % of search time), a
connectivity-respecting fallback order for unpriced plans, and the tier-dominance rule.
