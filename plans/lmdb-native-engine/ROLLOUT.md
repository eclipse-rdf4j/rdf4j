# LMDB native engine rollout ledger

Behavior-risk changes default on behind `rdf4j.lmdb.native.<feature>=false` kill switches and live for at
most two releases unless a measured reason justifies retention. Bit-identical changes do not require a
switch. Defaults and removal decisions are filled as implementations land.

| Feature | Property | Default | Measurement | Removal decision |
| --- | --- | --- | --- | --- |
| Proposal dispatch | Pending | Pending | Pending | Pending |
| Adaptive filter decorator | `rdf4j.lmdb.adaptiveFilterPlacement.enabled` | On | Sequential/worker movement parity, factorized suffix parity, and four-worker lease/close stress green; performance unmeasured | Retain default-on kill switch; review after paired benchmark or two releases |
| Elastic parallel admission | Pending | Pending | Pending | Pending |
| Parallel DISTINCT extrema | `rdf4j.lmdb.parallel.enabled` | On | Dispatch/parity and representative-fallback tests green; performance unmeasured | Retain shared parallel kill switch; review after two releases |
| Parallel DISTINCT SUM/AVG | `rdf4j.lmdb.parallel.enabled` | On | Cross-worker duplicate parity and floating-fallback tests green; performance unmeasured | Retain shared parallel kill switch; review after two releases |
| Vectorized external worker root | `rdf4j.lmdb.chunkPipeline.externalRoot.experimental` | On | Default/kill-switch row and aggregate parity, refill, restriction, adaptive, and lifecycle tests green; 1.3x speed and worker CPU profiles unmeasured | Retain literal-`false` kill switch; review after paired benchmark or two releases |
| Shared batch columns | Pending | Pending | Pending | Pending |
| Wide primitive group keys | None (semantics-preserving storage) | On | Eight-key direct/query parity and primitive dispatch green; allocation/throughput unmeasured | Unguarded; keep the generic execution fallback outside native compilation |
| ORDER BY shared producer | `rdf4j.lmdb.parallel.enabled` plus the selected producer's existing switch | On | Full-sort and top-K generic parity, shared-ladder selection, early close, and composed producer/consumer telemetry green; core scaling unmeasured | Retain the shared producer switches; review parallel tie-order behavior after paired scaling evidence or two releases |
| Dense key/payload sort rows | None (semantics-preserving internal layout) | On | 12→4 live-row packing, remapped projection/DISTINCT, key-only spill merge, and forced-spill parity green; paired wide-row performance unmeasured | Unguarded; retain generic ORDER BY fallback outside native compilation |
| Parallel sort and merge | Pending | Pending | Pending | Pending |
| Radix native sort | Pending | Pending | Pending | Pending |
| Order-aware sort elimination | Pending | Pending | Pending | Pending |
| Range pushdown | `rdf4j.lmdb.native.rangePushdown` | On | Boundary, mixed-type refusal, disabled-switch, and generic-parity tests green; performance unmeasured | Retain default-on kill switch; review after two releases |
| Encoding v2 | Store-creation setting | New stores only | Pending | Permanent version gate |
| Wide varint decode | None (bit-identical) | On | Width/alignment parity green; paired JMH 65.441% lower time | Unguarded per plans 08 §3 and 13 §4 |
| Leapfrog intersection | Pending | Pending | Pending | Pending |
| Outer accumulation | Pending | Pending | Pending | Pending |
| Endpoint-restricted property paths | `rdf4j.lmdb.nativePath.targets.enabled` (`rdf4j.lmdb.nativePath.targets.maxValues` cap) | On, 65,536-target cap | VALUES/pattern, UNDEF refusal, nested-join, backward, per-source reset, disabled-switch, lazy-close, and counter tests green; large-store latency unmeasured | Retain default-on kill switch and bounded cap; review after two releases |
| Frontier property paths | `rdf4j.lmdb.nativePath.enabled`; parallel work also uses `rdf4j.lmdb.parallel.enabled` (`rdf4j.lmdb.nativePath.frontier.parallelMin` threshold) | On; parallel threshold 1,024 frontier IDs | Cyclic/diamond/self-loop plus compatible alternation/inverse/star/same-variable parity, unsigned order, seek sweep, direct CSR, deterministic parallel SET, and worker close tests green; fan-out/memo cost contracts green; ≥3× serial and near-linear parallel performance unmeasured | Retain whole-path and shared-parallel kill switches; review after paired benchmark or two releases |
| Query memory authority | `rdf4j.lmdb.queryMemory.maxBytes`; `rdf4j.lmdb.queryMemory.perQueryMaxBytes` | `max(1 GiB, maxHeap/2)` global; global/4 per query | Six-test ledger concurrency/reclaim/leak contract green; operator and CSR wiring pending | Bounds are safety controls, not kill switches; retain and validate defaults with the adversarial concurrency corpus |
| Oversized dictionary-cache guard | None (memory-safety admission) | On | Exact 100 MiB bypass and ordinary cache-hit class green | Unguarded; durable values and query results are unchanged |
| Persistent value hash cache | Existing config | Off, to re-evaluate | Pending | Pending |
| Specialization generator | Existing properties | On, to re-evaluate | Pending | Pending |
| CSR cache tunables | Existing `rdf4j.lmdb.csrCache.*` properties, including `rootScans` | On; 1,024 probes; root scans on; max(256 MiB, heap/4) global; max(64 MiB, global/16) entry | Admission/refusal metrics and concurrent commit/close accounting green; cached skew fixture yields four 16-row in-memory slices and raw/composite range fallback neighbors are green; large-heap effectiveness unmeasured | Retain inherited kill switch, root-scan switch, and bounds; revalidate on benchmark corpus before two-release review |

Revision note (2026-07-19, Codex): Created the Phase I rollout ledger from the inherited and new feature
flags enumerated in `13-verification.md`.

Revision note (2026-07-19, Codex): Recorded plan 10 §4.2 parallel DISTINCT SUM/AVG under the shared
parallel kill switch after focused duplicate and floating-fallback verification.

Revision note (2026-07-20, Codex): Recorded the bit-identical plan 08 §3 varint decoder as unguarded
after focused parity and paired JMH exceeded the acceptance target.

Revision note (2026-07-20, Codex): Recorded plan 08 §1 range lowering behind the default-on
`rdf4j.lmdb.native.rangePushdown` kill switch after disabled-switch parity passed; range performance
remains explicitly unmeasured.

Revision note (2026-07-20, Codex): Recorded plan 11 §2 endpoint restriction behind the default-on
`rdf4j.lmdb.nativePath.targets.enabled` kill switch with a 65,536-target default bound. Focused parity,
refusal, nested-lifecycle, and expansion/start-scan counter tests are green; large-store timing remains
explicitly unmeasured.

Revision note (2026-07-20, Codex): Recorded plan 04 §3's existing external-root worker property as a
default-on, literal-`false` kill switch after row/aggregate parity and lifecycle verification. The required
speedup and worker CPU profiles remain unmeasured, so the switch is retained pending paired evidence.

Revision note (2026-07-20, Codex): Revalidated plan 07 §4's inherited CSR controls with a proportional
entry cap, observable admission/refusal policy, and exact concurrent commit/close accounting. The defaults
remain on pending the benchmark-corpus effectiveness measurement required for their two-release review.

Revision note (2026-07-20, Codex): Recorded plan 07 §5 under the existing default-on CSR `rootScans`
switch. Prefix-balanced in-memory slices and raw/composite fallback coverage are green; retain the inherited
switch because paired throughput, allocation, and profile evidence remains unmeasured.

Revision note (2026-07-20, Codex): Recorded plan 02 §4's adaptive decorator behind its existing default-on
property after sequential/worker movement parity, factorized-suffix parity, and deterministic worker-lease
stress passed. Retain the kill switch pending paired performance evidence or the two-release review.

Revision note (2026-07-20, Codex): Recorded plan 11 §3's sorted frontier engine under the existing
whole-path kill switch and its wide-level workers under the shared parallel switch, with a 1,024-ID default
parallel threshold. Correctness and lifecycle selections are green; both performance gates are unmeasured.

Revision note (2026-07-20, Codex): Recorded plan 11 §4's compatible alternation/inverse/cycle extension
under the existing whole-path switch. Plan 11 §5's fan-out and memo-aware costing is an I/O-free advisory
estimate with the generic missing-statistics fallback, so it remains unguarded. Performance is unmeasured.

Revision note (2026-07-20, Codex): Recorded plan 10 §2's wide primitive group-key storage as unguarded
because it preserves native long-ID equality and result semantics. The generic evaluator remains the outer
fallback; allocation and throughput acceptance are still open.

Revision note (2026-07-20, Codex): Added plan 12 §1's two memory-ceiling properties after the standalone
authority's concurrent accounting and cleanup contract passed. They are safety bounds rather than feature
gates; defaults remain provisional until all structures are wired and the adversarial corpus is measured.

Revision note (2026-07-20, Codex): Recorded the plan 12 §2 oversized-entry guard as an unguarded
memory-safety admission check after a 100 MiB literal bypassed the cache while ordinary cache behavior
remained green.

Revision note (2026-07-20, Codex): Recorded plan 05 §1 under the existing switches of the producer selected
below ORDER BY, including the shared parallel kill switch for its only tie-order semantic risk. Plan 05
§§2–3's dense key-first row and spill format are internal, semantics-preserving representations and remain
unguarded; generic ORDER BY stays the compilation fallback. Performance decisions remain open.
