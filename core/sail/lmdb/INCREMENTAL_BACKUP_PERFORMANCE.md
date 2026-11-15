# Incremental backup write performance

The final measurements do not establish less than 5% overhead in every observed pair: 9 pair/metric comparisons were at or above 5%. The individual results and observed spread are retained below.

This is evidence for the measured host, configurations, and workloads. Two reversed-order pairs per workload expose order sensitivity but do not establish a precise confidence bound or a universal overhead guarantee. Negative measured overhead reflects the observed runs and their variation; it does not establish cost-free backup.

Fork: **HASMAC-AS/rdf4j**. Branch: **GH-0000-incremental-backup-throughput**. Base: `8fc7b0137164ffdc1f5d080b943fd362931137dd`. Frozen candidate JAR SHA-256: `e4d735e7ad7eb0dbba1d1665660923287a34adf96871eb1f627d3b9c11d74c0e`. The candidate is identified by its frozen source/compiled artifact, independently of the later report commit.

## Measurement contract

The primary comparison measures the marginal cost of journaling over equally durable writes: both conditions use forceSync=true. A forceSync=false disabled baseline compared with backup-enabled writes that force durability is not measured here.

The threshold is strictly **less than 5% added time per transaction**, which is stricter than a 5% TPS-loss threshold (5% added time corresponds to approximately 4.7619% TPS loss). For each pair, `ratio = (enabled seconds / enabled transactions) / (disabled seconds / disabled transactions)`; added time is `100 × (ratio − 1)` and TPS loss is `100 × (1 − 1/ratio)`. Aggregate overhead uses the geometric mean of paired ratios, not rounded TPS.

- **Writer:** the raw writer interval.
- **Publication:** writer interval plus the final durable private-journal publication tail.
- **Drained:** writer interval plus the recorded drain tail, which includes publication and store close. The publication tail is not added a second time.

Zero final backlog means publication is complete. Close can persist deferred cleanup; source-outbox reclamation can wait for later writes. These metrics do not certify that all maintenance queues are empty. Steady-state writes include cleanup they trigger. Explicit incremental-export gzip creation, retention requests, and backup-request I/O are outside the timed workload.

Each fork checks live cardinality, enabled native evaluation, full adjacency readiness, and, when backup is enabled, transaction/acknowledgement identity and the post-close acknowledgement checkpoint. Those checks are not a full data-equality comparison or a restore/replay test; correctness tests cover those paths.

## Host and configuration

| Setting | Recorded value |
| --- | --- |
| CPU | AMD EPYC 7763 64-Core Processor |
| OS | Linux 6.18.57 (x86_64) |
| Affinity / JVM processors | CPUs 0,1,2,3; ActiveProcessorCount=4 |
| Memory / JVM heap | 9.734 GiB visible; -Xms512m -Xmx1g |
| JDK | 25.0.5+8-LTS |
| Filesystem | Workspace on an overlay mount; underlying device and shared-host contention are not characterized |
| Isolation / indexes | SNAPSHOT_READ; spoc,posc,ospc |
| Query / adjacency | Native engine enabled; direct adjacency PREFER with FULL coverage, readiness checked |
| Commit durability / cleanup | forceSync=true for both conditions; asynchronous cleanup enabled |
| Workload | 64 live slots; alternating inserts/removals; 1 or 100 statement operations per transaction |
| Instrumentation | No JFR, strace, native-commit timing, thread-I/O instrumentation, or extra JVM arguments |
| Environment counters | Per-fork snapshots retained; unavailable cgroup counters remain unavailable, not zero |

Primary: three warmup rounds, each requiring both 30 seconds and 7,500 transactions, then at least 60 measured seconds. Fresh-values: two warmup rounds, each requiring both 30 seconds and 5,000 transactions, then at least 60 measured seconds. Each fork uses separate warmup stores and a fresh measurement store in the same JVM. Pair 1 is off/on; pair 2 is on/off. The primary uses precomputed values. Fresh-values creates a new lexical literal on every insertion, includes that model-construction cost, and retains exact slot statements until their removal. Fresh-values is reported separately.

## Final primary campaign

| Operations/transaction | Pair/order | Writer added time | Publication added time | Drained added time | Writer TPS loss | Publication TPS loss | Drained TPS loss |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 (off/on) | -1.1867% | -1.0526% | -0.8563% | -1.2010% | -1.0638% | -0.8637% |
| 1 | 2 (on/off) | +3.5687% | +3.5841% | +1.8055% | +3.4457% | +3.4601% | +1.7735% |
| 100 | 1 (off/on) | +13.8271% | +14.0363% | +13.9832% | +12.1475% | +12.3086% | +12.2678% |
| 100 | 2 (on/off) | -7.2887% | -7.2073% | -7.1187% | -7.8618% | -7.7671% | -7.6643% |

| Operations/transaction | Metric | Geometric mean added time | Geometric mean TPS loss | Pair minimum | Pair maximum | Sample SD (percentage points) | Every pair <5% |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | writer | +1.1630% | +1.1497% | -1.1867% | +3.5687% | 3.3626 | yes |
| 1 | publication | +1.2392% | +1.2240% | -1.0526% | +3.5841% | 3.2786 | yes |
| 1 | drained | +0.4658% | +0.4637% | -0.8563% | +1.8055% | 1.8822 | yes |
| 100 | writer | +2.7281% | +2.6556% | -7.2887% | +13.8271% | 14.9311 | NO |
| 100 | publication | +2.8675% | +2.7876% | -7.2073% | +14.0363% | 15.0215 | NO |
| 100 | drained | +2.8927% | +2.8114% | -7.1187% | +13.9832% | 14.9213 | NO |

### Every primary fork

| Fork | Transactions | Writer seconds | Publication tail seconds | Drain tail seconds | Writer TPS | Publication TPS | Drained TPS | Measured aggregate JIT ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| n1 off p1 | 41755 | 60.000248344 | 0.000000000 | 3.146069670 | 695.913786 | 695.913786 | 661.242038 | 1211 |
| n1 on p1 | 42257 | 60.001007138 | 0.081436697 | 3.357270963 | 704.271512 | 703.316931 | 666.953100 | 1003 |
| n1 on p2 | 40410 | 60.000267812 | 0.008933286 | 2.205931679 | 673.496994 | 673.396734 | 649.613709 | 1012 |
| n1 off p2 | 41852 | 60.000130616 | 0.000000000 | 3.283241918 | 697.531815 | 697.531815 | 661.342756 | 966 |
| n100 off p1 | 15192 | 60.021976729 | 0.000000000 | 0.160493310 | 253.107292 | 253.107292 | 252.432311 | 1393 |
| n100 on p1 | 13401 | 60.266806544 | 0.110752492 | 0.244019674 | 222.361210 | 221.953325 | 221.464502 | 1736 |
| n100 on p2 | 15367 | 60.000254501 | 0.052688936 | 0.228306341 | 256.115580 | 255.890871 | 255.144732 | 1683 |
| n100 off p2 | 14247 | 60.000511553 | 0.000000000 | 0.118031254 | 237.447976 | 237.447976 | 236.981792 | 1305 |

## Final fresh-values campaign

| Operations/transaction | Pair/order | Writer added time | Publication added time | Drained added time | Writer TPS loss | Publication TPS loss | Drained TPS loss |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 100 | 1 (off/on) | +5.7119% | +5.9098% | +5.7291% | +5.4033% | +5.5800% | +5.4187% |
| 100 | 2 (on/off) | +10.3525% | +10.6074% | +10.9561% | +9.3813% | +9.5901% | +9.8743% |

| Operations/transaction | Metric | Geometric mean added time | Geometric mean TPS loss | Pair minimum | Pair maximum | Sample SD (percentage points) | Every pair <5% |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 100 | writer | +8.0073% | +7.4137% | +5.7119% | +10.3525% | 3.2814 | NO |
| 100 | publication | +8.2331% | +7.6068% | +5.9098% | +10.6074% | 3.3217 | NO |
| 100 | drained | +8.3111% | +7.6733% | +5.7291% | +10.9561% | 3.6960 | NO |

### Every fresh-values fork

| Fork | Transactions | Writer seconds | Publication tail seconds | Drain tail seconds | Writer TPS | Publication TPS | Drained TPS | Measured aggregate JIT ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| n100 off p1 | 9437 | 60.001417161 | 0.000000000 | 0.298065951 | 157.279618 | 157.279618 | 156.502171 | 4773 |
| n100 on p1 | 8930 | 60.020964962 | 0.112339961 | 0.307965422 | 148.781347 | 148.503396 | 148.021852 | 9220 |
| n100 on p2 | 9026 | 60.070998784 | 0.138725186 | 0.498193068 | 150.255534 | 149.909340 | 149.019654 | 6113 |
| n100 off p2 | 9985 | 60.219254465 | 0.000000000 | 0.169122999 | 165.810754 | 165.810754 | 165.346386 | 5185 |

## JIT and validation

The table records aggregate compilation time, not writer-thread stall time. Compilation can continue during measurement despite the warmup requirements. Exact phase boundaries, warmup transaction counts, CPU measurements, validation flags, and publication backlog samples are included in the companion evidence.

| Campaign | Fork | Warmup 1 JIT ms | Warmup 2 JIT ms | Warmup 3 JIT ms | Measurement JIT ms |
| --- | --- | --- | --- | --- | --- |
| candidate-capture-long-acceptance | n1 off p1 | 19216 | 4174 | 1395 | 1211 |
| candidate-capture-long-acceptance | n1 on p1 | 20444 | 4899 | 1792 | 1003 |
| candidate-capture-long-acceptance | n1 on p2 | 18631 | 5593 | 1513 | 1012 |
| candidate-capture-long-acceptance | n1 off p2 | 19670 | 3854 | 1424 | 966 |
| candidate-capture-long-acceptance | n100 off p1 | 21008 | 8069 | 316 | 1393 |
| candidate-capture-long-acceptance | n100 on p1 | 22737 | 7380 | 405 | 1736 |
| candidate-capture-long-acceptance | n100 on p2 | 21668 | 8625 | 679 | 1683 |
| candidate-capture-long-acceptance | n100 off p2 | 20746 | 7094 | 190 | 1305 |
| candidate-capture-long-fresh-values | n100 off p1 | 23362 | 7632 | n/a | 4773 |
| candidate-capture-long-fresh-values | n100 on p1 | 22329 | 10495 | n/a | 9220 |
| candidate-capture-long-fresh-values | n100 on p2 | 24074 | 9093 | n/a | 6113 |
| candidate-capture-long-fresh-values | n100 off p2 | 24399 | 8287 | n/a | 5185 |

All 12 planned final forks completed, exited successfully, kept their runtime fingerprints unchanged, and passed their required validation checks. Completion/validation and the performance threshold are distinct results.

## Complementary fresh-value diagnostics

These diagnostics were planned after the final independent-fork campaigns exposed an unresolved fresh-value cost and substantial variation. They are retained separately. They do not change the twelve-fork acceptance membership, its failing comparisons, or the earlier history, and are not pooled with those measurements.

### Stronger-warmup fresh-value profile

The unchanged final candidate ran one fixed off/on pair with three warmups, each requiring at least 30 seconds and 7,500 transactions, then 30 measured seconds. Java execution sampling was 2 ms, native sampling 20 ms, stack depth 128, with native-commit timers and native writer-thread I/O. Both recordings had zero JFR data-loss events. Instrumentation and this different warmup protocol prevent treating it as an acceptance replacement.

| Fork | Transactions | Writer seconds | Publication tail seconds | Drain tail seconds | Writer TPS | Publication TPS | Drained TPS | Measured aggregate JIT ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| n100 off p1 | 4844 | 30.003233905 | 0.000000000 | 0.330036314 | 161.449263 | 161.449263 | 159.692640 | 1941 |
| n100 on p1 | 5136 | 30.002915671 | 0.004486435 | 0.170499361 | 171.183363 | 171.157769 | 170.216066 | 1611 |

| Campaign | Fork | Warmup 1 JIT ms | Warmup 2 JIT ms | Warmup 3 JIT ms | Measurement JIT ms |
| --- | --- | --- | --- | --- | --- |
| candidate-capture-fresh-profile | n100 off p1 | 28751 | 8416 | 1508 | 1941 |
| candidate-capture-fresh-profile | n100 on p1 | 25893 | 9333 | 2200 | 1611 |

The added-time ratios from its raw durations are writer -5.6864%, publication -5.6723%, drained -6.1824%. This pair differs from the earlier uninstrumented fresh results; it does not establish a negative intrinsic backup cost. Its stronger warmup reduced measured compilation but did not eliminate it.

| Measured cost (µs/logical transaction) | Backup off | Backup on |
| --- | --- | --- |
| Writer user CPU | 1570.65 | 1599.75 |
| Writer system CPU | 627.22 | 512.78 |
| Process CPU | 4894.72 | 4690.42 |
| values/main native commit elapsed | 1411.29 | 1336.49 |
| triples/main native commit elapsed | 1517.11 | 1373.98 |
| values/background native commit elapsed | 1095.36 | 1027.66 |

The values timer surrounds its native commit; the triples timer also includes its normal native transaction-ID bookkeeping. Main and background callers are separate; archive-journal commits are not timed by these seams. Background elapsed time overlaps foreground work and cannot be added to the writer interval. Both conditions have exactly one foreground triples commit per logical transaction. Fresh dictionary writes and background retirement account for material native time shared by both conditions.

The publisher used approximately **36.45 CPU µs/transaction** in the interior native-thread observation window, with **0.242 write-family calls** and **3575 physical write bytes/transaction**. These are thread/accounting observations, not exact archive-commit or fsync invocation counts. Existing publication already batches up to 256 stages with a 100 ms delay. This trace did not establish material foreground contention that would justify extending that delay.

Caller-level main Java samples attribute 28 samples to native removal capture, 20 to addition capture, and 15 to other backup/commit work, out of 1,800 main Java samples. Scaling these fractions by writer user CPU is only a sampling heuristic, approximately 56 µs/transaction; allocation bytes are not CPU measurements. These data do not identify a large new backup hot path accounting for the earlier fresh-value overhead.

Fine-grained frame attribution has a retained caveat: 13 execution samples name ByteArrayOutputStream under addition encoding even though frozen source/class constants use the custom MemoryOutput. An independent raw JFR reader reproduced three such stacks without the compact frame table. The anomalous add frame and MemoryOutput snapshot frame share RecordBuffer class ID 3722 and the same application classloader; MemoryOutput has class ID 3724. All 14 retained final/profile fork manifests have the exact e4-first dependency order, no production classes in the harnesses, and only the final runtime contains RecordBuffer. Those historical file checks do not retrospectively prove each loaded CodeSource. The cause of the inner-frame discrepancy remains unresolved, and no optimization is justified from it. The raw reader source, command, output and manifest audit are included in the companion evidence.

The companion JSON also embeds the exact JFC settings, CompactJfr reader and three Python analysis sources under `complementary_diagnostics.fresh_value_profile.source_files`. Its reproduction recipe explains how to extract those files, bind a new verified runtime, run both conditions, extract the measured phases and recompute the analyses. Binary JFR recordings, the complete compact event/stack tables, profile environment samples and extraction console logs are not embedded or delivered by these two documents; their hashes and historical paths identify the retained workspace artifacts. Profile fork consoles, derived summaries and independent raw-reader output are embedded.

### Same-JVM fresh-value crossover

This separate uninstrumented diagnosis uses one JVM and the unchanged public harness. Training is ABBA (A=off, B=on), with one warmup of at least 30 seconds and 7,500 transactions followed by a predeclared one-second training window for each invocation. The four training invocations are retained but are not performance evidence. Measurement is ABBA × 2: eight 30-second windows without additional warmups. Each invocation creates fresh stores; only JVM/JIT/heap history is shared. This design reduces independent startup differences while introducing correlated state and condition carryover, so its four pairs remain descriptive and have no five-percent pass gate.

| Pair / block / order | Writer added time | Publication added time | Journal + shutdown added time |
| --- | --- | --- | --- |
| 1 / 1 / off/on | +0.8262% | +1.1022% | +0.6808% |
| 2 / 1 / on/off | -1.3799% | -1.0330% | -0.8306% |
| 3 / 2 / off/on | +19.6853% | +19.9554% | +19.6550% |
| 4 / 2 / on/off | -12.5308% | -12.3707% | -12.0832% |

| Metric | Geometric mean added time | Pair minimum | Pair maximum | Sample SD (percentage points) |
| --- | --- | --- | --- | --- |
| writer | +1.0087% | -12.5308% | +19.6853% | 13.3695 |
| publication | +1.2698% | -12.3707% | +19.9554% | 13.4026 |
| journal_plus_shutdown | +1.2353% | -12.0832% | +19.6550% | 13.1619 |

| Order | Writer geometric mean added time | Publication geometric mean added time | Journal + shutdown geometric mean added time |
| --- | --- | --- | --- |
| off/on | +9.8518% | +10.1261% | +9.7587% |
| on/off | -7.1225% | -6.8742% | -6.6262% |

#### All crossover windows

| Ordinal / phase / backup | Transactions | Writer seconds | Publication tail seconds | Drain tail seconds | Writer-window aggregate JIT ms |
| --- | --- | --- | --- | --- | --- |
| 1 / training / off | 216 | 1.001631959 | 0.000000000 | 0.130114566 | 963 |
| 2 / training / on | 258 | 1.001829676 | 0.066774016 | 0.169254464 | 123 |
| 3 / training / on | 207 | 1.002052075 | 0.077690467 | 0.151423432 | 5 |
| 4 / training / off | 257 | 1.002092212 | 0.000000000 | 0.098112384 | 96 |
| 5 / measurement / off | 5210 | 30.095108084 | 0.000000000 | 0.320058910 | 460 |
| 6 / measurement / on | 5151 | 30.000134659 | 0.082114471 | 0.275320294 | 370 |
| 7 / measurement / on | 5133 | 30.001437421 | 0.105531987 | 0.432797691 | 224 |
| 8 / measurement / off | 5064 | 30.012273768 | 0.000000000 | 0.264313948 | 110 |
| 9 / measurement / off | 5552 | 30.001934755 | 0.000000000 | 0.283408663 | 200 |
| 10 / measurement / on | 4639 | 30.003022287 | 0.067718476 | 0.275759483 | 67 |
| 11 / measurement / on | 5150 | 30.001054441 | 0.054925339 | 0.441493051 | 66 |
| 12 / measurement / off | 4505 | 30.003302814 | 0.000000000 | 0.286501942 | 279 |

#### Whole-invocation metadata

Invocation counters include setup, any training, measured work, validation, publication and close. They are separate from the global compilation counter sampled around the writer window, and neither compilation counter is writer stall time. Post-close worker quiescence is checked outside the invocation and measured phases, with a 10-second bound. The intentional global arena-reaper singleton is exempt; thread inventories are retained and do not claim exhaustive observation of virtual/internal threads.

| Ordinal / phase / backup | Invocation seconds | Heap before / after bytes | Threads before / after | GC count | GC ms | Invocation JIT ms | Quiescence seconds |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 / training / off | 53.292489 | 8535352 / 475611280 | 6 / 10 | 104 | 1302 | 29499 | 0.000211 |
| 2 / training / on | 46.363424 | 475611280 / 208952384 | 10 / 9 | 102 | 1075 | 11075 | 0.000209 |
| 3 / training / on | 49.243362 | 208952384 / 396799824 | 9 / 9 | 102 | 1102 | 2686 | 0.000133 |
| 4 / training / off | 42.982421 | 396799824 / 387352440 | 9 / 10 | 99 | 955 | 685 | 0.000080 |
| 5 / measurement / off | 30.459197 | 388401016 / 292345880 | 10 / 9 | 68 | 659 | 495 | 0.000100 |
| 6 / measurement / on | 30.324113 | 293394456 / 313613088 | 9 / 9 | 65 | 620 | 377 | 0.000074 |
| 7 / measurement / on | 30.482662 | 313613088 / 426041176 | 9 / 9 | 64 | 648 | 311 | 0.000105 |
| 8 / measurement / off | 30.316315 | 426041176 / 168558272 | 9 / 9 | 64 | 664 | 118 | 0.000121 |
| 9 / measurement / off | 30.326887 | 168558272 / 190840832 | 9 / 9 | 69 | 672 | 212 | 0.000092 |
| 10 / measurement / on | 30.331879 | 190840832 / 158519136 | 9 / 9 | 58 | 659 | 141 | 0.000085 |
| 11 / measurement / on | 30.493543 | 158519136 / 187178824 | 9 / 9 | 65 | 648 | 93 | 0.000090 |
| 12 / measurement / off | 30.340651 | 188227400 / 320805664 | 9 / 9 | 55 | 570 | 335 | 0.000082 |

All twelve invocations report one JVM PID (44) and the same verified CodeSource map. Eight selected production classes, including RecordBuffer, MemoryOutput and the codecs, resolve to the frozen final candidate JAR. The harness class bytes exactly match the immutable d7 harness; manifest rewriting only binds the runtime. Source and runtime hashes, the driver compile/launch commands, every public-main argument list, per-window readiness/acknowledgement results, consoles and metadata are included in the companion JSON.

| Scratch diagnostic source | SHA-256 |
| --- | --- |
| BackupCrossoverDriver.java | `7f6d1cc095fadec318b22ffad0c79aedb60e293ba6512dc747a58d132f9292be` |
| run-crossover.py | `ed4920b1026132a790d1a1ac2fbcfe69d3e683fe80a45edb5105373ee34b85a9` |
| analyze-crossover.py | `c3e8a3f29b385a48805be2202ded46af5c39487400bb0af5a5bada8ea5190e2e` |

Crossover reproduction is separate from the repository acceptance runner: its complete scratch driver, orchestrator and independent analyzer sources are embedded under `complementary_diagnostics.same_jvm_fresh_crossover.source_files`, with exact compile/launch commands and frozen inputs alongside them. Reproduction requires explicit new filesystem paths, candidate/harness artifact hashes, source hashes and correctness-provenance bindings. Historical hash constants identify this exact experiment; for a new equivalent build, verify classes/source/JAR identity, run the recorded correctness gate, freeze dependencies and recompute the new hashes before binding a new campaign. Do not silently bypass the historical checks. These scratch sources do not modify production or the public harness.


## Original-main descriptive reference

Original main uses its exact frozen module from the base commit and the same dependency JARs and native/full-adjacency harness. It has one enabled fork per workload, two 20-second/5,000-transaction warmups, and 30 measured seconds. It observes the legacy file acknowledgement. The final candidate has longer warmup/measurement windows and two enabled forks, so the ratios below are **unpaired descriptive comparisons**, not acceptance evidence or uncertainty estimates.

| Fork | Transactions | Writer seconds | Publication tail seconds | Drain tail seconds | Writer TPS | Publication TPS | Drained TPS | Measured aggregate JIT ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| n1 on p1 | 7684 | 30.003127925 | 26.027947224 | 26.035187498 | 256.106631 | 137.138186 | 137.120467 | 4821 |
| n100 on p1 | 4616 | 30.001678971 | 10.732875532 | 10.747583395 | 153.858056 | 113.319025 | 113.278124 | 3812 |

| Operations/transaction | Metric | Original-main TPS | Final enabled geometric TPS | Descriptive TPS ratio |
| --- | --- | --- | --- | --- |
| 1 | writer | 256.106631 | 688.712383 | 2.6892× |
| 1 | publication | 137.138186 | 688.194249 | 5.0183× |
| 1 | drained | 137.120467 | 658.226312 | 4.8004× |
| 100 | writer | 153.858056 | 238.642348 | 1.5511× |
| 100 | publication | 113.319025 | 238.318757 | 2.1031× |
| 100 | drained | 113.278124 | 237.708858 | 2.0985× |

## Earlier compressed-catalog campaign: retained variance history

These eight forks used the earlier compressed-catalog candidate and the shorter two-20-second/5,000-transaction warmup, 30-second measurement protocol. They are retained as development history and are not pooled with either final campaign. The 100-operation result included both a pair above 5% and a negative-overhead pair; its favorable geometric mean alone did not establish all-pair acceptance.

| Operations/transaction | Pair/order | Writer added time | Publication added time | Drained added time | Writer TPS loss | Publication TPS loss | Drained TPS loss |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 (off/on) | -1.1444% | -1.0111% | -1.0871% | -1.1577% | -1.0214% | -1.0990% |
| 1 | 2 (on/off) | -8.0385% | -7.9208% | -8.6849% | -8.7411% | -8.6021% | -9.5109% |
| 100 | 1 (off/on) | +7.0438% | +7.5251% | +7.5969% | +6.5803% | +6.9984% | +7.0606% |
| 100 | 2 (on/off) | -5.5359% | -5.5115% | -5.2954% | -5.8603% | -5.8329% | -5.5915% |

| Operations/transaction | Metric | Geometric mean added time | Geometric mean TPS loss | Pair minimum | Pair maximum | Sample SD (percentage points) | Every pair <5% |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | writer | -4.6537% | -4.8809% | -8.0385% | -1.1444% | 4.8748 | yes |
| 1 | publication | -4.5284% | -4.7432% | -7.9208% | -1.0111% | 4.8859 | yes |
| 1 | drained | -4.9619% | -5.2209% | -8.6849% | -1.0871% | 5.3725 | yes |
| 100 | writer | +0.5575% | +0.5544% | -5.5359% | +7.0438% | 8.8952 | NO |
| 100 | publication | +0.7963% | +0.7900% | -5.5115% | +7.5251% | 9.2182 | NO |
| 100 | drained | +0.9452% | +0.9363% | -5.2954% | +7.5969% | 9.1163 | NO |

### Every earlier fork

| Fork | Transactions | Writer seconds | Publication tail seconds | Drain tail seconds | Writer TPS | Publication TPS | Drained TPS | Measured aggregate JIT ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| n1 off p1 | 20902 | 30.000713414 | 0.000000000 | 2.806498525 | 696.716765 | 696.716765 | 637.116011 | 609 |
| n1 on p1 | 21144 | 30.000739342 | 0.040477523 | 2.825547003 | 704.782631 | 703.833007 | 644.117942 | 553 |
| n1 on p2 | 22578 | 30.000383387 | 0.038390824 | 2.877421485 | 752.590382 | 751.628540 | 686.724679 | 902 |
| n1 off p2 | 20763 | 30.000275817 | 0.000000000 | 3.110150059 | 692.093637 | 692.093637 | 627.083447 | 347 |
| n100 off p1 | 8179 | 30.000634787 | 0.000000000 | 0.180939231 | 272.627565 | 272.627565 | 270.993156 | 3326 |
| n100 on p1 | 7641 | 30.001441796 | 0.134872568 | 0.336899744 | 254.687760 | 253.547926 | 251.859515 | 4001 |
| n100 on p2 | 7976 | 30.000834729 | 0.007755815 | 0.267965267 | 265.859269 | 265.790557 | 263.505656 | 3615 |
| n100 off p2 | 7535 | 30.002991566 | 0.000000000 | 0.191124578 | 251.141623 | 251.141623 | 249.551931 | 3564 |

## Correctness and artifact provenance

The final frozen candidate passed **399 tests across 53 Surefire classes**, with zero failures, errors, or skipped cases. This is the selected backup, publication, cleanup, startup, and compatibility gate, not a claim that the entire repository suite ran. The evidence includes the exact selection, all 399 testcase names, report SHA-256 values, and successful copyright, formatting, install, and test execution metadata.

The report generator verified all **2701 compiled classes** and **2703 class/resource entries** against the frozen JAR, current output, and source/compiler inventories. It also verified production and test input hashes, the **124 dependency JARs**, the complete source patch, and the bound campaign inputs. No build or test was performed during the timed campaigns.

Complete source-patch SHA-256: `24c2f2003d02c70224237f459aae6414d8f0f05581d05220aff1916d698ab33d`. Runtime-manifest SHA-256: `f44ba39b907c6b7252192cb8b87bd9df4581db7168b5ec3df925050cd0acf2cf`. Measured harness source SHA-256: `3149cd23f0c25efa8f3307706971128aa092e01b5659afd8cec4d0618848edce`. Measured harness JAR SHA-256 before per-runtime manifest rewriting: `d7e3db24782a9e5ff141661fdbe99bc742304ab1749be964b802c2c112bcde95`.

The checked-in harness and the frozen measured harness differ only in Javadoc line wrapping. Both source hashes and their exact diff are retained in the evidence. Every measured fork's rewritten executable harness hash is also retained; production code was not rebuilt between final forks.

## Architecture and review

### Atomic source capture and durable publication

`LmdbSailStore.prepareBackupStage()` prepares a stage before commit. `TripleStore.writeBackupBoundary()` checks its transaction number, predecessor boundary, UUID, and record count, then calls `LmdbBackupOutbox.write()` using the same native transaction that mutates the RDF statement indexes. Committing the triples transaction therefore commits its recoverable backup source atomically. Aborting that native transaction rolls back both together. The separate value dictionary retains the store's existing paired commit ordering; this change does not claim a single LMDB transaction across both environments.

Sources: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` — `prepareBackupStage()`, `publishCommittedDelta()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` — `writeBackupBoundary()`.

Enabled journaling forces synchronous commits in both native environments, including when the general `forceSync` setting is false. The dictionary commits before triples. `syncBackupPublicationBoundary()` crosses the sink gate to observe that completed boundary; it avoids forcing the same pages again. Publication also waits for the owning source scope to release, including scopes containing physical checkpoints. Startup recovery separately synchronizes historical external stages.

Sources: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` and `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` — constructors and native environment flags; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` — `syncBackupPublicationBoundary()`.

The worker publishes contiguous batches of up to 256 stages through `onCommitBatch()`. `LmdbBackupJournal.append()` commits the batch and its head together in a separate archive LMDB environment using synchronous commit flags. Acknowledgement advances only after that succeeds. If the process stops between archive commit and acknowledgement, replay checks existing archived bytes and predecessor identities. Legacy external-stage deletion first checkpoints a durable file acknowledgement. The optimization removes repeated file creation, synchronization, and model conversion while retaining the source-to-archive durability ordering.

Sources: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PostCommitBackupPublisher.java` — `publish()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupServiceImpl.java` — `onCommitBatch()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupJournal.java` — constructor, `append()`, `appendStage()`.

### Identity and reclamation

Every prepared stage receives fresh UUID entropy, including attempts later abandoned. A retry may reuse a logical transaction number, but it cannot inherit the aborted attempt's stage identity. Stage headers, predecessor links, committed notifications, archive heads, and acknowledgements carry the identity alongside the transaction number. `nextIdentity()` batches entropy acquisition without reusing identities.

Source: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PostCommitBackupPublisher.java` — `prepare()`, `nextIdentity()`, `committed()`, `readStage()`.

Reclamation requires an archive-durable acknowledgement tuple captured coherently for the current writer attempt. `writeBackupAcknowledgement()` validates and persists its identity-aware proof in the same native transaction as deletion. Concurrent publication cannot enlarge that transaction's deletion horizon after an older proof was written. Replacement writes retain the new source; maintenance without replacement retains the authoritative current head. Packed cleanup validates checksums, ordering, and range boundaries, and deletes only fully acknowledged packs. Reclaim watermarks become committed only after native success, so rollback and map-growth replay cannot skip unfinished cleanup. Cleanup work is bounded per attempt.

Sources: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` — `writeBackupBoundary()`, `writeBackupAcknowledgement()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupOutbox.java` — `reclaim()`, `reclaimPackedBucket()`, `committed()`.

### Bounded memory and compact native staging

The publisher retains at most 4,096 pending notifications and a 256-stage publication batch. Pending and in-flight owned publication payloads share a 1 MiB budget; the sole source writer can additionally hold one provisional chunk of at most 64 KiB. Overflow falls back to the committed native source. Release bookkeeping retains live source scopes, rather than every checkpoint in a stalled backlog. Unpublished durable history remains on disk: bounded notification memory does not mean discarding transactions or capping recoverable history.

Source: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PostCommitBackupPublisher.java` — `committed()`, `releasedThrough()`, `readStageForPublication()`, `Stage.openNativeSource()`.

Small complete frames share a reserved MAIN-catalog value. Initialization probes a 960-byte reservation in a nested transaction, with a 640-byte fallback, and accepts only a single-leaf catalog without overflow. Later schema growth triggers atomic evacuation when that condition ceases to hold. The new `R4H2` envelope compresses the existing `R4P1` pack only after its raw representation exceeds the reservation. Decoded size is capped at 1,920 bytes and the native pack limit before allocation. Readers validate lengths, padding, decompression, and the inner pack checksum. Both `R4H1` and `R4H2` remain readable. If compression cannot fit, rollover preserves the complete old pack in the unchanged cold-pack encoding; individual chunk frames and public delta records are unchanged.

Source: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupOutbox.java` — `initializeCatalog()`, `reserveCatalog()`, `catalogFits()`, `tryPutCatalogPack()`, `copyCatalogPack()`, `appendSmallFrame()`.

### Lexical capture and ownership

The removal fast path copies supported dictionary terms directly into the public record encoding while the physical dictionary writer remains admitted. Native buffers and numeric term IDs do not escape into asynchronous backup work. Inline literals and nested triple terms retain the protected model fallback. Literal tags, lengths, language, and datatype validation remain active. Metadata reuse is limited to 32 owned UTF-8 entries of at most 4 KiB each, scoped to an opaque physical-writer identity that changes across rollback and dictionary checkpoints.

Source: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` — `writeBackupRecord()`, `NativeBackupWriter`, `backupMetadataFor()`, `NativeBackupMetadataCache`.

`LmdbBackupRecordBuffer` keeps encoded records in memory up to 64 KiB, then spills. Memory snapshots own their bytes; spill snapshots are tied to the buffer generation and reject use after invalidation. `EncodedRecords` lets publication consume existing lexical bytes without decoding RDF models and encoding them again. The empty-string shortcut emits the same zero length as the existing codec.

Sources: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupRecordBuffer.java` — `addNativeStatement()`, `snapshot()`, `clear()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupDeltaCodec.java` — `EncodedRecords`, `writeString()`.

### Historical input, public export, and downgrade limits

The candidate recovers historical UUID-named `R4O1` stage files and file acknowledgements, reads existing public delta logs, and exports native history into the unchanged `txn-*.delta.gz` representation. Public delta versions and record encodings remain supported. Export continuity still requires complete transaction and generation coverage.

Sources: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PostCommitBackupPublisher.java` — `LegacyRecoveryIndex`, recovery constructor; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupServiceImpl.java` — `exportTransactionLogs()`, `collectRestoreDeltaLogs()`, `validateLogCoverage()`.

Public export compatibility does **not** imply live-store downgrade compatibility. The first enabled open installs new native outbox databases, before any user transaction; disabling journaling does not remove them. Unpatched `8fc7` rejects this schema during ordinary discovery and cached/CLEAN prepared admission. Its `PreparedBulkStore.capture()` still invokes `LegacyStoreDiscovery.validateCurrentMetadata()`, and ordinary discovery also rejects unknown database names. A full native snapshot contains those databases and has the same limitation. These conclusions come from the static downgrade audit; no old-binary restore test was performed.

The old archive reader does not discover `txlog/journal/<generation>`. Missing unexported history fails its coverage check rather than silently restoring an earlier boundary. Rollback therefore needs an old-readable pre-upgrade snapshot plus fully exported compatible history, or a logical migration into a separately created old-format store. Existing store-format and manifest version numbers alone do not express this boundary.

Audit references at baseline `8fc7`: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PreparedBulkStore.java` — `readSealed()`, `readCurrent()`, `capture()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LegacyStoreDiscovery.java` — `validateCurrentMetadata()`, `readDatabaseCatalog()`, `validateKnownDatabases()`; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBackupServiceImpl.java` — `collectRestoreDeltaLogs()`, `validateLogCoverage()`.

## Implementation references

These primary sources informed the storage tradeoffs and ordering. [LMDB 0.9.35 source](https://github.com/LMDB/lmdb/blob/LMDB_0.9.35/libraries/liblmdb/mdb.c) shows dirty-page flushing split across noncontiguous page runs; this motivated the packing hypothesis without proving syscall causality for these measurements. [PostgreSQL WAL configuration](https://www.postgresql.org/docs/current/wal-configuration.html) describes amortizing synchronization through group commit. [SQLite WAL](https://www.sqlite.org/wal.html) explains commit/checkpoint separation. [RocksDB WAL](https://github.com/meta/rocksdb/wiki/Write-Ahead-Log-(WAL)) discusses recovery durability and log retention. These comparisons inform the RDF4J-specific source/archive protocol described above; they do not imply identical implementations or establish unmeasured performance causes. The implementation reuses the existing LMDB and LZ4 dependencies.

## Reproduction

Use Linux, Python 3, a JDK 25+ on PATH, Maven, and four allowed CPU IDs. The recorded runtime was Temurin 25.0.5+8.
Use a quiet host and the same filesystem for both conditions. The runner executes forks sequentially, reverses
on/off order in pair 2, preserves every fork, and fingerprints the runtime before and after execution.
Use new output directories for each campaign.

From the repository root, build dependencies and compile the candidate:

~~~bash
mvn -T2 -Pquick -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am install
mvn -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb dependency:build-classpath \
  -DincludeScope=test "-Dmdep.outputFile=$(pwd)/core/sail/lmdb/target/backup-dependencies.txt"
~~~

Both setup commands may resolve plugins and dependencies; use offline mode once those artifacts are cached.
The quick profile compiles tests without running them. Run the exact 399-test selection recorded in the
companion JSON after setup:

~~~bash
BACKUP_TEST_SELECTION=$(python3 -c \
  'import json; print(json.load(open("core/sail/lmdb/backup-performance-evidence.json"))["candidate"]["test_selection"])')
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb \
  "-Dtest=$BACKUP_TEST_SELECTION" -DforkCount=1 '-DargLine=-Xmx2g' surefire:test
~~~

Freeze the built module and dependency JARs before benchmarking. This standalone preparation checks that the
module JAR contains the actual compiled classes/resources; it does not rebuild anything:

~~~python
from pathlib import Path
import hashlib
import json
import os
import shutil
import zipfile

module = Path("core/sail/lmdb").resolve()
target = module / "target"
output = target / "backup-throughput-runtime"
output.mkdir(exist_ok=False)
candidates = [path for path in target.glob("rdf4j-sail-lmdb-*.jar")
              if not path.name.endswith(("-tests.jar", "-sources.jar", "-javadoc.jar"))]
assert len(candidates) == 1, candidates
module_jar = candidates[0]
classes = target / "classes"
with zipfile.ZipFile(module_jar) as archive:
    actual = {path.relative_to(classes).as_posix(): path
              for path in classes.rglob("*") if path.is_file()}
    assert {name for name in archive.namelist() if name.endswith(".class")} == {
        name for name in actual if name.endswith(".class")}
    for name, path in actual.items():
        assert archive.read(name) == path.read_bytes(), name
dependencies = [Path(value).resolve() for value in
                (target / "backup-dependencies.txt").read_text().strip().split(os.pathsep) if value]
frozen = []
for index, source in enumerate([module_jar, *dependencies]):
    assert source.is_file() and source.suffix == ".jar", source
    destination = output / f"{index:03d}-{source.name}"
    shutil.copyfile(source, destination)
    frozen.append(destination)
(output / "runtime-classpath.txt").write_text(os.pathsep.join(map(str, frozen)) + "\n")
(output / "runtime-sha256.json").write_text(json.dumps({
    path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in frozen}, indent=2) + "\n")
~~~

Save that Python block as a temporary preparation script or run it through a quoted Python here-document.
Compile only the standalone harness against the frozen runtime:

~~~bash
python3 scripts/prepare-lmdb-backup-throughput-benchmark.py \
  --classpath-file core/sail/lmdb/target/backup-throughput-runtime/runtime-classpath.txt \
  --output core/sail/lmdb/target/backup-throughput-harness.jar
~~~

Run the fixed primary and fresh-values campaigns separately. Substitute four allowed CPU IDs if 0–3 are
unavailable; that changes the measured host setup and should be recorded:

~~~bash
python3 scripts/run-lmdb-backup-throughput.py \
  --classpath-file core/sail/lmdb/target/backup-throughput-runtime/runtime-classpath.txt \
  --harness-jar core/sail/lmdb/target/backup-throughput-harness.jar \
  --cpus 0,1,2,3 --force-sync true --publication-observer publisher \
  --statements 1,100 --conditions off,on --pairs 2 \
  --warmup-rounds 3 --warmup-seconds 30 --min-warmup-transactions 7500 \
  --measurement-seconds 60 --drain-timeout 180 --fork-timeout 360 \
  --output backup-throughput-results/primary --label primary

python3 scripts/run-lmdb-backup-throughput.py \
  --classpath-file core/sail/lmdb/target/backup-throughput-runtime/runtime-classpath.txt \
  --harness-jar core/sail/lmdb/target/backup-throughput-harness.jar \
  --cpus 0,1,2,3 --force-sync true --publication-observer publisher \
  --statements 100 --conditions off,on --pairs 2 --fresh-values \
  --warmup-rounds 2 --warmup-seconds 30 --min-warmup-transactions 5000 \
  --measurement-seconds 60 --drain-timeout 180 --fork-timeout 360 \
  --output backup-throughput-results/fresh-values --label fresh-values
~~~

The runner uses `scripts/run-single-benchmark.sh --no-build` and emits `campaign.json`,
`runtime-fingerprints.json`, `summary.json`, and every fork's `result.json`, `command.json`, console log,
and environment samples. Recompute ratios from the raw durations/counts, retaining the complete planned run
order. Do not compile or run other JVM workloads during a campaign.

To reproduce the descriptive original-main reference, build commit
`8fc7b0137164ffdc1f5d080b943fd362931137dd` in a separate checkout. Use its frozen LMDB JAR as the first
classpath entry and the same frozen dependency JARs. Reuse the candidate-compiled harness: compiling its
diagnostic APIs against the older module is not supported. Run the same runner with
`--classpath-file <reference-classpath-file> --publication-observer file --conditions on --pairs 1
--statements 1,100 --warmup-rounds 2 --warmup-seconds 20 --min-warmup-transactions 5000
--measurement-seconds 30` and a new output directory. Keep this reference separate from the final on/off
acceptance analysis because its timing protocol and sample count differ.

## Complete machine-readable evidence

[backup-performance-evidence.json](backup-performance-evidence.json) is self-contained for the measurements: it contains every final, original-main, and earlier-compressed fork result and command, raw counts/durations, phase/JIT/CPU/validation fields, pair and aggregate calculations, runtime/dependency/source hashes, the full 399-test inventory, and input-artifact hashes. It does not require the original temporary workspace to inspect those data. Complementary fresh-profile and crossover results remain separate, including all twelve crossover windows, exact commands, source/class/CodeSource provenance, thread/GC/heap/readiness metadata and crossover source files. Crossover window consoles and environment samples are embedded; original independent-fork environment samples and build/test log contents are not duplicated. Binary JFR recordings are not embedded or delivered by these two documents; SHA-256 values identify the historical workspace files, and derived summaries and independent raw-reader evidence are embedded. Historical workspace paths alone are not downloadable copies of omitted recordings or logs.

Evidence SHA-256: `8a8c63e5c6dc43da575e3394071c5e3c0525167cc9b89afa4843bad387db6d74`.
