# RDF4J Workbench

The Workbench (`rdf4j-http-workbench`) is the browser UI for RDF4J Server. This module holds:

| Path | What it is |
| --- | --- |
| `src/main/java` | Servlets that render pages and proxy requests to RDF4J Server |
| `src/main/webapp/` | `WEB-INF` (deployment descriptors), images and favicons |
| `src/main/frontend/classic/*.ts` | TypeScript sources of the classic (lit-html) pages |
| `src/main/frontend/graph-explorer/` | The React graph explorer island (TypeScript/TSX), bundled by Vite |
| `src/main/frontend/static/scripts/` | Hand-written or vendored JavaScript only (see below), served as is |
| `src/main/frontend/static/styles/` | Plain CSS, served as is |
| `src/test/frontend/` | All Node front-end tests (`npm test`): `classic/`, `graph-explorer/`, and pure-Node layout `benchmarks/` |
| `package.json`, `tsconfig.classic.json`, `tsconfig.json`, `vite.config.ts` | The npm project that builds and tests both front ends |

All commands below run from the **repository root** unless stated otherwise.

## The one thing to know

**The front end is an npm project that Maven builds for you.** `mvn package` (also with `-Pquick`) runs
`frontend-maven-plugin`, which downloads a private Node into `target/node`, runs `npm ci` (only when `node_modules`
is missing) and `npm run build`, and writes the result to `target/frontend/`. The WAR plugin adds that directory as a
web resource, so the WAR, the Spring Boot runner (which unpacks the WAR) and the Docker images always carry freshly
compiled scripts. Nothing compiled is committed any more. Before that, in the `initialize` phase, Maven copies
`src/main/frontend/static/` and the classic `.ts` sources (to `scripts/ts/`, where the source maps look for them) into
`target/frontend/`, so the URL of every served file is its path below `static/`.

`npm run build` does two things, in this order:

1. `build:classic`: `tsc -p tsconfig.classic.json` compiles `src/main/frontend/classic/*.ts` to
   `target/frontend/scripts/*.js` (+ `.js.map`) with exactly the flags the old `compileTypescript.sh` used
   (`--noImplicitAny --sourcemap --sourceRoot /rdf4j-workbench/scripts/ts`, default target and no module system).
   The files share the global `workbench` namespace, reference each other with `/// <reference path="..." />`, and
   are loaded with plain `<script>` tags. `codemirror.d.ts`, `jquery.d.ts` and `yasqe.d.ts` produce no output.
2. `build:explorer`: `vite build` bundles `src/main/frontend/graph-explorer/main.tsx` to
   `target/frontend/scripts/graph-explorer/graph-explorer.mjs` (an ES module, plus hashed chunks and workers under
   `scripts/graph-explorer/`) and `target/frontend/styles/graph-explorer.css`.

Java-only loops can pass `-DskipFrontend=true`; the WAR then lacks the compiled scripts (it still has the static
scripts and styles), so do not use it for anything that serves the Workbench.

## Prerequisites

| Tool | Version | Needed for |
| --- | --- | --- |
| JDK | 25 or newer | Maven build, running the server |
| Maven | 3.9+ | Maven build |
| Node.js + npm | 22.12+ **only if you run npm yourself**; Maven downloads its own Node 22 | `npm run dev`, front-end tests, Playwright |
| Docker | any recent | Only for the Docker/Tomcat runtime |

TypeScript is pinned to `5.9.3` in both `tools/workbench/package.json` and `e2e/package.json`.

## Regenerate npm lockfiles

When dependency specifications change, regenerate each lockfile from its `package.json` in a fresh temporary
directory with no `node_modules` or existing lockfile. Use the Maven-managed Node `22.23.3` with npm `11.12.1` for
generation; npm `10.9.9` remains the configured clean-install/build runtime. Generating from a clean directory lets
npm record the full registry-declared platform-optional closure and `libc` constraints instead of tailoring the lock
to the host machine. Copy the generated lockfile back, then run `npm ci` in the project with the configured npm
`10.9.9` and confirm the lockfile hash is unchanged.

From the repository root, repeat this for `tools/workbench` and `e2e` as needed:

```bash
repo="$PWD"
project=tools/workbench # or e2e
scratch="$(mktemp -d)"
cp "$repo/$project/package.json" "$scratch/package.json"
(
  cd "$scratch"
  export PATH="$repo/tools/workbench/target/node:$PATH"
  test "$(node --version)" = v22.23.3
  generator_npm="$("$repo/tools/workbench/target/node/node" \
    "$repo/tools/workbench/target/node/node_modules/npm/bin/npx-cli.js" \
    --yes --package=npm@11.12.1 npm --version)"
  test "$generator_npm" = 11.12.1
  printf 'Lock generator npm: %s\n' "$generator_npm"
  "$repo/tools/workbench/target/node/node" \
    "$repo/tools/workbench/target/node/node_modules/npm/bin/npx-cli.js" \
    --yes --package=npm@11.12.1 npm install --package-lock-only --ignore-scripts --no-audit --no-fund \
    --registry=https://registry.npmjs.org
)
cp "$scratch/package-lock.json" "$repo/$project/package-lock.json"
lock_before="$(shasum -a 256 "$repo/$project/package-lock.json" | awk '{print $1}')"
(
  cd "$repo/$project"
  export PATH="$repo/tools/workbench/target/node:$PATH"
  "$repo/tools/workbench/target/node/node" \
    "$repo/tools/workbench/target/node/node_modules/npm/bin/npm-cli.js" ci
)
lock_after="$(shasum -a 256 "$repo/$project/package-lock.json" | awk '{print $1}')"
test "$lock_before" = "$lock_after"
printf 'npm ci left %s/package-lock.json unchanged (%s)\n' "$project" "$lock_after"
```

The lockfile follows the current package metadata: for example, Rolldown `1.0.1` declared a WASI binding, while
`1.2.14` declares an Android ARM EABI binding instead. Fresh generation removes only the old WASI fallback closure
that the upgraded package no longer declares; do not restore those stale entries by hand.

The repository convention is to use a workspace-local Maven repository (`-Dmaven.repo.local=.m2_repo`) and to run
Maven offline (`-o`). The first build on a fresh checkout must download dependencies, so drop `-o` once.

## Public deployment settings

Serve Workbench over HTTPS and configure the servlet container to issue secure, HttpOnly session cookies. Connection
credentials are held in the HTTP session after a POST; they are never stored in a Workbench cookie. Changing the
connection rotates the session ID. Query text is kept in the current browser tab's session storage for editing and
paging, scoped to that session, and is not stored in cookies. On the first request after an upgrade, Workbench expires
legacy query and credential cookies that the browser still sends.

The Add page accepts browser uploads and files placed in Workbench's approved import directory. By default, that is the
private `imports` directory under Workbench's application data directory (for example, below the directory selected by
`org.eclipse.rdf4j.appdata.basedir`). Workbench creates this directory with owner-only permissions on POSIX systems.
Set `-Dorg.eclipse.rdf4j.workbench.add-file-directory=/absolute/existing/directory` to approve a different directory;
it must already exist and be readable by Workbench. The Add page lists its direct, readable regular files, excluding
hidden and dot-prefixed files and symbolic links. A submitted filename cannot select a file outside that directory.
Keep the approved directory writable only by trusted administrators. Local file imports have a 32 MiB limit by default;
`-Dorg.eclipse.rdf4j.workbench.add-file-max-bytes=<bytes>` can set it from 1 byte up to 256 MiB.

URL imports are denied by default. Set `-Dorg.eclipse.rdf4j.workbench.add-url-allowed-origins=https://data.example.org`
to approve exact, comma-separated HTTP(S) origins. A source URL must match one of them; redirects are rejected. An
explicitly empty origins property disables URL imports. Set `-Dorg.eclipse.rdf4j.workbench.add-url-max-bytes=<bytes>`
to adjust the 32 MiB URL source limit (1 byte to 256 MiB). Each Java system property overrides the corresponding
`add-url-allowed-origins`, `add-url-max-bytes`, `add-file-directory`, or `add-file-max-bytes` servlet init parameter.
Multipart uploads are limited to 32 MiB per file and 36 MiB per request.

Graph requests are capped at 5,000 result rows and 60 seconds by default, including when callers omit or disable
their own limits. The `graph-max-result-rows`, `graph-max-query-timeout`, and `graph-max-query-length` servlet init
parameters configure these ceilings; the query length defaults to 150,000 characters and cannot exceed one million.
JSONP is disabled by the default `org.eclipse.rdf4j.workbench.jsonp.enabled=false` application property.
Keep it disabled on public deployments: enabling JSONP lets another origin load Workbench responses as scripts.

Private saved queries created before session-backed ownership used a browser-claimed username. They are intentionally
not assigned to a new credential identity automatically, because Workbench cannot verify which password owned them.
An operator should review and re-save any legacy private queries under the intended account before public release.

## Build everything from a clean checkout

### 1. Build the front end (optional: Maven does it in step 3)

```bash
cd tools/workbench && npm ci && npm run build && cd ../..
```

`npm ci` installs the pinned dependencies into `tools/workbench/node_modules` (ignored by git). Other scripts:

- `npm run dev`: Vite dev server on `http://localhost:5173/dev/index.html?repository=<id>` that mounts the graph
  explorer standalone and proxies `/rdf4j-workbench` and `/rdf4j-server` to a server-boot running on port 8080.
- `npm run typecheck`: type-checks the explorer sources without emitting.
- `npm test`: all Node unit tests under `src/test/frontend/` (`npm run test:classic` and `npm run test:explorer` run
  one half each; see [Front-end unit tests](#front-end-unit-tests-node-no-server)).

Files in `src/main/frontend/static/scripts/` are hand-written or vendored, so edit them directly (or not at all, for
vendored code):

- `workbench-theme.js`, `workbench-lit-html.mjs`, `cookies.html` (hand-written)
- `yasqe*`, `diff.min.js`, `svg-pan-zoom.min.js`, and the versioned `vendor/` directories
  (vendored third-party libraries; exact releases and compatibility holds are listed in
  [FRONTEND_DEPENDENCIES.md](FRONTEND_DEPENDENCIES.md))

### 2. Build and install with Maven

Build the whole repository without tests (about 30 seconds on a warm machine; the first run on a fresh checkout must
be online so Maven can fetch `frontend-maven-plugin` and the Node tarball, which are then cached in `.m2_repo`):

```bash
mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
```

Or only the Workbench and the modules it depends on:

```bash
mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl tools/workbench -am -Pquick install
```

The `quick` profile skips tests, formatting, import sorting, japicmp and the enforcer. Outputs in
`tools/workbench/target/`:

- `frontend/`: the compiled front end (`scripts/*.js`, `scripts/graph-explorer/`, `styles/graph-explorer.css`).
- `rdf4j-workbench.war`: the deployable web application, including `target/frontend`.
- `rdf4j-workbench-classes.jar`: the servlet classes, installed as the
  `rdf4j-http-workbench-<version>-classes.jar` artifact. The Spring Boot runner depends on it and on the WAR.
- `node/`: the Node runtime Maven downloaded (tarball cached in `.m2_repo/com/github/eirslett/`), npm's cache is
  `.m2_repo/npm-cache`.

Always install before running tests in this or any downstream module. Test runs resolve sibling modules from
`.m2_repo`, so they only see changes that have been installed there.

### 3. Format before committing

```bash
mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
```

The `formatting` profile (active on JDK 25+) runs Spotless on Java and XML files during `process-resources`.
It does not touch TypeScript, JavaScript or CSS. Also check copyright headers on new files:

```bash
cd scripts && ./checkCopyrightPresent.sh && cd ..
```

## The graph explorer (React island)

The Graph page (`/repositories/<id>/graph`) is a React application under `src/main/frontend/graph-explorer/`,
mounted into the lit-html outlet by `main.tsx` (`window.workbench.graphExplorer.mount(outlet, ctx)`, registered in
`classic/workbenchRoutes.ts`). It talks only to the Workbench: `transport.ts` posts one SPARQL query per request
to `GraphServlet` (`/repositories/<id>/graph`) and receives SPARQL Results JSON (`CONSTRUCT` and `DESCRIBE` results
arrive as `?source ?predicate ?target` rows), so the Workbench session carries the server credentials and the
deployment policy (`query-execution`, `menu.item.graph.visible`) applies.

- `npm run dev` in `tools/workbench` serves `src/main/frontend/dev/index.html?repository=<id>` against a server-boot on
  port 8080 (`/rdf4j-workbench` and `/rdf4j-server` are proxied), with hot reload of the island.
- `npm run test:explorer` runs the explorer's unit tests (`src/test/frontend/graph-explorer/`), including
  `css-inventory.test.mjs` (every rule scoped under `.graph-explorer`, tokens aliased to the Workbench's) and
  `query-fixtures-current.test.mjs` (the generated SPARQL under `src/test/resources/graph-explorer-queries` is what the
  generators produce; refresh it with `npm run fixtures:queries`). `GraphExplorerQueryTest` runs those queries on a
  MemoryStore from Maven.
- `graph-canvas-scope.ts` decides what the Graph view draws: resources and the links between them, while literal
  values and `rdf:type` links stay in the data (details card, tables, cards, timeline, map) unless “Values” is pressed.
  The 300- and 50,000-node stress tests in the Tools menu exist only under `npm run dev`.
- The graph is kept in IndexedDB per repository (`use-stored-workspace.ts`, `workspace-storage.ts`): restored when the
  page mounts, saved at most every 800 ms while it changes (not while it moves; less often for large graphs), on hide
  and on unmount. The page starts empty. `workspace-format.ts` is the compact stored form (format 2: tables for nodes,
  predicates, types and batches, an edge as four numbers; 11.5 MB for 50,000 nodes and 200,000 edges), which
  `workspace.ts` validates like the plain form; a record that cannot be read is kept aside under
  `<scope>:unreadable`.
- "Add to graph" on the other pages goes through a `localStorage` queue
  (`rdf4j-workbench-graph-additions:<basePath>|<repositoryId>`): `classic/graphAdditions.ts` writes it,
  `graph-additions.ts` takes it when the Graph page mounts and on `storage` events from other tabs.
- `link-counts.ts` and `linkCountQuery` give the "+N" badges (links in the repository minus loaded ones); Expand
  passes the loaded statements to `expandQuery`, which leaves them out with `MINUS { VALUES … }`.
- `edge-hit.ts` picks the edge under a click or the mouse from the curves (no invisible hit paths);
  `statement-details.ts` builds the statement card's queries (predicate, RDF 1.2 reifiers, `rdf:Statement`
  reifications, references to the triple term); `statement-links.ts` keeps the RDF 1.2 links from a reifier or
  referrer to a statement (drawn as dashed connectors to the edge's middle). `graph-export.ts` writes Turtle, SVG and
  PNG; `graph-literals.ts` finds places for the Map view and dates for the Timeline; `blank-labels.ts` names blank
  nodes.
- Scale: the page is measured on 50,000 nodes and 200,000 edges (load ~5 s, 60 fps pan and zoom, no interaction
  blocking more than 0.7 s). From 500 drawn nodes `large-graph-canvas.tsx` draws with WebGL (`graph-indexed-gl.ts`);
  above 1,500 nodes the worker uses `graph-layout-multilevel.ts`, which lays out neighbourhoods of at most 256 nodes
  and only their strongest links (`strongestLinks`). The Triples view pages by 500 rows and the Filters panel lists
  12 values at a time. The servlet's optional `row-limit` parameter caps the rows of one query (queued "Add to graph"
  queries use 5,000).
- `graph-facets.ts` is the filter model: OR within a filter, AND across filters, counts taken over the nodes that match
  the other filters (so a filter's choices say what they would select), ranges for numbers and dates.
- `src/main/frontend/graph-explorer/STYLE-MAP.md` records how the explorer's stylesheet maps onto the Workbench design
  system; `node scripts/css-inventory.mjs --print` lists the current class inventory.
- The Playwright specs `workbench-graph-explorer*.spec.js` in `e2e/tests` cover navigation, mount/unmount, the Query
  page hand-over and the light/dark appearance (`…-visual`), gestures (`…-interaction`), filters (`…-filters`), the
  stored graph, "Add to graph", Find and the downloads (`…-store`), "+N" paging (`…-paging`), the Map (`…-map`),
  statement cards (`…-statements`) and features used together (`…-combinations`: reloads, other tabs, undo, RDF 1.2
  data, selection-scoped views, dragging without lag).

## Run the Workbench

All three options serve RDF4J Server at `http://localhost:8080/rdf4j-server/` and the Workbench at
`http://localhost:8080/rdf4j-workbench/`.

### Spring Boot runner (fastest, used by the e2e tests)

`tools/server-boot` embeds Tomcat and runs both the server and the Workbench in one JVM. After step 3:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl tools/server-boot spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dorg.eclipse.rdf4j.appdata.basedir=/tmp/rdf4j-data"
```

Or run the packaged jar directly, which also lets you pick a port:

```bash
java -Dorg.eclipse.rdf4j.appdata.basedir=/tmp/rdf4j-data \
  -jar tools/server-boot/target/rdf4j-server-boot-*.jar --server.port=8080
```

How server-boot picks up front-end changes: its build unpacks `rdf4j-workbench.war` (from `.m2_repo`) into the jar
as classpath resources (`rdf4j/workbench-webapp`). At startup it extracts them to
`${java.io.tmpdir}/rdf4j-server-webapp*/rdf4j-workbench/` and serves them from there. So after changing TypeScript,
TSX or CSS, either:

- rebuild and restart: `mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl tools/server-boot -am -Pquick install`, or
- for quick iteration, run `npm run build` in `tools/workbench` and copy the changed files from `target/frontend/`
  (or `src/main/frontend/static/`) into that extracted directory of the running server and reload the page, or
- for the graph explorer only, use `npm run dev` against the running server-boot.

### Jetty Maven plugin

The Workbench POM configures `jetty-ee11-maven-plugin` to run the Workbench from `src/main/webapp` plus
`target/frontend` together with `tools/server/target/rdf4j-server.war`. Step 2 must have built that WAR first.

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl tools/workbench jetty:run
```

### Docker (Tomcat 11 or Jetty 12)

`docker/build.sh` runs `mvn install -Pquick`, builds the distribution zip with `-Passembly`, and builds the image
from the WARs inside it:

```bash
cd docker && APP_SERVER=tomcat ./build.sh && APP_SERVER=tomcat docker compose up -d && cd ..
```

Use `APP_SERVER=jetty` for the Jetty image. Stop it with `cd docker && docker compose down -v`.

### Any servlet container

Deploy `tools/workbench/target/rdf4j-workbench.war` and `tools/server/target/rdf4j-server.war` to a Jakarta
Servlet 6.1 container (Tomcat 11, Jetty 12 ee11). The full distribution zip, which contains both WARs, comes from
`mvn -o -Dmaven.repo.local=.m2_repo -Passembly package -DskipTests` and ends up in `assembly/target/`.

## Test

Blank nodes in query results and Explore use `<_:id>` references. You can copy a reference into a SPARQL query or
update, the Explore resource field, or a resource/graph field in Remove, Add and Clear to reuse that exact node in
the same repository. Explore's **Copy resource** keeps the brackets and any Unicode escapes needed for the ID;
**Query this resource** opens a query using the reference. Nested triple terms retain these references too.
An ordinary SPARQL `_:label` still has SPARQL's existential or fresh-allocation meaning. References are scoped to
the repository and its stored blank node IDs. RDF downloads and the N-Quads view continue to use RDF serialization
syntax, which can assign different blank node labels.
Blank node aliases returned by `SERVICE` or federation are not reusable handles into their source repository;
ordinary result transport does not carry the provenance needed to establish that identity.

Focused coverage for this feature is in `ValueDecoderBlankNodeReferenceTest`,
`ExploreServletPageDataTest#fixedBlankNodeResourcesKeepTheirExactIdentityInRowsAndMetadata`,
`src/test/frontend/classic/workbench-blank-node-references.test.js` and
`e2e/tests/workbench-blank-node-references.spec.js`.

### Java unit tests

After step 2, either use the repository's runner:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench
python3 .codex/skills/mvnf/scripts/mvnf.py QueryServletTest
python3 .codex/skills/mvnf/scripts/mvnf.py QueryServletTest#someMethod
```

or plain Maven:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl tools/workbench verify
```

Never combine `-am` with a test run, and do not use `-q` when running tests. Reports land in
`tools/workbench/target/surefire-reports/`. These tests do not need Node.

### Front-end unit tests (Node, no server)

```bash
cd tools/workbench && npm test && cd ../..
```

`npm test` runs both halves; each also has its own script:

- `npm run test:classic` runs `node --test "src/test/frontend/classic/*.test.js"`, the tests of the classic pages.
  Most load the compiled `scripts/*.js` files into a fake browser harness. They read them from
  `tools/workbench/target/frontend/scripts` (override with `WORKBENCH_SCRIPT_DIR`), so run `npm run build` (or a Maven
  build) first, or they fail with a message naming that command; hand-written files such as `workbench-theme.js` come
  from `src/main/frontend/static/scripts`. Tests name a script by its served path (`scripts/template.js`) and
  `workbench-scripts.js` resolves it. A few tests compile individual `.ts` files to a temporary directory with the
  `tsc` from `node_modules/.bin`, which `npm run` puts on `PATH`.
- `npm run test:explorer` runs the graph explorer tests, `src/test/frontend/**/*.test.mjs`.

Coverage of every script compiled from `src/main/frontend/classic` (Node 22+):

```bash
cd tools/workbench && npm run test:classic:coverage && cd ../..
```

It runs the classic unit tests and prints a per-script coverage table. It fails when the total falls below floors set a
little under what the suite reaches today (55 % lines, 70 % branches, 60 % functions; override with
`WORKBENCH_COVERAGE_LINES`, `WORKBENCH_COVERAGE_BRANCHES` and `WORKBENCH_COVERAGE_FUNCTIONS`). It is not a 100 % gate:
only scripts that a test loads under their file path are counted, so scripts that tests evaluate under a relative name
(`workbenchViews.js`, `workbenchApp.js`, `queryStream.js`) show less coverage than the tests give them.

### Playwright end-to-end tests

`e2e/run.sh` builds RDF4J, starts it, waits for both endpoints, installs Playwright browsers and runs the suite:

```bash
cd e2e && ./run.sh spring-boot && cd ..
```

```bash
cd e2e && ./run.sh docker-tomcat && cd ..
```

Useful environment variables:

- `E2E_BROWSER=chromium` (or `firefox`, `webkit`): install and run only one browser. CI runs each in its own job.
- `E2E_SKIP_PLAYWRIGHT_INSTALL=true`: skip `npx playwright install --with-deps`.
- `E2E_DATA_DIR=/path`: app data directory for the Spring Boot runtime (default: a fresh temp directory).

`run.sh` calls plain `mvn install -Pquick`, so it uses your default `~/.m2` repository rather than `.m2_repo`.

To run specs against a server you already started, skip `run.sh`:

```bash
cd e2e && RDF4J_SERVER_BASE_URL=http://127.0.0.1:8080/rdf4j-server RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8080/rdf4j-workbench npx playwright test --project=chromium tests/workbench-navigation.spec.js && cd ..
```

Both URLs default to `127.0.0.1:8080`. On Linux, `run.sh` sets `FONTCONFIG_FILE=e2e/fontconfig/fonts.conf` so
layout-sensitive specs see the same font metrics as CI; set it yourself when running Playwright directly.
Use `npx playwright test --ui` for the interactive runner. Reports go to `e2e/playwright-report/`.

## What CI runs

`.github/workflows/pr-verify.yml` covers this module with:

- `formatting-and-quick-compile`: `mvn -Pformatting spotless:check` and a quick compile.
- `build`: `install -Pquick`, then all Java unit tests.
- `frontend-unit-tests`: `npm ci && npm run build` and `npm run test:classic` in `tools/workbench`, then `npm ci` and
  `npm run test:unit` in `e2e/` (a unit test of a Playwright helper), on Node 22.
- `e2e`: `e2e/run.sh` for {Spring Boot, Docker Tomcat} × {chromium, firefox, webkit} on Ubuntu with Node 22.
- `copyright-check`: `scripts/checkCopyrightPresent.sh`.

Every Maven job compiles the front end as part of `install -Pquick`.

## Checklist for a front-end change

1. Edit files under `src/main/frontend/` (classic TypeScript, the explorer, the CSS or the hand-written JS).
2. Run `npm run build` in `tools/workbench` (or a Maven build) and fix any compiler errors.
3. Run `npm test` in `tools/workbench`.
4. Rebuild server-boot (or copy the files into the running server) and check the page in a browser.
5. Run the relevant Playwright specs.
6. Commit the sources only; never commit `target/` or `node_modules/`.
