---
title: "About"
date: 2018-04-05T16:09:45-04:00
description: "description"
layout: "doc"
hide_page_title: "true"
---

# The Eclipse rdf4j framework

Eclipse rdf4j (formerly known as Sesame) is an open source Java framework for processing RDF data. This includes parsing, storing, inferencing and querying of/over such data. It offers an easy-to-use API that can be connected to all leading RDF storage solutions. It allows you to connect with SPARQL endpoints and create applications that leverage the power of linked data and Semantic Web.

Rdf4j offers two out-of-the-box RDF databases (the in-memory store and the native store), and in addition many third party storage solutions are available. The framework offers a large scala of tools to developers to leverage the power of RDF and related standards. rdf4j fully supports the SPARQL 1.1 query and update language for expressive querying and offers transpararent access to remote RDF repositories using the exact same API as for local access. Finally, rdf4j supports all mainstream RDF file formats, including RDF/XML, Turtle, N-Triples,  N-Quads, JSON-LD, TriG and TriX.


# rdf4j databases

The rdf4j core framework provides a set of vendor-neutral APIs for highly scalable storage, reasoning, and retrieval of RDF and OWL. Here, we list some available database solutions that implement the rdf4j APIs.

## Core databases

Rdf4j offers a set of database implementations out of the box.

The rdf4j **Memory Store** is a transactional RDF database using main memory with optional persistent sync to disk. It is fast with excellent performance for small  datasets. It scales with amount of RAM available.

The rdf4j **Native Store** is a transactional RDF database using direct disk IO for persistence. It is a more scalable solution than the memory store, with a smaller memory footprint, and also offers better consistency and durability. It is currently aimed at medium-sized datasets in the order of 100 million triples.

On top of these core databases, rdf4j offers a number of functional extensions. These extensions add functionality such as improved full-text search, RDFS inferencing, rule-based reasoning using SHACL/SPIN, and geospatial querying support. For more information see the rdf4j documentation.

The core databases are mainly intended for small to medium-sized datasets. However, rdf4j-compatible databases are developed by several third parties, both open-source/free and commercial, and they often offer better scalability or other extended features. Because these triplestores are compatible with the rdf4j APIs, you will be able to switch your project to a different database with a minimal amount of code changes.

## Ontotext GraphDB™

[Ontotext GraphDB™](http://www.ontotext.com/products/ontotext-graphdb/) (formerly OWLIM) is a leading RDF triplestore built on OWL (Ontology Web Language) standards.  GraphDB handles massive loads, queries and OWL inferencing in real time. Ontotext offers GraphDB in several editions, including  GraphDB™ Free, GraphDB™ Standard and GraphDB™ Enterprise. Since release 8, GraphDB is fully compatible with the rdf4j framework.

## Halyard

[Halyard](https://merck.github.io/Halyard/) is an rdf4j-based horizontally scalable triplestore with full support for named graphs and SPARQL, implemented on top of Apache HBase.

## Stardog

[Stardog](http://www.stardog.com/) is a fast, lightweight, pure Java RDF store for mission-critical apps. It supports highly scalable storage and retrieval as well as OWL reasoning.
## Systap Blazegraph™

[Blazegraph™](http://www.blazegraph.com/) (formerly known as Bigdata) is an enterprise graph database by Systap, LLC that provides a horizontally scaling storage and retrieval solution for very large volumes of RDF.

## MarkLogic rdf4j API

The [MarkLogic rdf4j API](https://github.com/marklogic/marklogic-rdf4j) is a full-featured, easy-to-use interface, that provides access to the MarkLogic triplestore via the rdf4j APIs. It offers several additional features such as permissions, and combination queries. More details can be found in the [MarkLogic Developer documentation](https://docs.marklogic.com/guide/semantics/clientAPIs#id_23335).

## Strabon

[Strabon](http://www.strabon.di.uoa.gr/) is a spatiotemporal RDF store based on rdf4j. You can use it to store linked geospatial data that changes over time and pose queries using two popular extensions of SPARQL. Strabon supports spatial datatypes enabling the serialization of geometric objects in OGC standards WKT and GML. It also offers spatial and temporal selections, spatial and temporal joins, a rich set of spatial functions similar to those offered by geospatial relational database systems and support for multiple Coordinate Reference Systems. Strabon can be used to model temporal domains and concepts such as events, facts that change over time etc. through its support for valid time of triples, and a rich set of temporal functions.

## Openlink Virtuoso rdf4j Provider

The [Openlink Virtuoso rdf4j Provider](http://vos.openlinksw.com/owiki/wiki/VOS/VirtSesame2Provider) is a fully operational Native Graph Model Storage Provider for the Eclipse rdf4j Framework, allowing users of Virtuoso to leverage the Eclipse rdf4j framework to modify, query, and reason with the Virtuoso quad store using the Java language.

