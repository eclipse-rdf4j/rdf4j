# Read planes observed once per request; idle cleanup no longer perturbs pinned readers (2026-11-10)

Implemented test-first by an implementer agent in a private copy, reviewed by the coordinator (main diff read in
full, tests skimmed), merged into the working tree uncommitted. Evidence:
`/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/impl-planemask/`.

## Symptom
`LmdbDetachedPlanningOwnershipTest#actualPackedPipelineKeepsARealWarmHitAndSchemaTwoDiagnostics` red 3 of 10 runs
before and after the decision-cache rounds: the third explain was not a pipeline-cache hit.

## Cause (traced, not inferred)
1. Reader. `LmdbEstimatorRuntime.frontierReadPlaneMask()` is `selection.isCurrent() ? planeMask : 0`, and currency
   requires an even publication version. `LmdbQueryOptimizerPipeline` read it at the cache gate, again for the cache
   key (`read.planes=` in the planning configuration) and twice more for the detached refresh work. A publication
   registered between two reads made one request see 3 at the gate and 0 in its key, so it missed
   (`exact-alias-miss`) and published a family under `read.planes=0`.
2. Writer. On a settled store the `LmdbValueCleanup` idle pass (10 ms backoff, doubling to 1 s) opened a publication
   scope (`publicationVersion++`), then found the query's own pinned reader, returned and closed the scope
   (`publicationVersion--`). Captured window: 86 µs. Every idle tick therefore made every pinned reader transiently
   not current for shared statistics.
3. On an unsettled store the misses are legitimate: startup maintenance activates RDF-term domains and publishes
   Frontier statistics, so the request's ranges and revisions really change. The test now settles its store.

## Invariant
A request observes its read planes exactly once, when its planning reads are pinned
(`beginFrontierReadScope` → `PlanningReadScope.observedReadPlanes()` = the captured selection's represented mask;
`LmdbFrontierReadSelection.capture` returns UNAVAILABLE, mask 0, when the read authority is not current at capture).
The cache gate, the key and the detached refresh work use that one value. A request that observed 0 runs uncached
and publishes nothing. Later currency checks decide per read whether shared evidence may be used; they never change
the request's planes. An idle cleanup that must defer to a pinned reader does not register a publication to find
that out (same predicate checked before and, as before, inside the scope).

## Review notes
- The writer pre-check is the same predicate as the in-scope check, so cleanup is deferred in exactly the cases it
  was already abandoned; no new starvation. The unlocked pre-read is rechecked under the publication gate.
- Hit path: two currency validations per request replaced by two field loads; no new allocation, lock or hash.
- A package-private test seam `afterPipelineCacheGateForTest(Runnable)` was added to the pipeline (one null check
  per cached request).
- Tests use latches, a real store and a real publication; no sleeps, no reflection.

## Open (not fixed)
1. A planning that runs while a publication is in flight has Frontier evidence declined for some reads. If the
   publication is abandoned the revision ends unchanged, so the planning is published under the full-planes key
   although it is not reproducible. Pre-existing; the writer fix removes the known settled-store trigger. Principled
   closure: count "evidence declined because the read was not current" in the planning scope and report the session
   as not `reproducible()`, the same contract as a wall-clock cut.
2. The request `Revision` has two more currency-gated components read more than once per request
   (`adaptiveEvidenceAllowed && readAuthorityAllowsEvidence()`, the adjacency generation in
   `frontierPlanningRevision`). A publication between `context()` and the cache's later `revision()` calls shows as
   "revision moved" (request uncached or not retained). Same observe-once treatment applies.
3. `LmdbPackedPlanCacheTest#multipleFreshAnonymousPathVariablesReuseExecutablePlan` and
   `#boundedCorrelatedAnonymousQueriesReuseTheHonestIncumbent` (red 10 of 10 under load before and after) fail for a
   different reason: they plan cold then hot on an unsettled store while startup maintenance moves the evidence.
   Probably outdated tests that need a settled store; not yet triaged together with the five stable reds of that
   class.
4. A reader admitted in the microsecond gap between the cleanup's pre-check and its registration still sees one
   window (the in-scope recheck then aborts the pass).
