# Add bounded asynchronous dictionary retirement

This living plan follows `.agent/PLANS.md`. The repository is `eclipse-rdf4j/rdf4j`, based on main commit `b9cf33cc1ac9a3dae09b2fe996b7b72faa055495`. No issue number was supplied; the local branch is `GH-0000-async-value-cleanup`. The user explicitly authorized committing and pushing this feature branch on 2026-10-30.

## Purpose / Big Picture

Allow applications to opt into best-effort dictionary reclamation without adding durable retirement index writes or running dictionary garbage collection on application commits. The default remains the existing durable policy. RDF publication, rollback, snapshots, dependency counts, and guarded ID reuse retain their existing correctness guarantees. Losing pending cleanup candidates after a crash leaves reusable dictionary records and does not lose RDF data.

## Progress

- [x] (2026-10-30) Inspect publication and retirement ownership paths.
- [x] (2026-10-30) Establish failing configuration and policy tests.
- [x] (2026-10-30) Implement bounded coordinator and transaction streams.
- [x] (2026-10-30) Integrate worker ownership and safe lifecycle.
- [x] (2026-10-30) Verify crash recovery and concurrency cases.
- [x] (2026-10-30) Measure latency, pressure, and retained memory.
- [x] (2026-10-30) Format and record complete reviewable changes.
- [in_progress] Commit and push the verified feature branch.

## Surprises & Discoveries

`ValueStore.startTransaction` has a timed foreground eviction path in addition to the two named `LmdbSailStore` reclamation paths. Async mode must suppress all three. `gcIds` removes forward mappings and updates dependency counts, while delayed physical reverse-record deletion and ID reuse use existing epoch protection. `ValueStore` already persists a reference-count generation UUID and records its pre-startup native transaction ID, allowing conservative spool identity checks without a dictionary format change.

The ordinary candidate set and `NoneWriteState.retiredIds` can both retain arbitrarily many boxed IDs. Async mode needs a dedicated append-only primitive stream at collection time. Re-addition need not remove a hint from that stream because the worker rechecks authoritative statement usage.

## Decision Log

Use bounded producer reservations acquired before conflicting ownership, fixed-size primitive candidate buffers, and non-durable spill files for large transactions. Reserve the worker workspace separately, including its bounded root sets and at most three recursive children per root. The queue holds completed, published streams in FIFO handoff order and the single worker consumes compact chunks. Completion transfers ownership only after the producer has released its writer and read view. Producer slots are retained until all associated work is acknowledged. No entire-transaction capacity reservation is required.

Protect candidate numeric identity with a dedicated, constant-size `ValueStore.CleanupReservation`, acquired before candidate observation and held until its stream is acknowledged. The guard prevents physical reverse-record deletion/recycling without a native read transaction. Reusing the existing lazy-value epoch reservation was rejected: a large spilled stream would pin an unbounded history of subsequent maintenance epochs. Async mode also suppresses the obsolete per-epoch timer hints because physical cleanup scans durable `unused_ids` directly. Logical dictionary retirement can still complete, persist its existing reverse-record continuation, and free queue capacity. Readers must be quiescent under existing publication coordination before direct asynchronous retirement; reintroduced values are rechecked against authoritative triples and dictionary dependency counts.

Persist recursive continuations in the existing retirement mechanism inside the worker's maintenance transaction. This bounds memory and avoids a worker blocking on its own queue. Drain the legacy durable backlog through the same worker. The same worker-only durable continuation handles standalone lazy dictionary values: forward GC behind one old value can retain a new revision descriptor for every maintenance batch. Metadata-only retirement recording avoids those new epochs; legacy/physical GC waits until dictionary protection clears. Physical reclamation also checks the exact current by-ID retirement authority before releasing a numeric ID, covering replayed spools and protected-value continuations.

A connection-local admission gate excludes new native snapshot acquisition while that same connection waits for its first cleanup reservation. The worker does not need this gate, and already-admitted writers bypass it. Direct-source participants hand off only after their final writer/publication ownership ends; queue detachment is atomic with same-owner writer acquisition, and a producer admitted before a concurrent detachment is re-reserved before mutation. Both admitted streams and admission waiters have explicit limits.

Temporary transaction-delta stores use whole-directory disposal in async mode. They retain no retirement candidate list and start no additional cleanup worker; the existing model close deletes their complete store. Default durable overflow behavior is unchanged.

Use a versioned, checksummed spool bound to the existing store generation and exact shutdown native dictionary transaction ID. Startup accepts only matching provenance, establishes fresh process-local guards, and never interprets saved process-local reader revisions as restart horizons. Durable mode may process a valid best-effort spool but does not treat it as complete history.

Shutdown stops admission and scheduling, wakes waiters, and allows approximately 3.5 seconds for worker completion and spooling. A worker still using native resources at the deadline prevents environment closure; the store reports the condition and retains ownership until a later safe close. Never force-close native handles.

## Outcomes & Retrospective

The initial raw-RDF option round-trip failed as expected; the existing durable cascade test passed. The final feature selection passes all 275 cases: 157 RDF configuration, one startup lifecycle, 27 transaction integration, 26 coordinator, 17 dictionary helper, one protected lazy-value regression, and 46 existing cache/overlay cases. All three real child-process crash boundaries pass. The integration suite covers full admission, large spill streams, pinned readers, stale reintroduction, failed maintenance, duplicate acknowledgement, direct sinks, legacy format compatibility, durable backlog, and async spool import into durable mode. NONE rollback preserves already-published checkpoints; a separate SNAPSHOT rollback test verifies that unpublished candidates are discarded.

The coordinator coverage includes corruption, partial spools, duplicate replay, changed recovery bounds, interrupted admission, bounded waiters, same-owner initialization, shutdown wakeup, and bounded retry scheduling. Large-literal tests verify bounded allocation after removal of lexical copies, lazy cache-array creation, and safe optional-overlay invalidation. Regression cases exposed and fixed direct-owner reacquisition and same-connection snapshot admission races.

Observation-only prepared transactions no longer need cleanup admission, rejected reentrant shutdown leaves cleanup operational, and temporary transaction-overflow models use whole-directory disposal without an additional worker. The formatted-source broad selection executed 577 cases across 32 classes, including existing snapshot, format, GC, shutdown, model, and Sail API regressions. It had no assertion failures, one existing disabled test, and one socket fixture blocked by sandbox networking before exercising the implementation. That exact unchanged loopback-socket case passed with appropriate permission, yielding 576 passing cases and one existing disabled case. The final 100,000-triple comparisons are recorded below. A preliminary 1,000-triple smoke comparison passed every scenario; it is not used as the final performance claim.

The final synthetic 100,000-triple comparisons completed 6,080 measured commits across eight mode/scenario combinations, without failed transactions, scenario errors, RDF-content differences, admission timeouts, or reservation-bound violations. Default-sync measurements used ten measured and two warmup cycles of burst, pressure, and pinned-reader work; forced-sync pressure used five measured and one warmup cycle. Managed repository readers and explicit legacy-epoch checks prevented accidental raw-value retention from contaminating validation; measured writers used the same Sail API in both modes.

Performance acceptance is qualified. Async reduced aggregate commit p99 from 500.1 to 340.8 ms (burst), 27.1 to 23.0 ms (pressure), and 589.4 to 291.6 ms (pinned). Under default-sync pressure, full-transaction p99 increased from 28.6 to 91.7 ms and throughput fell from 70,206 to 29,097 row mutations/s, with 56.0 s of admission waiting. Forced-sync pressure reduced commit p99 from 39.2 to 19.7 ms while worsening transaction p99 from 43.8 to 150.0 ms and throughput from 35,919 to 20,049 mutations/s. Operation-specific insert tails can regress even when aggregate commit tails improve. This is not a general transaction-latency or throughput win. Raw CSVs also preserve native-delete scope, allocation, bounded reservations, spill/backlog growth, and shutdown timing; queue emptiness is not complete physical reclamation.

## Context and Orientation

The implementation lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. `LmdbStore` owns public startup settings and constructs `LmdbSailStore`. The latter serializes native publication through writer admission and publication scopes. `commitPendingPublication` commits dictionary additions before authoritative triples; `completePostCommitValueMaintenance` currently runs another synchronous recovery pass after application views release. `ValueStore` owns dictionary mappings, dependency counts, and guarded reuse. `RetiredValueIdStore` stores a durable by-ID index and ordered retirement index with boot epochs. A boot epoch distinguishes process-local triple revisions across restart.

`config/LmdbStoreConfig.java` and `config/LmdbStoreSchema.java` handle RDF serialization. Defaults must remain absent from exported configuration so old/default exports are compatible. New settings are startup-only and copied into coordinator final fields.

## Plan of Work

First create raw RDF configuration tests that fail against the unmodified implementation. Prepare Java 25 and Maven and build module dependencies with tests skipped. Capture the initial Surefire result before production changes.

Next add a package-private coordinator contract with durable and asynchronous implementations and transaction candidate streams. Unit tests use deterministic hooks and latches to verify admission, acknowledgement, rollback, spill, retry, shutdown, and spool corruption. Integrate candidate collection in all ordinary and NONE write paths, publication success handoff, rollback cleanup, and post-commit readiness after application ownership is released.

Then add a worker maintenance operation which acquires the existing publication ownership, proves reader quiescence, rechecks statement usage, uses `ValueStore.gcIds`, persists recursive work, and commits before acknowledgement. A failed native maintenance transaction rolls back and retains the same batch. Retry waiting occurs outside ownership. Keep the durable branch unchanged except for routing through the coordinator.

Finally run focused integration tests, existing retirement/snapshot suites, configuration round trips, and a reproducible same-workload comparison including 100,000 triples. Record measured rather than inferred latency, throughput, contention, native deletion, allocation, retained-work, and shutdown statistics.

## Concrete Steps

Work from `/workspace/scratch/3fb0faa31cb8/rdf4j`. Java 25 and Maven are installed under the sibling `tooling` directory. All Maven commands use `-Dmaven.repo.local=.m2_repo`. Build prerequisites with `-pl core/sail/lmdb -am -Pquick install`; never use `-am` or `-q` when running tests. Run focused tests with `-pl core/sail/lmdb -Dtest=<test selection> test`. Prefer offline execution after initial dependency downloads. Keep install output in `maven-build.log` and preserve test reports and logs.

## Validation and Acceptance

Default false retains durable retirement/recovery. Async commits never reclaim dictionary records and never report rollback because cleanup enqueue failed after publication. Rollback discards uncommitted candidates. Large transactions spill without exceeding memory limits. Pinned readers remain valid; stale work cannot reclaim recycled IDs or a different store incarnation. A full queue and recursive work cannot deadlock writers or the worker. Maintenance failure keeps candidates retryable. Shutdown wakes producers and cannot close LMDB under an active worker. Partial/corrupt spool data is ignored safely, duplicate replay is idempotent, and old durable work continues to drain.

Performance acceptance requires lower foreground commit latency on the same workload with unchanged query results. Record pressure and long-reader behavior honestly; finite admission timeout can reject a write before publication when reclamation is indefinitely pinned.

## Idempotence and Recovery

All native maintenance is transactional and may replay. Candidate files are cleanup hints, never authoritative RDF state. Temporary streams may disappear after crash. Published spools must validate completely before any candidate is eligible. Failures retain current native ownership until safe release. No dictionary format migration or new dependency is needed.

## Artifacts and Notes

Initial evidence, final reports, benchmark data, complete changed source files, and a git patch will be retained for review. Downloadable artifacts must be verified as complete before delivery.

## Interfaces and Dependencies

Use JDK collections, locks/conditions, primitive arrays, NIO file channels, checksums, and existing RDF4J/LMDB helpers only. Expose configuration `asyncValueCleanup`, `asyncValueCleanupMaxCandidates`, `asyncValueCleanupMaxBytes`, `asyncValueCleanupBatchSize`, `asyncValueCleanupMaxPendingProducers`, `asyncValueCleanupAdmissionTimeoutMillis`, and `asyncValueCleanupShutdownTimeoutMillis`. The default policy is durable; the async defaults are 65,536 candidates, 8 MiB estimated retained memory, batches of 256, up to eight admitted producers, a 30-second admission timeout, and a 3,500-millisecond shutdown effort budget.

Revision note (2026-10-30): created after inspecting both publication paths, hidden foreground eviction, existing generation metadata, and epoch-based ID protection. Implementation details will be revised as tests establish the ownership protocol.

Revision note (2026-10-30): replaced epoch pinning with a dedicated reuse guard after finding induced unbounded epoch metadata; added exact post-close transaction fencing and streamed spool recovery under changed bounds. Integrated tests exposed and clarified the pre-existing NONE checkpoint semantics.

Revision note (2026-10-30): added reader-quiescent worker fairness, actual-progress backoff, conservative boxed-workspace accounting, and an identity check inside native maintenance ownership. Updated the 178-test integration result and new lifecycle review findings; held final benchmarks until those fixes are included.

Revision note (2026-10-30): native lifecycle regressions demonstrated and fixed observation-only admission, rejected self-shutdown, implicit/explicit direct owner completion, same-owner reacquisition, and same-connection snapshot admission during a queue wait. A real pinned-value test demonstrated epoch growth from 1 to 195; worker-only durable continuations avoid it, and a current-retirement guard prevents recycled-ID hazards during continuation/spool replay. Async cache arrays now allocate lazily; optional overlays are safely retired instead of copying lexical payloads during cleanup.

Revision note (2026-10-30): completed final correctness verification and both 100,000-triple measurement invocations. Report the observed foreground-commit benefit together with admission, transaction-tail, throughput, and incomplete-reclamation limits. No tuning changed the verified production implementation to conceal pressure costs. Package the exact source, applicable patch, complete reports, and measurements for review.

Revision note (2026-10-30): the user explicitly requested commit and push. Publish the verified feature branch with the measured performance qualification and unresolved disposable-stream observation; preserve unrelated untracked files and all diagnostic evidence.
