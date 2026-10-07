# RDF4J Workbench

The Workbench (`rdf4j-http-workbench`) is the browser UI for RDF4J Server. This module holds:

| Path | What it is |
| --- | --- |
| `src/main/java` | Servlets that render pages and proxy requests to RDF4J Server |
| `src/main/webapp/scripts/ts/*.ts` | TypeScript sources for the front end |
| `src/main/webapp/scripts/*.js`, `*.js.map` | JavaScript compiled from those sources (**committed**) |
| `src/main/webapp/scripts/` (other files) | Hand-written or vendored JavaScript (see below) |
| `src/main/webapp/styles/*.css` | Plain CSS, served as is |
| `compileTypescript.sh` | Compiles `scripts/ts/*.ts` into `scripts/` |

All commands below run from the **repository root** unless stated otherwise.

## The one thing to know

**Maven does not compile the TypeScript.** The Maven build copies whatever JavaScript is in
`src/main/webapp/scripts/` into the WAR. If you change a `.ts` file, you must run
`compileTypescript.sh` yourself and commit the `.ts` file together with the regenerated `.js` and `.js.map` files.
Otherwise the WAR, the Spring Boot runner, the Node unit tests and the Playwright tests all keep using the old
JavaScript.

## Prerequisites

| Tool | Version | Needed for |
| --- | --- | --- |
| JDK | 25 or newer | Maven build, running the server |
| Maven | 3.9+ | Maven build |
| Node.js + npm | 22 (CI uses 22 for e2e and 20 for unit tests; coverage needs 22+) | TypeScript compiler, front-end tests, Playwright |
| Docker | any recent | Only for the Docker/Tomcat runtime |

The TypeScript compiler is **not** installed globally. It is pinned to `typescript@5.9.3` in `e2e/package.json`.
Use that copy so the generated JavaScript matches what is committed.

The repository convention is to use a workspace-local Maven repository (`-Dmaven.repo.local=.m2_repo`) and to run
Maven offline (`-o`). The first build on a fresh checkout must download dependencies, so drop `-o` once.

## Build everything from a clean checkout

### 1. Install the Node tooling (once, and whenever `e2e/package-lock.json` changes)

```bash
cd e2e && npm ci && cd ..
```

This installs `tsc` (TypeScript 5.9.3) and Playwright into `e2e/node_modules`.

### 2. Compile the TypeScript

```bash
PATH="$PWD/e2e/node_modules/.bin:$PATH" bash tools/workbench/compileTypescript.sh
```

The script runs, in `src/main/webapp/scripts/ts`:

```
tsc --noImplicitAny --sourcemap --sourceRoot /rdf4j-workbench/scripts/ts --outDir ../ *.ts
```

- There is no `tsconfig.json`; every `*.ts` file in `scripts/ts` is compiled in one run.
- Each `foo.ts` becomes `scripts/foo.js` plus `scripts/foo.js.map`. There is no bundler. The files share the global
  `workbench` namespace, reference each other with `/// <reference path="..." />`, and are loaded with plain
  `<script>` tags.
- `codemirror.d.ts`, `jquery.d.ts` and `yasqe.d.ts` are type declarations only; they produce no output.
- The script stops with a non-zero exit code on any compiler error and prints
  `Replaced repository JavaScript files with compiled TypeScript versions.` only on success.

Files in `scripts/` that are **not** generated, so edit them directly (or not at all, for vendored code):

- `workbench-theme.js`, `workbench-lit-html.mjs`, `cookies.html` (hand-written)
- `jquery-1.11.0*`, `codemirror.4.5.0.min.js`, `yasqe*`, `diff.min.js`, `svg-pan-zoom.min.js`, `viz/`,
  `vendor/lit-html-3.3.3/` (vendored third-party libraries)

### 3. Build and install with Maven

Build the whole repository without tests (about 30 seconds on a warm machine):

```bash
mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
```

Or only the Workbench and the modules it depends on:

```bash
mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl tools/workbench -am -Pquick install
```

The `quick` profile skips tests, formatting, import sorting, japicmp and the enforcer. Outputs in
`tools/workbench/target/`:

- `rdf4j-workbench.war`: the deployable web application.
- `rdf4j-workbench-classes.jar`: the servlet classes, installed as the
  `rdf4j-http-workbench-<version>-classes.jar` artifact. The Spring Boot runner depends on it.

Always install before running tests in this or any downstream module. Test runs resolve sibling modules from
`.m2_repo`, so they only see changes that have been installed there.

### 4. Format before committing

```bash
mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
```

The `formatting` profile (active on JDK 25+) runs Spotless on Java and XML files during `process-resources`.
It does not touch TypeScript, JavaScript or CSS. Also check copyright headers on new files:

```bash
cd scripts && ./checkCopyrightPresent.sh && cd ..
```

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

How server-boot picks up front-end changes: its build copies `tools/workbench/src/main/webapp` into the jar as
classpath resources (`rdf4j/workbench-webapp`). At startup it extracts them to
`${java.io.tmpdir}/rdf4j-server-webapp*/rdf4j-workbench/` and serves them from there. So after changing JavaScript
or CSS, either:

- rebuild and restart: `mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl tools/server-boot -am -Pquick install`, or
- for quick iteration, copy the changed `scripts/*.js` / `styles/*.css` into that extracted directory of the running
  server and reload the page.

### Jetty Maven plugin

The Workbench POM configures `jetty-ee11-maven-plugin` to run the Workbench from `src/main/webapp` together with
`tools/server/target/rdf4j-server.war`. Step 3 must have built that WAR first.

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

### Java unit tests

After step 3, either use the repository's runner:

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
cd e2e && npm run test:unit && cd ..
```

This runs `node --test tests-unit/*.test.js`. Most tests load the **committed** `scripts/*.js` files into a fake
browser harness, so compile the TypeScript (step 2) first or they test stale code. A few tests compile individual
`.ts` files to a temporary directory with the `tsc` from `e2e/node_modules/.bin`, which `npm run` puts on `PATH`.

Coverage of every script compiled from `scripts/ts` (Node 22+):

```bash
cd e2e && npm run test:unit:coverage && cd ..
```

It runs the same unit tests and prints a per-script coverage table. It fails when the total falls below floors set a
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
- `frontend-unit-tests`: `npm ci` and `npm run test:unit` in `e2e/` on Node 20.
- `e2e`: `e2e/run.sh` for {Spring Boot, Docker Tomcat} × {chromium, firefox, webkit} on Ubuntu with Node 22.
- `copyright-check`: `scripts/checkCopyrightPresent.sh`.

No CI job compiles the TypeScript. If the committed JavaScript is out of date, CI tests the old JavaScript.

## Checklist for a front-end change

1. Edit `src/main/webapp/scripts/ts/*.ts` (or the CSS / hand-written JS).
2. Run `compileTypescript.sh` (step 2) and fix any compiler errors.
3. Run `npm run test:unit` in `e2e/`.
4. Rebuild server-boot (or copy the files into the running server) and check the page in a browser.
5. Run the relevant Playwright specs.
6. Commit the `.ts` file together with the regenerated `.js` and `.js.map` files.
