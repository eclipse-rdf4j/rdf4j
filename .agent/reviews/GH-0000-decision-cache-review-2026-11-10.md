# Independent review of the decision-cache change (2026-11-10) and decisions

Reviewer traced the code; nothing was run. Line numbers are in the working tree after the merge
(core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/). "Decision" lines are the coordinator's.

## Findings

**C1. High — the miss fingerprint depends on data and evidence revisions, so the cache can stay stuck (P6).**
`LmdbPipelinePlanCache.java:569-574` mixes in `routingFingerprint` = `mix64(query.hashCode() ^ context.routingHash())`
(lines 77, 1068-1079), which includes the data, frontier, LEO and learned-evidence revisions,
`predicateRangeVersion` and `executionSnapshotEpoch`. The exact key itself does not depend on revisions.
Scenario: the cache is full and a client loops "commit, then run Q". Every miss of Q gets a new fingerprint, so
`recordMiss` never reports a repeat and `admitFlight` returns BYPASS. Nothing else evicts the families already held.
Replay-only families stop matching after every commit but are only evicted when a proven repeat needs room, so dead
ones build up. Execution feedback that bumps `feedbackEpoch` likely has the same effect without commits (unverified).
Decision: FIX. (a) The fingerprint is built from revision-independent parts only. (b) New rule P7: a family that can
no longer be served to anyone — a replay-only family whose pinned revisions differ from the current ones — is dead
and may be reclaimed by any admission or publication that needs room, before LRU and without proof of reuse.

**C2. Medium-high — queries that differ only in non-constant attributes look like repeats (P3).**
`ConstantFingerprint` (lines 1132-1161) hashes only `Var` values, `ValueConstant` values and VALUES rows;
`StructuralKey.hashCode` uses `ShapeFingerprint` (`PackedPlanCache.java:427-461`), which hashes class names,
variable names and value kinds. Missed: Slice offset/limit, Compare and MathExpr operators, FunctionCall IRIs,
OrderElem direction, projection and extension aliases, statement-pattern scope. The exact key does tell these apart.
Scenario: paging with `LIMIT 10 OFFSET k` — every page after the first is treated as a proven repeat and evicts hot
families at admission and again at publication. Evictions only, never a wrong plan.
Decision: FIX. The fingerprint must distinguish everything the exact key distinguishes, up to hash collisions.

**C3. Medium — eviction for proven repeats that can never publish (thrash, P2/P3).**
`admitFlight` (`LmdbPlanDecisionCache.java:1373-1380`) evicts as soon as the key is a repeat and its transient
identity would fit. Keys that repeat but can never publish: the same query with different `trackTime`,
`trackResultSize` or `runtimeTelemetry` (in the dependencies but not the family key, so a live ranked family blocks
it, lines 1525-1531); unpriced decisions in BUDGETED mode (`buildPublication` returns null); families larger than
the budget (line 1512). Each miss of such a key evicts LRU families; one-off queries then fill the freed space.
Related, pre-existing: at publication `makeRoom` evicts every other family before discovering
`publicationBytes > familyHeadroom()` (lines 1564, 1631, 1771).
Decision: FIX both. A fingerprint whose flight failed to publish is remembered as non-publishable and does not evict
again (until it publishes or ages out of the doorkeeper); check headroom before `makeRoom`.

**C4. Medium-low — two keys in one family keep replacing each other's replay-only entry (P6).**
`blocksBuild` is false for a replay-only family (lines 1525, 2337), so any build replaces it and `removeAliases`
drops the other keys' aliases. K1 (normal run) and K2 (same query with `trackTime`), both unpriced, alternate and
neither ever hits. Same when appending an alias fails for lack of room (line 1634).
Decision: FIX. A replay-only family is replaced only by the same exact key or by a ranked decision; another key with
a replay-only decision gets its own entry or alias, never a replacement.

**C5. Low-medium — a replay-only build can join a ranked variant and gain ranked treatment (R2).**
`appendCompatibleExactAlias` (lines 1593-1612) only blocks ranked-joining-replay-only. An unpriced decision with the
same plan and dependencies as a ranked family gets an alias to the ranked variant; that key then gets stale reuse
and detached refresh.
Decision: FIX. This is the one finding that breaks the "never a certificate" rule.

**C6. Low — a lifecycle-blocked replay-only entry swallows every later re-plan (R5).**
After the blocking observation each identical re-plan is appended as an alias to the blocked variant (line 1606)
without checking posterior or quality, and touches the family (line 1643) so it stays most-recently-used. The key
re-plans on every request and the dead bytes stay until evidence changes.
Decision: FIX.

**C7. Low, unverified — a wall-clock limit inside the planner is not detected (R3).**
`deadlineBounded` (`LmdbQueryOptimizerPipeline.java:367`) only looks at the goal's deadline. Foreground filter
sampling during planning is time-limited (`LmdbFilterSelectivityStats.java:805-816`, 1668-1687; on by default). On
a timeout the planner gets no estimate and no revision is bumped, so the entry keeps a decision a re-plan might not
reproduce, until background sampling bumps the filter revision.
Decision: VERIFY, then fix if a cheap, exact signal exists (a per-planning "a sample timed out" flag); otherwise
report what it would take. Do not guess with a timer.

**C8. Pre-existing — if `detachedTemplate` or `retainable` throws after planning, the flight and its reservation
leak and threads waiting on the flight hang.**
Decision: FIX, test-first (waiters must be released with the failure or re-plan; the reservation must be returned).

## No defect found
R2 serving paths all refuse replay-only (family lookup, canary, refresh submit/schedule/publish, variant publish;
detached refresh uses a ranked certificate only for complete comparisons; every certificate copy carries the
authority). R1 exact-hit checks cover data revision of the pinned snapshot, frontier, LEO, learned evidence,
predicate-range version, flags and system properties; overlay queries are keyed separately. R6 holds. P1
accounting: every charge fit-checked under the admission lock, single releases, `retained` equals the sum of
families. Lock order is always segment → admission lock → identity-budget monitor; no lock across planning; nothing
new allocated on ranked exact hits.

## Behaviour notes for the roadmap
- Once full, a new key needs two identical misses within the doorkeeper window (size `max(16, min(4096,
  maximumFamilies))`); one-off queries only fill free space.
- The doorkeeper takes about 4 KB of the family budget once.
- Replay-only entries stop matching on every commit or evidence change.
- Admission, including the eviction loop, runs under the segment lock and waits behind publications that copy
  templates.
- The miss fingerprint hashes every constant on each miss, which may load LMDB values; requests that end up waiting
  on another thread's flight compute it too.
- A replay-only entry can capture a plan the inner packed cache served from a match on other VALUES rows or resumed
  from a saved search, so "deterministic re-plan" depends on that cache's contents.
- Bypasses caused by a full segment, and requests waiting on another thread's flight, record no miss.
- In BUDGETED mode replay-only caching is disabled entirely (every search has a deadline), matching the inner
  packed cache's `reusableFor`.
