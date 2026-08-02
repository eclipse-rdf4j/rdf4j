# Preserve Frontier evidence from costing through cache and LEO

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md`.

This work follows the completed physical-cost and semi/anti work in
`.agent/execplans/GH-0000-frontier-physical-cost-and-semi-anti-planning.md`. That plan established primitive physical
cost vectors, typed semi/anti candidates, correlation domains, source-scan feedback, and LMDB feedback format 14.
This plan starts at the next architectural boundary: the selected recipe and plan cache currently detach the Frontier
state which produced those estimates, several contextual search paths still pass rows without the state, and LEO
learns a scalar residual instead of correcting the originating Frontier transform and physical dimension.

## Purpose / Big Picture

RDF4J's LMDB planner estimates a join prefix with a Frontier state: a query-local weighted relation that preserves
which RDF term IDs occur together, their multiplicity, and the mathematical guarantee behind the estimate. Today the
memo retains that state while candidates are costed, but the selected recipe copies only scalar rows and work. The
arena is then closed, so a cached plan cannot compare its evidence with a newer synopsis. Some dependent and
correlated-filter searches also reset the prefix to a scalar row count before making the ordering decision. Finally,
selected-plan contextualization can produce telemetry by estimating an already selected tree, which makes it unclear
whether a displayed number was the number that won the search.

After this change, any supported transition which receives Frontier evidence produces another Frontier state with
honest lineage. Candidate costs are recorded as immutable events at the instant the candidate is measured. Recipe
extraction and materialization only copy the winning event. A bounded detached evidence bundle survives the arena and
can be retained by the store-owned plan cache. When data or LEO feedback changes, the cache replays or pairs that
evidence against the current generation and reuses the plan only when the winner remains statistically safe. Runtime
observations carry the originating event and stable Frontier learning key, so LEO corrects the relevant transform and
individual physical work dimensions rather than multiplying an entire plan by one residual.

The observable demonstrations are SOCIAL_MEDIA q9, q4, HIGHLY_CONNECTED q10, and LIBRARY q10. q9 must choose the
Frontier-supported `VALUES -> name -> ab -> da -> cd -> bc` order and expose only estimates recorded during candidate
search. q4 must remain a five-probe streaming anti plan. The q10 queries must retain their beneficial materialized
plans. Repeating an unchanged query must produce a zero-estimator cache hit, while changing the data or LEO revision
must either validate a newly recorded decision event or fully replan.

## Progress

- [x] (2026-07-31 20:31Z) Read the repository ExecPlan, Maven, and HotSpot performance instructions.
- [x] (2026-07-31 20:31Z) Complete the required offline root clean install; `BUILD SUCCESS` in 39.987 seconds.
- [x] (2026-07-31 20:33Z) Confirm the active recipe, inherited-context, correlated-filter, fallback, and LEO loss sites.
- [x] (2026-07-31 20:38Z) Capture the failing BagEstimate sidecar contract and preserve state through every transform.
- [x] (2026-07-31 20:50Z) Capture and pass the detached inline-payload contract after arena closure.
- [x] (2026-07-31 20:52Z) Carry detached state ordinals and guarantees from memo metadata into selected recipes.
- [x] (2026-07-31 21:06Z) Introduce typed Frontier dispositions and retain bound/opaque lineage at LMDB fallbacks.
- [x] (2026-07-31 21:10Z) Carry `PackedEvidenceContext` through inherited leaf, filter, and join costing.
- [x] (2026-07-31 21:14Z) Preserve winning Frontier states across correlated-filter DP lanes and scheduling.
- [x] (2026-07-31 21:50Z) Preserve the previously costed outer Frontier state through dependent-subquery assembly.
- [x] (2026-07-31 21:50Z) Record provider calls as immutable costing events and make selected-plan assembly estimator-free.
- [x] (2026-07-31 21:50Z) Export and import resource-free detached evidence with stable tuple pairing and digests.
- [x] (2026-07-31 22:45Z) Restart exactly once in scalar mode after an explicit whole-Frontier-session failure.
- [x] (2026-07-31 22:58Z) Retain unary, binary, unkeyed, and budget-degraded lineage without using state zero.
- [x] (2026-07-31 23:14Z) Add bounded exact `DISTINCT` and deterministic `GROUP` kernels with focused red/green evidence.
- [x] (2026-08-01 00:39Z) Finish exact/bounded set, slice, intersection, and value-bounded zero-length-path transitions.
- [x] (2026-08-01 02:58Z) Add bounded cross-generation cache lookup, validation, and single-flight replacement.
- [x] (2026-08-01 02:58Z) Add paired decision-risk inference and adaptive confidence from 0.51 through 0.999.
- [x] (2026-08-01 02:58Z) Route observations to state-specific LEO cardinality and physical-dimension posteriors.
- [x] (2026-08-01 04:55Z) Make correlated-filter winner emission copy its retained DP events without provider replay.
- [x] (2026-08-01 08:48Z) Separate operator result-row work from LMDB source scans throughout immutable cost events.
- [x] (2026-08-01 11:36Z) Cost every concrete join implementation as a physical event across all search kernels.
- [x] (2026-08-01 11:45Z) Prevent nested costing events from inheriting their parent's derived telemetry fields.
- [x] (2026-08-01 14:03Z) Pass all 1,087 query-algebra evaluation tests and preserve the first full LMDB red gate:
  1,629 tests with seven focused failures in `initial-evidence.frontier-lmdb-module-red.txt`.
- [x] (2026-08-01 14:11Z) Repair the seven focused LMDB planning regressions without weakening their structural
  contracts; the 78-test Frontier integration class and 46-test estimate-audit harness both pass.
- [x] (2026-08-01 21:26Z) Measure faithful q9 locked-order variants, repair opaque unary-prefix reconstruction, and
  correct the structural gate to the empirically faster `cd -> bc` winner without adding a preference rule.
- [x] (2026-08-01 21:41Z) Add an alpha-renamed uncached-planning harness, profile q9 in Docker, and remove
  allocation-heavy LMDB page/node decoding without changing candidate semantics or search-space bounds.
- [x] (2026-08-01 22:40Z) Assemble dense-DP and sparse-DPhyp winners from retained immutable costing events, and
  replay exact correlated factor lattices instead of re-running already completed sub-lattice searches.
- [x] (2026-08-01 23:05Z) Reuse identical primitive finite-surface cardinality measurements query-locally and reduce
  uncached SOCIAL_MEDIA q9 planning from 3,654.378 to 772.904 ms/op without changing the candidate space.
- [x] (2026-08-02 02:23Z) Separate learned statistical guarantees from payload disposition at every transform and
  bridge boundary; learned bound-only evidence remains rankable without being opened as tuple evidence.
- [x] (2026-08-02 05:40Z) Eliminate completed correlated-lattice replay and exact duplicate decision-trace records
  across dense DP and sparse DPhyp without changing the candidate space or winner comparison.
- [x] (2026-08-02 06:35Z) Profile uncached q9 planning in Docker, remove metric-snapshot point reads, reduce mapped
  query-index access checks, and canonicalize each completed Frontier payload exactly once; q9 reaches 627.240 ms/op.
- [x] (2026-08-02 19:06Z) Trace the immutable candidate lifecycle for nested subselects and re-enumerate a prioritized
  JOIN when a descendant winner changes; the focused repeated-direct-lookup regression now passes in `AUTO` mode.
- [ ] Restore the formal packed join hypergraph and DPhyp CSG/CMP receiver so bounded search eliminates only
  topologically impossible subproblems and never substitutes traversal priority for enumeration completeness.
- [ ] Verify LMDB, query snapshots, benchmarks, and the Theme corpus.

## Surprises & Discoveries

- Observation: `PackedCostContext` already has a query-local `evidenceStateId`, and the normal dense, sparse, and
  multiword join searches use it. The loss is not a missing provider interface; it is a set of state-free contextual
  call sites.
  Evidence: `PackedJoinEnumerator` passes state IDs in its ordinary subset kernels, but
  `contextualizeLeaf`, dense correlated-filter transitions, scheduled filters, and inherited-prefix initialization
  still call `reset` without a state.

- Observation: the packed rewrite removed the repository's previously oracle-tested `Hypergraph` and
  `SubgraphEnumerator`; the current so-called sparse-DPhyp kernel is cardinality-layer subset expansion over pairwise
  output-mask adjacency, while dense and correlated search use the same adjacency closure. It does not construct
  hyperedges or emit CSG/CMP pairs.
  Evidence: `PackedJoinEnumerator.adjacency` creates only pairwise shared-binding edges, `optimizeSparseLong` scans
  retained states by cardinality, and repository history at `cef5254a05` and `577e0791cb^` contains the removed DPhyp
  implementation plus randomized DPsub-oracle tests. The bounded AAS q2 test misses the endpoint-bound path at 256
  work units, while the same estimator selects it with an unbounded budget.

- Observation: a prioritized JOIN candidate can become stale even after a later logical fallback refresh records its
  direct child winners as current. The tier-one fallback cannot replace the earlier tier-zero costed winner, and the
  old logical-input fingerprint then prevents fixed-point propagation from scheduling another costing event.
  Evidence: the logging-enabled subselect run retained event 100 with two pre-refresh child winners, rejected event
  110 with the current child winners, and selected the stale 74,477-by-74,477 type cross product. Re-enumerating only
  when the direct winner identities changed selects the bound direct-lookup alternative and passes
  `LmdbSubSelectDirectLookupEstimateTest#subSelectPlanDoesNotDoubleCountRepeatedDirectLookupRows`.

- Observation: the memo already retains the selected state ID and `EvidenceGuarantee` in primitive columns.
  Evidence: `PackedPhysicalMetadataArena` has `evidenceStateIds` and `evidenceGuarantees`, while
  `PackedPlanRecipe.Extractor.append` copies rows, work, access, and planned metrics but omits both columns.

- Observation: the existing selected-plan contextualizer is a second cost search, not a passive annotation pass.
  Evidence: `PackedCascadesPlanner.compute` selects `rootWinnerId`, invokes
  `PackedSelectedPlanContextualizer.contextualize`, and only then extracts the recipe. The contextualizer calls
  `estimate`, `refineOperator`, and `refineIntermediateJoin`, restores incumbent planned metrics, and can offer a new
  winner.

- Observation: the plan cache uses exact `Context` equality including `dataRevision`, so it has no route for finding
  a structurally matching stale-generation plan.
  Evidence: `PackedPlanCache.PlanEntry.matches` compares the complete context, and segment routing includes
  `context.queryHash`, which itself includes the data and predicate-range revisions.

- Observation: Frontier already distinguishes `CERTIFIED_BOUND_ONLY`, `SCALAR_FALLBACK`, and `UNRESOLVED` guarantees.
  The new composability/opacity distinction must therefore be orthogonal rather than another overloaded guarantee.
  Evidence: `EvidenceGuarantee.isComposablePointEstimate` accepts only exact, unbiased, and learned-calibrated states.

- Observation: `withRowsPreservingEvidence` was the most misleading latent sink: it delegated through
  `EvidenceProfile.toBagEstimate`, which necessarily created a state-free result.
  Evidence: the first focused contract failed at the ordinary `withRows` assertion, and the same implementation trace
  showed the explicit state loss in `withRowsPreservingEvidence`. Constructing the rebased profile with the existing
  sidecar makes the focused method pass 1/1.

- Observation: detaching a CALIBRATE state must not copy the raw tuple payload a second time.
  Evidence: `FrontierStateArena.calibrate` shares its parent's payload owner and records the correction as an immutable
  lineage node. The bundle therefore exports the raw parent payload once and retains only the calibration overlay on
  the child.

- Observation: arena IDs cannot be used as detached ordinals even when they often appear topological.
  Evidence: canonical keys are sorted and assigned IDs before materialization, so a parent can have a numerically
  larger canonical ID than its child. Export now performs an iterative lineage traversal and assigns new parent-first
  ordinals.

- Observation: correlated-filter DP needs two state columns per subset, matching its pending/applied FILTER lanes.
  Evidence: the focused provider rejected the scheduled FILTER because both the factor transition and operator input
  had state ID zero. Storing each successful provider output in the corresponding lane makes the same focused method
  pass while retaining the distinct prefix and child states.

- Observation: inherited-prefix costing has three independent contextual identities in addition to rows and state.
  Evidence: the focused contract distinguishes binding layout, correlation mask, and semantic-scope mask and observes
  all three unchanged in contextual leaf and FILTER refinement calls.

- Observation: an event-sourcing wrapper around the provider boundary is sufficient to make exact cache hits
  bit-for-bit stable without adding allocations to the dense memo rows.
  Evidence: a provider that deliberately changes its estimate on a second invocation is called once; the cold plan
  and exact cache hit retain the same event digest, rows, and work, while selected-plan assembly makes no provider
  call.

- Observation: selected dependent subqueries do not require a second estimator pass once contextual winners have
  already been installed in the memo.
  Evidence: the assembler now walks the selected winner graph, reconstructs only primitive input context, and links
  the matching contextual dependent winner; the focused contract passes with a provider that rejects any assembly-
  time invocation.

- Observation: detached inline Frontier payloads can be imported without remapping individual tuple columns.
  Evidence: import predeclares canonical keys parent-first, recreates each stratum as a paired tuple relation, and a
  subsequent export has the same state digest and `(x,y)` pairing after the source arena has closed.

- Observation: a non-composable state must be stopped before payload-opening join kernels, not treated as a provider
  failure.
  Evidence: the work-budget regression initially triggered the new whole-session failover because the join resolver
  attempted to open a bound-only state. Checking disposition at the transition retains its typed degradation and the
  focused LMDB methods pass 2/2.

- Observation: exact `GROUP` cannot reuse the finite BGP surface estimator because the latter deliberately accepts
  only bounded basic-graph-pattern unions and joins.
  Evidence: the new aggregate contract first failed with `exact_only_group_boundary`. Bounded replay of the exact
  Frontier input through RDF4J's aggregate evaluator now produces a database-exact child with `GROUP` lineage while
  sampled, over-budget, nondeterministic, and unsupported aggregates retain a bound-only state.

- Observation: packed INTERSECTION dispatch was not missing from the generic incumbent search; the first direct test
  instantiated the optimizer without a snapshot-backed triple source, which correctly disabled Frontier for the
  complete session.
  Evidence: the same algebra, planned with a `SailDatasetTripleTermSource`, reached the LMDB binary transition and
  retained the left bag multiplicity for mappings in the right support. Sampled/incompatible inputs now keep a
  two-parent bound-only state.

- Observation: a value-bounded zero-length path is a finite relation even when the endpoint value does not occur in
  the database.
  Evidence: RDF4J's zero-length iterator returns one compatible mapping whenever either endpoint is fixed, or exact
  zero when two fixed endpoints differ. Materializing that relation as an exact Frontier leaf avoids a store scan and
  does not claim exactness for the unbounded, snapshot-enumerating form.

- Observation: a root-only decision certificate cannot validate a physical choice made by a child operator.
  Evidence: LEO raised the cost of an exact materialized semi/anti scan above streaming, but stale validation retained
  the cached materialized winner until the certificate included the root decision's complete child-decision closure.

- Observation: replayed local cost events form a dependency DAG; substituting each event objective for the complete
  candidate cost either drops child changes or counts a prior generation's delta twice.
  Evidence: focused cache tests first observed no root movement after a leaf-event change, then observed `13` instead
  of `7` on a second unchanged generation. Retaining child event/cost edges and rebasing event baselines fixes both.

- Observation: connected Frontier component rows can be the complete intermediate-join rows.
  Evidence: SOCIAL_MEDIA-q9-shaped bridge planning retained the two-factor state only on the appended leaf. Comparing
  the component rows with the composed join output before attaching the immutable event preserves disconnected
  multiplication semantics and lets LEO update the actual intermediate transform.

- Observation: certified stale reuse needs independent shadow replans, not only audits after validation declines.
  Evidence: the first low-confidence certified-reuse contract opened one planning session and recorded zero audits.
  Spending the residual error probability `1 - confidence` on deterministic audit generations now opens a fresh
  session, assigns an independent lane, and records stable/flip regret in the adaptive posterior.

- Observation: ordinary scalar `setRows` cannot be inferred to describe a joined component.
  Evidence: interpreting an unscoped scalar leaf estimate as component rows made the same connected pair estimate 10
  rows in one orientation and 100 in the reverse orientation. The packed subset DP then selected `large -> small`
  over the lower-cost `small -> medium` path. Only `setComponentRows` and `setContextualRows` have sufficient scope to
  replace or compose a join cardinality.

- Observation: the correlated-filter DP formerly depended on a post-hoc canonical-root row stamp to make equivalent
  schedules comparable.
  Evidence: without that stamp, the state-zero scalar fallback estimated the same complete FILTER expression as 500
  or 50,000 rows depending on when the filter ran. Frontier/contextual estimates do not need this compatibility path;
  a scalar provider with no scoped estimate must instead reuse the logical group's already-costed equivalence
  cardinality during candidate costing and carry that selected DP value into emission.

- Observation: retaining only rows and state IDs in correlated-filter DP lanes is not enough for event sourcing.
  Evidence: JDWP showed the winning DP path entering `emitScheduledFilterOrder`, which reserves the seed budget a
  second time and invokes the factor and FILTER providers again. Equivalent typed semi/anti alternatives therefore
  spent four additional units apiece, starved a later finite anchor, and could stamp a different answer from a
  provider whose second invocation changes. The DP must retain the winning transition's physical metadata/event and
  assemble memo winners from those immutable records.

- Observation: a generic correlated `FILTER` seed is theorem-dominated when its logical group already contains the
  three typed semi/anti implementations.
  Evidence: JDWP showed the canonical direct `FILTER(EXISTS)` consuming five bounded-search units before the
  streaming, memoized, and materialized alternatives for the same group each received their own seed. Skipping only
  that redundant generic seed preserves all physical algorithms, while a commuted FILTER in a separate group remains
  the sole schedule capable of exposing its late correlation and must still be seeded.

- Observation: repeating the relation-ID order cannot propagate a late logical-alternative winner to a fixed point.
  Evidence: the SOCIAL_MEDIA q7 winner trace cost relations 14--19 with group 10's `144077` materialized incumbent,
  then relation 22 improved group 10 to the `343` memoized semi/anti candidate. A second linear pass repeated exactly
  that order, again refreshing relations 14--19 before relation 22, and left the selected root at `144200` work rows.
  Logical-expression IDs describe append order, not the dependency order of equivalence-group winners.

- Observation: inherited-prefix planning recognizes a safe subtree and then silently returns its unbound winner at a
  deterministic unary wrapper.
  Evidence: PHARMA q7's OPTIONAL right side is `Extension(StatementPattern(?comp, name, ?optName))`. Both UNION
  branches assure `?comp`, but `optimizeWithInheritedPrefix` stops at the Extension and retains the global 13.2K-row
  POSC scan. The materialized plan later labels `?comp` bound even though event 47 was measured with input context
  zero, inflating the LeftJoin to 731.9K work and violating event-sourced context fidelity.

- Observation: the physical vector conflates rows produced by an algebra operator with rows scanned from an LMDB
  access path.
  Evidence: after contextualizing PHARMA q7's OPTIONAL name lookup, telemetry correctly changed the child from an
  unbound POSC scan to a bound SPOC lookup, but the exact LeftJoin still retained 499.6K `sequentialRows`. That number
  is the generic pre-Frontier result-cardinality work fallback, not an LMDB source scan. The focused physical-cost
  contract fails because `PackedCostEstimate` has no independent tenth `resultRows` dimension.

- Observation: a memo-only binding-preserving wrapper has no original packed-query relation in its equivalence group.
  Evidence: LIBRARY q3's relocated FILTER and HIGHLY_CONNECTED q10's typed ANTI_JOIN were valid selected prefix
  winners, but prefix reconstruction tried to convert their synthetic logical source to a base query relation and
  aborted the complete Frontier session. Walking through binding-preserving unary winners retains the physical
  source's group identity while the selected event's Frontier state continues to carry the FILTER/semi/anti row
  distribution.

- Observation: preserving the logical join state is insufficient when the physical implementation is selected by a
  different cost surface.
  Evidence: JDWP showed a deterministic Extension bridge choosing a semantically required independent hash join at
  objective cost 26 versus 28 for dependent iteration, while the memo retained only the logical scalar. Recording a
  separate physical event across canonical, correlated-filter, scheduled-filter, inherited-prefix, greedy, and
  filter-pushdown paths retains the actual hash build/probe/result vector and an implementation-specific LEO key.

- Observation: event-derived telemetry is not provider metadata and cannot be nested in a later event's payload.
  Evidence: a physical hash event had correct primitive dimensions `2 + 2 + 2 = 6`, but its payload retained the
  logical parent event's `optimizer.costEventWorkRows=2`; materialization replayed that stale field over the child.
  Reserving the `optimizer.costEvent*` namespace and reconstructing it exclusively from primitive event columns makes
  the arena-level and LMDB explain regressions pass.

- Observation: applying cardinality LEO only to the logical join event does not correct the physical join event which
  becomes the retained prefix for downstream search.
  Evidence: `leoCalibratedPrefixChangesTheFollowingBridgeState` finds the two-factor physical event with four exact
  `result_rows` observations but an uncalibrated `MEASURE_UNBIASED` output state; the three-factor event is calibrated.

- Observation: the first full LMDB gate exposed contextual-plan failures as a coherent family rather than budget
  exhaustion.
  Evidence: the search status is `COMPLETE`, yet finite VALUES plans retain `[P]` scans instead of `[P,O]`, a
  correlated OPTIONAL right side selects an independent hash over global inputs, and trained streaming semi/anti
  feedback loses the typed physical alternative. The same gate also shows an alternative-path UNION degrading at
  `union_child_state_non_composable` and collapsing a 90-row bag estimate to one row.

- Observation: a physical join event can carry a valid bound-only or opaque Frontier state without allowing the
  state transition to replace the implementation's already-costed physical vector.
  Evidence: `degradeOperator` correctly constructed nonzero lineage, but in authoritative mode it also replaced an
  independent hash event's build/probe vector with scalar result work. The resulting zero build/probe telemetry made
  broad hash plans look cheaper than finite bound probes across the VALUES, alternative-path, and semi/anti failures.

- Observation: an independent hash nested under an inherited binding context is constructed once per outer mapping.
  Evidence: the correlated OPTIONAL plan priced one 2.1K-row RHS scan although `LeftJoinIterator` constructs the
  right-side `HashJoinIteration` 300 times. The dependent alternative already prices its bound factor across all 300
  invocations; the hash alternative must aggregate the independent child and hash dimensions over the same execution
  partitions.

- Observation: transform cardinality and physical implementation work need distinct learning identities on the same
  immutable event.
  Evidence: overwriting the logical join's learning key with `implementation=dependent-iteration` routed actual rows
  only to the physical key, so the following bridge transform could not find the four cardinality observations.

- Observation: a finite lookup derived solely from binding-assignment values is not a complete connected-component
  estimate when the appended statement pattern also bridges an already joined prefix component.
  Evidence: the candidate trace priced two exact `branchName` probes as two complete rows after a 400-row
  copy-to-branch prefix. That let the search postpone the finite anchor and collapse 400 rows to two, even though the
  two probes describe only the local assignment-to-name surface and the complete joined prefix contains 200 rows.

- Observation: the query-local exact-surface budget was charged repeatedly for the same topology and finite domain.
  Evidence: scoped tracing evaluated the identical `type -> locatedAt -> name -> type` surface twice, consuming 800
  rows each time, then exhausted the 4,096-row budget immediately before the dependent `EXISTS` probe. The existing
  factor-cost cache could not reuse it because scalar prefix-row fields differed even though the exact database
  surface did not.

- Observation: caching only complete derived requests still rescans every exact prefix for each appended factor.
  Evidence: the finite `name -> locatedAt -> type -> EXISTS(type)` candidate consumed 202, then 402, then 602 scan
  rows for nested prefixes even though each successful surface already retained the paired exact relation required
  to price the next access. These cumulative charges are neither candidate work nor independent evidence.

- Observation: learned filter evidence was keyed differently solely because a UNION branch marked the same filter as
  a variable-scope boundary.
  Evidence: the recorded key and UNION key had identical input topology, condition, assured binding shape, nullable
  mask, predicate/context identity, and determinism; only `scope=new` differed. The scalar estimator reused the
  observation while Frontier saw no calibration and UNION consequently summed two sampled-zero summaries.

- Observation: the bootstrap binary-join incumbent can be costed before join enumeration has produced a contextual
  candidate state.
  Evidence: the MINUS audit's first connected two-pattern JOIN entered `resolveRawJoinState` with two composable
  sampled leaves but candidate state zero. Multiplying those coordinated samples produced
  `correlated-random-product-unresolved`; its scalar-compatible local cost tied the later valid bridge candidate and
  allowed the opaque incumbent to reach the boolean kernel.

- Observation: an exact conditional bridge is replayable only when its operation recipe identifies the appended
  statement factor.
  Evidence: replacing the invalid sample product with `extendInner` restored Frontier source attribution, but the
  MINUS output initially remained equal to its left input. The bridge recorded the parent JOIN relation as its recipe,
  so alternate-lane replay could not resolve a `LeafState`. Recording the factor relation restored independent-lane
  averaging and the focused MINUS audit passed all variance-reduction assertions.

- Observation: a learned filter can legitimately select a different filter/join schedule from the cold plan, so its
  immutable costing event can have different input rows, raw pass ratio, and effective sample size.
  Evidence: the LEO residual integration test originally reconstructed the learned posterior from the cold plan's
  scalar mirrors and expected `0.22742987122611596`; the selected learned event recorded its own 94-row input and
  posterior `0.22504058628206663`. Computing the posterior solely from that selected event matches its recorded rows
  exactly and the complete 78-test integration class passes.

- Observation: a memo-equivalent unary implementation can be one logical join factor even when its memo-local helper
  child cannot be flattened into packed-query relation IDs.
  Evidence: unrestricted q9 planning aborted with `initial join factor winner expands to -1 factors instead of one`.
  Retaining the winner group's original factor as an opaque prefix component preserves its rows, cost, Frontier state,
  and unary semantics; the same focused IT then plans through Cascades with no fallback.

- Observation: q9's requested `bc -> cd` closure is not the fastest plan on the supported SOCIAL_MEDIA fixture.
  Evidence: a faithful `EXACT_SEQUENCE` harness ran all ordinary LMDB rewrites, excluded planning time, preserved the
  nullable-name proof by comparing against the untouched query, rotated execution order, and collected 21 samples per
  variant. All variants returned the same result; medians were 5.357 ms for the unrestricted
  `VALUES -> name -> ab -> da -> cd -> filters -> bc` winner, 5.375 ms for locked `cd -> bc`, and 8.034 ms for locked
  `bc -> cd`. The Frontier winner is therefore about 33 percent faster than the originally requested order.

- Observation: exact finite-surface cardinality access, rather than the packed DP itself, dominated the first faithful
  uncached q9 planning profile.
  Evidence: alpha-renaming only variable identifiers forced a structurally equivalent cache miss on every JMH
  invocation. The ten-iteration Docker baseline averaged 5,709.149 ms/op. JFR attributed about 93 percent of sampled
  allocation pressure to `HeapByteBuffer.slice`, its backing constructor, and one `LmdbNode` allocation per decoded
  B-tree node, beneath `LmdbFiniteSurfaceCache -> LmdbFiniteJoinSurfaceEstimator -> planningCardinality`.

- Observation: retaining one page buffer and one mutable node carrier per range walk materially reduces planning
  cost while leaving the exact range algorithm and every costed candidate unchanged.
  Evidence: the identical ten-iteration Docker run averaged 3,654.378 ms/op, a 36.0 percent reduction. Sampled
  allocations fell from 34,009 to 12,974 and garbage collections from 308 to 87; `ByteBuffer.slice` and
  `LmdbPage.node` disappeared from the allocation profile. `LmdbKeyComparator`, `LmdbPage.readNode`, and exact range
  counting remain the leading CPU path, so subsequent tuning can target per-comparison work independently of planner
  search-space changes.

- Observation: dense DP and sparse DPhyp retained winner rows and costs but reconstructed the selected path by calling
  the provider again.
  Evidence: a provider which counts complete ordered prefixes observed the selected two-, three-, and four-factor
  prefixes twice in dense search, and the same duplication at 17 factors in sparse search. Retaining the winning
  factor, transition, join-event, implementation, and physical-metadata IDs makes both kernels assemble the winner
  with one provider invocation per candidate. The full 66-test packed-search class and five-test subset-kernel class
  pass. Docker wall time remained statistically flat because exact finite-surface I/O still dominated the workload.

- Observation: different DP/DPhyp candidates repeatedly request the same exact primitive LMDB cardinality surface.
  Evidence: the focused contract observed two calls for the identical `(101,-1,-1,-1)` probe through one query-scoped
  finite-surface budget. Replaying that measured value once per complete primitive key reduced the ten-iteration q9
  Docker result from 3,654.378 to 798.521 ms/op. A subsequent exact mapped-index address fast path measured
  772.904 ms/op; its confidence interval overlaps 798.521, so only the probe replay is classified as a demonstrated
  wall-clock improvement.

- Observation: manually factoring the query-index row offset is slower than leaving the repeated expression to
  HotSpot.
  Evidence: the experiment moved `Math.multiplyExact` from 0.77 percent to 6.22 percent of execution samples and
  measured 817.706 ms/op. The experiment was removed. The preceding single-mapping fast path remains because it
  eliminated `FrontierQueryIndex.segment` and the one-element immutable-list lookup from the profile while retaining
  an exact multi-mapping branch; its wall-clock result is classified as neutral.

- Observation: `LEARNED_CALIBRATED` identifies a statistically corrected estimate, not the presence of a replayable
  tuple payload.
  Evidence: calibrating a sampled zero to a positive posterior correctly produced a `BOUND_ONLY` state, but the
  guarantee-only linear-transform guard admitted it and then failed with `frontier payload is not resident`.
  Payload-consuming transforms and the LMDB bridge now require both `COMPOSABLE_PAYLOAD` disposition and a composable
  guarantee; the focused contract fails before the change and passes after it, while q10 and both sampled-zero paths
  remain green.

- Observation: a complete correlated factor/filter lattice could be followed by a context-free filter-placement rule
  which rebuilt the same legal alternatives and displaced its contextual child winner.
  Evidence: the AAS end-bound property-path candidate was costed correctly inside the dense lattice, then the later
  rewrite reconstructed the path from context-free child winners and selected the opposite start direction. Recording
  exact lattice coverage by factor/filter multiset, scope, inherited state, and base winners lets the fallback rule run
  only when that search was absent, incomplete, or stale; the three focused path/MINUS regressions pass.

- Observation: decision-certificate assembly received byte-for-byte identical immutable alternatives more than once.
  Evidence: a focused memo contract observed three trace rows for two distinct candidates. Collision-safe primitive
  interning over the complete decision goal, context, event, Frontier evidence, physical vector, comparison tier,
  ordered children, and costs reduces that to two while retaining alternatives which differ only in state or child
  provenance.

- Observation: q9's post-replay profile spent planner CPU and allocation on generic representation overhead rather
  than additional estimator information.
  Evidence: production callers materialized complete `LinkedHashMap` snapshots to read one planned metric; mapped
  query-index rows performed repeated foreign-memory address/session checks; and one completed Frontier payload was
  heap-sorted for `pointRows`, effective sample size, maximum weight, and sealing. Allocation-free point lookup,
  absolute views over the same arena-owned mapped segments, and an idempotent canonicalization barrier reduce the
  identical Docker workload from 900.663 to 627.240 ms/op. Payload sort CPU falls from about 9.1 to 3.1 percent, while
  a bitwise floating-point contract proves canonical diagnostic order is unchanged.

## Decision Log

- Decision: after prioritized JOIN enumeration, re-enumerate that JOIN exactly when its direct child winner
  identities change; do not promote the executable-fallback tier to hide the stale certificate.
  Rationale: a costing event is immutable for its original input states. Changed winner identities define a new
  context that must be measured, whereas tier promotion would let an intentionally incomplete fallback displace a
  fully costed candidate and would conceal the event-sourcing violation.
  Date/Author: 2026-08-02 / Codex

- Decision: keep Frontier state continuity separate from statistical guarantee by adding an internal disposition.
  Rationale: an opaque or bound-only operator can retain useful tuple lineage without claiming a composable point
  estimate, while a database-exact state can still be temporarily replay-only because of a payload budget.
  Date/Author: 2026-07-31 / Codex

- Decision: use primitive parallel arrays for candidate event IDs, state IDs, dispositions, and decision certificates.
  Rationale: the packed enumerator is an allocation-sensitive dynamic program; an object per candidate would damage
  locality and create avoidable garbage.
  Date/Author: 2026-07-31 / Codex

- Decision: state ID zero is a session boundary, not a local operator fallback.
  Rationale: a supported operator which cannot produce a point estimate can still derive an unresolved or bound-only
  state retaining its parent and reason. A local zero prevents every downstream Frontier transform from recovering.
  Date/Author: 2026-07-31 / Codex

- Decision: reconstruct correlation prefixes through unary operators only when their algebraic output binding layout
  is identical to their selected child, and never repurpose a physical expression's source-logical ID as provenance.
  Rationale: physical source IDs are group-scoped memo identities. FILTER, query root, slice, reduced, distinct,
  materialize, order, semi, and anti preserve bindings; projection, extension, and group do not and therefore remain
  explicit prefix nodes or conservative boundaries.
  Date/Author: 2026-08-01 / Codex

- Decision: publish a finite binding lookup as component rows only when its participating assignments cover the
  entire prefix component connected by the appended factor; otherwise decline that partial estimate and use the
  full contextual surface/transition estimator.
  Rationale: component-row composition may multiply genuinely unrelated components, but it cannot invent join
  selectivity for connected prefix relations omitted from the measurement. Coverage is an algebraic property of the
  costing event, not a row-count heuristic.
  Date/Author: 2026-08-01 / Codex

- Decision: cache bounded exact derived surfaces by prefix topology, factor topology, and the complete finite-binding
  domain, independently of scalar prefix-row mirrors.
  Rationale: an exact surface is a database relation determined by those structural inputs. Reusing it prevents
  equivalent candidates and physical algorithms from spending the shared scan budget repeatedly while preserving
  the existing 4,096-row cap and declining genuinely distinct or over-budget refinements.
  Date/Author: 2026-08-01 / Codex

- Decision: memoize every successful exact derived relation as a completed-prefix Frontier surface and append later
  factors directly to that paired relation.
  Rationale: exact natural-join prefixes are reusable evidence, not work that must be rediscovered for each suffix.
  Incremental composition charges the shared budget only for the new access, preserves multi-column tuple pairing and
  multiplicity, and avoids increasing the cap or introducing an order-specific preference.
  Date/Author: 2026-08-01 / Codex

- Decision: omit the filter node's variable-scope marker from its learning fingerprint only when every condition
  dependency is assured by the filter input and the expression is repeatable.
  Rationale: such a filter is locally closed, so a surrounding algebra scope cannot change its selectivity. Filters
  with external, correlated, or nullable dependencies retain the scope marker and therefore cannot borrow evidence
  across semantically different binding contexts.
  Date/Author: 2026-08-01 / Codex

- Decision: when a binary logical JOIN has no previously costed contextual candidate, replace the invalid product of
  two non-exact coordinated Frontier measures with the existing exact conditional LMDB bridge whenever either
  physical child is a single statement factor. Preserve the child order and record that factor as the replay recipe.
  Rationale: exact conditional expansion is linear in the retained random measure and therefore composable; direct
  multiplication is not. This supplies a mathematically valid bootstrap estimate without a join-order preference,
  row threshold, or query-specific rule, and keeps independent design-lane replay available to downstream boolean
  kernels.
  Date/Author: 2026-08-01 / Codex

- Decision: tests and telemetry consumers must validate learned estimates from the immutable selected costing event,
  never by combining scalar fields retained from an earlier cold or competing plan.
  Rationale: learning can change both the physical winner and the contextual input surface. Mixing event generations
  creates a numerically plausible but causally false posterior, exactly the reconstruction that event-sourced costing
  is designed to prohibit.
  Date/Author: 2026-08-01 / Codex

- Decision: retain selected-plan contextualization only as a pre-finalization candidate phase until its costing is
  folded into enumeration; never use it to reconstruct telemetry after final winner commitment.
  Rationale: contextual dependencies are real planning inputs. Recording their cost events before the final recipe
  preserves correctness while allowing the migration to remove post-selection stamping incrementally.
  Date/Author: 2026-07-31 / Codex

- Decision: export a detached, immutable bundle containing selected states, decision alternatives, pruning proofs,
  and transitive ancestors, with inline, replayable-exact, and bound-only payload forms.
  Rationale: retaining the live arena would leak query-local store resources, while retaining only the selected
  scalar or state summary is insufficient to validate a changed generation safely.
  Date/Author: 2026-07-31 / Codex

- Decision: exact cache hits reuse the original immutable event; stale-generation validation always creates new cost
  events before a plan is reused.
  Rationale: telemetry must describe what was actually costed. A changed generation cannot stamp old numbers, and
  materialization must not call an estimator merely to refresh annotations.
  Date/Author: 2026-07-31 / Codex

- Decision: propagate late winner changes with an exact dependency worklist keyed by the child-winner IDs last used
  to cost each logical expression.
  Rationale: a logical candidate is stale precisely when one of its immutable child winner references changes. The
  worklist revisits only those candidates, enqueues consumers only after an actual output-group winner change, and
  terminates at the algebra DAG's fixed point without a pass count, percentage threshold, or query-specific rule.
  Date/Author: 2026-08-01 / Codex

- Decision: contextualize repeatable unary operators by recursively costing their child under the inherited evidence
  context, then recording a new operator event over that exact contextual child.
  Rationale: a deterministic Extension is part of the physical RHS invocation, not a reason to discard the outer
  bindings. Safety is proved from scalar relocation/effect facts and assured dependencies; unsupported scope or
  effect boundaries continue to return the existing unbound candidate honestly.
  Date/Author: 2026-08-01 / Codex

- Decision: add `resultRows` as a first-class physical cost dimension and reserve `sequentialRows` exclusively for
  source rows scanned.
  Rationale: result production, projection/extension work, and OPTIONAL merge work are real execution costs, but they
  must learn and replay independently from storage access. The dimension is carried through packed estimates,
  metadata, immutable events, recipes, cache validation, telemetry, and LEO. Operator kernels publish local result
  rows while their selected child access events retain the actual LMDB scan dimensions.
  Date/Author: 2026-08-01 / Codex

- Decision: statistical cache reuse compares candidate objective-cost differences directly with paired deterministic
  Frontier inclusion, not independent scalar cardinality deltas.
  Rationale: direct paired differences retain covariance across shared tuples and cost dimensions and avoid an
  arbitrary percentage-change heuristic.
  Date/Author: 2026-07-31 / Codex

- Decision: initialize validation at 0.99 confidence, bound it to [0.51, 0.999], and require posterior expected regret
  at or below one percent.
  Rationale: the user selected a one-percent loss budget. Independent audit lanes and shadow replans can safely reduce
  evidence effort for stable decisions, while flips and audit misses raise it without query-specific thresholds.
  Date/Author: 2026-07-31 / Codex

- Decision: preserve raw data evidence and LEO calibration as separate Frontier states.
  Rationale: exact database cardinalities must remain protected, learned corrections must be auditable, and a new LEO
  revision must be removable/replayable without rebuilding the raw synopsis evidence.
  Date/Author: 2026-07-31 / Codex

- Decision: keep `BagEstimate.equals` and `hashCode` scalar-compatible, preserve the evidence sidecar in every fluent
  transformation except `withoutEvidenceState`, and add an explicit Frontier fingerprint comparison.
  Rationale: changing public equality semantics risks unrelated map/set behavior. Frontier memo and cache code should
  use an explicit identity rather than silently depending on scalar equality.
  Date/Author: 2026-07-31 / Codex

- Decision: a decision certificate contains the selected root decision plus the transitive closure of every child
  decision referenced by any fully costed root alternative.
  Rationale: this retains relevant physical choices and pruning dependencies without allowing unrelated sampled memo
  groups to downgrade an otherwise exact validation.
  Date/Author: 2026-08-01 / Codex

- Decision: replay complete candidate costs as an immutable local-event residual plus recursively replayed child
  candidate costs; inclusive events remain atomic.
  Rationale: event objectives are operator-local measurements, while plan selection compares complete costs. The DAG
  preserves both meanings and supports generation-by-generation baseline rebasing.
  Date/Author: 2026-08-01 / Codex

- Decision: schedule certified shadow replans with exactly the validation's residual error probability and derive the
  audit lane from an independent avalanche of the data/LEO generation identity.
  Rationale: this supplies unbiased miss detection without a query rule or row-change threshold; exact confidence-one
  reuse remains zero-work, while lower-confidence tiers receive proportionally more independent audits.
  Date/Author: 2026-08-01 / Codex

- Decision: seed append events with the factor's isolated rows/work and treat unchanged unscoped scalar rows as
  compatibility mirrors. When a legacy scalar provider changes that seeded row value for the supplied prefix, promote
  its result to a complete-prefix contextual estimate; explicit component/contextual APIs remain authoritative. For a
  complete correlated FILTER candidate with state zero and no scoped provider rows, use the logical group's previously
  costed equivalence cardinality while the candidate is still in the DP, then carry that exact selected value into
  emission.
  Rationale: the provider's own before/after event distinguishes an actual legacy contextual refinement from the
  default isolated estimate without inspecting a query shape or applying a threshold. Logical-equivalent physical
  schedules then retain one output cardinality, while Frontier and explicitly scoped provider estimates remain
  candidate-specific and are never overwritten.
  Date/Author: 2026-08-01 / Codex

- Decision: correlated-filter DP transitions own their cost events and physical metadata; winner emission is a
  copy-only graph assembly step and consumes no search work.
  Rationale: provider work was already reserved and measured when the transition competed. Replaying it after winner
  selection violates telemetry provenance, double-charges bounded search, and permits a mutable provider response to
  alter the selected candidate. Primitive retained-event/metadata/winner columns preserve locality while making the
  selected graph bit-for-bit identical to the winning DP path.
  Date/Author: 2026-08-01 / Codex

- Decision: seed typed semi/anti alternatives instead of their equivalent generic FILTER whenever both occupy the
  same logical group.
  Rationale: typed alternatives are a complete physical refinement of the pure existence predicate. The generic
  seed contributes no new access schedule, whereas FILTER commutations and unsupported/untyped predicates remain
  distinct logical coverage obligations. This is a capability implication, not a budget threshold or query rule.
  Date/Author: 2026-08-01 / Codex

- Decision: cost logical join transforms and concrete runtime join implementations as consecutive immutable events.
  The logical event owns Frontier cardinality calibration; the physical event retains that state and applies only the
  selected implementation's dimension-specific LEO posterior.
  Rationale: dependent iteration and independent hash share result cardinality but have different access work,
  memory, runtime semantics, and learned residuals. Combining them in one scalar loses both provenance and the cost
  surface needed to compare future candidates.
  Date/Author: 2026-08-01 / Codex

- Decision: reserve every `optimizer.costEvent*` metric for reconstruction from `PackedCostingTrace` primitive
  columns; never persist those names in provider metric payloads.
  Rationale: a candidate may be the input to a later candidate event. Derived telemetry from the parent is not part of
  the provider's answer and otherwise overwrites the child's immutable rows, work, dimensions, phase, or digest during
  restoration and materialization.
  Date/Author: 2026-08-01 / Codex

- Decision: Frontier annotation of a physical event is cost-preserving, and nested independent work is multiplied by
  the measured inherited execution-partition count.
  Rationale: state disposition and statistical guarantee describe evidence, not a replacement cost model. Runtime
  iterator construction supplies an algebraic invocation count, so accounting for it is part of the physical model
  rather than a row-count threshold or join-order heuristic.
  Date/Author: 2026-08-01 / Codex

- Decision: retain one transform learning key for output cardinality and add a sibling physical learning key for
  access dimensions.
  Rationale: all implementations of one logical transform share result cardinality, while source scans, seeks, hash
  work, and memory are implementation-specific. Feedback records both observations from the originating event and
  planning applies each posterior at the layer it can actually correct.
  Date/Author: 2026-08-01 / Codex

- Decision: represent an unflattenable memo-local unary winner by its equivalent logical factor when constructing a
  later join prefix, and reconstruct component contributions conservatively instead of aborting planning.
  Rationale: logical-group equivalence proves the factor's bindings and semantics, while the immutable winner supplies
  its rows, cost, and Frontier state. Flattening the helper would erase the unary operation; state-free scalar fallback
  would erase evidence. Treating it as one opaque component is the exact abstraction already used by join enumeration.
  Date/Author: 2026-08-01 / Codex

- Decision: amend q9's structural acceptance from `bc -> cd` to the measured `cd -> bc` winner.
  Rationale: result-equivalent, pipeline-faithful locked variants show `cd -> bc` is materially faster, and Frontier's
  exact correlated surfaces predict the same ordering (`5,696` versus `8,957` intermediate rows). Forcing the slower
  order would require the query-specific heuristic explicitly prohibited by this plan.
  Date/Author: 2026-08-01 / Codex

- Decision: optimize LMDB range walks by reusing decode carriers and page buffers, while retaining the allocating
  compatibility entry points and all existing corruption checks.
  Rationale: Docker JFR proved these allocations occur inside the exact physical estimator for every DP/DPhyp
  candidate. Reuse changes neither the cardinality formula nor candidate enumeration, so it makes each legitimate
  costing event cheaper without introducing a row threshold, early-exit rule, selectivity guess, or join-order
  preference.
  Date/Author: 2026-08-01 / Codex

- Decision: retain the winning physical transition and immutable event in both dense DP and sparse DPhyp tables, then
  assemble the selected path by linking those records rather than invoking the estimator again.
  Rationale: the provider result which won is already the complete measurement. Replay removes duplicate work and
  prevents a later provider answer from changing telemetry; it does not prune, reorder, or approximate a candidate.
  Date/Author: 2026-08-01 / Codex

- Decision: memoize exact finite-surface cardinality measurements inside the existing query-scoped scan budget by the
  complete primitive `(subject, predicate, object, context)` key.
  Rationale: each key denotes the same physical lookup in the same planning scope. Candidate cost still counts every
  logical runtime probe, while the planner performs the identical database measurement once. The key contains no
  query name, row threshold, selectivity class, factor position, or preferred order.
  Date/Author: 2026-08-01 / Codex

- Decision: retain only profile-supported constant-factor changes and remove the manually factored row-offset
  experiment.
  Rationale: exact semantic equivalence is necessary but not sufficient for a performance patch. Docker JFR showed
  the manual factorization made HotSpot output worse, so preserving it would add complexity without evidence.
  Date/Author: 2026-08-01 / Codex

- Decision: make Frontier payload composability the conjunction of typed disposition and statistical guarantee.
  Rationale: LEO may validly correct a bound or opaque state's scalar distribution without inventing supporting
  tuples. Keeping those axes independent preserves the learned estimate for ranking and prevents local provider
  failures or false tuple composition. This is a representation invariant, not a candidate preference or threshold.
  Date/Author: 2026-08-02 / Codex

- Decision: deduplicate planner work only by complete immutable subproblem identity.
  Rationale: completed-lattice reuse validates structural scope, factor/filter sets, inherited Frontier context, and
  base winners; trace interning compares every decision/event/evidence/child field; finite surfaces retain all four
  primitive IDs. No estimate threshold, predicate identity, query text, candidate rank, or preferred order participates
  in reuse, so a semantically distinct alternative cannot be pruned as a duplicate.
  Date/Author: 2026-08-02 / Codex

- Decision: retain only constant-factor changes whose Docker JFR stack and exact contracts both support them.
  Rationale: direct planned-metric lookup preserves snapshot APIs; `ByteBuffer` views read the same mapped bytes under
  the same arena lifetime; and idempotent payload canonicalization preserves tuple ordering and floating-point bits.
  These changes make each candidate cheaper to measure without changing which candidates exist or how they compare.
  Date/Author: 2026-08-02 / Codex

- Decision: restore formal DPhyp as the source of join-search topology and remove the experimental cost-priority
  traversal as a substitute for it.
  Rationale: the hypergraph encodes shared-binding connectivity and semantic eligibility; the paper-faithful CSG/CMP
  enumerator emits each legal connected partition once in dynamic-programming order, and the Frontier-aware receiver
  alone compares candidate costs. This removes disconnected and duplicate subproblems by structural proof. It adds no
  estimate threshold, query identity, preferred start, row ordering, or fixed join sequence. Cost ordering may only
  break an exact objective tie after DPhyp has produced the same legal candidate set.
  Date/Author: 2026-08-02 / Codex

## Outcomes & Retrospective

Implementation is in progress. Frontier states now survive supported transitions, recipe extraction, arena closure,
strict and structural cache lookup, and LEO calibration. Costing telemetry is copied from immutable provider events;
stale validation replays the complete decision dependency DAG and rebases it after each certified generation.
Resource-free bundles preserve tuple pairing, weights, guarantees, lineage, calibration overlays, digests, and replay
descriptors. LEO uses stable transform/access keys with independent dimension posteriors and format-15 persistence.
Logical and physical join events are separate across every packed search kernel, and nested events cannot leak parent
telemetry into their selected child event.
Dense DP, sparse DPhyp, and correlated-filter DP now assemble winners from their original immutable events. Exact
correlated sub-lattices and identical finite-surface primitive probes are replayed query-locally, reducing faithful
uncached SOCIAL_MEDIA q9 planning from 5,709.149 to 772.904 ms/op while preserving the complete candidate space.
Focused generated populations pass all five confidence tiers' anytime coverage, false-reuse, and one-percent regret
gates. Workload snapshots, broad module verification, benchmarks, and Theme classification remain outstanding.

## Context and Orientation

The packed Cascades planner lives under
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed`.
`PackedQueryCodec` encodes RDF4J's object query algebra into integer relations. `PackedMemo` stores logical and
physical alternatives. `PackedJoinEnumerator` performs join-order dynamic programming. A `PackedCostSession` is a
query-local provider which fills a reusable `PackedCostEstimate`; LMDB implements it with
`LmdbFrontierPackedCostSession`. `PackedPhysicalMetadataArena` copies a candidate estimate into primitive memo
columns. `PackedWinnerTable` identifies the winning physical expression. `PackedPlanRecipe` detaches the selected
winner graph, and `PackedPlanMaterializer` recreates ordinary RDF4J `TupleExpr` nodes.

A Frontier state lives in `core/queryalgebra/evaluation` under the optimizer `cost` package. `FrontierStateArena`
owns immutable state rows for one planning session. Each `EvidenceStateRef` names one row and carries an immutable
`EvidenceStateSummary`; payload tuples remain arena-scoped. `FrontierStateKey` records the canonical operation,
binding layout, masks, store UUID, generation, lane, and estimator seed. `FrontierLinearTransforms` derives joins,
outer joins, semi/anti joins, projections, and summary states. A state ID is meaningful only inside its arena.

LMDB's Frontier session lives in
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbFrontierPackedCostSession.java`. It opens the synopsis and
query-index snapshots, derives states, publishes physical candidate costs, and applies current LEO feedback.
`LmdbLeoFeedbackStore`, `LmdbOperatorFeedbackStats`, and `LmdbLeoSurfaceStats` persist and query learned residuals.
`LmdbStoreConfig` and `LmdbStoreSchema` already expose Frontier budgets and design/audit lane counts. The store owns a
`PackedPlanCache`, currently bounded by entry count and keyed by a context containing the data revision.

In this document, a costing event means the immutable input context, output estimate, physical vector, access path,
and Frontier state emitted by exactly one provider invocation. A decision certificate is the set of winning and
competing event IDs plus the comparisons or lower bounds which prove the winner. A detached evidence bundle is a
resource-free copy or replay recipe for every Frontier state needed by that certificate. A disposition says whether a
state can participate in an ordinary point-estimate transform, provides only certified bounds, or crosses an opaque
operator.

## Plan of Work

Milestone 1 establishes state-continuity contracts. Add a package-private `FrontierStateDisposition` and carry it in
`PackedCostEstimate` and `PackedPhysicalMetadataArena`. Add a small state-transition validator which rejects a zero
output after nonzero input unless the transition is tagged as initial-unavailable, semantic-scope-isolation, Frontier
disabled, or whole-session abort. Replace `degradeOperator` with explicit helpers for identity, transformed,
bound-only, opaque-boundary, and initial-unresolved states. Each helper derives a `FrontierStateArena` state retaining
its parent and degradation reason. Unexpected exceptions leave the local output untouched, abort the Frontier
session, and cause the planner boundary to retry once with the scalar cost session.

The operator matrix is exhaustive. Query root and ordering preserve the input. Projection, deterministic extension,
multi-projection, and deterministic filter transform retained tuples. Unsupported or nondeterministic expressions
retain available columns and a bound-only state. Union, optional, MINUS, and intersection compose child states or
derive a bound-only state with both parents. Exact distinct and group operations use bounded exact kernels; sampled
versions retain bounds until a design-valid nonlinear estimator exists. Reduced retains tuple support with bag bounds.
Slice retains a capped bound-only state. Zero-length paths use an exact transform. Arbitrary paths retain a replay or
bound-only state. Service and tuple functions create opaque states. Missing leaf annotations create an initial exact,
sampled, or unresolved state and are the only local source allowed to begin without a parent.

Milestone 2 closes planner-side context losses. Introduce `PackedEvidenceContext`, a primitive carrier for rows,
state ID, guarantee/disposition, binding layout, and correlation/scope masks. Change inherited-prefix entry points to
accept it. Seed contextual leaf costing, inherited ordering, dependent EXISTS, and selected dependent plans with the
outer state. Add evidence-state, guarantee/disposition, and cost-event arrays to dense correlated-filter DP lanes and
pass them through factor and scheduled-filter transitions. Preserve multi-column tuple identity. A semantic scope
barrier starts an explicitly tagged isolated context; it does not masquerade as accidental scalar loss.

Milestone 3 makes costing event-sourced. Add `PackedCostingTraceArena` with primitive columns for phase, relation,
physical expression, input context fingerprint, input/output state IDs, guarantee/disposition, row/work values, every
physical dimension, access metadata, and provider planned metrics. Wrap every planner-to-provider call and inject a
recorder into LMDB for internal access candidates and exact refinements. Assign an event ID only after a provider
invocation finishes successfully. Copy it into physical metadata and winners. Mark accepted, rejected, and pruned
events without mutating their measured values.

Move contextual costing before final recipe commitment. The first safe cut keeps
`PackedSelectedPlanContextualizer` as a named candidate-finalization phase but records all its provider calls and
allows it to change the winner only before `PackedPlanRecipe.extract`. Then migrate its contextual join, filter, and
dependent-subquery costing into enumeration. Once migrated, the class becomes a verification/assembly walker with no
cost-session reference. Delete incumbent planned-metric restoration and any metric blending. A missing measurement is
`unmeasured`.

Extend recipe rows with the winning event ordinal/digest, detached state ordinal, guarantee/disposition, and all ten
physical dimensions. Recipe extraction copies the event snapshot verbatim. Materialization copies recipe fields onto
the selected query nodes and never opens or calls a cost session. Candidate telemetry is retained by the decision
certificate rather than reconstructed from the selected child.

Milestone 4 detaches Frontier evidence and extends the cache. Add `FrontierEvidenceBundle` export/import APIs to
`FrontierStateArena`. Export canonical keys, summaries, operation and parent IDs, tuple/mask/weight payloads,
calibration overlays, guarantees, dispositions, and stable 128-bit digests for all certificate states and ancestors.
Deduplicate payload owners. Inline bounded exact and sampled payloads. Store large exact states as resource-free
replay recipes with snapshot identity and digest. Store unsupported/budgeted states as summary/bound-only lineage.
Never retain a cursor, snapshot, query-index lease, or arena token.

Split `PackedPlanCache.Context` matching into strict and structural forms. Strict matching retains every revision and
serves zero-work hits. Structural matching excludes only mutable data, predicate-range, and LEO revisions while
retaining query shape, value/binding variant, dataset, goal, catalog, provider semantic version, and immutable
Frontier configuration. Route stale candidates by the structural hash and verify the detached query snapshot before
use. Add immutable bundles and decision certificates to `PlanEntry`. Keep the 1,024-entry count limit and add weighted
evidence accounting, default 64 MiB, divided across cache segments. An entry above the total budget remains eligible
for strict hits but not stale-generation reuse. Zero evidence bytes disables structural reuse. Existing single-flight
ownership performs validation and atomically replaces the entry.

Milestone 5 implements statistical revalidation. Exact states replay against the current snapshot and compare content
digests. Sampled states reuse deterministic tuple/lane hashes to form paired Horvitz-Thompson objective-cost
differences, including covariance from shared tuples and multi-column domains. Reapply current LEO posteriors and
recompute every affected candidate event and pruning proof. Invalid or missing dependencies and invalidated pruning
bounds force a full replan.

Use one-sided, family-wise, anytime-valid confidence sequences over direct candidate cost differences. Exact
components have zero variance. The validation policy starts at 0.99 confidence, is bounded to [0.51, 0.999], and
requires posterior expected positive regret no greater than 0.01 of the current best expected cost. Independent audit
lanes and shadow replans update a hierarchical Beta-Binomial stability posterior with Jeffreys `Beta(0.5, 0.5)`
priors. Select evidence effort by minimizing validation work plus expected execution regret under that bound. Stable
audits may lower effort; flips and audit misses raise it. If the current Frontier work/memory budgets cannot certify a
decision, replan. Add LMDB config values for initial, minimum, and maximum confidence, expected-regret ratio, and
cache-evidence bytes, with the stated defaults and validation.

Milestone 6 makes LEO state-specific. Define `FrontierLearningKey` from the raw transform fingerprint, binding and
correlation layout, access kernel/index/mode, operator family, and estimator semantic version. Cost events retain that
key and the exact predicted dimensions. Runtime nodes carry the event digest and learning key so completion routes
directly to the prediction. Record actual result rows and each available physical counter independently; absence is
not zero.

Maintain a raw state and a derived `CALIBRATE` state. For non-exact cardinality and each physical dimension, persist
hierarchical Normal-Inverse-Gamma sufficient statistics over `log1p(actual) - log1p(predicted)`, backing off from the
exact transform/access key to its operator family by posterior precision. Exact cardinality is never changed, though
its physical costs may be calibrated. Remove the global physical multiplier and hardcoded minimum-observation and
confidence gates. Increment a distinct LEO revision when a posterior changes enough to alter its serialized
sufficient statistics; cache validation then replays raw evidence, applies the new posterior, and reruns the decision
certificate.

Bump LMDB feedback persistence to format 15 and continue reading 12 through 14. Import older scalar residuals only as
low-precision family priors. Version-13 incomplete physical counters and absent legacy dimensions cannot override
cold or state-specific costs. Write only format 15.

Milestone 7 validates correctness, quality, and cost. Run focused methods, affected classes, then the complete
query-evaluation and LMDB modules. Capture q9 and q4 structure-plus-estimate snapshots and logging-enabled runs. Run
forced q9 variants and require the cost ordering to match measured execution ordering. Run the supported q4, q9,
HIGHLY_CONNECTED q10, and LIBRARY q10 benchmarks, followed by the full Theme comparison. Investigate every movement
above 20 percent. If primitive trace/bundle work causes unexplained planning CPU or allocation growth, capture JFR or
async-profiler evidence before tuning.

## Concrete Steps

Run commands from the repository root. The initial clean install is already complete and recorded in
`maven-build.log`. Before each behavior-changing production slice, add the smallest failing method and run it through
the repository wrapper, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedFrontierSessionContractTest#recipeRetainsDetachedFrontierState \
      --retain-logs

Immediately persist the first failure in `initial-evidence.frontier-continuity.txt` with
`scripts/agent-evidence.py`, retaining the exact log and Surefire report path. Do not run tests with Maven `-am` or
`-q`. After the fix, rerun the identical selector, then its class. Broaden with:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run copyright checks before formatting, then format from the root:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Use the query-plan snapshot skill for SOCIAL_MEDIA q9 and q4 with `structure+estimates`, preserving logs under `/tmp`.
Use the supported benchmark wrapper:

    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:9
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:4
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query HIGHLY_CONNECTED:10
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query LIBRARY:10

Run the Theme history analyzer from
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results` and classify all
changes over 20 percent against the fresh baseline and develop.

## Validation and Acceptance

State-continuity tests must show that every supported nonzero input yields a nonzero child state and that exactness is
never retained after a sampled or unresolved transform. They must cover identity, filter, projection, extension,
multi-projection, union, optional, MINUS, intersection, distinct, reduced, group, slice, zero/arbitrary paths, service,
tuple function, unsupported expression, missing source, budget exhaustion, nested scopes, and unexpected provider
failure. Multi-column tests must prove tuple pairing survives.

Planner tests must prove inherited and dependent subqueries receive the exact outer state, dense correlated-filter
pending/applied lanes carry state, and scope isolation is explicit. SOCIAL_MEDIA q9's fixture must enumerate the legal
orders and select `VALUES -> name -> ab -> da -> cd -> bc` from measured Frontier costs without a query-specific rule.

Telemetry tests use a provider which returns a different value on its second call. The materialized plan must contain
the event from the invocation which won; extraction and materialization must make no second call. Rejected candidate
events must remain visible only as alternatives. Exact cache hits must make zero estimator/store calls. A validated
stale hit must stamp a newly recorded validation event.

Bundle tests must round-trip exact and sampled tuples, masks, weights, operation lineage, and calibration overlays
after the source arena closes. No store lease may remain. Cache tests cover strict and structural hits, byte eviction,
oversized entries, data-only and LEO-only revisions, invalidated pruning proofs, incompatible provider versions, and
concurrent single-flight validation.

Statistical tests use deterministic generated finite populations with an exhaustive cost oracle. Force 0.999, 0.99,
0.95, 0.75, and 0.51 policies and measure independent audit coverage, false reuse, and expected positive regret. A
tier is authoritative only if its observed audit coverage is compatible with the nominal level and no accepted
decision exceeds one-percent expected regret. Ambiguous and over-budget cases must replan.

LEO tests prove an observation updates the originating transform/access key, cardinality and every physical dimension
learn independently, exact rows remain unchanged, downstream Frontier states use the calibrated parent, and a LEO
revision can retain or invalidate a cached winner. Use held-out fixtures to show q-error and plan regret improve rather
than only fitting observed queries. Persistence tests cover versions 12 through 14 and a format-15 round trip.

End-to-end results are unchanged. SOCIAL_MEDIA q9 selects the measured cycle order above. SOCIAL_MEDIA q4 retains
five exact SPOC probes and `streaming-correlated`. HIGHLY_CONNECTED q10 and LIBRARY q10 retain bounded beneficial
materialization. Candidate telemetry is event-sourced, every stale reuse is statistically certified, and no
query-specific heuristic, fixed join preference, new dependency, or breaking query-evaluation API is introduced.

## Idempotence and Recovery

All tests, snapshot commands, and benchmarks are safe to repeat. Keep untracked evidence, logs, snapshots, prepared
stores, and benchmark results. Never reset, clean, or overwrite unrelated working-tree changes. If a behavior-changing
production edit is made before its failing test is observed, revert only that edit with `apply_patch`, return to the
failing-test step, and preserve its Surefire evidence. If offline dependency resolution fails, rerun the exact build
once without `-o`, then return offline. If a Frontier session throws unexpectedly, the planner may retry exactly once
with the existing scalar session; repeated failure propagates and is never swallowed.

## Artifacts and Notes

The initial install is in `maven-build.log`:

    [INFO] BUILD SUCCESS
    [INFO] Total time: 39.987 s (Wall Clock)

The initial focused failure belongs in `initial-evidence.frontier-continuity.txt`. Later evidence files should use
descriptive suffixes so the first failure is never overwritten. Query snapshots and logging runs belong under `/tmp`;
Maven logs belong under `logs/mvnf` or the exact workspace path printed by the runner.

## Interfaces and Dependencies

Add package-private `FrontierStateDisposition`, `PackedEvidenceContext`, `PackedCostingTraceArena`,
`PackedDecisionCertificate`, `FrontierEvidenceBundle`, `FrontierValidationPolicy`, and `FrontierLearningKey`. Extend
`PackedCostEstimate`, `PackedPhysicalMetadataArena`, `PackedPlanRecipe`, `PackedPlanningResult`, and
`PackedPlanCache.PlanEntry` with primitive IDs or immutable detached values. Do not expose arena-local IDs as durable
identities; cached and explain telemetry uses bundle ordinals plus stable digests.

Add backward-compatible LMDB configuration properties for the 64-MiB evidence budget, 0.99 initial confidence, 0.51
minimum, 0.999 maximum, and 0.01 expected-regret ratio. Existing constructors and public query algebra remain
compatible. LMDB feedback format becomes 15 with reads for 12, 13, and 14. No dependency is added. The hot planner
path continues to use primitive arrays, integer IDs, reusable scratch carriers, and bounded materialization on JDK 25.

Revision note (2026-07-31 20:33Z): Created the follow-on ExecPlan, recorded the verified scalar-loss architecture,
fixed implementation order, statistical reuse policy, one-percent regret choice, LEO model, and validation contract.

Revision note (2026-07-31 20:38Z): Recorded the first failing Surefire contract and completed the latent
`BagEstimate` sidecar-preservation slice without changing its scalar equality semantics.

Revision note (2026-08-01 11:45Z): Completed event-sourced physical join costing across all planner search paths,
separated logical cardinality LEO from implementation-dimension LEO, and reserved derived event telemetry so nested
events remain immutable during restoration and materialization.

Revision note (2026-08-01 19:10Z): The q9 logging run localized its planning stall to snapshot-backed bridge
expansion, where `refinementWorkUnits` bounded input probes and retained particles but not rows traversed inside one
LMDB cursor. Added one query-local row budget shared by the bridge survey and replay passes. Cursor rows rejected by
the selector still consume work; mapped query-index rows remain governed by their existing bounded kernel. Exhausting
an ordered snapshot prefix now publishes an `UNRESOLVED` bound-only state with the input lineage and operation recipe,
rather than claiming an unbiased sample or failing the whole session. A sampled-synopsis plus exact-`VALUES` fixture
proves the snapshot path, bounded degradation, nonzero lineage, and unchanged query results; the existing zero-probe
and mapped-resampling contracts remain green.

Revision note (2026-08-01 21:05Z): A bounded q9-shaped planner fixture reproduced budget starvation at 4,000 work
units: nested strict filter regions consumed the budget before the containing region could compare the complete
factor-and-predicate schedule. The observed failure was `VALUES -> ab -> bc -> cd -> da -> name`; exact search costs
41,675 candidate transitions. Relocatable regions are now scheduled in their structural containment order, with
supersets before strict subsets. This ordering is a topological dependency rule only: it reads no predicate identity,
row estimate, selectivity, access path, or workload name, and it does not alter the unbounded candidate set. The same
4,000-work fixture now selects `VALUES -> name -> ab -> da -> bc -> cd`.

Revision note (2026-08-01 21:12Z): The supported Docker JFR planning loop separated one cold SOCIAL_MEDIA q9 plan
from exact cache reuse. The first operation measured 6,764.598 ms/op; subsequent operations measured 1.137–1.268
ms/op. The recording is `profiles/lmdb-opt/social-q9-planning-baseline.jfr`; the cold-planning chunk is
`/tmp/social-q9-jfr-chunks/social-q9-planning-baseline_58.jfr`. Its dominant planner-side CPU includes LMDB page
decoding, native cursor access, range counting, Frontier heavy-candidate tracking, and primitive maps. Allocation is
dominated by `HeapByteBuffer.slice`, `HeapByteBuffer.<init>`, and `LmdbPage.node`. Because the recording contains one
cold operation plus cache hits and setup, the next profile must repeatedly force structurally equivalent uncached
plans before changing page or buffer code. Optimization work is restricted to canonical subproblem/event reuse and
profile-proven implementation costs; fixed row thresholds, predicate-name rules, workload-specific join orders, and
selectivity heuristics are prohibited.

Revision note (2026-08-01 21:18Z): With containment scheduling, the real q9 winner improved to
`VALUES -> name -> ab -> da -> cd -> bc` and planned work fell from about 104.7K to 50.7K, leaving only the final two
cycle edges reversed. At the decisive prefix, the `cd` POSC reverse expansion exhausts the exact snapshot row budget
and becomes an `UNRESOLVED` bound-only Frontier state; the competing `bc` transition is not retained in the root
decision certificate because dense-DP losers are currently discarded. The next estimator change must preserve the
query-index sampling design (including tuple covariance and inclusion weights) or retain an honest bound; it may not
replace the missing distribution with a scalar fanout or a preference rule. Dense-DP finalists will also retain their
originating immutable cost events so this comparison can be explained without recosting.

Revision note (2026-08-01 21:26Z): Exact factor-domain refinement now retains the decisive q9 alternatives as
originating events: at the shared 598-row prefix, `bc` produces 8,957 rows while `cd` produces 5,696 rows. A
pipeline-faithful locked-order experiment excluded planning time, rotated 21 measurements per variant, and verified
identical results against the untouched nullable query. The unrestricted `cd -> bc` winner measured 5.357 ms median,
locked `cd -> bc` 5.375 ms, and the originally requested `bc -> cd` 8.034 ms. The acceptance gate is corrected to the
faster `cd -> bc` order; no preference heuristic is introduced. The same experiment exposed and now covers an
unflattenable memo-local unary prefix that previously aborted Cascades and silently selected the normalized fallback.

Revision note (2026-08-01 21:41Z): Added `planUncachedQuery`, whose lexical alpha-renaming preserves comments, IRIs,
quoted strings, and variable syntax while generating a fresh structural cache key for every otherwise identical q9
plan. The baseline was 5,709.149 ms/op. Its JFR showed exact LMDB finite-surface range walks allocating a sliced key
buffer and node object for each comparison. `LmdbBtreeRangeCounter` now reuses one node carrier and page buffer,
`LmdbPage` exposes an allocation-free decoder behind its compatible allocating method, and `LmdbDataFile` supports a
caller-owned page buffer. The same Docker workload is 3,654.378 ms/op (36.0 percent faster), with sampled allocations
down 61.8 percent and GCs down 71.8 percent. This is a profile-proven constant-factor optimization only: the planner
visits and costs the same exact candidate space.

Revision note (2026-08-01 23:05Z): Dense DP and sparse DPhyp tables now retain their winning factor, transition,
physical implementation, metadata, state, and costing-event IDs; selected-plan emission links those records and makes
no provider call. Exact correlated factor lattices likewise replay complete retained sub-lattice states when their
scope, factor/filter multiset, base winners, and Frontier state all match. Below that memo layer, the shared
finite-surface scan budget now memoizes `planningCardinality` by its complete four-ID lookup key. The focused red gate
observed two identical physical measurements and the green gate observes one. Docker q9 fell from 3,654.378 to
798.521 ms/op, and the final exact single-mapping address path measured 772.904 ms/op. This is 78.9 percent below the
page-reuse baseline and 86.5 percent below the original faithful uncached baseline. A manual row-offset factoring
experiment was slower in both JFR shape and point estimate and was removed. None of these changes inspect query text,
predicate identity, estimates, selectivity, or candidate rank; candidate enumeration and objective comparison remain
unchanged.

Revision note (2026-08-02 02:23Z): Removed the legacy whole-operator LEO cardinality path so event-keyed Frontier
calibration is the sole non-scalar correction path. A positive learned posterior without supporting particles now
retains `LEARNED_CALIBRATED` guarantee and `BOUND_ONLY` or `OPAQUE_BOUNDARY` disposition. Every linear transform and
LMDB provider transition checks both axes before payload access; exact cardinalities remain untouched and physical
dimensions remain independently learned. No query fingerprint, row cutoff, fixed order, or preference rule was added.

Revision note (2026-08-02 06:35Z): Exact completed-lattice coverage now prevents the fallback filter rule from
re-enumerating an already complete correlated search, and collision-safe decision-trace interning collapses only
fully identical immutable alternatives. The supported Docker q9 loop then localized constant-factor costs: whole-map
metric snapshots, checked foreign-memory reads, and four canonical sorts per completed Frontier payload. Direct point
lookups, absolute views over the same arena-owned mapping, and one idempotent canonicalization barrier reduce uncached
q9 planning from 900.663 to 627.240 ms/op. The profile records
`profiles/lmdb-opt/social-q9-planning-uncached-canonical-once.jfr`; bitwise diagnostic-order, segmented-index, memo,
and planner regressions remain the semantic gates. None of the reuse keys or constant-factor paths inspect estimates,
selectivity, predicates, query text, workload identity, or candidate rank.

Revision note (2026-08-02 08:52Z): The post-dedup Docker recording contains 44,999 execution samples. Its remaining
general duplicate-work signal is LEO structural-key construction: join-permutation canonicalization is on 23,212
sample stacks and `learningRawTransform` on 3,827. The session already caches the complete `operation@topology`
string, but alternating Frontier operations for one packed relation evict each other and rematerialize and
canonicalize the identical logical topology. Split that cache into an immutable per-relation topology digest and a
last complete transform. This preserves the exact `LeoOperatorKey` fingerprint byte-for-byte and changes neither the
DP/DPhyp candidate set nor any cost; it only prevents repeated materialization/canonicalization of the same packed
relation. The pre-existing original-plan preference remains outside this change and is consulted only after every
objective dimension is bitwise tied. No new estimate threshold, preference, or workload key is permitted.

Revision note (2026-08-02 09:01Z): The per-relation topology-cache experiment measured
668.309 +/- 109.177 ms/op and retained 23,184 join-permutation samples, statistically indistinguishable from and
slightly above the 627.240 +/- 88.360 baseline with 23,212 samples. It demonstrated that the repeated work occurs
once in each deliberately uncached plan rather than through operation-cache eviction, so the field and code were
removed. The next optimization targets the canonicalizer algorithm itself and must reproduce the current structural
fingerprint exactly for every factor permutation; it may prune only lexicographically dominated prefixes, never
plans or cost candidates.

Revision note (2026-08-02 09:05Z): The retained LEO canonicalizer uses a reversible variable-ordinal map and a
prefix string while traversing the same complete permutation tree. Once a complete canonical string exists, a branch
is skipped only when its already-emitted prefix is lexicographically greater and therefore cannot possibly become the
minimum under any suffix. An independent test enumerates all 720 orders of a six-factor cycle and proves the exact
fingerprint for three input tree orders. Docker q9 measures 597.525 +/- 88.508 ms/op versus the 627.240 +/- 88.360
pre-change run; canonicalizer stack presence falls from 23,212 to 1,469 samples (93.7 percent). The recording is
`profiles/lmdb-opt/social-q9-planning-uncached-canonical-prefix.jfr`. This prunes only impossible representations of
one immutable LEO key: it neither skips nor reprices a DP/DPhyp plan candidate and uses no estimate, predicate,
workload identity, or cost threshold.

Revision note (2026-08-02 12:45Z): A cost-priority work-queue experiment made the synthetic descending-prefix fixture
green but did not repair bounded AAS q2 and changed its fallback plan. The experiment identified budget starvation but
is not the architectural fix: a costly prefix can unlock a cheap bound access, so partial accumulated cost is not an
admissible ordering proof. Repository history confirmed that the binary packed rewrite had removed the prior formal
DPhyp layer. The active milestone therefore restores the oracle-tested hypergraph/CSG-CMP abstraction and routes
Frontier/event-aware costing through its receiver; no cost or cardinality value participates in topology generation.
