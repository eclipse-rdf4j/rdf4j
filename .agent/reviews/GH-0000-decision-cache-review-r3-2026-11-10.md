# Third review of the decision-cache change (round-4 diff, 2026-11-10) and decisions

Read-only review by an independent agent; nothing was run. Line numbers are in the round-4 private copy
(`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`). "Decision" lines are the coordinator's. D1's session
mechanism (a faked provider-work refusal) was rejected by the coordinator before this review and is replaced in
round 5 by an explicit `PackedCostSession.reproducible()` flag; it is not reviewed here.

**E1. Medium, confirmed — a ranked family without detached refresh work stays "able to serve" after its evidence
moves.** `canServe` 1711-1718 assumes stale serving through use-and-refresh. A ranked decision published with
`detachedRefreshWork == null` (UNSUPPORTED summary, or `detachedRefreshWork(...)` empty) can never serve again once
any revision moves, yet it is neither dead nor replaceable. An identical re-plan re-appends the alias
(`appendCompatibleExactAlias` 1750-1796), touches the family and keeps the key a proven repeat, so each request can
evict live families; a different plan is BLOCKED. Pre-existing stuck state that D2 does not cover.
Decision: FIX. A ranked variant can serve only if its evidence is current or its champion has refresh work.

**E2. Medium-low, confirmed — a request from an older pinned snapshot replaces a retired family and clears its
tombstone.** `mayReplace` 1688-1695 and `MissDoorkeeper.retiredUnder` 2685-2690 treat any evidence difference as
"new evidence". A reader pinned at an older revision re-plans, replaces the retired family (or clears the
tombstone), and the retired decision is served again at the newer revision.
Decision: FIX. Replacement of a retired family and clearing of a tombstone require evidence that is not older in
any monotone component; otherwise BLOCK and keep the tombstone.

**E3. Medium-low, confirmed (missing check) — detached refresh can publish a planning that was cut.**
`LmdbDetachedPlanRefresh.execute` 179-287 plans on a pooled thread with no planning scope and no cut comparison;
`publishRefresh` then installs a challenger or promotes a champion from non-reproducible inputs.
Decision: FIX. The refresh opens a planning scope and drops its result when a cut was recorded.

**E4. Low-medium, confirmed — the diagnostics opt-in sits in configs shared with timing benchmarks.**
`benchmark/ConfigUtil.java:62`, `ThemeQueryBenchmark.java:1002`, `ThemeQueryBenchmarkDelta.java:841` build stores
for JMH timing runs too, so timings and cache hit rates no longer reflect the shipped default.
Decision: FIX. Opt in only where plans are captured or explained; timing configs keep the store default.

**E5. Low-medium, confirmed — the digest hashes strings with the 32-bit `String.hashCode`.**
`PackedQueryIdentityDigest.append(String)` 93-101. Two queries differing in a same-hash constant share a miss
fingerprint; with tombstones one query's retirement blocks the other's publication for as long as evidence is
unchanged.
Decision: FIX. 64-bit mix per code unit.

**E6. Low — two more ways a cut is missed.** (a) confirmed: the uncached path and optimizers that run before the
packed session opens have no planning scope (`LmdbQueryOptimizerPipeline.optimize` 186-191, `optimizeUncached`
403-413). (b) suspected: `LmdbHeuristicFilterPassCache.getOrCompute` 120-131 can retain and share an estimate whose
live sample was cut.
Decision: FIX (a) by opening the scope in `optimize()`. For (b) establish reachability; a memo value whose
computation recorded a cut is not retained or handed to waiters.

**E7. Low, confirmed — a HARD_INVALID ranked family is never dead.** `dead()` 2551-2552.
Decision: FIX.

**E8. Low, confirmed — tombstones are not durable and cost evictions.** No-op with an uncharged doorkeeper; ages
out after `capacity` distinct misses; every `capacity+1`-th miss of a tombstoned key is a proven repeat and evicts
live families for a flight that will be BLOCKED; requests without a fingerprint skip the check.
Decision: FIX the eviction (admission must not evict for a key retired under the request's evidence). Bounded,
non-durable tombstones are accepted and documented.

**E9. Low, suspected — the tombstone digest ignores planner inputs outside `EvidenceSnapshot`**
(`predicateRangeVersion`, `adaptiveEvidenceAllowed`; `retirementDigest` 2840-2855).
Decision: VERIFY, fix if the range version can move without the data revision.

**E10. Low, confirmed — undercharges.** `publishVariant` 1994 charges only the recipe (352 B) for a pipeline-family
variant, not its own records (~700 B); challenger posteriors on the refresh path (~100 B each) are never charged.
Decision: FIX. A charge may exceed what is retained, never fall below it.

**E11. Low, confirmed — the T7 guard replaces the store's plan-execution publisher**, so it cannot catch broken
store wiring; it also leaks its temp directory.
Decision: FIX the test.

**E12. Informational — `canServe` javadoc "nothing revives them" is false** (a refresh submitted before the block
can promote a different champion). Decision: correct the comment.

## No defect found
D5 null-safety; lock coverage of every path into `evict` and the doorkeeper, lock order, no lock across planning;
ranked exact-hit path allocates/locks/hashes nothing new; T9 charge/release pairs (retained = sum of family
charges, fit-checked, no double release); D6 equal keys ⇒ equal digests including language-tag folding, no LMDB
loads, "no fingerprint" distinct from 0; D3 doorkeeper accounting; D1 scope pairing, monotonic count, no stale
mark; only foreground filter sampling records cuts (the other planning wall-clock limit is the cascades deadline,
already handled); D4 consumers do not read trace content; D2 replacement and the same-evidence exception agree for
same-or-newer evidence; the new D1/D2/D3/D6/T9 tests can fail for the reason in their name.
