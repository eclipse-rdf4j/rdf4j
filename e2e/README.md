# End-to-end tests

This directory contains end-to-end tests for the project. The suite can run against either the Spring Boot wrapper with embedded Tomcat or the Docker image that deploys the regular WAR files to Tomcat.

The tests are written using Microsoft Playwright and interact with the server and workbench in a real browser.

## Running the tests

Requirements:
 - java
 - maven
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

If Playwright browsers are already installed locally, set `E2E_SKIP_PLAYWRIGHT_INSTALL=true` to skip the browser installer.

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
