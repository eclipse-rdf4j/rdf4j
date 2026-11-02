# Persistent adjacency: integration follow-up

## Quoted terms held across map growth

The original `LmdbAutogrowSingleCommitIT.singleQuotedCommitCompletesWithHeldReadCommittedReader` failed after its writer successfully committed: the next `hasNext()` on the old raw dataset raised `MapResizeConflictException`. The same failure was reproduced locally on published `892b26cf72b23cc9311eb7cc7c693fcdad866d7e` (one test, one error).

A low-level `SailSource.dataset(READ_COMMITTED)` is not the renewable raw `ValueStore.getTripleTerms()` cursor. The dataset owns a paired native snapshot and declares dictionary term membership when `getTriples()` is used. Once that observed membership snapshot is invalidated by map growth, the caller must retry the entire read. These lifetime rules predate the persistent-adjacency implementation. Silently renewing the old cursor would mix membership epochs.

The test now retains the same single-commit workload and 60-second deadline. It proves actual post-seed growth using both metrics and native map size/mapping generation, requires a typed conflict identifying a changed environment, closes the old dataset, and retries the complete term read. It also verifies the escaped lazy term, exactly 65,536 statements and 32,768 terms, and cold-reopen equivalence. The existing value-only invalidation test now covers both `SNAPSHOT_READ` and `READ_COMMITTED`. The separate renewable raw-value cursor test is unchanged. There is no production isolation change.

The first expanded oracle timed out in AssertJ's quadratic unordered element matcher. A captured JUnit timeout stack identifies that exact frame. Model equality and a term List-size-plus-HashSet-equality proof retain all membership and duplicate checks without pairwise quadratic matching. The focused corrected integration test passes in 37.45 seconds under its unchanged deadline. The complete five-case integration class and both value-only isolation controls subsequently pass (seven cases, no failures/errors/skips).

## Full-theme initial import remains under investigation

The old CI integration log stopped after `Loading theme dataset: EXPLORATION`, before the loader reported completion. The original generators emit 14,772,659 statements; the final `ANALYTICS` and `EXPLORATION` themes emit no statements. The silence therefore localizes to final initial-NONE commit or the immediately following connection close, not those virtual themes or later query readiness. The old log has no post-commit marker, so it does not distinguish those two phases.

Three bounded diagnostic controls use the real importer, retain exact independent live/reopen membership checks, and all succeed:

| Control | Unique rows | Final commit |
| --- | ---: | ---: |
| Prefix sampling, 1 GiB maps | 109,998 | 1.673 s |
| Region sampling, forced 1 MiB sort runs | 549,994 | 7.590 s |
| Region sampling, 16 MiB initial maps and small runs | 110,000 | 1.381 s |

These controls do not establish full-scale success or identify the CI stall's root cause. The small-map control did not separately capture before/after native sizes, so it is not an independent proof of a particular final-growth branch. Full-scale local replay is constrained by available disk; no evidence or unrelated files were deleted to make room. Test-only phase and bounded thread diagnostics are intended to distinguish actual merge/compaction CPU work, GC pressure and blocked ownership on the unchanged full CI workload. No speculative production fix or timeout relaxation is justified by the smaller passing controls.

Local evidence: `repair-evidence/quoted-held-reader-red*`, `quoted-held-reader-oracle-stack*`, `quoted-held-reader-linear-green*`, `quoted-reader-contract-controls*`, and `smoke-commit-diagnostic/summary.json`. The archived full CI log is `/tmp/6352-integration-124998589571.log`.

The published diagnostic scope is test-only: ten scheduled snapshots, at most32 platform threads/64 frames per thread/32 run files and64KiB per record, plus the direct caller stack (including a virtual caller). It does not take store/native-reader locks or change CPU-accounting settings. Scope close cancels and joins its daemon with a bounded cleanup failure, preserving any original workload exception through suppression. Six lifecycle cases pass, including throwing optional counters and deliberately blocked output. The combined gate is15 distinct cases; the final six-case rerun overlaps it. Full-scale CI evidence is still required.
