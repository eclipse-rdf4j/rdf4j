# End-to-end tests

This directory contains end-to-end tests for the project. These tests use docker to run the RDF4J server and workbench.

The tests are written using Microsoft Playwright and interact with the server and workbench using the browser.

## Running the tests

Requirements:
 - docker
 - java
 - maven
 - npm
 - npx

The tests can be run using the `run.sh` script. This script will build the project, start the server and workbench and run the tests.

To run the tests interactively use `npx playwright test --ui`

The RDF4J server and workbench can be started independently using the `run.sh` script in the `docker` directory.
