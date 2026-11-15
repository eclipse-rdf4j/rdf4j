# Reduce incremental backup transaction overhead

This ExecPlan follows `.agent/PLANS.md` and uses Routine D for a substantial persistence refactor. It is a living document; keep its progress, decisions, discoveries, and outcomes current.

## Purpose / Big Picture

The user needs LMDB incremental backup enabled with less than 5% write overhead. Current upstream main forces per-transaction external staging and acknowledgement files, syncs the value database redundantly, and repeats serialization. Preserve committed data, transaction identity, backup restore compatibility, configured durability, bounded memory, map-growth safety, and point-in-time restore. Measure actual completed publication, not only foreground enqueue time.

## Progress

- [x] Identify current upstream and fork branches.
- [x] Recover previous optimization and measurement evidence.
- [x] Establish baseline build and recovery tests.
- [x] Port recovered changes preserving upstream fixes.
- [x] Validate recovered crash and lifetime invariants.
- [ ] in_progress: Measure and optimize remaining write costs.
- [x] Validate bounded publisher and current candidate correctness.
- [ ] Format, review, and publish verified changes.

## Context and Orientation

The isolated checkout is on `GH-0000-incremental-backup-throughput`, based on upstream `8fc7b0137164ffdc1f5d080b943fd362931137dd`. Origin is HASMAC-AS/rdf4j; upstream is eclipse-rdf4j/rdf4j. HASMAC main is older, at `ec7e48248eb20c4f7681d8ea5cbed3100718527a`.

Production code is under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. `LmdbSailStore` captures transactional changes and coordinates the value and triple databases. `PostCommitBackupPublisher` owns the staged transaction queue. `LmdbBackupServiceImpl` exports incremental journals and restores backups. `LmdbBackupRecordBuffer` holds transaction changes, spilling oversized transactions to disk. Recovered `LmdbBackupOutbox` persists staged payloads inside the same LMDB commit as the triples; recovered `LmdbBackupJournal` batches archive writes, and `LmdbBackupChunkCodec` encodes bounded chunks. Native transaction identity includes a UUID so aborted transactions cannot impersonate a later reuse of a numeric transaction number.

A prior unpublished iteration17 checkpoint exists under `../checkpoint17`, with the exact diff at `../recovered/rdf4j-incremental-backup-iteration-17.patch`. Its base is `49349cc0dc843fbd67dc1c7e52f67b1f9c5d1349`. The patch applies cleanly to current upstream, but complete old files must not replace current files because upstream has additional changes. Historical tests passed, but no iteration17 TPS result exists. The previous instrumented durable one-statement result was about 5.96% publication added time and 5.52% fully drained added time. This does not meet the current acceptance target.

## Plan of Work

First build current main and capture focused backup tests before editing production. Preserve the baseline LMDB JAR and runtime classpath for comparable original-main measurements. Prepare a standalone harness with explicit adjacency and native-engine configuration, checked at runtime. Use the same JVM, workload, index configuration, durability policy and filesystem for backup-off and backup-on. The primary comparison uses forceSync=true on both sides; backup enabled currently imposes stronger durability even when forceSync=false. Report a separate default-setting comparison if it is measured, without mixing the two policies.

Then apply the recovered diff, retaining current upstream fixes. Read and review atomic staging, publication acknowledgements and source reclamation. Re-run current backup correctness tests and the recovered dedicated tests, especially crash/reopen, aborted identities, old formats, map growth, spilled-record ownership, and value retirement. Fix any regression at its source before timing.

Use an initial short direction run to verify harness invariants. Then run a fixed balanced sequence of independent JVMs for one-statement and 100-statement transactions with backup disabled and enabled. Record foreground, publication and fully drained TPS, final acknowledgement and native head equality, final backlog, and exact source/artifact provenance. Do not run builds or tests concurrently with timing. If overhead exceeds the target, profile the measured bottleneck and change only the demonstrated expensive path. Re-run the focused correctness coverage and a fresh balanced measurement block after each promoted change.

Finally format changed sources, independently review the final diff, verify complete-source and diff deliverables, and put the implemented work on the requested fork branch. Do not claim less than 5% from a single favorable sample, enqueue-only rates, or unmeasured historical data.

## Concrete Steps

From the repository root, `../env-check/mvn` selects JDK25, current proxy settings, and the workspace Maven repository `.m2_repo`. The toolchain resides at `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8` and the Maven distribution beneath that toolchain directory. Build dependencies with tests skipped:

    ../env-check/mvn -Pquick -T 2 -pl core/sail/lmdb -am install

Run tests without `-am` or `-q`:

    ../env-check/mvn -o -pl core/sail/lmdb -Dtest=LmdbBackupServiceTest,LmdbPostCommitPublicationTest,LmdbBackupRecordBufferTest test

Record exact expanded commands and outcomes as they become available. Save test summaries outside target before another run replaces them. Baseline and candidate timing commands will be added after the standalone harness is prepared.

## Validation and Acceptance

All selected backup, recovery and affected lifetime tests must pass without weakening assertions. A sub-5% claim requires backup-on completed throughput greater than 95% of the comparable backup-off throughput, on both measured transaction sizes. Also report added execution time as baseline TPS / enabled TPS minus one, so the two common definitions of overhead are unambiguous. Prefer the stricter added-time threshold under 5%. Include publication completion and final draining in the measurement; validate repository contents and restored journal contents separately from timing. State the actual workload, durability and hardware limits of any claim.

## Idempotence and Recovery

Do not alter the prior checkpoint or old working trees. All changes occur in the isolated branch. Preserve logs and intermediate evidence in scratch. Applying the recovered patch is a one-time operation; inspect git status before retrying. Never force-push a changed remote head. Any migration must preserve old journal input and reject malformed or ambiguous state rather than delete evidence.

## Surprises & Discoveries

Current upstream performs forced file staging, duplicate value synchronization, a triple synchronization, per-transaction gzip publication, and a forced external acknowledgement. The recovered design removes this protocol overhead but already demonstrates that native page writes and capture CPU can dominate the remaining few percent. Fewer outbox buckets and several prior packing variants were measured and rejected; their structural compactness did not predict better TPS.

## Decision Log

On 2026-11-15, use current upstream main as the branch base because HASMAC main predates the relevant backup work. Recover prior iteration17 to preserve completed design, tests and negative results. Apply its diff rather than overwriting full files. Enable adjacency and the native engine explicitly in performance experiments. Use equal configured durability for the primary comparison and drain publication before accepting a result.

## Outcomes & Retrospective

The native outbox, batched archive, bounded publisher, compressed catalog, and narrow capture changes are implemented. The frozen capture candidate passes399selected tests. Its longer fixed eight-fork campaign averages1.2392% added time through publication for n1 and2.8675% for n100, but n100 pairs range from-7.2073% to14.0363%. The separate fresh-value n100 campaign misses5% in both orders, with8.2331% mean added time. The requested performance target is therefore still unproved across the tested workloads. A stronger-warmup fresh-value diagnostic is in progress to separate capture CPU, native commits, publication, and residual compilation before choosing another change.

## Interfaces and Dependencies

Reuse existing LWJGL LMDB, LZ4 and Java runtime libraries. Add no new third-party dependencies. Preserve public Sail backup interfaces and exported backup formats. Changes to internal source staging are validated through actual backup export, reopen and restore tests.

## Artifacts and Notes

The earlier checkpoint README states: `Candidate17 correctness checkpoint: 378 selected cases pass; small-frame LZ4 policy; target unproved; prepared fixed4 direction unrun.` This is historical context, not evidence that the current branch passes.

Revision 2026-11-15: Created the continuation plan after recovering prior work and verifying current upstream. The current active step is baseline build and focused recovery tests.


Baseline evidence 2026-11-15: the unmodified current-main backup selection passes31/31. The new transaction-cost regression has1failure/0errors: expected0stage files, actual8, after8durable native+adjacency-enabled commits with publication blocked. Full reports are preserved under `../env-check/baseline`. Production has not changed before this reproduction.

Porting milestone: recovered17 diff applied cleanly to upstream8fc7b013, excluding the historical plan and unused profiling/parent helpers. Current harness explicitly verifies adjacency/native mode, supports old file ACK observation and fresh-value coverage. Candidate build and selected backup/retirement correctness run now active. Independent review found unbounded pending Stage metadata; a bounded optional cache and release-scope/lazy-recovery repair is being drafted separately before final promotion.

Candidate17 current-main validation:314 tests across44classes, zero failures/errors/skips. Transaction-cost regression now passes with zero stage files. Frozen jarSHA256 a5bfeb385e4221131c1c0a26536a6def75e3b06f939314fe60a821f4d933cae0; harnessSHA256 fba131c8a328f399f01628a6fed627a3c62a8c298485108332cda52534e67101. Initial compatibility and direction measurements use only these frozen artifacts; final acceptance will use the bounded-publisher candidate after its own tests.

Benchmark correction: the inherited `spoc,ospc,psoc` configuration cannot build the incoming adjacency plane. Its prerequisite smoke failed before writing; no TPS from that run is evidence. Corrected both conditions to `spoc,posc,ospc`, verified native evaluation and actual full adjacency readiness, and froze a separate harness with SHA256 d7e3db24782a9e5ff141661fdbe99bc742304ab1749be964b802c2c112bcde95. Both frozen production runtimes remained unchanged. Corrected original-main smoke passed all runtime and acknowledgement checks.

Candidate17 first corrected 10-second direction pair: disabled751.870TPS, enabled685.265TPS through durable publication, added time9.719%. Fully drained558.131 versus505.860TPS, added time10.333%. Source head, archive acknowledgement UUID/transaction, and post-close checkpoint matched; final backlog0. This single short pair misses the target and is not acceptance evidence. Profile the actual writer, archive and native commit costs before promoting another performance change.

Bounded publisher followup integrated for validation: cap the optional Stage cache at4096, retain release state only for active publication scopes, lazily recover native stages by contiguous transaction and UUID, and index historical UUID-named stages in a reconstructible temporary disk index. Four new real-store tests cover stalled publication, failed publication plus reopen, out-of-order scope cleanup, and legacy backlog recovery beyond the cache. Production and test code need compilation and execution before promotion. Profiling continues against the immutable, already tested candidate17 artifact; no builds overlap timing.

Bounded publisher validation completed:383 tests in52classes, zero failures/errors/skips. Includes all378 historical selected cases, the new per-transaction stage-file regression, and4bounded-publisher cases. The legacy case now covers24external stages followed by1native stage across failure, reopen, export, restore, and acknowledged deletion. Frozen bounded JAR SHA2561b7918e7e7e5d15cb39db155b636436a514eaba68a4017fdd583e7db29f3b5d7, verified against2702compiled entries. Temporary first test compilation failed on a checked IOException inside a test Runnable; the fixture now uses the equivalent pure native logical-boundary getter. Production compilation succeeded throughout.

Candidate17 diagnostic profile: one triples commit per logical transaction in both modes, native commit840.16→976.59microseconds, writer system CPU178.69→249.30microseconds, writer write calls3.679→6.047 per transaction, physical written bytes47935.5→48178.0. This is consistent with fragmented native dirty-page runs; it does not prove a particular pack rollover causes each syscall. Encoder CPU is minor. Diagnostic includes JFR and incomplete JIT stabilization, so its TPS is not acceptance evidence. Raw data and exact provenance are in `../benchmarks/candidate17-profile`.

Original-main real reference completed with explicit native evaluation/full ready adjacency, forceSync=true, two warmups each>=20seconds and>=5000transactions, and30-second measurements. n1: writer256.107TPS, publication137.138TPS, fully drained137.120TPS;3925pending at writer stop and26.028seconds to finish publication. n100: writer153.858TPS, publication113.319TPS, fully drained113.278TPS;1484pending and10.733seconds to finish publication. Both source/ack/checkpoint identities and sizes validated, finalbacklog0, immutable runtime fingerprints unchanged. One fork per size is a descriptive before/after reference, not a variance estimate or a five-percent result.

Next measured hypothesis: a versioned compressed catalog envelope can hold more unchanged Pack-v1 frames inside the same960-byte native record. A synthetic exact-codec probe found eight representative stages fit; decoded size remains bounded at1920bytes and cold packs stay unchanged. New format tests and a real eight-stage/rollover/reopen regression are being run before production changes. Existing format, corruption, legacy, and map-growth assertions remain required. Larger evacuated cold packs may cause occasional append-before-reclaim leaf splits; measurement must decide whether this tradeoff improves native write cost.

Compressed-catalog validation completed:398 selected tests, zero failures/errors/skips. The new14case selection failed before production changes (1capacity regression failure,3unsupported-positive-format errors,10malformed-input rejection passes), then passed with R4H2. Initial combined397run exposed3old fixture setup assumptions: hot packs can cross bucket intervals, bucket reuse can take320transactions, and prepending to a full cold pack can exceed its native bound before the intended overlap check. Fixtures now choose an actual same-destination hot frame, allow512commits for the existing rotation, and inject a size-preserving overlap. Exact rejection, proof, rollback, retry, and archive assertions remain. An additional real native/ready-adjacency case proves encoded-size non-fit below the decoded limit, native abort preserving all source/proof/RDF state, and a successful retry. All398 pass after formatter and JDK25quick install. No current throughput claim is made; bind the frozen runtime to the fixed8fork comparison next.

Compressed-catalog primary comparison completed on frozen JAR736132eb735224e84843bb5d22a178881cff8484509397d8fbec568158c367c6 and unchanged harnessd7e3db24782a9e5ff141661fdbe99bc742304ab1749be964b802c2c112bcde95. All8forks passed content, native/full-adjacency, final source/ack/checkpoint identity, backlog0, and unchanged-runtime validation. Geometric-mean added time through publication was-4.5284% for n1 and+0.7963% for n100. Individual n100 pairs were+7.5251% and-5.5115%; retain both and do not infer a precise speedup or a universal<5% guarantee. Raw evidence is in`../benchmarks/candidate-compressed-catalog-acceptance`.

The n100 paired JFR diagnostic shows one main-thread triples commit per logical transaction in both conditions. Writer write calls19.613→19.720/transaction (+0.54%); native commit1538.90→1477.60microseconds in this noisy pair. Main user CPU1167.88→1282.52microseconds, system CPU410.10→259.31microseconds. Of1960enabled main Java samples,93include backup work:52raw removal capture,23addition capture,18commit/other. Large copies contribute few samples (snapshot4, publication payload1); the chunk encoder contributes2. The borrowed wrapper encloses47samples but only8are ThreadLocal self work; do not conflate inclusive wrapper cost with entry cost. Weighted allocation samples identify the per-record lambda and UTF-8 encoding. Exact analysis, raw JFR, commands and limitations are retained in`../benchmarks/candidate-compressed-catalog-n100-profile`; these instrumented timings are not acceptance evidence.

Final narrow capture decision: remove only the raw encoder's borrowed-read-context wrapper in favor of a local MemoryStack frame, keeping all native writer identity/consumer checks and external Sail admission. Independent audit confirms the raw encoder never consults ReadContext and the wrapper adds no lifetime lock or epoch protection. Add a real nested-stack/IOException/retry regression. Skip empty UTF-8 encoding and zero-byte output while preserving the exact public wire format. Avoid a new metadata cache or snapshot ownership rewrite because sampled CPU does not justify their complexity. Rebuild and rerun the complete correctness selection before freezing a new artifact.

Final verification protocol is fixed before the new runtime is built: n1 andn100 each run off/on/on/off in fresh JVMs, with3warmups each>=30seconds and>=7500transactions, then60seconds measured. A separate fresh-value n100 off/on/on/off campaign uses2warmups each>=30seconds and>=5000transactions, then60seconds measured. Both retain the same harness, indexes, forceSync=true, native engine, full ready adjacency and final publication/close drains. Do not pool the two value workloads or replace the earlier results. The longer phases address observed residual compilation and native/host variability, rather than selecting favorable short forks.

Compatibility audit: current main's standard cached/CLEAN and fallback startup paths reject the new native outbox layout before admitting writes; no global format-version bump is added. Enabling the candidate installs this internal schema even before a user commit, and disabling journaling does not undo it. New full native snapshots require a compatible runtime. Historical stage/public delta inputs remain readable, and explicit incremental export encoding is unchanged. Document pre-upgrade snapshots or data migration for downgrade; static audit details are retained in`../review-tests/downgrade-compatibility-audit.md`.

Capture validation completed:399tests across53classes, zero failures/errors/skips, including nested MemoryStack exception/retry coverage. Frozen JAR SHA256 e4d735e7ad7eb0dbba1d1665660923287a34adf96871eb1f627d3b9c11d74c0e; complete source patch SHA25624c2f2003d02c70224237f459aae6414d8f0f05581d05220aff1916d698ab33d. All12long primary/fresh forks passed cardinality, native/full-adjacency, source/archive/checkpoint identities, final publication backlog0, and unchanged-runtime checks. These benchmark validations do not compare exact restored RDF per fork; correctness tests cover restore separately. Publication-plus-shutdown includes store close but does not assert empty reclamation queues. Explicit export/gzip work remains outside write TPS timing.

Fresh-value investigation decision: retain both failing pairs (+5.9098% and+10.6074% publication added time), including their substantial measured compilation, without claiming host noise explains them. Prepare a diagnostic fixed off/on pair with3warmups each>=30seconds and>=7500transactions, then30seconds measured, exact native commit timers and writer-thread I/O, JFR Java2ms/native20ms sampling. This diagnostic remains separate from acceptance. An exact-codec probe of128reused and128fresh stages finds frames1243..1311 versus1281..1318bytes; LZ4 high compression gives1239..1274 versus1275..1282bytes, too small a gain to justify a new compression policy from size evidence alone. No production change was made for this probe. Continue the one active optimization step before publishing a truthful complete result.

The stronger-warmup fresh profile completed on the same frozen capture runtime, with zero JFR data-loss events. Disabled/enabled writer throughput was 161.449/171.183 TPS, reversing the earlier uninstrumented comparison. Main user CPU increased only 29.10 microseconds per transaction; dictionary-plus-triples native commit wall time decreased about 218 microseconds per transaction in this pair. Backup capture/commit stacks account for 63 of 1,800 main Java samples, suggesting about 56 microseconds per transaction when scaled by user CPU; this is a sampling heuristic, not a direct timer. Writer write-family calls increased 1.31%, physical bytes decreased 3.76%, and the publisher consumed 0.18 CPU seconds in a 29.15-second interior window. These observations do not support an additional compression policy, native-pointer lifetime rewrite, or changing the existing 100 ms publication delay. Retain the prior 8.2331% fresh-value result and all profiler limitations.

Next measurement decision, fixed before execution: a complementary fresh-value crossover calls the unchanged public benchmark main repeatedly in one JVM. Train both conditions in off/on/on/off order; each training invocation has one warmup of at least 30 seconds and 7,500 transactions followed by one second explicitly classified as training. Then measure eight 30-second windows in off/on/on/off/off/on/on/off order, with no internal warmup. Every invocation creates a fresh store and validates native evaluation, full adjacency, cardinality, source/archive/checkpoint identity, and completed publication plus close. Preserve all training and measured windows without target-based repeats or exclusions. Shared compilation/GC history changes this experiment's scope, so report it separately from independent forks. Verify the actual production CodeSource and rewritten harness manifest: the original immutable harness manifest points to its historical runtime. The codec-size probe was rerun with the capture runtime first and produced identical output; both relevant chunk-codec class hashes were also identical across those runtimes.
