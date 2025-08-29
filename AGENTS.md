# Codex Agent Playbook

Welcome, Codex Agent! Your persistence, curiosity, and craftsmanship make a difference. Take your time, work methodically, validate thoroughly, and iterate. This repository is large and tests can take time — that’s expected and supported.

## Purpose & Contract
- Bold goal: deliver correct, minimal, well‑tested changes with clear handoff.
- Bias to action: when inputs are ambiguous, choose a reasonable path, state assumptions, and proceed.
- Ask only when blocked or irreversible: escalate questions only if you are truly blocked (permissions, missing deps, conflicting requirements) or a choice is high‑risk/irreversible.
- Definition of Done:
  - Code formatted and imports sorted.
  - Compiles with quick profile.
  - Relevant module tests pass; failures triaged or explained.
  - Only necessary files changed; headers correct for new files.
  - Clear final summary: what changed, why, where, how verified, next steps.

## Environment
- JDK: 11 (see root `pom.xml` `java.version=11`).
- Maven default: run offline using `-o` whenever possible.
- Network: only when needed to fetch missing deps/plugins; then omit `-o`.
- Large project: tests may take 5–10 min per module. Be patient and thorough.

## Quick Start (First 10 Minutes)
1. Discover
   - List modules: open root `pom.xml` or see “Maven Module Overview” below.
   - Search code fast with `rg`: `rg -n "<symbol or string>"`.
2. Build sanity
   - Compile fast (skip tests): `mvn -o -Pquick verify -DskipTests | tail -1000`
3. Format
   - `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
4. Targeted tests
   - By module: `mvn -o -pl <module> test`
   - Single class: `mvn -o -pl <module> -Dtest=ClassName test`
   - Single method: `mvn -o -pl <module> -Dtest=ClassName#method test`
5. Inspect failures
   - Unit: `<module>/target/surefire-reports/`
   - IT: `<module>/target/failsafe-reports/`

## Working Loop
- Plan
  - Break task into small, verifiable steps; keep one step in progress.
  - Share short progress updates as you switch phases.
  - Decide and proceed autonomously; document assumptions as you go.
- Change
  - Make minimal, surgical edits. Keep style consistent.
- Format
  - `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
- Compile
  - `mvn -o -Pquick verify -DskipTests | tail -1000`
- Test
  - Start with the smallest scope that exercises your change.
- Triage
  - Read reports; fix root cause; expand test scope only when needed.
- Iterate
  - Keep moving without waiting for confirmation between steps. Escalate only at blocking points. Repeat until Definition of Done is satisfied.

## Planning & Progress
- Living plan: update as you learn; one active step at a time.
- Good steps: 5–7 words each, outcome‑oriented.
- Progress updates: one crisp sentence when switching steps or after long runs.
- Decide early: if scope is unclear, pick the most reasonable option, note the assumption, and continue.
- Escalate sparingly: ask only if options diverge significantly in cost/impact or you are blocked (permissions, network fetches beyond policy, missing secrets).
- Checkpoint cadence: send updates to inform, not to request permission.

## Testing Strategy
- Prefer module tests you touched: `-pl <module>`.
- Narrow further to a class/method for tight feedback loops.
- Expand scope when:
  - Your change crosses module boundaries.
  - Failures in a neighbor module indicate integration impact.
- Read reports:
  - Surefire (unit): `target/surefire-reports/`
  - Failsafe (IT): `target/failsafe-reports/`
- Helpful flags:
  - `-Dtest=Class#method`
  - `-DtrimStackTrace=false`
  - `-DskipITs` to focus on unit tests when appropriate.

## Triage Playbook
- Missing dep/plugin offline
  - Remedy: re-run without `-o` for that step only.
- Compilation errors
  - Fix imports, generics, visibility; re-run quick verify with tests skipped.
- Flaky/slow tests
  - Run the specific failing test; read its report; stabilize cause before broad runs.
- Formatting failures
  - Run formatter command; re-verify.
- License header missing
  - Add header for new files only (see “Source File Headers”); don’t change years on existing files.

## Code Formatting
- Always run before finalizing:
  - `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
- Style: no wildcard imports, 120-char width, curly braces always, LF line endings.
- New files must include the exact RDF4J header (see below).

## Source File Headers
Use this header for new Java files only (replace ${year} with current year):

```
/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
```

Do not modify existing headers’ years.

## Pre‑Commit Checklist
- Format: `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
- Compile: `mvn -o -Pquick verify -DskipTests | tail -1000`
- Tests: `mvn -o -pl <module> test` (extend scope if needed)
- Reports: zero new failures in `target/surefire-reports/` or explain remaining issues.

## Navigation & Search
- Fast file search: `rg --files`
- Fast content search: `rg -n "<pattern>"`
- Read big files in chunks:
  - `sed -n '1,200p' path/to/File.java`
  - `sed -n '201,400p' path/to/File.java`

## Autonomy Rules (Act > Ask)
- Default: act with assumptions. Document assumptions in your plan and final answer.
- Keep going: chain steps without waiting for permission; send short progress updates before long actions.
- Ask only when:
  - Blocked by sandbox/approvals/network policy or missing secrets.
  - The decision is destructive/irreversible, repo‑wide, or impacts public APIs.
  - Adding dependencies, changing build profiles, or altering licensing.
- Prefer reversible moves: take the smallest local change that unblocks progress; add/execute targeted tests to validate before expanding scope.
- Choose defaults:
  - Tests: start with `-pl <module>` then `-Dtest=Class#method`.
  - Build: use `-o` quick profile; drop `-o` once to fetch missing deps, then return offline.
  - Formatting: run formatter/impsort/xml‑format proactively before verify.
  - Reports: read surefire/failsafe locally; expand scope only when needed.
- Error handling:
  - On compile/test failure: fix root cause locally, rerun targeted tests, then broaden.
  - On flaky tests: rerun class/method, stabilize cause before repo‑wide runs.
  - On formatting/license issues: apply prescribed commands/headers immediately.
- Communication:
  - Preambles: 1–2 sentences grouping upcoming actions.
  - Updates: inform to maintain visibility; do not request permission unless in “Ask only when” above.

## Answer Template (Use This)
- What changed: summary of approach and rationale.
- Files touched: list file paths.
- Commands run: key build/test commands.
- Verification: which tests passed, where you checked reports.
- Assumptions: key assumptions and autonomous decisions you made.
- Limitations: anything left or risky edge cases.
- Next steps: optional suggestions for follow-ups.

## Mindset & Motivation
- Thorough > fast: slow is smooth and smooth is fast.
- You have time: long builds/tests are normal here.
- Work visibly: keep your plan current and share progress.
- Be curious: trace root causes; avoid band‑aids.
- Be surgical: minimal diffs; don’t fix unrelated issues unless asked.
- Autonomous‑first: keep making progress independently; ask only when blocked or the decision is irreversible/high‑risk.

## Running Tests
- By module:
  - `mvn -o -pl core/sail/shacl test`
- Entire repo:
  - `mvn -o verify` (long; only when appropriate)
- Useful flags:
  - `-Dtest=ClassName`
  - `-Dtest=ClassName#method`

## Build
- Build without tests:
  - `mvn -o verify -Pquick`
- Verify with tests:
  - `mvn -o verify`
- When offline fails due to missing deps:
  - Re-run the exact command without `-o` once to fetch, then return to `-o`.

## Maven Module Overview

The project is organised as a multi-module Maven build. The diagram below lists
all modules and submodules with a short description for each.

```
rdf4j: root project
├── assembly-descriptors: RDF4J: Assembly Descriptors
├── core: Core modules for RDF4J
    ├── common: RDF4J common: shared classes
    │   ├── annotation: RDF4J common annotation classes
    │   ├── exception: RDF4J common exception classes
    │   ├── io: RDF4J common IO classes
    │   ├── iterator: RDF4J common iterators
    │   ├── order: Order of vars and statements
    │   ├── text: RDF4J common text classes
    │   ├── transaction: RDF4J common transaction classes
    │   └── xml: RDF4J common XML classes
    ├── model-api: RDF model interfaces.
    ├── model-vocabulary: Well-Known RDF vocabularies.
    ├── model: RDF model implementations.
    ├── sparqlbuilder: A fluent SPARQL query builder
    ├── rio: Rio (RDF I/O) is an API for parsers and writers of various RDF file formats.
    │   ├── api: Rio API.
    │   ├── languages: Rio Language handler implementations.
    │   ├── datatypes: Rio Datatype handler implementations.
    │   ├── binary: Rio parser and writer implementation for the binary RDF file format.
    │   ├── hdt: Experimental Rio parser and writer implementation for the HDT file format.
    │   ├── jsonld-legacy: Rio parser and writer implementation for the JSON-LD file format.
    │   ├── jsonld: Rio parser and writer implementation for the JSON-LD file format.
    │   ├── n3: Rio writer implementation for the N3 file format.
    │   ├── nquads: Rio parser and writer implementation for the N-Quads file format.
    │   ├── ntriples: Rio parser and writer implementation for the N-Triples file format.
    │   ├── rdfjson: Rio parser and writer implementation for the RDF/JSON file format.
    │   ├── rdfxml: Rio parser and writer implementation for the RDF/XML file format.
    │   ├── trix: Rio parser and writer implementation for the TriX file format.
    │   ├── turtle: Rio parser and writer implementation for the Turtle file format.
    │   └── trig: Rio parser and writer implementation for the TriG file format.
    ├── queryresultio: Query result IO API and implementations.
    │   ├── api: Query result IO API
    │   ├── binary: Query result parser and writer implementation for RDF4J's binary query results format.
    │   ├── sparqljson: Query result writer implementation for the SPARQL Query Results JSON Format.
    │   ├── sparqlxml: Query result parser and writer implementation for the SPARQL Query Results XML Format.
    │   └── text: Query result parser and writer implementation for RDF4J's plain text boolean query results format.
    ├── query: Query interfaces and implementations
    ├── queryalgebra: Query algebra model and evaluation.
    │   ├── model: A generic query algebra for RDF queries.
    │   ├── evaluation: Evaluation strategy API and implementations for the query algebra model.
    │   └── geosparql: Query algbebra implementations to support the evaluation of GeoSPARQL.
    ├── queryparser: Query parser API and implementations.
    │   ├── api: Query language parsers API.
    │   └── sparql: Query language parser implementation for SPARQL.
    ├── http: Client and protocol for repository communication over HTTP.
    │   ├── protocol: HTTP protocol (REST-style)
    │   └── client: Client functionality for communicating with an RDF4J server over HTTP.
    ├── queryrender: Query Render and Builder tools
    ├── repository: Repository API and implementations.
    │   ├── api: API for interacting with repositories of RDF data.
    │   ├── manager: Repository manager
    │   ├── sail: Repository that uses a Sail stack.
    │   ├── dataset: Implementation that loads all referenced datasets into a wrapped repository
    │   ├── event: Implementation that notifies listeners of events on a wrapped repository
    │   ├── http: "Virtual" repository that communicates with a (remote) repository over the HTTP protocol.
    │   ├── contextaware: Implementation that allows default values to be set on a wrapped repository
    │   └── sparql: The SPARQL Repository provides a RDF4J Repository interface to any SPARQL end-point.
    ├── sail: Sail API and implementations.
    │   ├── api: RDF Storage And Inference Layer ("Sail") API.
    │   ├── base: RDF Storage And Inference Layer ("Sail") API.
    │   ├── inferencer: Stackable Sail implementation that adds RDF Schema inferencing to an RDF store.
    │   ├── memory: Sail implementation that stores data in main memory, optionally using a dump-restore file for persistence.
    │   ├── nativerdf: Sail implementation that stores data directly to disk in dedicated file formats.
    │   ├── model: Sail implementation of Model.
    │   ├── shacl: Stacked Sail with SHACL validation capabilities
    │   ├── lmdb: Sail implementation that stores data to disk using LMDB.
    │   ├── lucene-api: StackableSail API offering full-text search on literals, based on Apache Lucene.
    │   ├── lucene: StackableSail implementation offering full-text search on literals, based on Apache Lucene.
    │   ├── solr: StackableSail implementation offering full-text search on literals, based on Solr.
    │   ├── elasticsearch: StackableSail implementation offering full-text search on literals, based on Elastic Search.
    │   ├── elasticsearch-store: Store for utilizing Elasticsearch as a triplestore.
    │   └── extensible-store: Store that can be extended with a simple user-made backend.
    ├── spin: SPARQL input notation interfaces and implementations
    ├── client: Parent POM for all RDF4J parsers, APIs and client libraries
    ├── storage: Parent POM for all RDF4J storage and inferencing libraries
    └── collection-factory: Collection Factories that may be reused for RDF4J
        ├── api: Evaluation
        ├── mapdb: Evaluation
        └── mapdb3: Evaluation
├── tools: Server, Workbench, Console and other end-user tools for RDF4J.
    ├── config: RDF4J application configuration classes
    ├── console: Command line user interface to RDF4J repositories.
    ├── federation: A federation engine for virtually integrating SPARQL endpoints
    ├── server: HTTP server implementing a REST-style protocol
    ├── server-spring: HTTP server implementing a REST-style protocol
    ├── workbench: Workbench to interact with RDF4J servers.
    ├── runtime: Runtime dependencies for an RDF4J application
    └── runtime-osgi: OSGi Runtime dependencies for an RDF4J application
├── spring-components: Components to use with Spring
    ├── spring-boot-sparql-web: HTTP server component implementing only the SPARQL protocol
    ├── rdf4j-spring: Spring integration for RDF4J
    └── rdf4j-spring-demo: Demo of a spring-boot project using an RDF4J repo as its backend
├── testsuites: Test suites for Eclipse RDF4J modules
    ├── model: Reusable tests for Model API implementations
    ├── rio: Test suite for Rio
    ├── queryresultio: Reusable tests for QueryResultIO implementations
    ├── sparql: Test suite for the SPARQL query language
    ├── repository: Reusable tests for Repository API implementations
    ├── sail: Reusable tests for Sail API implementations
    ├── lucene: Generic tests for Lucene Sail implementations.
    ├── geosparql: Test suite for the GeoSPARQL query language
    └── benchmark: RDF4J: benchmarks
├── compliance: Eclipse RDF4J compliance and integration tests
    ├── repository: Compliance testing for the Repository API implementations
    ├── rio: Tests for parsers and writers of various RDF file formats.
    ├── model: RDF4J: Model compliance tests
    ├── sparql: Tests for the SPARQL query language implementation
    ├── lucene: Compliance Tests for LuceneSail.
    ├── solr: Tests for Solr Sail.
    ├── elasticsearch: Tests for Elasticsearch.
    └── geosparql: Tests for the GeoSPARQL query language implementation
├── examples: Examples and HowTos for use of RDF4J in Java
├── bom: RDF4J Bill of Materials (BOM)
└── assembly: Distribution bundle assembly
```

## Safety & Boundaries
- Don’t commit or push unless explicitly asked.
- Don’t add new dependencies without explicit approval.
- Use approvals sparingly: request approval only for network fetches when offline fails, destructive operations, or repo‑wide impacts. Otherwise proceed locally and continue working.
