# Graph explorer filters show repository values for every drawn node

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

The RDF4J Workbench has a Graph page (the "graph explorer", a React application in `tools/workbench/src/main/frontend/graph-explorer`). It draws resources from a repository as nodes and the statements between them as links. Its Filters panel lets a user narrow the drawn nodes by node kind, type, datatype, language and by the values of their properties (called "predicates" in RDF). Today those property filters only know the statements that happen to be loaded into the page. If a user loads ten people with their names and `knows` links and wants to filter the people by birthday, there is no birthday filter, because no `birthDate` statements were loaded.

After this change, the Filters panel knows, for every drawn resource node, every outgoing predicate and its values (IRIs shown with their labels, blank nodes, and literal values such as dates and numbers) straight from the repository, up to sensible caps. Choosing a value or a date range selects the matching drawn nodes even when none of those statements are drawn. Loading happens in the background as soon as the graph settles, two requests at a time, pausing while the user's own queries run, and every result is cached in memory and in the browser's IndexedDB for an hour. Any write made through the Workbench (Add RDF, Remove, Clear, SPARQL Update, repository configuration, repository create or delete) invalidates the cache. The old "Count in the whole repository" button disappears because every count is now a repository count.

To see it: start a Workbench, create a repository with people that have `rdfs:label`, `foaf:knows` and `ex:birthDate`, open the Graph page, run `CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(?p != <http://example.org/birthDate>) }`, open Filters, and observe a "birth date" filter with a date range that selects the right people.

## Progress

- [x] (2026-10-19 21:55Z) Design approved by the user (plan file ~/.claude/plans/fizzy-chasing-acorn.md); ExecPlan written.
- [x] (2026-10-19 22:50Z) Milestone 1: query builders (`statementsQuery`, `predicateCensusQuery`, `predicateProbeQuery`, `predicateValuesQuery` in rdf-vocabulary.ts; `exactMatchQuery` and `BLANK_VALUE_ID` in facet-queries.ts to avoid an import cycle), 11 query fixtures, plan checks in GraphExplorerQueryTest (51/51), `max-result-rows` in GraphServlet page data (GraphServletTest 18/18) and on the Graph mount (`data-max-result-rows`, `data-max-query-length`).
- [x] (2026-10-19 23:05Z) Milestone 2: `RepositoryFacetStore` in repository-facets.ts; repository-facets-store.test.mjs 12/12.
- [x] (2026-10-19 23:30Z) Milestone 3: facet-store-persistence.ts (IndexedDB backend + memory backend, TTL, revision pruning; 6 tests), data-revision.ts, classic `graphAdditions.dataChanged`/`repositoryChanged` and `sendInPlace` marks on send and on every outcome, create/delete marks (classic tests 30/30).
- [x] (2026-10-19 23:45Z) Milestone 4: facet-value-labels.ts with `valueLabelQuery` (VALUES inside each scoped branch; plan check in GraphExplorerQueryTest, 53/53); 6 tests.
- [x] (2026-10-20 00:30Z) Milestone 5: `facetIndex`/`facetCounts` with repository values, ranges from ids (95% rule), `RANGE_VALUE` bounds-based ranges, server confirmation for incomplete cells, stable `matches`; use-repository-facets.ts hook, foreground-tracker.ts, limits from the mount; old count path removed (facet-values.ts and three tests deleted; regressions ported to GraphExplorerFacetRegressionTest, GraphExplorerBlankNodeTest and language-tag-case.test.mjs). Explorer suite 504/504.
- [x] (2026-10-20 01:10Z) Milestone 6: panel status line with Refresh, per-filter note for incomplete cells, HelpTip; browser measurements (below); new Playwright test in workbench-graph-explorer-filters.spec.js; suites: explorer unit 504/504, classic unit 850/850, tools/workbench module 1228/1228, explorer Playwright specs 75/75 (against a separate server-boot on :8093).

## Surprises & Discoveries

- Observation: In this RDF4J version a join whose right side contains a `GRAPH` scope, a subquery or `MINUS` is evaluated independently and hash-joined, so a `VALUES` block placed outside such a pattern does not restrict it: the right side is a full scan.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/JoinQueryEvaluationStep.java`, `isOutOfScopeForLeftArgBindings` (requiresIndependentEvaluation, result-set modifiers, difference in scope) selects `HashJoinIteration`.
- Observation: The Graph servlet silently truncates SELECT results at `graph-max-result-rows` (default 5,000, configurable lower) without any marker in the response.
  Evidence: `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/GraphServlet.java` (row limit read at startup, applied while writing rows).

- Observation: A FILTER inside the scoped (GRAPH/UNION) pattern opens a new scope that RDF4J hash-joins with the VALUES block; with the FILTER after the scoped pattern every statement pattern has its subject inlined as a constant and no HashJoinIteration appears.
  Evidence: first GraphExplorerQueryTest run: plans of facet-statements-all, facet-census, facet-values contained `Join (HashJoinIteration)` with `Filter (new scope)` on the right; after moving FILTER(!isTRIPLE(?o)) outside, all six plan checks pass.
- Observation: Fixture data added for these queries must not touch subjects or predicates of existing fixtures: a duplicate `ex:dup ex:relation ex:two` changed expand-portable (9→10), link counts and predicate usage.
  Evidence: first run failures; fixed by using `ex:dupOf ex:dupTarget`.

## Decision Log

- Decision: The two special value ids (`BLANK_VALUE_ID`, `LONG_LITERAL_VALUE_ID`) live in facet-value-ids.ts.
  Rationale: graph-facets.ts, facet-queries.ts and repository-facets.ts all need them; a shared leaf module avoids import cycles.
  Date/Author: 2026-10-20 / Claude.
- Decision: The value label query is a new builder (`valueLabelQuery`) rather than the canvas `labelQuery`.
  Rationale: `labelQuery` puts its VALUES outside the scoped unions, which RDF4J hash-joins after an unrestricted scan in the "all" and named-graph scopes; the new builder keeps VALUES inside each branch and is plan-checked.
  Date/Author: 2026-10-19 / Claude.
- Decision: `exactMatchQuery` lives in facet-queries.ts, not rdf-vocabulary.ts.
  Rationale: it needs `rangeSparqlFilter` from facet-queries.ts, which imports rdf-vocabulary.ts; keeping it there avoids an import cycle.
  Date/Author: 2026-10-19 / Claude.

- Decision: Prefetch repository values in the background as soon as the graph settles, not only when the Filters panel opens.
  Rationale: user choice; the panel should usually be ready when opened.
  Date/Author: 2026-10-19 / user.
- Decision: Persist per-node values in IndexedDB for one hour and invalidate on any write made through the Workbench, plus manual refresh.
  Rationale: user choice; balances freshness and speed. Writes from other clients show within the hour, which the panel says.
  Date/Author: 2026-10-19 / user.
- Decision: Remove "Count in the whole repository" and its code path.
  Rationale: user choice; all counts are repository counts now.
  Date/Author: 2026-10-19 / user.
- Decision: Fetch most nodes in one pass with a per-node cap of 101 statements; only nodes that reach the cap get a predicate census, candidate-predicate probes and per-predicate value queries capped at 1,000 values.
  Rationale: typical nodes have tens of statements, so one pass halves requests compared to census-then-values; hubs (millions of statements) stay bounded and still reveal rare predicates through probes.
  Date/Author: 2026-10-19 / Claude (after an independent design review).
- Decision: Cap values per (node, predicate) at 1,000 and mark such cells incomplete; do not stop a predicate after 1,000 distinct values across nodes. Selections that touch incomplete cells are verified with an exact SPARQL query for those nodes.
  Rationale: a global distinct cap silently drops nodes from filters (1,500 people have ~1,500 distinct birthdays); exact verification keeps matches equal to the generated SPARQL.
  Date/Author: 2026-10-19 / Claude.
- Decision: Two background request slots, paused while foreground explorer queries run, 30 s client timeout.
  Rationale: browsers allow six HTTP/1.1 connections per origin; the explorer's metadata scheduler uses three; user actions must not queue behind background loads.
  Date/Author: 2026-10-19 / Claude.
- Decision: Value labels use their own loader and caches rather than `RdfExplorer.resolve`.
  Rationale: resolving thousands of value IRIs through the explorer would evict canvas labels from its 4,096-entry persistent store and invalidate its label memo on every batch.
  Date/Author: 2026-10-19 / Claude.

## Outcomes & Retrospective

Completed 2026-10-20. With 2,000 people loaded by name only (no other statements), the Filters panel lists age and birth date (as ranges), email, height, knows, lives in (city labels from the repository), nickname, shoe size, status and works for. A birth-date range 1990-01-01 – 1995-12-31 selects 207 nodes, equal to a direct SPARQL count. A 20,000-statement hub lists its one rare predicate and shows the "more than 1,000 values" note on `member`. Adding a statement through Add RDF and returning shows the new filter; the Playwright test shows the same live from another tab.

Performance (MemoryStore, 46,078 statements, Chrome): with an empty cache, the values of all 2,000 nodes arrive in about 41 requests, two at a time, median 20 ms each, about 0.5 s after the graph settles; a reload within the hour sends no value requests at all (IndexedDB). Recounting 5,000 nodes × 20 values for a new selection takes well under the 80 ms test bound.

Not done: interning value ids (memory has been fine at this scale); viewport-based priority (only focus/selection vs background is used); the canvas `labelQuery` still places VALUES outside its scoped unions, which the plan checks showed hash-joins in the "all" and named scopes — worth fixing separately.

## Context and Orientation

The Workbench web application lives in `tools/workbench`. Its classic pages are TypeScript compiled to browser scripts from `tools/workbench/src/main/webapp/scripts/ts` (rendered with lit-html; the main file is `workbenchViews.ts`). The Graph page mounts a React application whose sources are in `tools/workbench/src/main/frontend/graph-explorer`, bundled by Vite (`tools/workbench/vite.config.ts`). The Java servlets are in `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench`. The explorer sends SPARQL to the Graph servlet (`commands/GraphServlet.java`), which answers SPARQL JSON results. Terms used below:

A "node" is a drawn resource. Its id is produced by `sparqlNodeId(term)` in `graph-explorer/rdf-terms.ts`: an IRI's id is the IRI itself; a blank node from a store that keeps blank-node ids is `<_:label>` (a "fixed blank reference", which can be put back into SPARQL); other blank nodes get per-response scoped ids; a literal's id is `__rdf_literal__:` followed by JSON of its type, value, datatype and lowercased language. `sparqlTermFromNodeId(id)` reverses an id into a term, `isQueryableResource(id)` tells whether an id can be sent back to the server, and `sparqlFacetValueToken(id)` writes a value id as a SPARQL token.

A "facet" is one filter in the Filters panel: a predicate (or kind, type, datatype, language) with its values. `graph-explorer/graph-facets.ts` exports `facetModel(input)` which builds facets from loaded links whose source is a selectable node, computes counts (how many nodes choosing a value would select, given the other filters), and the set of matching nodes. Values are OR-ed within a facet and AND-ed across facets. `graph-explorer/use-facet-filters.ts` wraps it in React state; `graph-explorer/filter-panel.tsx` renders it. `GraphExplorerApp.tsx` passes `filterNodes` (metadata nodes in canvas scope, about line 808), all loaded edges, `metadataNodeById`, and `hiddenFilterPredicates` (rdf:type and label predicates) to the hook, and uses `filters.matches` as the selection while filters are active.

Batching and caching helpers are in `graph-explorer/request-batching.ts`: `QueryFailure(message, status, retryAfterMs)`; `AdaptiveBatchLoader` (batches keys, halves on saturation or on HTTP 408/413/414/429/502/503/504, grows on fast replies, one request in flight per loader); `BoundedCache` (least-recently-used by entries and bytes, with expiry, `snapshot`/`restore`); `QueryScheduler(n)` (first-in-first-out, at most n running). Labels for canvas nodes come from `RdfExplorer` in `graph-explorer/rdf-explorer.ts` (`resolve(ids)` then `label(id)`), cached in memory and in `localStorage["graph-field-metadata-v4"]`.

Query builders are in `graph-explorer/rdf-vocabulary.ts`. `scoped(pattern, scope)` wraps a pattern for the chosen graph scope: unchanged for "default", `{ P } UNION { GRAPH ?__graph { P } }` for "all", `GRAPH <g> { P }` for one named graph. In RDF4J the default graph is the union of all graphs, so "all" returns named-graph statements twice unless the query uses DISTINCT. Existing per-node builders (`expandQuery`, `linkCountQuery`) use one subquery per node joined by UNION. The SPARQL these builders produce is exported by `tools/workbench/scripts/export-query-fixtures.mjs` (`npm run fixtures:queries`) into `src/test/resources/graph-explorer-queries` with expected row counts, and `src/test/java/org/eclipse/rdf4j/workbench/commands/GraphExplorerQueryTest.java` runs them against an RDF4J MemoryStore; the Node test `query-fixtures-current.test.mjs` fails when the committed fixtures differ from the generators.

The Graph servlet accepts SELECT, CONSTRUCT and DESCRIBE, rejects queries longer than `graph-max-query-length` (150,000 characters by default) with HTTP 413, truncates results at `graph-max-result-rows` (5,000 by default) without saying so, and times out after 60 seconds. Its page data already exposes `max-query-length`.

Workbench writes go through `sendInPlace(...)` in `workbenchViews.ts` for the Add, Remove, Clear, Update and configuration pages; its completion reports `committed`, `fallback`, `done` or `failed`. The classic "Add to graph" queue (`scripts/ts/graphAdditions.ts`) and the explorer (`graph-explorer/graph-additions.ts`) share localStorage keys built from `repositoryScope(basePath, repositoryId, serverIdentity)` in `graph-explorer/repository-identity.ts` (a JSON tuple); the data revision introduced here uses the same scope string.

## Plan of Work

Milestone 1 adds four builders to `rdf-vocabulary.ts`. Every builder puts the `VALUES` block inside the pattern given to `scoped`, uses `SELECT DISTINCT` in each per-node subquery, and filters out triple-term objects with `FILTER(!isTRIPLE(?o))`. `statementsQuery(ids, scope, perNodeCap = 101)` returns `?s ?p ?o` with one capped subquery per node. `predicateCensusQuery(id, scope, cap = 10001)` returns `?p (COUNT(?o) AS ?n)` over a capped `DISTINCT ?p ?o` subquery of one node. `predicateProbeQuery(id, predicates, scope)` returns `?p` for each candidate predicate that the node has, one `LIMIT 1` subquery per predicate. `predicateValuesQuery(id, predicates, scope, cap = 1001)` returns `?p ?o` with one capped subquery per predicate. `exactMatchQuery(ids, predicate, values | range, scope)` returns the ids among `ids` that have a selected value (VALUES of tokens from `sparqlFacetValueToken`) or a value inside a range (`rangeSparqlFilter` from `facet-queries.ts`). Fixtures for each, in default, all and named-graph scope, go into `export-query-fixtures.mjs` with expected counts; new data rows are added to `GraphExplorerQueryTest` (a hub with 300 statements on one predicate plus one rare predicate, and a statement duplicated in a named graph). A new JUnit test asserts that the "all"-scope statements query returns no duplicate rows and that its evaluation plan (from `TupleQuery.explain(Explanation.Level.Executed)`) contains no `HashJoinIteration` over an unbound scan. `GraphServlet` adds `max-result-rows` to the page data beside `max-query-length`, tested in `GraphServletTest`; the explorer reads it from its mount data.

Milestone 2 adds `graph-explorer/repository-facets.ts` with `RepositoryFacetStore`. The constructor takes `{ execute(query, signal), scope, maxRows, maxQueryLength, now, persistence?, foreground: { active(): boolean; idle(): Promise<void> } }`. `want(nodeIds, priority)` registers queryable ids and schedules loading; `get(nodeId)` returns `{ predicates: Map<predicate, { values: string[]; complete: boolean }>, predicatesComplete, checkedAt } | undefined`; `subscribe(listener)` and `version` support React; `status()` reports `{ wanted, loaded, loading, failed, budgetExhausted }`; `clear()` drops memory, bumps the generation and reloads wanted nodes. Loading uses its own `QueryScheduler(2)`, waits for `foreground.idle()` before each request, applies a 30-second `AbortSignal.timeout`, and splits batches on `QueryFailure` 408/413/429/5xx or when a reply has `rows >= maxRows`. Nodes that return fewer than 101 statements are complete; nodes that return 101 go through census, probes (candidates are predicates seen on other loaded entries or in loaded edges, supplied by the caller) and per-predicate values. Value ids come from `sparqlNodeId`; blank values without `rdf4j:reference` become the single value id `__rdf_blank_value__`; literals longer than 1,024 characters record only predicate presence (`__rdf_long_literal__`). A reply is stored only when its generation equals the current one. A budget of 100,000 value rows per store limits eager loading; past it, `want(ids, "on-demand")` for a specific predicate still loads.

Milestone 3 adds `graph-explorer/facet-store-persistence.ts`: an IndexedDB database `graph-field-facet-values` (version 1) with one object store keyed by `[baseNamespace, revision, nodeId]`, entries `{ storedAt, entry }`, batched `getAll` on key ranges, one write transaction per reply, and a cleanup that deletes rows older than one hour or from other revisions of the same base namespace. `baseNamespace` is JSON of the repository scope, the graph scope and the inference setting (cookie `infer`). `revision` is `localStorage["rdf4j-workbench-data-revision:" + repositoryScope]` (missing means "0"). In `workbenchViews.ts`, `sendInPlace` writes a new revision (time plus random) when it sends and again on every outcome, and the repository create and delete pages do the same; `graphAdditions.ts` gains `dataRevisionKey(context)` so both sides agree. The explorer listens for `storage` events on that key and clears the store. The "Clear cache and refresh labels" button clears the store too.

Milestone 4 adds a value-label loader inside the store module: it batches IRI value ids through `labelQuery` (same builder as canvas labels) on the store's scheduler, picks the best label with `labelComparator` for the user's languages and label predicates, caches in memory and in the IndexedDB database (a second object store `labels`, keyed `[baseNamespace, iri]`, six-hour expiry), and resolves ids in priority order: those the panel shows first, then others in the background, at most 20,000 per session. `RdfExplorer.label(id)` is consulted first, since canvas nodes are already labelled.

Milestone 5 changes `graph-facets.ts`: `facetIndex({ nodes, edges, nodeById, repositoryValues, typesOf, hiddenPredicates, labelOf })` builds, once per data change, a per-node map of facet values merging loaded edges and repository values, value labels (lowercased once for search), kinds and literal terms from ids via `sparqlTermFromNodeId`, and range descriptions (offered when at least 95% of a predicate's values parse as numbers or dates). `facetCounts(index, selection, ranges)` computes counts, coverage and matches per selection, returning the previous `matches` set when unchanged. Range selections are stored as bounds (a facet entry whose value set contains one id `__range__`, with the bounds in the existing range drafts) and matched numerically. Sorting uses one `Intl.Collator`. Each facet lists at most 1,000 values. `use-facet-filters.ts` uses both, drops `repositoryCounts`, `countInRepository`, `forgetRepositoryCounts` and `endpointQueryable`; `facet-queries.ts` drops `countFacetValues` and `facetCountQuery`; `facet-values.ts` (`mergeFacetValues`) is deleted with its test; GraphExplorerApp drops `countFacetInRepository`, `graphIsPartial` and every `forgetRepositoryCounts` call. When a selection touches incomplete cells, `use-facet-filters.ts` asks the store to run `exactMatchQuery` for those nodes and merges the answer into matches. A new hook `use-repository-facets.ts` creates the store per explorer (endpoint, scope, server identity), wraps `queryRepository` to count foreground requests, calls `want` for queryable `filterNodes` 500 ms after the graph revision settles (priority: selected and focused, then nodes in the viewport, then the rest), and exposes `repositoryValues` via `useSyncExternalStore`, published at most every 250 ms.

Milestone 6 updates `filter-panel.tsx`: a status line under the heading ("Loading values from the repository · 1,240 of 3,000 nodes", then "Values from the repository, checked 2 min ago · Refresh"), notes on facets with incomplete cells or on-demand loading, a `HelpTip` (from `help-tip.tsx`, `inline` mode) that explains where values come from and when they refresh, and value swatches by kind for values that are not loaded. Then it measures performance in a browser and runs the suites.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j` unless stated.

Build the classic scripts and the explorer, and run the explorer unit tests:

    cd tools/workbench && npm run build && npm test

Run the classic page unit tests (TypeScript 6 must be first on PATH because the harness spawns `tsc`):

    cd e2e && PATH="$PWD/../tools/workbench/node_modules/.bin:$PATH" node --test tests-unit/*.test.js

Refresh the query fixtures after changing builders:

    cd tools/workbench && npm run fixtures:queries

Run the Java tests (the mvnf runner performs the root quick install first):

    python3 .codex/skills/mvnf/scripts/mvnf.py GraphExplorerQueryTest
    python3 .codex/skills/mvnf/scripts/mvnf.py GraphServletTest

Browser and Playwright checks use a separate server so a developer's own server on port 8080 is untouched: copy `tools/server-boot/target/rdf4j-server-boot-*.jar` to a scratch folder, start it with `java -Dorg.eclipse.rdf4j.appdata.basedir=<scratch>/appdata -jar <scratch>/server-boot.jar --server.port=8093`, find its extracted webapp under `$TMPDIR/rdf4j-server-webapp*/rdf4j-workbench`, copy `tools/workbench/target/frontend/scripts/.` and `target/frontend/styles/graph-explorer.css` and `src/main/webapp/styles/workbench-refresh.css` into it, and run Playwright with `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8093/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:8093/rdf4j-server npx playwright test --reporter=line --project=chromium tests/workbench-graph-explorer-filters.spec.js` from `e2e`.

## Validation and Acceptance

Unit acceptance: the explorer suite passes, including new tests that fail before their milestone: query shapes and caps; the store queries only uncached nodes, never more than two requests at once, waits while a foreground request runs, splits on 413 and on saturated replies, drops replies from an older generation, records empty nodes, marks incomplete cells; persistence round-trips within an hour and not after, ignores other revisions; value labels are fetched once per IRI within their expiry; the facet model merges repository values, offers a date range for unloaded literal dates, and recomputes counts for 5,000 nodes × 20 values in under 50 ms per selection. The classic unit suite passes, including a test that `sendInPlace` writes a new data revision on send and on every outcome.

Java acceptance: `GraphExplorerQueryTest` passes with the new fixtures and the plan check; `GraphServletTest` shows `max-result-rows` in the page data.

End-to-end acceptance: on a repository of people where birthdays are not loaded, the Filters panel shows a "birth date" filter with a date range; entering a range selects exactly the people born in it; adding a person's birthday through Add RDF in another tab makes the value appear without reloading the Graph page; reloading the Graph page shows the filters without any value queries (network panel) within the hour; a node with 20,000 statements still lists its one rare predicate.

Performance acceptance (recorded in Outcomes): on 2,000 drawn people with 12 predicates, all values are loaded within a few seconds after the graph settles, with at most two value requests in flight; the facet recompute after a selection change stays below 50 ms.

## Idempotence and Recovery

All steps are additive and can be repeated. The fixture export overwrites its files deterministically. The IndexedDB database can be deleted from the browser's developer tools without harm; the store rebuilds it. If a milestone fails halfway, its tests show what is missing; revert only that milestone's files with git.

## Artifacts and Notes

Request timeline with an empty cache (start+duration ms since navigation): explorer metadata 1455–1745 (12 requests), then value batches in pairs 2321+24 2321+23 2349+22 2352+22 … 2793+14 (41 requests). After a reload: 13 requests, all explorer metadata, none for values.

    GraphExplorerQueryTest: tests=53, failures=0
    tools/workbench module: tests=1228, failures=0, errors=0
    Playwright (chromium, :8093): 75 passed

## Interfaces and Dependencies

No new libraries. In `tools/workbench/src/main/frontend/graph-explorer/rdf-vocabulary.ts` define:

    export function statementsQuery(ids: string[], scope: GraphScope, perNodeCap?: number): string
    export function predicateCensusQuery(id: string, scope: GraphScope, cap?: number): string
    export function predicateProbeQuery(id: string, predicates: string[], scope: GraphScope): string
    export function predicateValuesQuery(id: string, predicates: string[], scope: GraphScope, cap?: number): string
    export function exactMatchQuery(ids: string[], predicate: string, match: { values: string[] } | { range: QueryRange }, scope: GraphScope): string

In `tools/workbench/src/main/frontend/graph-explorer/repository-facets.ts` define:

    export type RepositoryCell = { values: string[]; complete: boolean };
    export type RepositoryEntry = { predicates: Map<string, RepositoryCell>; predicatesComplete: boolean; checkedAt: number };
    export class RepositoryFacetStore {
      constructor(options: RepositoryFacetStoreOptions);
      want(nodeIds: readonly string[], priority: "focus" | "viewport" | "background"): void;
      get(nodeId: string): RepositoryEntry | undefined;
      status(): { wanted: number; loaded: number; loading: number; failed: number; budgetExhausted: boolean };
      subscribe(listener: () => void): () => void;
      readonly version: number;
      clear(): void;
      dispose(): void;
    }

In `tools/workbench/src/main/frontend/graph-explorer/graph-facets.ts` define `facetIndex(input)` and `facetCounts(index, selection, ranges)`; keep `facetModel(input)` as a thin wrapper so existing callers and tests keep working.
