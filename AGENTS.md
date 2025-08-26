# Codex Agent Guidelines

**Welcome, Codex Agent!** Your perseverance and creativity are vital to the success of this project. Approach each task with determination and keep pushing forward. Even if you stumble, learn from the experience and try again. Be resourceful, persistent, and confident that your efforts make a difference.

This is a large multi-module Maven project. Building and testing the entire repository can take considerable time. It's not unusual for test execution to take 5-10 minutes per module, but you should run the tests anyway to ensure your changes don't break anything. Sometimes the tests don't take that long, you can run them and see how long they actually take.

Take your time. You have a lot more time than you think :)

## Build
- Always invoke Maven in offline mode using the `-o` flag.
- Occasionally, you may need online access to download dependencies or plugins. When Maven requires internet access, omit `-o`.
- To build the entire project without running tests:
  ```bash
  mvn -o install -Pquick
  ```
- To build the project and run all tests:
  ```bash
  mvn -o verify
  ```

Running all modules sequentially will take a long time.

Maven stores all build output in each module's `target/` directory, `target/surefire-reports/` for test reports, and `target/failsafe-reports/` for integration test reports. When tests are failing, you should look in these directories for more information.

## Code Formatting
- This project has strict code formatting requirements. Always run the super fast automatic formatter before finalizing your code:
```bash
mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format
```

## Running Tests
- To test a specific module, use the `-pl` option (never use `-am` when running tests, if compilation fails then first build the project before running tests). Example for running the SHACL tests:
  ```bash
  mvn -o -pl core/sail/shacl test
  ```

## Pre-commit checklist
Before finalizing your work, make sure the following commands succeed:
1. **Format the code**
   ```bash
   mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format
   ```
2. **Check that the code compiles**
   ```bash
   mvn -o -Pquick verify -DskipTests | tail -1000
   ```
3. **Run the tests for the relevant module(s)**
   ```bash
   mvn -o -pl <module> test
   ```
   You can also run from a module subdirectory; just remember to include `-o`.

## Source File Headers
- All new source files must include the standard RDF4J copyright header.
- Use the template from `CONTRIBUTING.md` exactly as provided:
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
- Replace `${year}` with the current year for new files only.
- Do not modify or omit any other part of the header.

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
