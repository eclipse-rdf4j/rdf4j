# End-to-end tests

This directory contains end-to-end tests for the project. The suite can run against either the Spring Boot wrapper with embedded Tomcat or the Docker image that deploys the regular WAR files to Tomcat.

The tests are written using Microsoft Playwright and interact with the server and workbench in a real browser.

## Running the tests

Requirements:
 - Node.js 22 or newer
 - JDK 25 or newer
 - Maven 3.9.16 or newer
 - npm
 - npx
 - docker (for `docker-tomcat`)

The tests can be run using the `run.sh` script. The script builds the selected runtime, waits until the HTTP endpoints are reachable, and then executes the Playwright test suite.

Run against the Spring Boot implementation:

```bash
./run.sh spring-boot
```

Run against the Docker/Tomcat image:

```bash
./run.sh docker-tomcat
```

The default runtime is `spring-boot`, so `./run.sh` keeps the original local behavior.

CI sets `E2E_SHARDS=2` to run two Playwright processes within each existing browser/runtime job. Each spec stays on one
worker, preserving its shared repository and setup/teardown lifecycle. Linux Firefox and WebKit processes get separate
Xvfb displays so focus, scrolling and clipboard state cannot leak between them. Both processes finish even if one fails.
An isolated phase then runs tests tagged `@performance` with their existing timing budgets, plus the complete
`workbench-round3-server`, `workbench-destructive-actions`, `workbench-live-route-parity`, and
`workbench-visual-refinement-operation-states` specs. These specs submit live delete-safety checks, which scan every
repository configuration and must not overlap another spec's repository teardown. The phase uses one worker after both
functional shards finish, also preventing competing browser work from distorting performance assertions.
All results are merged into `playwright-report/` and a JSON summary
beside the raw blob reports. Any test or report-merge failure fails the job, as does an entirely empty test selection.
Explicit `--grep` or `--grep-invert` selections passed to `run-playwright.js` use one process so those filters retain
their original meaning.

To use the same concurrency locally, run `E2E_SHARDS=2 ./run.sh spring-boot`. The default remains one process. Raw reports
are saved under `blob-report/`, with separate test output under `test-results/shard-1/`, `test-results/shard-2/`, and
`test-results/isolated/`.

If Playwright browsers are already installed locally, set `E2E_SKIP_PLAYWRIGHT_INSTALL=true` to skip the browser installer.

Some specs serve an endpoint of their own to the server, such as a SPARQL endpoint that never answers. They give the server the address in `RDF4J_E2E_HOST_FROM_SERVER` (default `127.0.0.1`). For `docker-tomcat`, `run.sh` sets it to `host.docker.internal`, which `docker-compose.e2e.yml` maps to this host also on Linux; set it yourself when the server runs in a container you started.

Some specs also stand in for RDF4J Servers of their own (`tests/server-stand-in.js`: one that refuses the user, one without repositories) and point the Workbench at them. The Workbench connects only to servers its accepted-server-prefixes allow, so `run.sh` starts it with `-Dorg.eclipse.rdf4j.workbench.accepted-server-prefixes="/rdf4j-server http://<RDF4J_E2E_HOST_FROM_SERVER>:"`. Start a server of your own the same way to run those specs against it.

The Workbench imports RDF only from approved URL origins. Some specs add from a missing file on the server itself and from `RDF4J_E2E_REFUSED_URL_ORIGIN` (default `http://127.0.0.1:9`, where nothing listens), so `run.sh` starts the Workbench with `-Dorg.eclipse.rdf4j.workbench.add-url-allowed-origins=http://127.0.0.1:8080,<that origin>`. `run.sh` also exports `RDF4J_SERVER_BASE_URL` and `RDF4J_WORKBENCH_BASE_URL`; specs that change repository configuration run only when both are set, so set them yourself, together with the properties above, when you run the specs against a server you started.

To run the tests interactively use `npx playwright test --ui`

## HTTP compression

The focused HTTP compression spec can be run against any RDF4J Server endpoint:

```bash
RDF4J_SERVER_BASE_URL=http://127.0.0.1:8080/rdf4j-server \
RDF4J_E2E_TARGET_NAME=spring-boot npm run test:compression
```

To verify both the Spring Boot runner and the regular WAR deployment in Docker:

```bash
./run-http-compression.sh
```
