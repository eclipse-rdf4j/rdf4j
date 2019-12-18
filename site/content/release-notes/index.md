---
title: "Eclipse RDF4J release notes"
layout: "doc"
---

# 3.0.3 

RDF4J 3.0.3 is a patch release with a number of bug fixes.

For a complete overview, see [all issues fixed in 3.0.3](https://github.com/eclipse/rdf4j/milestone/44?closed=1).

# 3.0.2 

RDF4J 3.0.2 is a patch release with a number of bug fixes.

For a complete overview, see [all issues fixed in 3.0.2](https://github.com/eclipse/rdf4j/milestone/43?closed=1).

# 3.0.1 

RDF4J 3.0.1 is a patch release with a number of bug fixes, including a major issue in `DROP GRAPH` execution on native stores.

For a complete overview, see [all issues fixed in 3.0.1](https://github.com/eclipse/rdf4j/milestone/41?closed=1).

# 3.0.0

RDF4J 3.0 is a major new release of the Eclipse RDF4J framework. Some highlights:

- Major improvements to the SHACL Sail
- Cleanup of core APIs (removing deprecated and obsolete code)
- Preparing RDF4J for Java 11/12 compatibility
- RDF4J Server / Workbench upgrade to Servlet API 3.1

For a complete overview, see [all issues fixed in 3.0](https://github.com/eclipse/rdf4j/milestone/17?closed=1).

## Upgrade notes

RDF4J 3.0 contains several [backward incompatible changes](https://github.com/eclipse/rdf4j/issues?utf8=%E2%9C%93&q=is%3Aissue+label%3A%22Not+backwards+compatible%22+-label%3A%22wontfix%22+milestone%3A3.0.0). We distinguish changes that affect users of the RDF4J tools (Console, Workbench, Server), changes that affect users of the RDF4J APIs and libraries, and changes that affect third party Sail / Repository implementations. See the linked issues for further details.

### RDF4J tools users

- Due to an [up upgrade to Spring 5.1](https://github.com/eclipse/rdf4j/issues/1343), RDF4J Server and RDF4J Workbench now require a servlet container that supports Servlet API 3.1. For Apache Tomcat, that means Tomcat 8 or better. For Eclipse Jetty, it means Jetty 9 or better.
- The [RDFS inferencer has changed its default behavior for inferred triples](https://github.com/eclipse/rdf4j/issues/1227). 

### rdf4j API/library users

- The [deprecated Graph API has been removed](https://github.com/eclipse/rdf4j/issues/389). Any code that still uses `Graph`, `GraphImpl`, or `GraphUtil` will need to be rewritten to use corresponding features in the [Model API](https://rdf4j.eclipse.org/documentation/programming/model/#the-model-interface).
- [Deprecated methods have been removed from the Model API](https://github.com/eclipse/rdf4j/issues/748). Any existing code that uses the deprecated methods will need to be rewritten to use the recommend new methods.
- The [deprecated package org.eclipse.rdf4j.model.util.language](https://github.com/eclipse/rdf4j/issues/675) has been removed.
- The [SHACL Sail shape persistence directory](https://github.com/eclipse/rdf4j/issues/1504) has been moved.

### third-party sail / repository implementors

- invoking `init()` (or `initialize()`) on a Repository or Sail object is [no longer mandatory](https://github.com/eclipse/rdf4j/issues/1223). 
- we allow [injection of optimizers](https://github.com/eclipse/rdf4j/issues/1280) into a Sail object. 
- as part of a massive restructure of our code repositories and the way in which the project is tested, [testsuites and compliance tests have been moved and refactored](https://github.com/eclipse/rdf4j/issues/1236). If you were depending on a particular testsuite and can no longer find it, contact us. 

# 2.5.5 

RDF4J 2.5.5 is a backport patch release containing a single bug fix:

- [#1548 DROP GRAPH on unknown graph deletes entire dataset on native store](https://github.com/eclipse/rdf4j/issues/1548) 

# 2.5.4

RDF4J 2.5.4 is a patch release containing several bug fixes, including:

- handling of negation in property paths
- stack overflow in Models.isomorphic
- NullPointerException in Native Store Context Cache initialization 

For a complete overview, see [all issues fixed in 2.5.4](https://github.com/eclipse/rdf4j/milestone/40?closed=1).

# 2.5.3

RDF4J 2.5.3 is a patch release containing several bug fixes, including:

- custom HTTP header handling in HTTPRepository
- handling of queries with COUNT in the LuceneSail
- performance of context id retrieval in native store
- handling of FROM/FROM NAMED clauses in SparqlBuilder

For a complete overview, see [all issues fixed in 2.5.3](https://github.com/eclipse/rdf4j/milestone/37?closed=1).
