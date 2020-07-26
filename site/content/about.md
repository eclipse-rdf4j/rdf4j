---
title: "The Eclipse RDF4J Framework"
date: 2020-01-27
description: "description"
toc: true
---

Eclipse RDF4J is an open source modular Java framework for working with [RDF](https://www.w3.org/TR/rdf11-primer/) data. This includes parsing, storing, inferencing and querying of/over such data. It offers an easy-to-use API that can be connected to all leading RDF storage solutions. It allows you to connect with [SPARQL](https://www.w3.org/TR/sparql11-overview/) endpoints and create applications that leverage the power of [Linked Data](http://linkeddata.org/) and [Semantic Web](http://www.w3.org/2001/sw/).

RDF4J offers two out-of-the-box RDF databases (the in-memory store and the native store), and in addition many third party storage solutions are available. The framework offers a large scala of tools to developers to leverage the power of RDF and related standards. RDF4J fully supports the SPARQL 1.1 query and update language for expressive querying and offers transparent access to remote RDF repositories using the exact same API as for local access. Finally, RDF4J supports all mainstream RDF file formats, including RDF/XML, Turtle, N-Triples,  N-Quads, JSON-LD, TriG and TriX.

<div class="shadowed">
<img src="/images/rdf4j-architecture.svg" alt="RDF4J Architecture">
<strong>Eclipse RDF4J Modular Architecture</strong>
</div>

# RDF4J databases

The RDF4J core framework provides a set of vendor-neutral APIs for highly scalable storage, reasoning, and retrieval of RDF and OWL. Here, we list some available database solutions that implement the RDF4J APIs.

## Core databases

RDF4J offers a set of database implementations out of the box.

The RDF4J **Memory Store** is a transactional RDF database using main memory with optional persistent sync to disk. It is fast with excellent performance for small  datasets. It scales with amount of RAM available.

The RDF4J **Native Store** is a transactional RDF database using direct disk IO for persistence. It is a more scalable solution than the memory store, with a smaller memory footprint, and also offers better consistency and durability. It is currently aimed at medium-sized datasets in the order of 100 million triples.

The RDF4J **ElasticsearchStore** is an experimental RDF database that uses Elasticsearch for storage. 
This is useful if you are already using Elasticsearch for other things in your project and you want to add some small scale graph data. 
A good usecase is if you need reference data or an ontology for your application. The built-in read cache makes it a good choice for data that updates infrequently, 
though for most usecases the NativeStore will be considerably faster.

On top of these core databases, RDF4J offers a number of functional extensions. These extensions add functionality such as improved full-text search, RDFS inferencing, rule-based reasoning and validation using SHACL/SPIN, and geospatial querying support. For more information see the [RDF4J documentation](/documentation).

## Third party database solutions

The core RDF4J databases are mainly intended for small to medium-sized datasets. However, RDF4J-compatible databases are developed by several third parties, both open-source/free and commercial, and they often offer better scalability or other extended features. Because these triplestores are compatible with the RDF4J APIs, you will be able to switch your project to a different database with a minimal amount of code changes. Here, we list a few options, in no particular order of preference.

### Ontotext GraphDB

[Ontotext GraphDB](http://www.ontotext.com/products/ontotext-graphdb/) is a leading RDF triplestore built on OWL (Ontology Web Language) standards.  GraphDB handles massive loads, queries and OWL inferencing in real time. Ontotext offers GraphDB in several editions, including  GraphDB™ Free, GraphDB™ Standard and GraphDB™ Enterprise. 

Ontotext are a long-term contributor to the RDF4J project.

### Halyard

[Halyard](https://merck.github.io/Halyard/) is an RDF4J-based horizontally scalable triplestore with full support for named graphs and SPARQL, implemented on top of Apache HBase.

### Stardog

[Stardog](http://www.stardog.com/) is a fast, lightweight, pure Java RDF store for mission-critical apps. It supports highly scalable storage and retrieval as well as OWL reasoning.

### Amazon Neptune

[Amazone Neptune](https://aws.amazon.com/neptune/) is a fast, reliable, fully managed graph database service on Amazon Web Services (AWS) that makes it easy to build and run applications that work with highly connected datasets. 

### Systap Blazegraph™

[Blazegraph](http://www.blazegraph.com/) (formerly known as Bigdata) is an enterprise graph database by Systap, LLC that provides a horizontally scaling storage and retrieval solution for very large volumes of RDF.

### MarkLogic RDF4J API

The [MarkLogic RDF4J API](https://github.com/marklogic/marklogic-rdf4j) is a full-featured, easy-to-use interface, that provides access to the MarkLogic triplestore via the RDF4J APIs. It offers several additional features such as permissions, and combination queries. More details can be found in the [MarkLogic Developer documentation](https://docs.marklogic.com/guide/semantics/clientAPIs#id_23335).

### Strabon

[Strabon](http://www.strabon.di.uoa.gr/) is a spatiotemporal RDF store based on RDF4J. You can use it to store linked geospatial data that changes over time and pose queries using two popular extensions of SPARQL. Strabon supports spatial datatypes enabling the serialization of geometric objects in OGC standards WKT and GML. It also offers spatial and temporal selections, spatial and temporal joins, a rich set of spatial functions similar to those offered by geospatial relational database systems and support for multiple Coordinate Reference Systems. Strabon can be used to model temporal domains and concepts such as events, facts that change over time etc. through its support for valid time of triples, and a rich set of temporal functions.

### Openlink Virtuoso RDF4J Provider

The [Openlink Virtuoso RDF4J Provider](http://vos.openlinksw.com/owiki/wiki/VOS/VirtSesame2Provider) is a fully operational Native Graph Model Storage Provider for the Eclipse RDF4J Framework, allowing users of Virtuoso to leverage the Eclipse RDF4J framework to modify, query, and reason with the Virtuoso quad store using the Java language.

# Related projects

Several projects extend or make use of RDF4J in some way, and provide additional functionality on top of the core RDF4J framework. Here, we offer a non-exhaustive list of such projects, both commercial and free/open-source.

## metaphactory

[metaphactory](https://www.metaphacts.com/product) supports knowledge graph management, rapid application development, and end-user oriented interaction. metaphactory runs on top of your on-premise, cloud, or managed graph database and offers capabilities and features to support the entire lifecycle of dealing with knowledge graphs. It is a commercial platform with RDF4J at its core. 

The metaphactory platform is developed by [metaphacts GmbH](https://www.metaphacts.com/), who are a significant contributor to the RDF4J project.

## Neosemantics

[Neosemantics](https://neo4j.com/labs/neosemantics-rdf/) is a plugin that enables the use of RDF in Neo4j. You can use it to import existing RDF datasets, build integrations with RDF generating endpoints or easily construct RDF endpoints on Neo4j, and more.

## Other

- [Apache Marmotta](http://marmotta.apache.org/)<br>
  a Linked Data publication platform.
- [Carml](https://github.com/carml/carml)<br>
  a library that transforms structured sources to RDF based and declared in an RML mapping.
- [KOMMA](http://komma.enilink.net/)<br>
  a framework for the management and editing of RDF, RDFS and OWL. It provides Object-Triple-Mapping (comparable to JPA), an Editing framework, Eclipse RCP and RAP integration, on top of Eclipse RDF4J.
- [RDF4J Schema Generator](https://github.com/ansell/rdf4j-schema-generator)<br>
  a command line tool and maven plugin to generate vocabulary java classes from RDFS or OWL.
- [RML-Mapper](https://github.com/RMLio/RML-Mapper)<br>
  another RML mapping library. 
- [Semantic Turkey](http://semanticturkey.uniroma2.it/)<br>
  an RDF service backend for Knowledge Management, used by thesaurus management platform [VocBench](http://vocbench.uniroma2.it/).
- [Sesame Tools](https://github.com/joshsh/sesametools)<br>
  a collection of utility classes for use with Sesame/RDF4J.
