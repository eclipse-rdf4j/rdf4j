# Review of the cut-execution and bound-open feedback fixes (round 1 diff, 2026-11-10) and decisions

Read-only review by an independent agent; nothing was run. Diff under review:
`/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/impl-exact-fact/changes.diff`. RCA:
`.agent/research/GH-0000-phase1-continuation-2026-11-09/rca-exact-fact-context-and-timeout-completion.md`.
DES = `DefaultEvaluationStrategy`, OFS = `LmdbOperatorFeedbackStats`. "Decision" lines are the coordinator's.

Invariants. I1: a cut execution (thread interrupted, or a cancellation request reached it) is never published to
learning as completed. I2: an exact fact, and the absolute logical-row observation of an unconditioned key, are
written only when no open of the node received a binding for one of the node's own variables.

**1. Medium, confirmed by trace — the legacy completed-tree path escapes I2 and, per node, I1.** With
`trackResultSize`, `trackTime` or runtime telemetry on, `NodeTelemetryAccumulator` → `recordCompletedQuery` →
`recordObservation` → `recordFrontierOutcome` (OFS 1731; OUTPUT_ROWS 1779-1790, exact fact 1812-1831) writes for
every operator, typed ones included, with no bound-input check, and before the typed targets publish. Its per-node
branch (1754-1756) accepts `isCompleted(node)` in place of root completion, so a cut run still publishes leaves that
exhausted their own open.
Decision: FIX. Typed nodes are owned by the typed path: the legacy path writes neither exact fact nor OUTPUT_ROWS
for a node whose contract carries a typed descriptor. For contract-less nodes the exact fact requires root
completion and a position that is not re-invocable.

**2. Medium, behaviour change, no concrete failure found — cooperative cancellation became live below the feedback
wrappers.** Before, the root wrapper was not `CooperativeCancellation`, so the time limit's first step and LMDB's
close-time forward never reached an operator; timeouts relied on the interrupt and the forced close. The round-1
diff forwards, so every operator's `requestCancellation` now runs on the dispatch thread in production, into a
delegate that may be mid-close; verification covered three test classes.
Decision: DO NOT FORWARD in this change. `TimeLimitIteration.deliverTimeoutCancellation` starts its escalation
whatever `requestCancellation()` returns, so a root that records the request and returns false leaves delivery
exactly as it was. Only the root records requests; non-root counting wrappers classify by the interrupt alone and
do not implement the interface (this also removes finding 6). Making the feedback wrappers transparent to
cooperative cancellation is a separate change with its own qualification (open item).

**3. Low, confirmed — the `TimedIterator` hunk is dead code and a trackTime-only root never publishes a completed
run** (it calls `super.hasNext()`, which closes and publishes before the classification runs). Pre-existing.
Decision: FIX with tests (classify before closing, as `ResultSizeCountingIterator` does).

**4. Low/medium, confirmed — physical residuals and the lifecycle still learn from bound opens** (filters report
the contract's implementation id, so a FILTER under a bind join teaches unbound residuals).
Decision: FIX. The same gate covers the residual and lifecycle blocks.

**5. Low, suspected — a GRAPH body is opened once per candidate graph with the graph passed outside the binding
set**, so the gate cannot see it. Decision: ESTABLISH how the body is keyed; fix test-first if the key is
unconditioned over graphs, otherwise report.

**6. Low, suspected — sticky cancellation flag on re-invocable counting wrappers.** Removed by decision 2.

**7. Info — the reverse direction is not covered:** a key priced as bound but opened unbound (hash join, replay
join evaluating its right side once). Outside I2 as stated. Decision: record as an open item.

**8. Low — test gaps.** No test covers `ResultSizeCountingIterator` or `TimedIterator`; two tests stay green without
the hunk they are named for; `LearnedState` reads private fields by reflection; `LmdbTimeLimitTestAccess` is a split
package. Decision: ADD tests for the two wrappers and for "a lower operator swallows the interrupt, the root still
recorded the request"; replace reflection with package-private accessors; the split-package bridge stays (there is
precedent in the evaluation module).

**9. Info —** `FilterIterator.CompletionTrackingIteration` cannot violate I1 on LMDB (its channel is disabled
there). No action.

## Outcome of rounds 2 and 3 (implemented, merged into the working tree uncommitted)
- Finding 1 turned out to be unreachable from a real execution: nothing in production writes
  `optimizer.frontierStateDigest`, so `recordFrontierOutcome` returns before any write; only tests that stamp the
  metric reach it. The guard was kept (typed nodes are owned by the typed path; contract-less exact facts need root
  completion and a position that is not re-invocable) so the path cannot violate I1/I2 if the stamp returns. Three
  tests that pinned the old ownership were rewritten around their named invariants, each shown red under a
  one-line product mutation. One parameterised case (`completedTreePublicationHonorsTheTypedPopulationQualification`
  [1]) is now redundant with case [3].
- Finding 2: only the outermost feedback wrapper (the feedback root, or a counting wrapper created for a compilation
  root) records a request; it returns false and forwards nothing. Checked by the coordinator: 
  `TimeLimitIteration.deliverTimeoutCancellation` escalates whatever the return value is, and LMDB's
  `LmdbStoreConnection` result `requestCancellation` interrupts the evaluating thread and ignores the delegate's
  answer; with a cooperative delegate it additionally retains and releases an operation read attempt around the
  callback, which is the path it already takes for native delegates. Inner counting wrappers record nothing and (a
  coordinator edit after the merge) skip the per-open flag reset.
- Finding 3 fixed (`TimedIterator` classifies before closing). Consequence to watch: time-tracking runs now report
  exhausted closes, so paths that look at node completion may learn from runs they skipped before.
- Finding 4 fixed (one gate for row observations, physical residuals and the lifecycle).
- Finding 5: a GRAPH body carries no learning key (`PlanningGraphDomain.allowsNativeEvidence()` is true only for
  the default graph domain), so there is nothing to fix.
- Finding 8: tests added for both wrappers and for a swallowed interrupt; reflection replaced by package-private
  getters; the split-package bridge stays.
- Still open: the reverse direction (finding 7); legacy physical-residual, lifecycle and contract-less OUTPUT_ROWS
  writes on a per-node close; filter-outcome and selectivity recording; semi/anti hit-rate counts; making the
  feedback wrappers transparent to cooperative cancellation; why q11's OPTIONAL leaf got an unbound identity while
  the executor ran it as a bind join (selected-to-executed divergence).

## No defect found
Every typed LMDB writer is gated by root completion, so I1 reduces to the root's classification, which is correct
for the root, counting-as-root and result-size wrappers; flag-before-forward ordering; the interrupt status is only
read; the root resets its flag on bind; prepared-query bindings are hidden from `hasBinding`, so no false positives
from them; bound fast paths bypass the wrappers and produce no unflagged observations; hot path: nothing per row,
one volatile read and one `isInterrupted()` per end of stream, allocation-free `hasBinding` calls per open until
the first hit; static initialisation of the new constants is safe.
