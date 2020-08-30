---
title: "Repository and SAIL configuration"
toc: true
weight: 4
---

RDF4J repositories and SAIL configuration can be set up and changed by means of a configuration file in Turtle syntax. Here, we document the way the various components (repositories and SAILs) work together and how a full database configuration can be defined by "stacking" SAILs and wrapping in a Repository implementation.

# Repository configuration

A Repository configuration consists of a single subject. Typically this subject is a blank node (`[]` in Turtle syntax), and is assigned its configuration parameters through use of RDF properties. 

It takes following configuration parameters:

- `rep:repositoryID` (String): the repository identifier (required). This must be unique within the running system.
- `rdfs:label` (String): a human-readable name or short description of the repository (optional).
- `rep:repositoryImpl`: this specifies and configures the specific repository implementation (required). This is typically supplied as a nested blank node, which in turns has the implementation-specific configuration parameter. Every repository implemenation _must_ specify a `rep:repositoryType`.

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.


[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "SPARQL endpoint at http://example.org/" ;
   rep:repositoryImpl [ rep:repositoryType "example:repositoryType"; 
                        # your implementation config here 
   ].
```

## SPARQLRepository

The `SPARQLRepository` repository implementation is a client interface for a remote SPARQL endpoint.
Its `rep:repositoryType` value is `"openrdf:SPARQLRepository"`.

It takes the following configuration parameters:

- `sparql:query-endpoint` (IRI): the SPARQL _query_ endpoint URL (required).
- `sparql:update-endpoint` (IRI): the SPARQL _update_ endpoint URL (optional). Only needs to be defined if different from the query endpoint.

### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sparql: <http://www.openrdf.org/config/repository/sparql#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "SPARQL endpoint at http://example.org/" .
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SPARQLRepository" ;
      sparql:query-endpoint <http://example.org/sparql> ;
      sparql:update-endpoint <http://example.org/sparql/update> ;
   ];
```

## HTTPRepository

The `HTTPRepository` repository implementation is a client interface for a
store on a (remote) RDF4J Server. It differs from `SPARQLRepository` in that it
implements several RDF4J-specific  extensions to the standard SPARQL Protocol
which enhance transactional support and general performance.  Its
`rep:repositoryType` value is `"openrdf:HTTPRepository"`.

It takes the following configuration parameters:

- `hr:repositoryURL` (IRI): the location of the repository on an RDF4J Server (required).
- `hr:username` (string): username for basic authentication (optional).
- `hr:password` (string): password for basic authentication (optional).

### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix hr: <http://www.openrdf.org/config/repository/http#>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "test repository on locally running RDF4J Server" .
   rep:repositoryImpl [
      rep:repositoryType "openrdf:HTTPRepository" ;
      hr:repositoryURL <http://localhost:8080/rdf4j-server/repositories/test>
   ];
```

## SailRepository

The {{< javadoc "SailRepository" "repository/sail/SailRepository.html" >}} repository implementation is the main implementation for direct access to a local RDF database (a SAIL implementation). Its `rep:repositoryType` is `openrdf:SailRepository`.

It takes the following configuration parameters:

- `sr:sailImpl`: this specifies and configures the specific SAIL implementation (required). This is typically supplied as a nested blank node, which in turns has the SAIL-specific configuration parameters. Every SAIL implementation _must_ specify a `sail:sailType` property.

### Example configuration

In this example we configure a simple SailRepository using a MemoryStore SAIL. See the section [SAIL Configuration](#sail-configuration) for more details.

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "Test Memory store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "openrdf:MemoryStore" ;
         sail:iterationCacheSyncThreshold "10000";
         ms:persist true;
         ms:syncDelay 0;
         sb:evaluationStrategyFactory "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory"
      ]
   ].
```

## DatasetRepository

The {{< javadoc "DatasetRepository" "repository/dataset/DatasetRepository.html" >}} is a wrapper around a `SailRepository` that dynamically loads datasets specified in the `FROM` and `FROM NAMED` clauses in SPARQL queries.

### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "Test Memory store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:DatasetRepository" ;
      rep:delegate [
        rep:repositoryType "openrdf:SailRepository" ;
        sr:sailImpl [
          ... 
        ]
      ]
   ].
```

## ContextAwareRepository

The {{< javadoc "ContextAwareRepository" "repository/contextaware/ContextAwareRepository.html" >}} is a wrapper around any other `Repository`. It can be used to configure the default context(s) on which query, update, or delete operations operate per default.

It takes the following parameters:

- `car:readContext`. Specifies the context(s) used per default when executing read operations (including SPARQL queries). Can be specified with multiple values to include more than one context;
- `car:insertContext`. Specifies the context used for insertion operations. 
- `car:removeContext`. Specifies the context(s) used for delete operations. Can be specified with multiple values to include more than one context.

### Example configuration


```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix car: <http://www.openrdf.org/config/repository/contextaware#>.
@prefix ex: <http://example.org/>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "Test Memory store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:ContextAwareRepository" ;
      car:readContext ex:namedGraph1, ex:namedGraph2;  
      rep:delegate [
        rep:repositoryType "openrdf:HTTPRepository" ;
      ]
   ].
```

# Sail Configuration

Sail implementations come in two basic types: _base_ Sails, and Sail Wrappers / Adapters (sometimes also referred to as "Stackable Sails").

Each Sail configuration identifies one base Sail, which typically functions as the actual database layer. This base Sail configuration can optionally be wrapped in one or more Stackable Sail configurations. The entire "Sail stack" is then usually wrapped as part of a [SailRepository configuration](#SailRepository).

Every Sail in a Sail configuration has at least one required parameter:

- `sail:sailType` - this identifies the type of Sail being configured.

## Base Sails

Base Sail implementations are responsible for persistence of data and handling of queries, transactions and update operations on that data. A base Sail is typically a database.

### Memory Store

A Memory Store is an RDF4J database that keeps all data in main memory, with optional persistence on disk. Its `sail:sailType` value is `"openrdf:MemoryStore"`. 

It takes the following configuration options:

- `ms:persist` (boolean). Specifies if the store persists its data to disk.
- `ms:syncDelay` (integer). Specifies the amount of time (in milliseconds) between an update operation completing and the store syncing its contents to disk.
- `sail:iterationCacheSyncThreshold` (integer). Specifies the size of the internal cache for query result iterations before on-disk overflow is enabled. 
- `sb:evaluationStrategyFactory` (string). Specifies the full classname of the `EvaluationStrategyFactory` implementation used for this store. This controls how SPARQL queries are evaluated.


#### Example configuration 

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "Test Memory store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "openrdf:MemoryStore" ;
         sail:iterationCacheSyncThreshold "10000";
         ms:persist true;
         ms:syncDelay 0;
         sb:evaluationStrategyFactory "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory"
      ]
   ].
```

### Native Store

### Elasticsearch Store

## Adapter SAILs

### Inferencer

### SHACLSail

### LuceneSail

