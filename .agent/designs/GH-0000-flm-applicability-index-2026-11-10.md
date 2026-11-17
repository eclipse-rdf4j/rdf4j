# FrontierLearningModel — constant-time "other applicability known" (spec, 2026-11-10)

Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/measure-validity/summary.txt`, section C.
FLM = `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/FrontierLearningModel.java`.

## Problem
Every model decision scans a whole key map to answer one boolean:
- `logicalDecision` → `hasDifferentLogicalApplicability` iterates `logicalExact.keySet()`;
- `physicalDecision` (logical population) → `hasDifferentPhysicalApplicability` iterates `physicalExact.keySet()` and
  allocates a `LogicalPopulation` per candidate;
- `physicalDecision` (event population) iterates `physicalExact.keySet()` inline.

Measured on the theme workload: a planning makes a median of 466 decisions (p90 1,949, max 8,928); the scans visit a
median of 1.64 million entries per planning (max 35.5 million; about 640 per logical scan, about 3,700 per physical
scan, capped at 4,096). Re-executing a planning's decisions costs a median of 14 % of the planning's wall time (p90
33 %), which is these scans. The boolean is consumed by `gateDecision` only as
`!exactApplicabilityKnown && differentApplicabilityKnown`, which selects the label INAPPLICABLE over
INSUFFICIENT_SUPPORT; neither carries an estimate. So the scans buy an annotation.

## Change (behaviour-neutral; Routine B)
1. Two counting indexes inside FLM, maintained under the model's monitor at every insertion into and removal from the
   two maps:
   - `logicalApplicabilityCounts: HashMap<LogicalLearningKey, int>` = number of `logicalExact` keys with that
     logical key;
   - `physicalApplicabilityCounts: HashMap<(PhysicalPopulation, PhysicalResidualKey), int>` = number of
     `physicalExact` keys with that population and physical key.
   A count that reaches zero is removed. Use a small private record for the pair key; do not allocate it per map
   entry during a decision (one allocation per decision is fine).
2. The three scans become: `different = count(group) > (exactKeyPresent ? 1 : 0)`. Since the gate only uses the
   answer when the exact key is absent, compute it only then (`count(group) > 0`), and keep the three private methods
   as the single place that answers it.
3. Mutation sites to cover (all of them; find by grep, these are the ones known on 2026-11-10): `cell(...)` (put of
   a new key), `evictOne(...)` and `evictOldest(...)` (removal), `clear()`, the sidecar reader (`put` of loaded keys
   followed by `evictOldest`). Route every insertion and removal through one private helper per map so a future site
   cannot miss the index.
4. No change to keys, gates, outcomes, persistence format, capacity, eviction order or the `planningVisibleMutations`
   counter.

## Proof obligations
- Equivalence: for every reachable model state and every argument, the new answer equals the old scan's answer.
  State it as an invariant test (below), not only by reading.
- `sourceRowsPredictor()` substitution happens before the key is formed in every read and write path; the index
  must count the substituted key, as the maps do.
- Memory: the index adds at most one entry per distinct group; account for it wherever FLM reports retained bytes
  (`independentPhysicalKeyBytes`, `PhysicalInventory`) only if those claim to be complete; say what you decided.

## Tests
- Pre-change green and post-change green from the same selection: `FrontierLearningModel*Test`,
  `LmdbPlanningRevisionVersionTest`, `LmdbMappedOperatorLearningContextTest`, and the classes found by
  `rg -l "LearningGateDecision|INAPPLICABLE" core/sail/lmdb/src/test`. Hit proof: an `rg -n` line for a test that
  asserts INAPPLICABLE from a different-applicability sibling for each of the three decision methods; if one of the
  three has no such test, add it first (it must pass before the change: characterisation).
- New invariant test (package-private access, no reflection): a seeded random sequence of leases, releases,
  observations, evictions at a small `maximumKeys`, `clear`, and a sidecar write/read round trip; after each step
  compare, for a sample of (key, applicability) pairs including absent ones, the gate outcome of each decision method
  with a reference computed by a test-local scan over a snapshot of the keys (expose the key sets through an existing
  inventory method if one suffices; otherwise add one package-private read-only accessor and say so). Show it red
  under a one-line mutation that drops the index update at each of: insertion, eviction, clear, load.
- Performance evidence (not a benchmark claim): a test-scope timing loop or the existing replay instrumentation
  showing entries visited per decision drop from O(keys) to O(1) at 4,096 physical keys. Report the numbers; do not
  assert on time.

## Deliverables
`changed-files.txt`, `changes.diff`, `evidence.txt` (pre/post snippets, hit proof, mutation outcomes); a list of
anything in this spec the code contradicted. Private copy only; nothing committed.
