---
title: "Eclipse rdf4j release notes"
layout: "doc"
---

# 3.0.0

Rdf4j 3.0 is a major new release of the Eclipse rdf4j framework. Some highlights:

- Major improvements to the SHACL Sail
- Cleanup of core APIs (removing deprecated and obsolete code)
- Preparing rdf4j for Java 11/12 compatibility
- Rdf4j Server / Workbench upgrade to Servlet API 3.1

For a complete overview, see [all issues fixed in 3.0](https://github.com/eclipse/rdf4j/milestone/17?closed=1).

## Upgrade notes

Rdf4j 3.0 contains several [backward incompatible changes](https://github.com/eclipse/rdf4j/issues?utf8=%E2%9C%93&q=is%3Aissue+label%3A%22Not+backwards+compatible%22+-label%3A%22wontfix%22+milestone%3A3.0.0). We distinguish changes that affect users of the Rdf4j tools (Console, Workbench, Server), changes that affect users of the Rdf4j APIs and libraries, and changes that affect third party Sail / Repository implementations. See the linked issues for further details.

### rdf4j tools users

- Due to an [up upgrade to Spring 5.1](https://github.com/eclipse/rdf4j/issues/1343), Rdf4j Server and Rdf4j Workbench now require a servlet container that supports Servlet API 3.1. For Apache Tomcat, that means Tomcat 8 or better. For Eclipse Jetty, it means Jetty 9 or better.
- The [RDFS inferencer has changed its default behavior for inferred triples](https://github.com/eclipse/rdf4j/issues/1227). 

### rdf4j API/library users

- The [deprecated Graph API has been removed](https://github.com/eclipse/rdf4j/issues/389). Any code that still uses `Graph`, `GraphImpl`, or `GraphUtil` will need to be rewritten to use corresponding features in the [Model API](https://rdf4j.eclipse.org/documentation/programming/model/#the-model-interface).
- [Deprecated methods have been removed from the Model API](https://github.com/eclipse/rdf4j/issues/748). Any existing code that uses the deprecated methods will need to be rewritten to use the recommend new methods.
- The [deprecated package org.eclipse.rdf4j.model.util.language](https://github.com/eclipse/rdf4j/issues/675) has been removed.

### third-party sail / repository implementors

- invoking `init()` (or `initialize()`) on a Repository or Sail object is [no longer mandatory](https://github.com/eclipse/rdf4j/issues/1223). 
- we allow [injection of optimizers](https://github.com/eclipse/rdf4j/issues/1280) into a Sail object. 
- as part of a massive restructure of our code repositories and the way in which the project is tested, [testsuites and compliance tests have been moved and refactored](https://github.com/eclipse/rdf4j/issues/1236). If you were depending on a particular testsuite and can no longer find it, contact us. 

# 2.5.4

Rdf4j 2.5.4 is a patch release containing several bug fixes, including:

- handling of negation in property paths
- stack overflow in Models.isomorphic
- NullPointerException in Native Store Context Cache initialization 

For a complete overview, see [all issues fixed in 2.5.4](https://github.com/eclipse/rdf4j/milestone/40?closed=1).

# 2.5.3

Rdf4j 2.5.3 is a patch release containing several bug fixes, including:

- custom HTTP header handling in HTTPRepository
- handling of queries with COUNT in the LuceneSail
- performance of context id retrieval in native store
- handling of FROM/FROM NAMED clauses in SparqlBuilder

For a complete overview, see [all issues fixed in 2.5.3](https://github.com/eclipse/rdf4j/milestone/37?closed=1).
