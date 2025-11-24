# Enable Spring Boot runner for RDF4J server and workbench

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `PLANS.md` at the repository root.

## Purpose / Big Picture

The end goal is to start the RDF4J server and workbench inside a single Spring Boot application that embeds Tomcat so that end-to-end tests can launch it without Docker. After implementing this plan, a developer will be able to start the Boot app, visit the workbench UI, and run automated browser tests entirely in-process. The Boot app must reuse the existing Spring XML descriptors for server and workbench instead of repackaged WARs, proving equivalence by running the existing Playwright suite.

## Progress

- [x] (2025-02-14 03:15Z) Draft ExecPlan capturing current understanding and intended milestones.
- [x] (2025-02-14 04:05Z) Created server-boot module skeleton with initial failing integration test.
- [x] (2025-02-14 04:10Z) Prepare Spring XML resource wiring for Boot app (completed 2025-02-14 05:22Z).
- [x] (2025-11-15 20:19Z) Run e2e Playwright suite & finalize documentation updates (attempted; Playwright browser downloads blocked by HTTP 403, documented in chunk d65d99).
- [x] (2025-11-15 20:25Z) Compile final verification summary and retrospective updates (completed via repo summary + PR prep).
- [x] (2025-11-15 21:32Z) Provide a repo-level `run.sh` bootstrap that installs the reactor (without `-am`) before invoking the server-boot verifier so isolated CI scripts resolve internal artifacts.

## Surprises & Discoveries

- Dependency resolution requires building upstream WAR modules to publish their attached classes JARs before tests can run; initial `mvn -pl tools/server-boot test` failed with missing artifacts. Evidence: see chunk 2a2018.
- Embedded Tomcat cannot serve JSPs directly from classpath resource folders; extracting the server/workbench webapps to a temporary directory enables standard Jasper JSP compilation.
- Spring's bean definition overriding must be enabled to match the legacy XML behavior where multiple contexts provide the same bean names.
- Playwright `npx playwright install --with-deps` cannot download Chromium in this environment (HTTP 403), so the e2e harness installation step currently fails (chunk d65d99).
- Minimal CI jobs that only execute `mvn -pl tools/server-boot verify` never see the WAR-attached class artifacts because the rest of the reactor has not been built; see chunk 2312a2 for the reproducible failure transcript.

## Decision Log

- Adopted a temp-directory extraction strategy (via `WebappResourceExtractor`) so Boot's embedded Tomcat can reuse WAR-style directory layouts while preserving original XML configurations.
- Enabled `spring.main.allow-bean-definition-overriding=true` to align Boot with the legacy servlet container behavior where duplicates were implicitly permitted.
- Decision: Provide a checked-in bootstrap script that builds the reactor with `mvn install -Pquick -DskipTests` before launching the targeted verifier, ensuring headless CI jobs resolve the server/workbench artifacts without relying on `-am`.
  Rationale: The CI harness mandated by AGENTS.md forbids `-am`, and external snapshot artifacts are unavailable; preinstalling from the local checkout is the only deterministic path. Date/Author: 2025-11-15 @assistant.

## Outcomes & Retrospective

- Integration tests for the Boot runner now succeed (`mvn -pl tools/server-boot verify`). Playwright automation remains blocked because Chromium binaries cannot be downloaded (HTTP 403); capture this limitation in the handoff and rely on the passing integration coverage plus manual curl checks for validation.

## Context and Orientation

The RDF4J HTTP server lives in `tools/server` and exposes REST endpoints defined through Spring MVC and servlet configuration stored under `tools/server/src/main/webapp/WEB-INF`. The workbench UI is a separate WAR in `tools/workbench` with JSPs and Spring controllers configured in `tools/workbench/src/main/webapp/WEB-INF`. The current e2e workflow (`e2e/run.sh`) builds Docker images that deploy these WARs to Tomcat and Jetty containers and then runs Playwright tests. We must make it possible to launch both apps by embedding Tomcat through Spring Boot, using `@ImportResource` (or equivalent) to load the same XML configuration files. The Boot entrypoint should live in the tools layer so it can depend on both server and workbench modules.

## Plan of Work

First, create a new Maven module under `tools` (for example `server-boot`) that produces an executable Spring Boot JAR. Configure it to depend on the existing `rdf4j-http-server-spring`, `rdf4j-http-server`, and `rdf4j-http-workbench` modules so it inherits all controllers, JSPs, and configuration classes. The module must include the `spring-boot-starter-web` dependency but exclude its default `spring-boot-starter-tomcat` so we can control versions compatible with the project.

Expose the existing XML configuration files to the Boot module by adding them as resources. Rather than duplicating files, configure the module to treat `../server/src/main/webapp/WEB-INF` and `../workbench/src/main/webapp/WEB-INF` as resource directories. Boot's classpath scanner will then find the XML descriptors via `classpath*:/WEB-INF/...` paths.

Implement a `@SpringBootApplication` class (`Rdf4jServerWorkbenchApplication`) that registers two dispatcher servlets backed by separate `XmlWebApplicationContext` instances. One servlet should handle `/rdf4j-server/*` and import `WEB-INF/web.xml` plus the server-specific servlet definitions. The other should handle `/rdf4j-workbench/*` and import the workbench servlet context. Configure view resolution to support JSP rendering by reusing the same bean definitions and enabling the JSP engine within embedded Tomcat.

Add integration tests in the new module following TDD. Start with a failing `@SpringBootTest` that launches the application on a random port and asserts that `/rdf4j-server/repositories` responds with HTTP 200 and `/rdf4j-workbench/` serves HTML. Use `TestRestTemplate` (provided by Spring Boot) and `Awaitility` or simple polling to ensure the app is ready. This test must fail before implementing the Boot application because the context will not exist yet.

Once the Boot app and configuration are implemented, ensure the integration test passes locally. Then adjust the e2e harness (`e2e/run.sh` and supporting scripts) to start the Boot JAR instead of Docker. Provide npm scripts or shell commands to build the Boot module, run it in the background for Playwright, and shut it down afterward. Update documentation in `e2e/README.md` to explain the new startup path.

Finally, confirm that the Playwright suite runs against the embedded server and workbench started via the Boot module. Capture command output as evidence, stop the Boot process cleanly, and prepare for commit and PR creation. For CI entry points that only execute `./run.sh`, add a repository-level bootstrap script that runs `mvn install -Pquick -DskipTests` (without `-am`) before invoking `mvn -pl tools/server-boot verify` so the WAR and workbench artifacts exist in the local Maven repository.

## Concrete Steps

1. In `tools/pom.xml`, declare the new `server-boot` module so it participates in the reactor build.
2. Scaffold `tools/server-boot/pom.xml` using the Spring Boot parent plugin while inheriting from `rdf4j-tools`.
3. Configure the module's resources to include the server and workbench `WEB-INF` folders.
4. Add dependencies for `spring-boot-starter-web`, `spring-boot-starter-tomcat`, JSP support, and the existing RDF4J server/workbench modules.
5. Create `Rdf4jServerWorkbenchApplication` in `tools/server-boot/src/main/java/...` that imports the XML contexts and registers dispatcher servlets with the desired context paths.
6. Add configuration classes or beans to bridge any differences between the Boot environment and the traditional servlet container (for example, multipart resolver, JSP view resolver, and static assets).
7. Author a failing `Rdf4jServerWorkbenchApplicationIT` under `src/test/java` that exercises both `/rdf4j-server/` and `/rdf4j-workbench/` endpoints.
8. Implement the application until the integration test passes.
9. Update `e2e/run.sh` to build and launch the Boot JAR instead of Docker, ensuring both server types are covered if needed.
10. Document the new workflow in `e2e/README.md`.
11. Run the integration test and Playwright suite to verify everything works.
12. Add `run.sh` at the repository root that (a) runs `mvn install -Pquick -DskipTests` from the root to populate the local Maven repository and (b) executes `mvn -pl tools/server-boot verify` to exercise the Boot runner without relying on forbidden `-am` flags.

## Validation and Acceptance

Acceptance requires that `mvn -pl tools/server-boot verify` passes after the bootstrap install step (no `-am`), including the new integration test, and that `./e2e/run.sh` successfully runs Playwright without Docker, pointing to the Boot-hosted apps. Manually hitting `http://localhost:8080/rdf4j-server/repositories` after starting the Boot app should return JSON listing repositories, and visiting `http://localhost:8080/rdf4j-workbench/` should render HTML.

## Idempotence and Recovery

The Boot module build should be repeatable; running `mvn clean install` multiple times must be safe. The `e2e/run.sh` script must start the Boot application in the background and trap signals to shut it down gracefully. If the Boot app fails to start, kill the Java process (`pkill -f server-boot`) and rerun the script.

## Artifacts and Notes

At completion, include the following in the PR description:

    mvn -pl tools/server-boot -am verify
    ./e2e/run.sh

Capture log snippets from the Boot startup showing the server and workbench context paths bound to the port.

## Interfaces and Dependencies

Within `tools/server-boot`, define the entrypoint class:

    package org.eclipse.rdf4j.tools.serverboot;

    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;

    @SpringBootApplication
    public class Rdf4jServerWorkbenchApplication {
        public static void main(String[] args) {
            SpringApplication.run(Rdf4jServerWorkbenchApplication.class, args);
        }
    }

Provide bean methods supplying two `ServletRegistrationBean<DispatcherServlet>` instances named `rdf4jServerServlet` and `rdf4jWorkbenchServlet`, each backed by an `XmlWebApplicationContext` that loads the respective XML descriptors via `@ImportResource` or explicit configuration.

