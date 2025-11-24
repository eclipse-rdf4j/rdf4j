# End-to-end tests

This directory contains end-to-end tests for the project. The suite now boots the RDF4J Server and Workbench using a Spring Boot wrapper with an embedded Tomcat instance, so Docker is no longer required.

The tests are written using Microsoft Playwright and interact with the server and workbench in a real browser.

## Running the tests

Requirements:
 - java
 - maven
 - npm
 - npx

The tests can be run using the `run.sh` script. The script builds the Spring Boot runner, launches it in the background, waits until the HTTP endpoints are reachable, and then executes the Playwright test suite.

To run the tests interactively use `npx playwright test --ui`
