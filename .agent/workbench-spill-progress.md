# Guarantee progress when Workbench result stores spill

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Its `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` sections must remain current.

## Purpose / Big Picture


Explore loads several result streams with a shared memory limit. Before this fix, those streams could fill the retained portion of the limit with rows and then all wait for more retained memory to write the metadata that would let the rows move to healthy browser storage. The change lets admitted rows finish spilling without acquiring more retained memory. The total accounted memory limit and the spare space for temporary write copies will remain enforced. Controlled worker tests reproduce the old stalemate and demonstrate progress, unchanged rows, and complete cleanup after the fix.

## Progress


- [x] (2026-11-04 18:14Z) Read architecture and repository test instructions.
- [x] (2026-11-04 18:16Z) Add deterministic failing shared saturation regression.
- [x] (2026-11-04 18:25Z) Implement admitted metadata ownership across workers.
- [x] (2026-11-04 18:40Z) Verify captured source and metadata lifetimes.
- [x] (2026-11-04 18:49Z) Bound serialized copies and queued writes.
- [x] (2026-11-04 18:55Z) Rerun focused suites and review invariants.
- [x] (2026-11-04 18:56Z) Report evidence and remaining validation limits.

## Surprises & Discoveries


The spill thresholds are asynchronous triggers, not admission caps. `createSpillingRowStore` continues accepting source rows while a worker is being created or copied. Before the fix, `createWorkerRowStore` reserved row-length metadata separately, and `catchUp` releases source row charges only after the complete copy. Consequently, even healthy storage cannot resolve retained-budget saturation. Reclassifying persistent metadata as temporary would merely consume the space intended to guarantee one transient transfer.

The selected checkout is at PR #6361 head `0f4f94d90f258b833e28c8ad17384bab57b0fd4c`. The added merge does not change the reviewed frontend files. The changed implementation is TypeScript with native Node tests. JVM verification is outside this frontend-only scope and is not claimed; the validation evidence uses the actual native Node runner rather than Surefire or Failsafe reports.

The accompanying E2E fix restores the Firefox Back-navigation marker and adds a graph-to-blank-to-Back browser-tracker regression. All 29 E2E helper tests pass, and Playwright discovery finds six Firefox Explore scenarios. This does not claim a full browser run.

The source-row payload also needs lifetime ownership while a worker waits for temporary memory. `catchUp` captures a slice before the worker's reservation resolves; truncation previously released those row bytes while the worker still held the objects. Index pinning alone does not cover them. The before-encoding regression now requires the original source charge and proves replacement admission remains blocked while the old copy still needs that space.

Review also reproduced a separate circular wait in temporary memory: 5,000 ordinary rows with 1,800-character literal values require a 36,719,996-byte transfer, exceeding the protected 32 MiB while four sources saturate retained admission. Each original 250-row record fits easily. A row-count-only copy bound is therefore insufficient; the copy and write-behind paths must also split by estimated serialized bytes.

## Decision Log


Decision: Reserve a source batch and its row-length metadata together at admission, using the same retained reservation. Own the metadata index in `createSpillingRowStore` and lend it to each worker attempt. Rationale: the rows and the metadata needed to spill them either both fit or neither is admitted. No additional retained allocation is needed by the copy loop; retries can use the original capacity without duplicate ownership. This is an internal ownership change and requires no new public budget API. Date/Author: 2026-11-04, Codex.

Decision: Keep metadata in variable-length arrays corresponding to accepted input batches, independent of worker-copy chunking. Charge 16 bytes per row plus 160 bytes per backing array before allocating it. Retain a partial array's complete charge after truncation until that array is removed. Rationale: input batches and 5,000-row copy chunks need not align, and the reservation must describe the actual long-lived arrays, not the number of worker messages. Date/Author: 2026-11-04, Codex.

Decision: Populate preallocated metadata slots during actual JSON encoding, not using a weaker read estimate. Rationale: read-window validation must continue to use the exact lengths of serialized rows, and worker errors or retries must not weaken oversized-response checks. Date/Author: 2026-11-04, Codex.

Decision: Find the first overlapping metadata batch by binary search, then stop at the window end. Rationale: a row store can admit many single-row input batches, and scanning all earlier batches on each write would make sustained writes quadratic. A regression exercises 10,000 single-row admissions followed by 1,000 write-behind appends. Date/Author: 2026-11-04, Codex.

Decision: Pin only references to overlapping backing arrays while a worker write is pending, and include their capacity in the 160-byte per-batch admission overhead. Rationale: pinning must itself remain accounted while a write waits for temporary memory. The copy and write-behind loops allow only one active writer per store, including retries; a simple reference snapshot avoids a new object per overlapping batch. Detached arrays remain charged until that writer releases them. Date/Author: 2026-11-04, Codex.

Decision: Defer releasing discarded source bytes while a copied worker write is active, and retain disposal's source charges until worker closure. Rationale: the original admission already owns these row objects; a copy must not need another retained allocation, but truncation cannot release its ownership while the worker still references the old rows. Conservatively retaining all removed source bytes until the active copy settles avoids extra per-row accounting arrays. Date/Author: 2026-11-04, Codex.

Decision: Calculate a worker write's size before pinning index slots. Rationale: synchronous estimation can throw for an accessor-backed row, so no unreleasable pin may be created before that fallible step completes. Date/Author: 2026-11-04, Codex.

Decision: Bound each worker write by both the existing row limit and the budget's protected temporary bytes, splitting admitted input batches when needed. Rationale: multiple individually safe records may coalesce into a transfer that needs space occupied by their own retained sources. Use the same temporary-reserve calculation as `SharedRowStoreBudget` and account partial source batches after successful writes. A single row is indivisible; allow one row through the normal transfer-budget request so existing oversized/resource-limit errors remain explicit. Date/Author: 2026-11-04, Codex.

Decision: Assign a pending write chain before sizing or reserving a stored write. Rationale: a budget grow can synchronously invoke pressure handlers, so `drain` must already appear active and must not recursively start duplicate writes while waiting for temporary capacity. A regression previously observed 656 pending requests and a stack overflow where exactly one request should exist. Date/Author: 2026-11-04, Codex.

Decision: Use a 160-byte fixed metadata-array allowance, beyond the 16 bytes per row. Rationale: this covers the index entry and its single writer-reference snapshot with additional margin for array growth under tiny input batches; ordinary records amortize that fixed cost. Date/Author: 2026-11-04, Codex.

Decision: Use explicit worker-create and append gates with normal Node turn draining, rather than delays or storage timeout races. Rationale: the regression should show inability to begin a healthy write before any timeout, and cleanup should cancel pending allocations deterministically. Date/Author: 2026-11-04, Codex.

## Outcomes & Retrospective


The implementation and regression coverage are complete. Four saturated stores spill without a new retained allocation; coalesced initial and write-behind copies respect protected temporary headroom and can split an admitted input batch. Captured source rows and metadata remain charged through truncation, retry and worker closure. The change preserves the public budget API, total and retained limits, exact serialized-length validation, and owned-window lifetime rules.

All 19 new regressions, 72 focused storage tests, the classic TypeScript build and all 996 classic frontend tests pass. The full suite was rerun after the final conservative sizing adjustment to 160 fixed bytes per metadata batch. Independent review found no remaining actionable defect and repeated its default-budget, source-lifetime and pressure-handler scenarios on that exact production snapshot.

The progress guarantee is specific: accepted rows already own persistent spill metadata, and splittable copies fit the protected temporary reserve, so they never wait for bytes that only finishing their own copy would free. A single oversized row remains indivisible and is attempted alone under the existing total-budget and timeout rules. Independent consumers retaining temporary memory or unavailable storage can still delay or fail writes. Memory figures remain conservative allocation estimates, not measurements of a browser engine's physical heap.

## Context and Orientation


`tools/workbench/src/main/frontend/classic/queryStream.ts` contains the public `RowStore` API, `SharedRowStoreBudget`, the memory-first wrapper `createSpillingRowStore`, and the private worker adapter `createWorkerRowStore`. A retained reservation accounts for data that outlives a single operation. A temporary reservation accounts for a bounded encoding, transfer, or read operation. The shared budget limits retained allocations to the total budget minus the smaller of half the budget and 32 MiB, leaving room for a temporary write.

The wrapper stores source rows while a worker opens and copies them. Once caught up, it continues buffering newly accepted batches and writes them in the background. The worker adapter keeps serialized row lengths so it can reserve enough memory for a requested read before transferring or parsing its JSON text. Owned windows are explicit handles whose reservations survive until their consumer calls `release`, even if the original store has been disposed.

`tools/workbench/src/test/frontend/classic/query-load-more-harness.js` compiles the current source with TypeScript into a fresh temporary directory and exposes the public API in a browser-like Node VM. Its `MemoryWorker` implements the row-store message protocol without IndexedDB. `workbench-explore-budget.test.js` covers shared budgets, pending allocations and owned windows. Related `row-store-memory-first.test.js`, `row-store-write-behind.test.js`, and `row-store-text-blocks.test.js` cover spill/retry/worker behavior. `tools/workbench/src/test/frontend/classic/row-store-spill-budget.test.js` contains the deterministic progress and lifecycle regressions.

## Plan of Work


First add a gated `MemoryWorker` and an observed-budget wrapper in the frontend test directory. Four stores will accept normal-sized batches while create replies are held. Releasing the replies must let writes start and all appends finish without timeout or budget overflow. The unmodified implementation must fail this assertion. Save its native test report in the checkout and the decisive excerpt in this plan before editing production.

Then introduce a private row-length index in `queryStream.ts`, with append admission sizing, actual serialization into existing slots, read-length estimation, truncation and disposal. `createSpillingRowStore.appendNow` will grow one reservation by source bytes plus metadata bytes, allocate the index batch only when rows are accepted, and undo the full pending charge on failure. `catchUp` and `drain` will release source-byte charges but preserve index charges. Every worker attempt receives the same index; the adapter no longer allocates retained row-length arrays. Worker reads continue to validate their returned text against reserved size before parsing.

Audit truncation during moving, especially a worker append that overlaps a newer source truncation and append. The index's backing arrays must not be released while an encoding still needs them, and replacement rows must get their own exact lengths. Audit append rejection, a worker that fails once and retries, both attempts failing, waiting budget allocation followed by disposal, and owned windows outliving a spill and disposal. Keep all user-visible row data and error contracts intact. `copyRows` must retain discarded source charges until its copy settles, while disposal uses worker closure as its terminal release even when unload abandons promises.

Use `writeEnd` to bound each shared-budget copy by its estimated serialized transfer bytes and the existing row limit. Derive the byte bound from `rowStoreTemporaryReserve`, also used by `SharedRowStoreBudget`. For a partial source batch in `drain`, debit only the successfully written source bytes and retain the remainder's original absolute end. Publish the pending write promise before its body can request pressure relief, preventing synchronous re-entry. Do not split or discard a single indivisible row; keep the existing transfer-budget rejection, retry and fallback behavior.

Finally run the new regression file and the existing storage/budget suites, compile the complete classic TypeScript project, and inspect the diff for scope and formatting. Keep the patch scoped to the result-store implementation, its regression tests, and this plan. Preserve unrelated files and untracked artifacts. Review the final diff and validation results before publication.

## Milestones


The first milestone produces a small test that fails specifically because no worker append begins after retained quota fills. It uses no faulty browser storage and must leave no reservations after cleanup, even on the old implementation. The second milestone changes admission and ownership so the same test passes while a budget observer proves every allocation remains within both retained and total limits. The third milestone establishes retry, truncation and disposal behavior with adversarial worker ordering and runs the existing frontend storage suite. Each milestone's commands and actual results will be added below.

## Concrete Steps


For a normal checkout with the project's existing dependencies installed, run from the repository root:

    npm --prefix tools/workbench run build:classic
    PATH="$PWD/tools/workbench/node_modules/.bin:$PATH" node --test tools/workbench/src/test/frontend/classic/row-store-spill-budget.test.js
    PATH="$PWD/tools/workbench/node_modules/.bin:$PATH" node --test --test-concurrency=4 tools/workbench/src/test/frontend/classic/*.test.js

The focused test harness compiles the current source itself. Other classic tests also use the complete build output. In the isolated validation checkout, the existing pinned TypeScript 6.0.3 and type packages were used without adding dependencies, and generated build output stayed outside the repository:

    export PATH=/tmp/pr6361-review-tools/node_modules/.bin:$PATH
    tsc -p tools/workbench/tsconfig.classic.json --outDir /tmp/pr6361-review-built
    WORKBENCH_SCRIPT_DIR=/tmp/pr6361-review-built node --test --test-concurrency=4 tools/workbench/src/test/frontend/classic/*.test.js

An initial broader test attempt omitted `WORKBENCH_SCRIPT_DIR`: 45 tests passed and eight reported missing compiled worker fixtures. Rebuilding classic and setting the supported output-directory override resolved those setup failures. The completed full classic run on the final 160-byte metadata snapshot passed all 996 tests with no skips or cancellations.

## Validation and Acceptance


The four concurrent stores must leave memory successfully with healthy gated workers, including when ordinary input batches do not align with copy chunks. Every admitted row must be readable in order. The observed retained allocations must never exceed the retained limit, and total allocations must never exceed `budget.maxBytes`. After releasing owned windows and disposing stores, `budget.usedBytes` must be exactly zero.

A failed first worker must not consume duplicate metadata capacity; its replacement must finish using the original admitted index. Truncating during a copy and then appending different rows must not expose stale row lengths or stale rows. Disposing a store while a retained allocation or a worker write is pending must reject accepted work appropriately, ignore late replies and release all owned reservations. Existing read-size validation tests must still reject oversized worker responses before `JSON.parse`.

## Idempotence and Recovery


Tests create only isolated in-memory workers and temporary compiler output. Repeat them freely. Preserve existing untracked artifacts, including the pinned `node_modules` symlink. If a test fails, inspect its exact gate and budget snapshot rather than increasing timeouts. If the implementation direction changes, update this plan before editing. A production patch may be made only after the new regression has been observed failing.

## Artifacts and Notes


Initial failure observed before production edits, with the full native report at `/tmp/pr6361-spill-initial.tap`:

    Command: PATH=/tmp/pr6361-review-tools/node_modules/.bin:$PATH node --test tools/workbench/src/test/frontend/classic/row-store-spill-budget.test.js
    tests 1
    pass 0
    fail 1
    AssertionError: every healthy worker must start writing even when source rows fill retained memory

The failing test held only worker creation replies, used ordinary 250-row input batches and allowed no storage timer to fire. Its cleanup assertion passed with zero remaining bytes. The native output is also preserved in the checkout's `initial-evidence.txt`.

An additional test-first lifecycle check exposed unused metadata after a failed stored truncate discarded its unwritten suffix. Before that correction, `/tmp/pr6361-spill-truncate-initial.tap` reported:

    tests 1
    pass 0
    fail 1
    AssertionError: discarded unwritten rows cannot leave unused metadata charged after a failed disk truncate
    2992 !== 2528

The final implementation discards that suffix's metadata at the same point as its source rows, before attempting the stored-prefix truncate. The regression passes and subsequent appends remain readable.

The final classic frontend result is preserved in `/tmp/pr6361-classic-all-final-160.tap`:

    tests 996
    pass 996
    fail 0
    cancelled 0
    skipped 0
    duration_ms 104435.302452

The focused six-file storage selection passed 72 tests, including the 19 new regressions. Source-lifetime and transfer-size extensions were also observed failing before their production edits:

    /tmp/pr6361-spill-source-initial.tap: 2 tests, 0 pass, 2 fail
    captured-source truncation charge: 2043487 !== 2097152
    pending stored-write disposal charge: 50744 !== 61953
    /tmp/pr6361-spill-estimate-initial.tap: 1 test, 0 pass, 1 fail; leaked pin 144 !== 0
    /tmp/pr6361-spill-chunk-initial.tap: 2 tests, 0 pass, 2 fail
    initial catch-up wrote 0 rather than 1000 rows; stored drain wrote 2 rather than 1002
    /tmp/pr6361-spill-reentry-initial.tap: 1 test, 0 pass, 1 fail; pending requests 656 !== 1

Independent final-snapshot checks used the default 256 MiB budget with four stores, 250-row input records and 1,800-character literals. All 150,000 rows were verified in order after 102 writes. The largest transfer was 33,547,388 bytes, below the 33,554,432-byte protected reserve. Peak accounted total was 268,412,348 of 268,435,456 bytes, and peak retained memory was 234,864,960 of 234,881,024 bytes. Disposal returned the budget to zero. A blocked-copy probe retained all 32,350 admitted source/metadata bytes after truncation until its 100 captured rows were encoded; a pressure probe had exactly one pending writer. Both also returned to zero on cleanup. The independent 19-regression plus eight-browser-health selection passed 27 tests.

The final reviewed production SHA-256 is `4d60007cec7b736b1fc1451b7f33fa36c4c6fc5136e2ee378bcd49be45affc14`. Independent logs are `/tmp/pr6361-independent-final-validation.log` and `/tmp/pr6361-independent-final-focused.log`; the large positive control is `/tmp/pr6361-independent-final-validation.js`.

The 19 new regression tests cover four-way shared saturation, different input/copy boundaries, already-spilled writes, retry ownership, two failed attempts, truncation during and before encoding, a partial input array, pending admission/transfer disposal, owned windows, failed truncate cleanup, rejected admission, many single-row batches, captured source ownership, synchronous estimate failure, abandoned unload writes, coalesced transfer limits, partial input writes, pressure-handler re-entry and indivisible-row limits. Every scenario checks bounded accounting and final cleanup. Existing worker read tests continue to reject an oversized response before parsing.

No JVM, full browser, or live IndexedDB validation is claimed for this frontend storage task. The accompanying E2E work has helper-test and scenario-discovery evidence, not a full Playwright browser execution.

## Interfaces and Dependencies


Keep the existing public `RowStore`, `RowStoreBudget`, `RowStoreBudgetReservation`, and `RowStoreOptions` contracts. Use only the current TypeScript source and Node built-ins. The private `RowStoreTextIndex` is passed from `createSpillingRowStore` to `createWorkerRowStore`; the long-lived metadata arrays belong only to that index. Its methods are `appendBytes(count)`, `append(count)`, `writer(start, count)`, `textCharacters(first, end, maximum)` and `truncate(count)`. A `RowTextWriter` exposes `encode(rows)` and idempotent `release()`; it captures existing batch references rather than allocating another set of row lengths. The temporary transfer reservation remains scoped to one worker append, and owned-read reservations retain their existing independent ownership.

Revision note (2026-11-04 18:14Z): Created the plan before any production change, selecting atomic admission and one shared metadata index to remove the circular retained-memory dependency.

Revision note (2026-11-04 18:16Z): Recorded the deterministic initial failure and advanced the sole active step to implementation.

Revision note (2026-11-04 18:27Z): Recorded successful implementation and lifecycle coverage, bounded metadata lookup, and a smaller pinned representation with explicit upfront capacity. Corrected JVM scope and made the plan maintainer-facing.

Revision note (2026-11-04 18:32Z): Recorded all 12 deterministic lifecycle tests, 989 passing classic tests, the failed-truncate correction and exact remaining validation limits. Added portable project commands and preserved environment-specific evidence.

Revision note (2026-11-04 18:36Z): Reopened lifecycle verification after review demonstrated uncharged captured source payload during a blocked copy. Strengthened source-lifetime assertions before implementing deferred release; also recorded the synchronous-estimation pin-leak regression.

Revision note (2026-11-04 18:44Z): Added a test-first byte-bound milestone after ordinary records reproduced a second shared-budget progress stall when coalesced writes exceeded protected temporary space.

Revision note (2026-11-04 18:52Z): Completed source lifetime, byte-bounded chunking and pressure re-entry corrections with test-first evidence. The 996-test suite passed; increased the per-input-batch estimate to 160 fixed bytes before one final build and full rerun.

Revision note (2026-11-04 18:55Z): Recorded the final 996/996 stable-snapshot result and independent default-budget validation, including exact source/metadata ownership, bounded transfers and zero cleanup balances. No actionable review finding remains.

Revision note (2026-11-04 18:56Z): Finished the maintainer handoff and froze production/tests. After three test-only line wraps, the same 19 regressions passed again; production is unchanged from the independently reviewed and full-suite-tested SHA-256 above. All validation processes are terminal.
