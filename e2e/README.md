# End-to-end tests

This directory contains end-to-end tests for the project. These tests use docker to run the RDF4J server and workbench.

The tests are written using Microsoft Playwright and interact with the server and workbench using the browser.

## Running the tests

Requirements:
 - docker (only needed when using the legacy docker-based workflow)
 - java
 - maven
 - npm
 - npx

The tests can be run using the `run.sh` script. This script will build the project, start the server and workbench using docker and run the tests.

### Running without docker

When developing locally it can be convenient to run the RDF4J server and workbench directly from Maven without relying on docker.

```
# build and install the server/workbench artifacts once
mvn -pl tools/workbench -am -DskipTests install

# start the server/workbench on http://localhost:8080
mvn -pl tools/workbench jetty:run -Prdf4j-dev-server

# stop the embedded Jetty instance
mvn -pl tools/workbench jetty:stop -Prdf4j-dev-server
```

Set `RDF4J_HTTP_PORT` if you need a different port and Jetty will be configured accordingly.

The `run-local.sh` script automates those steps, wires the correct base URL into the Playwright tests, and runs them against the embedded Jetty server:

```
./run-local.sh
# or for a custom port
RDF4J_HTTP_PORT=8091 ./run-local.sh
```

To run the tests interactively use `npx playwright test --ui`

The RDF4J server and workbench can be started independently using the `run.sh` script in the `docker` directory.
