# Workbench large-result handoff

Final verification snapshot, 2026-09-30. Work remains on `GH-6071-workbench-redesign` at `d6e9ea70a547b48942106cec515cdb6839c21728`. No stage, commit, push, branch change, or artifact cleanup was performed. The complete machine-readable source, fixture, preview, test, and screenshot record is `/tmp/rdf4j7-workbench-loadmore-20260930/final-acceptance-manifest.json`.

## Behavior and ownership

Execute sends the user's original query unchanged. It streams the first 1,000,000 rows, then drains the same evaluation to exhaustion. Progress reports processed rows and elapsed time every fixed additional 100,000 rows or three seconds, including while waiting for the next item; successful completion supplies the exact total and full elapsed time. Load more reruns the same frozen query, locally discards the already loaded prefix, and appends only new rows. No Workbench-generated LIMIT/OFFSET is inserted into SPARQL. Query-result Next/Previous, page-size and Records page controls are absent; unrelated repository/resource/export paging remains separate.

Results use a worker-owned IndexedDB store with bounded variable-height Table and Records windows. New stores use bounded `blocks-v1` rows; legacy flat rows remain compatible. The query-only compact stream negotiates native browser compression; measured zstd level 1 is preferred, with quality-aware fallback. Downloads and generic Workbench page responses keep their separate contracts.

The complete source/caller/finite-state matrix is `source-inventory.md`; exact framing, progress and storage semantics are `transport-contract.md`. Initial failing evidence remains append-only in repository `initial-evidence.txt` and the logs/reports under `/tmp/rdf4j7-workbench-loadmore-20260930/`.

## Scope and validation

The final mobile production change removes only the `max-width:640px` `margin-inline-start:0` override that displaced the shared disclosure anchor in `tools/workbench/src/main/webapp/styles/workbench-refresh.css`. The six-case in-repo RED reproduced the issue for main and saved query results in Chromium, Firefox and WebKit. The same six cases pass after the fix, asserting arrow-to-trigger alignment, Download and Options containment, exactly one streamed query and no page errors. Root accepted both captured mobile screenshots.

The final package checks passed: Workbench 737/737; HTTP client 137 total, 8 skipped, 129 executed passes, zero failures/errors; frontend Node suite 269/269; strict TypeScript compilation; copyright header check; offline root formatting; and the server-boot quick install/package. Generation `20260930-123802` serves 42/42 matching CSS/JavaScript assets. The updated full-query browser matrix passes 42/42 and the packaged block-storage suite 30/30 across Chromium, Firefox and WebKit on generation `20260930-123738`; the only later source change is the CSS override removal, and its directly affected native selection passes 6/6 on `20260930-123802`.

## Full-query acceptance

The timeout-zero deterministic query returns exactly 1,201,000 rows. Initial Execute streams 1,000,000, reports progress at 1.1m and 1.2m, and then reports exact total and complete query time. The first request uses the original query with `batch-size=1000000` and `batch-offset=0`; Load more uses the same frozen query with `batch-offset=1000000` and appends only the remaining 201,000 rows into the same result-store owner. First/middle/final seeking in Table and Records caused no extra query. Native response headers show compact-v2 NDJSON, zstd and `Vary: Accept, Accept-Encoding`.

The primary telemetry is `/tmp/rdf4j7-workbench-loadmore-20260930/full-query-reset-table-20260930-140121/`. It records first-window fetch end at about 2.444 seconds, completion after storage consumption at about 2.572 seconds, and 1,143,851 encoded versus 107,979,705 decoded bytes. The 201k remainder uses 232,908 encoded versus 21,738,561 decoded bytes and completes in about 0.726 seconds. After append, 17 data rows and 191 result nodes are mounted; the physical table scroll range reaches about 33.55 million pixels. A main-thread heap snapshot is about 12.6 MB, maximum sampled event-loop gap is 12.9 ms, and no long task was recorded. There is no public worker heap sample; these values are snapshots, not peaks, and response-consumption time is not server CPU time.

Re-Execute disposes result store `rows-muo21jcs-dc6v3m1qn48` in about 133 ms end-to-end (about 53.6 ms in worker disposal). The zero-row page-model store `rows-muo21j3i-rknz8erfkyg` is a separate owner. Pagehide cleanup for that separate page-model store remains unproved. A separate saved-query 1.201m completion is recorded in `/tmp/rdf4j7-workbench-loadmore-20260930/saved-large-query-20260930-141049/`; it is not mixed into primary timings.

## Final preview, fixtures, screenshots

Preview: `http://127.0.0.1:18817/rdf4j-workbench`; Server: `http://127.0.0.1:18817/rdf4j-server`. Generation is `20260930-123802`, PID 13226, JAR SHA-256 `4653c19b8bfc442cd979d556882680da007f05a485d313dabc5b4916eb573162`. It uses the retained appdata at `/private/tmp/rdf4j7-workbench-spacing-20260929/runtime/appdata` and remains available for review.

The retained review repository `spacing-review-50467-mun6z6qr` kept its configuration and was verified empty after restart; its 120 volatile MemoryStore statements were restored through the supported seed script and verified. The owned performance repository `workbench-loadmore-source-20260930-mz3b2` remains retained. Saved query URN `urn:uuid:d8a355a7-47e4-4449-860e-886a0db430af` is preserved. The six anchor tests create and dispose unique temporary repositories. No optional orphan cleanup or user-repository mutation was performed.

Root-review screenshots:

- Main query mobile Options: `/tmp/rdf4j7-workbench-loadmore-20260930/options-anchor-final-review/chromium-main-query-mobile-options.png`
- Saved query mobile Options: `/tmp/rdf4j7-workbench-loadmore-20260930/options-anchor-final-review/chromium-saved-query-mobile-options.png`
- Main result after full query: `/tmp/rdf4j7-workbench-loadmore-20260930/full-query-reset-table-20260930-140121/screenshots/million-main-query-final.png`
- Saved large query: `/tmp/rdf4j7-workbench-loadmore-20260930/saved-large-query-20260930-141049/saved-large-query-complete.png`
- Live progress, first-window complete, appended records, mobile and reset views are adjacent under the primary full-query folder.

The historical default-60-second object/v1 query ended with partial rows and timeout; it is not successful million-row evidence. The first offset-protocol 410-second storage run is also historical and superseded. The successful 1.201m run above uses the current original-query drain contract. The broad 21/24 compatibility report remains unchanged: the controlled six-case anchor regression resolves the actual Options arrow defect but does not relabel those older offscreen-focus cases as a full-suite pass.
