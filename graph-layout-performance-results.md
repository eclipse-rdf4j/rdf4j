# Graph layout performance results

Stopped at the user's request: “This is fast enough, stop iterating.” Eight measured improvements are retained. Iterations nine and ten were not completed or claimed. Unaccepted production experiments remain separate; passing characterization tests are retained.

For the supplied full graph, installed Chrome's query-to-settled-visible median improved **34.286 → 3.101 seconds**, about **11.1× / 91% faster**. Current Playwright WebKit is **3.460 seconds**; it is not native Safari. Main source contains accepted iteration 8, verified with isolated frontend builds; the user's live server on 8080 was not changed.

## Measurement and eight retained steps

Input: `lmdbnjnkj-graph.ttl`, SHA256 `0806a6a8e63059003a428b5b6a9d17fa411ce27331bfd4933d6b0de09b708970`. All 981 triples were parsed. The default values-off view draws 452 nodes and 614 edges, with 186 routes. Fixed network layout, seed 7, 800×600 viewport, light theme, two warmups and seven measurements; each repeat starts from the same unsolved input. CPU profiles are separate from timing samples.

Chrome baseline worker/visible medians: **33654.7 / 34285.9ms**. Historical WebKit baseline:38408 / 39171ms.

| Step | Retained change | Chrome worker / visible median, ms | WebKit worker / visible median, ms |
| --- | --- | --- | --- |
| 1 | Lazy ordered obstacle index | 31637.2 / 32276.5 | 36856 / 37608 |
| 2 | Segment any-match index; empty fast paths | 8121.8 / 8742.6 | 16326 / 16963 |
| 3 | Reuse arrow index; exact collision witnesses | 5622.1 / 6249.3 | 8424 / 9056 |
| 4 | Compute crossing orientations only when needed | 5463.8 / 6083.6 | 6108 / 6735 |
| 5 | Revalidate crossing-segment witness | 2937.2 / 3559.1 | 3072 / 3704 |
| 6 | Revalidate unit-clipping witness | 2870.9 / 3502.4 | 2992 / 3634 |
| 7 | Precomputed BoundsTree escape successors | 2509.6 / 3133.5 | 2886 / 3527 |
| 8 | Certify arrow-obstacle witness before obstacle0 | 2477.2 / 3100.5 | 2823 / 3460 |

WebKit provenance matters: earlier rows are historical Playwright WebKit27.2 reports. The old browser binary is no longer available to verify equality with the current official revision2370 executable (`18775828b550e916840169243afdacec3b3411a5ccd5ebf2da7eb401b68f7900`). An original→final same-binary replay was **not completed**, so39171→3460ms is not presented as a verified same-binary speedup. Step8 is paired against a proven same-binary/elevated-context step7 replay:2869/3505ms. Both worker and visible ranges are disjoint in that pair.

Final seven-sample median / p95 / range:

| Engine | Worker, ms | Query-to-visible, ms |
| --- | --- | --- |
| Installed Chrome155.0.8059.80 | 2477.2 / 2489.1 /2465.9–2489.1 | 3100.5 / 3115.5 /3090–3115.5 |
| Playwright WebKit27.2, revision2370 | 2823 / 2833 /2808–2833 | 3460 / 3469 /3445–3469 |
| Native Safari26.6.2 | Not measured | Not measured |

## Quality and verification

Accepted outputs preserve exact per-browser node positions, all 186 routes and complete layout output. No nodes, edges, iterations, spacing or quality settings were removed. Recorded primary metrics remain unchanged: 0 node overlaps, 1443 crossings, 226 fan overlaps and 68 routed edge–node intersections; layout area is approximately210294449.6. These are preserved baseline metrics, not claims of ideal layout quality.

Complete output hashes: Chrome `1ee85f4158346789a6840fd307c44bfffca88f6130c62035f5d6294f9db9cd0d`; WebKit `0f4f3e0b3bce741446901541fe43cea75e095c6a18c0d917040840b040640206`. Eleven raw phases, including cold/primer/warmup/sample phases, were audited per engine. Final rendered visible-edge counts were not measured by this harness;614 is the layout output edge count.

Accepted C8 matched44/44 focused tests and isolated typecheck/build. Subsequent passing characterization retained on unchanged production includes the independent original-clipping oracle3/3 and the exact ten-file selection81/81 (a historical log filename says67; actual TAP says81). The unaccepted primitive rewrite passed only in a separate checkpoint and is not applied. Common-control regressions rejected both cut-cache variants before browser runs. The step6 moving-ray synthetic control's +7.95% / about2ms incremental tradeoff remains explicit, while still34.76% better than original at that step.

Native Safari remote automation was enabled, but the idle session did not pass visibility/timer/rAF liveness; no native layout measurement exists. The requested Safari window was preserved. Browser-startup failures and an approval-service capacity failure are retained as infrastructure evidence, never layout timings. No final screenshot/interaction sweep or Maven packaging was run after the stop instruction.

## Source state and evidence

At the stop audit, branch `GH-0000-workbench-graph-explorer` was at HEAD `7dfe44ea8f453bcd6859b74d3afc3b79d2f2d966`, with no staging, commit or push. A subsequent explicit user request authorizes publishing the retained task scope; this report records the pre-publication audit and does not embed the resulting commit SHA. Final production SHA256 values:

- `graph-layout-fans.ts`: `4351ba55938916ce0c2cecc94f0f1942c38683f3c1bcf44bdcd0db94304c753d`
- `spatial-grid.ts`: `87047c807da9540511d17b47ffbbae33e0ab87b6df31e2f901084144d3083c2e`
- `graph-layout-stress.ts`: `3966ac1e630e03e8c4f4dbc1407c3a2140ea554e85347067e0056753d8c12b34` (unchanged production)

Original `.claude/launch.json`, `organic-layout-execplan.md` and root `package-lock.json` retain their original hashes. Existing `initial-evidence.txt` was appended, not replaced. Known owned commands completed and no exec session remains; system-wide `ps` inspection was denied, so no broader process inventory is claimed.

Existing retained evidence:

- [Full plan and iteration/rejection ledger](graph-layout-performance-execplan.md)
- [Chrome final raw report](logs/graph-layout-performance-20261019/cycle-8/candidate8-attempt5-chrome800-current155-elevated/benchmark-results.json)
- [WebKit final raw report](logs/graph-layout-performance-20261019/cycle-8/candidate8-attempt5-webkit800-current2370-guarded/benchmark-results.json)
- [Corrected paired output/quality audit](logs/graph-layout-performance-20261019/cycle-8/candidate8-attempt5/attempt5-comparison-audit.json)
- [C8 typecheck](logs/graph-layout-performance-20261019/cycle-8/candidate8-attempt5/typecheck.log), [C8 build](logs/graph-layout-performance-20261019/cycle-8/candidate8-attempt5/build.log)
- [Primitive pre-green](logs/graph-layout-performance-20261019/cycle-9/segment-clipping-oracle/focused-3-pre-green.log), [81-test pre-green](logs/graph-layout-performance-20261019/cycle-9/segment-clipping-oracle/expanded-67-pre-green.log)

The replayable browser runner is `e2e/benchmarks/graph-layout-performance.mjs`. Immutable sources, inputs, checkpoints, raw profiles and unaccepted experiments remain under `/private/tmp/rdf4j-layout-perf-20261018-KtTpza` and the durable `logs/graph-layout-performance-20261019/` tree.
