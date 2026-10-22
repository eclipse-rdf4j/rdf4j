# Workbench reads use the LMDB fast paths, page Explore by streaming, and cache Types and Graphs results

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

The RDF4J Workbench (module `tools/workbench`) browses a repository through four pages that read a lot of data: Summary (statement and graph counts), Types (every `rdf:type` class with its instance count), Named Graphs (every graph with its statement count) and Explore (every statement that mentions one resource). On a large LMDB repository these pages either time out after two seconds or, for Explore, read every matching statement twice for every page shown. After this change:

1. Every repository read the Workbench makes reaches the LMDB store's in-memory fast paths when the store has them: the "direct adjacency" index (an in-memory copy of the statement indexes, described below) and the "value overlay" (an in-memory compressed copy of the value dictionary, described below). Today the value overlay is skipped when the store turns a value such as an IRI into its internal number, so every Explore request and every SPARQL constant does a disk lookup even when the overlay could answer.
2. The Explore page shows its first page at once. It streams statements in the store's own (stable) index order, stops reading as soon as the page is full, and asks for the total count separately so the count never holds back the rows.
3. Summary counts wait five seconds instead of two. Types, Named Graphs and Explore give every operation five minutes.
4. Types and Named Graphs ask the LMDB store to cache their query results. The cache is valid until the next write to the store, so a second visit to either page is instant, also on a remote server.

To see it working, start the Workbench against an LMDB repository with a few hundred thousand statements, open Types twice (the second load is immediate), open Explore on a class IRI with many instances (the first rows appear immediately and "of N" fills in later), and run the tests named in "Validation and Acceptance".

## Progress

- [x] (2026-10-22 11:40Z) Research: census spike `LmdbWorkbenchAccessCensusTest` measured which repository calls consult adjacency (see Surprises).
- [x] (2026-10-22 11:52Z) M1: value→id lookups for read snapshots consult the value overlay's reverse index. `LmdbSnapshotIdOverlayTest` failed first (2 and 4 dictionary reads), passes after; 395 overlay/snapshot/adjacency tests green.
- [x] (2026-10-22 11:58Z) M2: the `lmdb-result-cache` query hint travels from the Workbench through HTTP and the server to the store (`AbstractHTTPQuery.applyForcedLmdbExecutionStrategy` renamed `applyLmdbQueryHints` so each query overwrites both sticky session hints). Tests: `RDF4JProtocolSessionResultCacheTest`, `SailRepositoryConnectionTest`, `DefaultQueryRequestHandlerTest`.
- [x] (2026-10-22 12:02Z) M3: the LMDB store caches hinted tuple-query results until the next write (`LmdbQueryResultCacheTest`, 8 tests).
- [x] (2026-10-22 12:40Z) M4: Types and Named Graphs use the hint, five-minute budgets, and a shared in-flight count per query; Summary uses five seconds. Named Graphs uses the cached queries only for LMDB-backed repositories (see Decision Log revision). Tests: `TypesAndGraphsPageDataTest` (12, incl. two LMDB cache tests), `BrowseListCountsTest` (5, incl. shared counts), Summary/Clear/BrowseCounts suites green.
- [x] (2026-10-22 13:10Z) M5: Explore streams its page in index order, stops after the page plus one look-ahead statement, and counts separately within five minutes (one `COUNT` query on LMDB, a streamed count elsewhere). `ExploreServletCoverageTest` (15, incl. "reads 301 statements for offset 200 limit 100 of 10,000"), `ExploreServletTest` (11), `ExploreServletPageDataTest` (46) green.
- [x] (2026-10-22 13:25Z) M6: front end shows the new budgets and Explore's deferred total (`data-has-more` marker, "of …" until `counts=true` answers, Next enabled by has-more, a stale total cookie never stands in). Classic JS suite 867/867.
- [x] (2026-10-22 14:55Z) M7: Spotless check clean; separate in-flight limit for page operations; consolidated verification: core/http/protocol 37, core/sail/api 104, core/http/client 187 (8 skipped), core/repository/http 14, core/repository/sail 54, tools/server-spring 205, core/sail/lmdb 9,486 (34 skipped), tools/workbench 1,330, classic JS 867, explorer JS 646 — all without failures. Playwright e2e not run.

## Surprises & Discoveries

- Observation: every pattern-bound `getStatements`, `hasStatement` and `size(context)` already consults the direct adjacency index through the statement access arbiter; whole-store `size()` is answered by LMDB's own entry count in constant time and `getContextIDs()` by the small per-context counter table, both cheaper than adjacency.
  Evidence (census, store with indexes `spoc,posc`, adjacency PREFER, 8,000 statements; `adjHit` = adjacency lookups served):

      call                   result  adjHit adjMiss    micros  fallback deltas
      size()                   8000       0       0       313  -
      size(g3)                  286       1       0      1451  -
      size(default)            6000       1       0      1012  -
      getContextIDs()             7       0       0       753  -
      stmts(s,_,_)                4       1       0      1082  -
      stmts(_,p,_)             2000       1       0      8020  -
      stmts(_,_,o)                1       1       0       407  -
      stmts(_,_,class)         2000       1       0      5505  -
      stmts(_,_,_,g3)           286       1       0      1476  -
      has(_,_,o)                  1       1       0       273  -
      types distinct              1       1       0      7675  -
      types counts                1       1       0      8603  -
      graph counts                7       7       0      8308  -

- Observation: the value overlay is consulted when a stored number is decoded into a value (statement iterators, context iterators, the native SPARQL engine), but not in the other direction. `ValueStore.getStatementId(ReadSnapshot, Value)` ends in the private `ValueStore.findId(ReadSnapshot, long txn, byte[])`, which is a plain `mdb_get`; only the non-snapshot `findId(byte[], boolean, ...)` asks `borrowValueOverlay(txn).findId(data)` first. Every `getStatements`, `size(context)` and SPARQL constant resolves its values through the snapshot path.
- Observation: `HTTPRepositoryConnection.getStatements` collects the whole response into a `StatementCollector` before returning, so the Explore page against a remote server loads every matching statement into Workbench memory for each of its four role scans, twice per page (once to count, once to render). `exportStatements(..., RDFHandler, ...)` streams instead, and when the handler throws, `SPARQLProtocolSession.getRDF` closes the Apache HttpClient 5 response without draining it, which aborts the transfer.
- Observation: the named-graph count query `SELECT ?g (COUNT(*) AS ?n) WHERE { GRAPH ?g { ?s ?p ?o } } GROUP BY ?g` is evaluated by one scan over the direct adjacency (`StatementPattern [index: direct-adjacency]`), which is no slower than one `size(context)` per graph (each a scan of its own) and needs a single HTTP request.
- Observation: `TripleStore.dataRevision` is set in the `finally` of the commit, after the LMDB commit and before `commit()` returns to the writer. A reader that reads the revision before opening its snapshot and again after finishing can therefore tell whether a commit happened in between.

- Observation: the deprecated `IterationWrapper.hasNext()` closes the wrapper as soon as the source is exhausted, so a recorder built on it drops its rows in `handleClose` before it can see the end. The recorder extends `AbstractCloseableIteration` and checks the source itself.
  Evidence: first run of `LmdbQueryResultCacheTest`, 5 of 8 failing with `stores()` 0.

- Observation: in this branch the Java formatter is Spotless, bound to `process-resources`; the `formatter-maven-plugin` and `impsort-maven-plugin` goals are not downloaded. `spotless:check` is the format gate.
- Observation: `HTTPRepositoryConnection.getStatements` buffers, but `exportStatements` streams and a handler exception closes the HTTP response unread, so Explore pages use `exportStatements` with a stop-after-page handler.
- Observation: an existing test (`BrowseCountsPerRepositoryTest.remoteRepositoriesAreCountedWithTheirSizeAndOnlyTheFilteredGraphsAreCounted`) records why graph counts use `size(context)`: a repository on a server may be a federation (a COUNT query streams every member's statements) or a SPARQL endpoint proxy (it misreads the default graph).
- Observation: a caller that reads the one row of a COUNT result and never asks `hasNext()` again leaves the cache recorder without its end, so nothing is cached. The Workbench reads such results to the end.
  Evidence: `anLmdbRepositoryCountsItsGraphsWithCachedQueriesThatMatchTheGraphSizes` first saw 1 hit per request instead of 2.

## Decision Log

- Decision: do not replace the arbitrated `getStatements`/`size(context)` paths. They already consult adjacency, and the arbiter's cost model is exactly the "use adjacency when it is likely faster than disk" rule the request asks for. Close the one gap found (value→id through the overlay) instead.
  Rationale: census evidence above; changing arbitration would affect every query, not only Workbench reads.
  Date/Author: 2026-10-22, Claude Code.
- Decision: answering `size(context)` from LMDB's per-context counter table (constant time, but only exact when the store holds no inferred statements) is out of scope and recorded as a follow-up.
  Rationale: it is not an adjacency or overlay path, its correctness depends on counter upkeep in every write path, and the Named Graphs page moves to one cached SPARQL query anyway.
  Date/Author: 2026-10-22, Claude Code.
- Decision: the cache is requested per query with a new experimental hint, `lmdb-result-cache=true`, carried exactly like the existing `lmdb-forced-strategy` hint (query parameter → `RDF4JProtocolSession` → server `ProtocolUtil` → `SailQuery` → `SlowQueryContextHolder` → store). Stores other than LMDB ignore it.
  Rationale: reuses an existing, tested hop-by-hop pattern; the request asks for the pages to "trigger" the cache, so it must be opt-in.
  Date/Author: 2026-10-22, Claude Code.
- Decision: the cache records rows while the caller reads them and keeps the entry only when the caller read to the end, the entry stayed within the row limit, and the store's data revision did not change between opening and finishing. A hit is served only while the revision is unchanged.
  Rationale: keeps streaming and the query time limit intact (no eager buffering inside `evaluate`), and the revision ordering in Surprises makes stale hits impossible after a writer's `commit()` returns.
  Date/Author: 2026-10-22, Claude Code.
- Decision: the Named Graphs counts become two cached SPARQL queries evaluated with inferred statements excluded (`size` counts explicit statements only): `SELECT ?context (COUNT(*) AS ?statements) WHERE { GRAPH ?context { ?s ?p ?o } } GROUP BY ?context` and, for the default graph, `SELECT (COUNT(*) AS ?statements) WHERE { GRAPH <http://rdf4j.org/schema/rdf4j#nil> { ?s ?p ?o } }`. The graph list keeps `getContextIDs()`.
  Rationale: one or two requests instead of one per graph, served by adjacency when available, and cacheable; `getContextIDs()` reads only the per-context table and is already cheap.
  Date/Author: 2026-10-22, Claude Code.
- Decision: Explore pages through `exportStatements` with a handler that skips `offset` rows, keeps `limit` rows plus one look-ahead row, and then stops; the total comes from a separate `counts=true` request that runs one SPARQL `COUNT` with the resource bound as `?r`. Keyset (seek) paging was considered and not done.
  Rationale: `exportStatements` keeps the context of each statement (SPARQL cannot bind the default graph to a variable), streams over HTTP, and follows the store's index order, which is stable while nothing is written. Seeking past the previous page would need a new repository API; skipping costs only the rows before the page, never the rows after it.
  Date/Author: 2026-10-22, Claude Code.
- Decision (revises the Named Graphs decision above): the two cached count queries are used only when the repository is LMDB-backed (`LmdbRepositories.isLmdbBacked`: a local `SailRepository` whose sail stack contains an `LmdbStore`, or a remote repository whose configuration names `rdf4j:LmdbStore`). Other repositories keep one `size(context)` per matching graph.
  Rationale: only an LMDB store has the cache, and the size path is the one that is exact for federations and SPARQL endpoint proxies.
  Date/Author: 2026-10-22, Claude Code.
- Decision: identical concurrent counts of one repository share a single running evaluation in the Workbench (keyed by repository and query), so reloading a page during a five-minute count does not start another count or use up the per-repository in-flight limit of eight.
  Rationale: with five-minute budgets, eight reloads would otherwise exhaust the limit and every page would answer "timed out" for five minutes.
  Date/Author: 2026-10-22, Claude Code.

- Decision: Explore pages and the Graphs listing run under their own per-repository in-flight limit (`BrowseList.forPageOperations`, 32), apart from the counts' limit of 8.
  Rationale: found in self-review during M7; with five-minute counts, a user exploring several large resources could fill the count limit and every page of that repository would then answer "took longer than five minutes" at once. Test: `ExploreServletCoverageTest.aPageIsShownWhileTheRepositoryRunsAsManyCountsAsItMay`.
  Date/Author: 2026-10-22, Claude Code.

## Outcomes & Retrospective

Delivered (2026-10-22): value→id lookups of read snapshots use the value overlay's reverse index; a write-invalidated LMDB result cache requested per query (`lmdb-result-cache`) end to end; Types and Named Graphs (LMDB repositories) answer repeat visits from that cache; Types, Named Graphs and Explore give every operation five minutes, Summary five seconds; identical concurrent counts share one evaluation; Explore pages stream in index order and stop after the page plus one statement, with the total counted separately.

Not changed, by evidence: `getStatements`, `hasStatement` and `size(context)` already consult the direct adjacency through the access arbiter; whole-store `size()` and `getContextIDs()` use LMDB counters that are cheaper than adjacency (census in Surprises).

Gaps and follow-ups: `size(context)` could be answered in constant time from LMDB's per-context counter table when the store holds no inferred statements (declined here, see Decision Log); Explore counts are not cached (the request named only Types and Graphs); deep Explore pages still read the rows before the page, because the repository API has no seek; Playwright e2e specs were not run (the paging spec polls its range text, so the deferred total should satisfy it).

Lesson: callers must read a cached query to its end for the cache to keep it; single-row reads are the trap.

## Context and Orientation

Terms used in this plan:

- LMDB store: the storage engine in `core/sail/lmdb`, package `org.eclipse.rdf4j.sail.lmdb`. Statements are stored as numbers in on-disk B-trees named after their key order, for example `spoc` (subject, predicate, object, context). `TripleStore.java` holds these trees; `ValueStore.java` holds the dictionary that maps each RDF value (IRI, literal, blank node) to a number ("id") and back.
- Read snapshot: `ValueStore.ReadSnapshot` and `LmdbSailStore.ReadView`, a consistent view of one LMDB read transaction. Datasets (`LmdbSailStore.LmdbSailDataset`) answer `getStatements`, `getStatementCount` and `getContextIDs` from one read view.
- Direct adjacency: `LmdbDirectAdjacencyStore`, an optional in-memory index of all statements grouped by subject and by object. It is enabled with `LmdbStoreConfig.setDirectAdjacencyEnabled(true)` and `setDirectAdjacencyMode(DirectAdjacencyMode.PREFER)`, and is "available" once its build finished. `LmdbSailDataset.statementsArbitrated` asks it for an estimate and lets `LmdbStatementAccessArbiter` choose between it and LMDB. `LmdbSailDataset.count` asks `LmdbDirectAdjacencyStore.tryCount` first. Its metrics (`snapshotMetrics().lookupHits`) show use.
- Value overlay: an optional immutable, compressed, off-heap copy of the dictionary records (`valueoverlay/ValueOverlayRegistry`, `valueoverlay/CompressedValueOverlay`). It is enabled with `LmdbStoreConfig.setValueOverlayEnabled(true)` and warmed in the background; `LmdbStore.awaitValueOverlayReady(timeout, unit)` waits for it. `ValueStore.borrowValueOverlay(long txn)` returns a lease for exactly the LMDB transaction given (or `null` when the overlay is absent, not ready, or the caller is the writer). A lease answers `get(id)` (id → record bytes) and `findId(byte[] record)` (record bytes → id, through a reverse hash index); a miss means "unknown, ask LMDB".
- Data revision: `TripleStore.getDataRevision()`, a counter set after every committed write.
- `SlowQueryContextHolder` (`core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/SlowQueryContextHolder.java`): a thread-local set by `SailTupleQuery.evaluate` around `SailConnection.evaluate`, carrying the raw query text and the forced LMDB strategy. The store reads it during `evaluateInternal`.
- Workbench: servlets in `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands`. `TypesServlet`, `ContextsServlet` (Named Graphs), `SummaryServlet` and `ExploreServlet`. `BrowseList` runs counts on worker threads within a time budget and answers `counts-timed-out` when the budget runs out. The repository is an `HTTPRepository` (remote server, the usual case) or a local `SailRepository`. Front end: `tools/workbench/src/main/frontend/classic/workbenchViews.ts` (`summaryPage`, `browseListPage`, `loadBrowseCounts`, `loadPageCounts`, `explorePage`), `explore.ts` and `paging.ts`.
- Server: `tools/server-spring`. `DefaultQueryRequestHandler.setQueryParameters` and `TransactionController.getQuery` apply query parameters; `ProtocolUtil.applyForcedLmdbExecutionStrategy` is the model for the new hint.

## Plan of Work

M1 — overlay for value→id. In `ValueStore.findId(ReadSnapshot snapshot, long txn, byte[] data)`, before the `mdb_get`, borrow the overlay lease for `txn` and return its `findId(data)` hit. Add two package-private `LongAdder` counters, `snapshotIdOverlayHits` and `snapshotIdDictionaryReads`, with a package-private accessor so a test can prove which path answered. The new test class `LmdbSnapshotIdOverlayTest` (package `org.eclipse.rdf4j.sail.lmdb`) builds a store with the overlay enabled, waits for it, then calls `getStatements(iri, null, null, true)` on a fresh connection with an IRI made by `SimpleValueFactory`, and asserts that the dictionary-read counter did not move while the overlay-hit counter did. It fails before the fix. Also keep `LmdbWorkbenchAccessCensusTest` as a regression guard: each pattern-bound call must record adjacency engagement and no fallback.

M2 — hint plumbing. Add `Protocol.LMDB_RESULT_CACHE_PARAM_NAME = "lmdb-result-cache"`. `RDF4JProtocolSession` gets `setLmdbResultCacheRequested(boolean)`/`isLmdbResultCacheRequested()` and adds `lmdb-result-cache=true` in `getQueryMethodParameters` when set. `AbstractHTTPQuery` gets `setLmdbResultCacheRequested(boolean)`/getter and `applyLmdbResultCache()` that pushes the flag onto the session; `HTTPTupleQuery` calls it next to `applyForcedLmdbExecutionStrategy()`. `SailQuery` gets the same setter/getter; `SlowQueryContextHolder.SlowQueryContext` gets `isResultCacheRequested()` and a `set(String, QueryType, String, boolean)` overload; `SailTupleQuery.evaluate` passes the flag. On the server, `ProtocolUtil.applyLmdbResultCache(HttpServletRequest, Query)` sets it on a `SailQuery` when the parameter is `true`; `DefaultQueryRequestHandler.setQueryParameters` and `TransactionController.getQuery` call it. Tests: a protocol-session test that the parameter is sent only when requested, a server `ProtocolUtil` test, and a `SailTupleQuery` test that the holder carries the flag during `evaluate`.

M3 — LMDB result cache. New class `LmdbQueryResultCache` in `org.eclipse.rdf4j.sail.lmdb`, owned by `LmdbStore` and closed with it. Its key is the evaluated `TupleExpr`'s string form plus the bindings, the dataset and the include-inferred flag. `LmdbStoreConnection.evaluateInternal` consults it only when the holder requests caching, the connection has no active transaction, and no explanation is being captured. On a hit it returns the cached rows as fresh `QueryBindingSet` copies. On a miss it reads the revision, evaluates as today, and wraps the result in a recording iteration that copies each row into plain values (`SimpleValueFactory`; blank nodes keep their id, so `BNodeOrigins` still recognises them as stored) and stores the entry when the caller reaches the end with the revision unchanged. Limits: system properties `rdf4j.lmdb.resultCache.maxRowsPerEntry` (default 100,000) and `rdf4j.lmdb.resultCache.maxRows` (default 1,000,000 rows in all, least recently used entries evicted). Tests in `LmdbQueryResultCacheTest`: a second identical hinted query is served from the cache (a hit counter), any committed add, remove or clear invalidates it, an unhinted query never uses it, a query read only partly is not cached, a query inside an open transaction is not cached, oversized results are not cached, and results equal uncached evaluation.

M4 — Types, Named Graphs, Summary. `BrowseList` gets `BROWSE_BUDGET_MILLIS = 300_000` (five minutes) used by Types and Named Graphs for counts and lists, and a single-flight map so identical concurrent counts per repository share one future. `TypesServlet` sets the hint and a 300-second maximum execution time on both of its queries. `ContextsServlet` lists graphs within the budget and counts with the two cached queries above (include inferred off). `SummaryServlet.COUNT_BUDGET_MILLIS` becomes 5,000. Update `TypesAndGraphsPageDataTest`, `SummaryServletPageDataTest`, `BrowseListCountsTest` and add tests that the hint is set (local `SailRepository` with LMDB: second request served by the cache).

M5 — Explore. `ExploreServlet` renders a page by streaming each role scan (subject, predicate, object, context — unchanged order and de-duplication) through `exportStatements` with a stop-after-page handler, within the five-minute budget. When `know_total` is absent it no longer counts; it writes metadata `has-more` (whether a row exists after the page) instead of `total-result-count`. With `counts=true` it answers `total-result-count` from one SPARQL `COUNT` (union of the four role patterns with the same de-duplication filters, `?r` bound), maximum execution time 300 seconds, or `counts-timed-out`. Update `ExploreServletPageDataTest`/`ExploreServletCoverageTest`, and add a test that a page over 10,000 matches reads at most offset + limit + 1 statements.

M6 — front end. Tooltips: Summary "Counting took longer than five seconds", Types/Graphs "Counting took longer than five minutes". Explore: when the page has `has-more` and no total, show "Rows a–b of …", request `counts=true`, then fill the total and the hidden `#workbench-total-result-count`; Next is enabled by `has-more` when the total is unknown. Update JS tests (`workbench-types-graphs.test.js`, `list-pages.test.js`, `review-fixes-query-paging.test.js`, `workbench-explore-*.test.js`) and the e2e specs that assert totals before rows.

M7 — format (`mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`), run module tests for `core/sail/lmdb` (targeted classes), `core/http/client`, `core/repository/http`, `core/repository/sail`, `tools/server-spring`, `tools/workbench` (Java and `npm test`), and write the retrospective.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSnapshotIdOverlayTest --module core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbQueryResultCacheTest --module core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbWorkbenchAccessCensusTest --module core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench
    (cd tools/workbench && npm test)

Expected: each selection reports `failures=0, errors=0`.

## Validation and Acceptance

- `LmdbSnapshotIdOverlayTest` fails before M1 (dictionary reads counted) and passes after.
- `LmdbQueryResultCacheTest` passes; its hit test shows the second evaluation served from the cache and the post-write evaluation not.
- In the Workbench against a local LMDB repository, Types and Named Graphs counts survive budgets longer than two seconds and a second visit is answered from the cache; Explore on a resource with thousands of statements renders its first page and then fills in "of N".

## Idempotence and Recovery

All changes are additive and can be re-run. The cache is in memory only; restarting the store empties it. The hint is ignored by stores other than LMDB and by older servers (an unknown query parameter is ignored), so a newer Workbench against an older server still works, only without the cache.

## Artifacts and Notes

Census spike: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbWorkbenchAccessCensusTest.java`.

## Interfaces and Dependencies

No new dependencies. New or changed signatures:

    // core/http/protocol Protocol
    public static final String LMDB_RESULT_CACHE_PARAM_NAME = "lmdb-result-cache";
    // core/http/client RDF4JProtocolSession, AbstractHTTPQuery; core/repository/sail SailQuery
    @Experimental public void setLmdbResultCacheRequested(boolean requested);
    @Experimental public boolean isLmdbResultCacheRequested();
    // core/sail/api SlowQueryContextHolder
    @Experimental public static SlowQueryContext set(String rawQueryText, Query.QueryType queryType, String forcedExecutionStrategy, boolean resultCacheRequested);
    @Experimental public boolean SlowQueryContext.isResultCacheRequested();
    // tools/server-spring ProtocolUtil
    @Experimental public static void applyLmdbResultCache(HttpServletRequest request, Query query);
    // core/sail/lmdb (package-private)
    final class LmdbQueryResultCache { ... }
