---
title: "Repository and SAIL configuration"
toc: true
weight: 4
autonumbering: true
---

RDF4J repositories and SAIL configuration can be set up and changed by means of a configuration file in Turtle syntax. Here, we document the way the various components (repositories and SAILs) work together and how a full database configuration can be defined by "stacking" SAILs and wrapping in a Repository implementation.

<!--more-->

## Repository configuration

A Repository configuration consists of a single RDF subject of type `rep:Repository`. Typically this subject is a blank node (`[]` in Turtle syntax), and is assigned its configuration parameters through use of RDF properties.

The parameter namespace is `http://www.openrdf.org/config/repository#`, commonly abbreviated to `rep`.
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

### SPARQLRepository

The `SPARQLRepository` repository implementation is a client interface for a remote SPARQL endpoint.
Its `rep:repositoryType` value is `"openrdf:SPARQLRepository"`.

The parameter namespace is `http://www.openrdf.org/config/repository/sparql#`, commonly abbreviated to `sparql`.
It takes the following configuration parameters:

- `sparql:query-endpoint` (IRI): the SPARQL _query_ endpoint URL (required).
- `sparql:update-endpoint` (IRI): the SPARQL _update_ endpoint URL (optional). Only needs to be defined if different from the query endpoint.

#### Example configuration

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

### HTTPRepository

The `HTTPRepository` repository implementation is a client interface for a
store on a (remote) RDF4J Server. It differs from `SPARQLRepository` in that it
implements several RDF4J-specific  extensions to the standard SPARQL Protocol
which enhance transactional support and general performance.  Its
`rep:repositoryType` value is `"openrdf:HTTPRepository"`.

The parameter namespace is `http://www.openrdf.org/config/repository/http#`, commonly abbreviated to `hr`.
It takes the following configuration parameters:

- `hr:repositoryURL` (IRI): the location of the repository on an RDF4J Server (required).
- `hr:username` (string): username for basic authentication (optional).
- `hr:password` (string): password for basic authentication (optional).

#### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix hr: <http://www.openrdf.org/config/repository/http#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "example repository on locally running RDF4J Server" .
   rep:repositoryImpl [
      rep:repositoryType "openrdf:HTTPRepository" ;
      hr:repositoryURL <http://localhost:8080/rdf4j-server/repositories/test>
   ];
```

### SailRepository

The {{< javadoc "SailRepository" "repository/sail/SailRepository.html" >}} repository implementation is the main implementation for direct access to a local RDF database (a SAIL implementation). Its `rep:repositoryType` is `openrdf:SailRepository`.

The parameter namespace is `http://www.openrdf.org/config/repository/sail#`, commonly abbreviated to `sr`.
It takes the following configuration parameters:

- `sr:sailImpl`: this specifies and configures the specific SAIL implementation (required). This is typically supplied as a nested blank node, which in turns has the SAIL-specific configuration parameters. Every SAIL implementation _must_ specify a `sail:sailType` property.

#### Example configuration

In this example we configure a simple SailRepository using a MemoryStore SAIL. See the section [SAIL Configuration](#sail-configuration) for more details.

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Memory store" ;
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

### DatasetRepository

The {{< javadoc "DatasetRepository" "repository/dataset/DatasetRepository.html" >}} is a wrapper around a `SailRepository` that dynamically loads datasets specified in the `FROM` and `FROM NAMED` clauses in SPARQL queries.

#### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Dataset Repository" ;
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

### ContextAwareRepository

The {{< javadoc "ContextAwareRepository" "repository/contextaware/ContextAwareRepository.html" >}} is a wrapper around any other `Repository`. It can be used to configure the default context(s) on which query, update, or delete operations operate per default.

The parameter namespace is `http://www.openrdf.org/config/repository/contextaware#`, commonly abbreviated to `car`.
It takes the following parameters:

- `car:readContext`. Specifies the context(s) used per default when executing read operations (including SPARQL queries). Can be specified with multiple values to include more than one context;
- `car:insertContext`. Specifies the context used for insertion operations.
- `car:removeContext`. Specifies the context(s) used for delete operations. Can be specified with multiple values to include more than one context.

#### Example configuration


```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix car: <http://www.openrdf.org/config/repository/contextaware#>.
@prefix ex: <http://example.org/>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Context-aware repository" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:ContextAwareRepository" ;
      car:readContext ex:namedGraph1, ex:namedGraph2;
      rep:delegate [
        rep:repositoryType "openrdf:HTTPRepository" ;
      ]
   ].
```

## Sail Configuration

Sail implementations come in two basic types: _base_ Sails, and Sail wrappers / adapters (sometimes also referred to as "Stackable Sails").

Each Sail configuration identifies one base Sail, which typically functions as the actual database layer. This base Sail configuration can optionally be wrapped in one or more Stackable Sail configurations. The entire "Sail stack" is then usually wrapped as part of a [SailRepository configuration](#SailRepository).

The parameter namespace is `http://www.openrdf.org/config/sail#`, commonly abbreviated to `sail`.
Every Sail in a Sail configuration has at least one required parameter:

- `sail:sailType` - this identifies the type of Sail being configured.
- `sail:iterationCacheSyncThreshold` (integer). Specifies the size of the internal cache for query result iterations before on-disk overflow is enabled.

### Base Sails

Base Sail implementations are responsible for persistence of data and handling of queries, transactions and update operations on that data. A base Sail is typically a database.

The parameter namespace for common parameters of base Sails is `http://www.openrdf.org/config/sail/base#`, commonly abbreviated to `sb`.

Base Sails commonly take the following parameter:

- `sb:evaluationStrategyFactory` (string). Specifies the full classname of the `EvaluationStrategyFactory` implementation used for this store. This controls how SPARQL queries are evaluated.

#### Memory Store

A Memory Store is an RDF4J database that keeps all data in main memory, with optional persistence on disk. Its `sail:sailType` value is `"openrdf:MemoryStore"`.

The parameter namespace for Memory Store  is `http://www.openrdf.org/config/sail/memory#`, commonly abbreviated to `ms`.
It takes the following configuration options:

- `ms:persist` (boolean). Specifies if the store persists its data to disk (required). Persistent memory stores write their data to disk before being shut down and read this data back in the next time they are initialized. Non-persistent memory stores are always empty upon initialization.
- `ms:syncDelay` (integer). Specifies the amount of time (in milliseconds) between an update operation completing and the store syncing its contents to disk (optional). By default, the memory store persistence mechanism synchronizes the disk backup directly upon any change to the contents of the store. Setting a delay on this synchronization can be useful if your application performs several transactions in sequence and you want to prevent disk synchronization in the middle of this sequence to improve update performance.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Memory store" ;
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

#### Native Store

A Native Store is an RDF4J database that persists all data directly on disk in an indexed binary format. Its `sail:sailType` value is `"openrdf:NativeStore"`.

The parameter namespace for Native Store  is `http://www.openrdf.org/config/sail/native#`, commonly abbreviated to `ns`.
It takes the following configuration options:

- `ns:tripleIndex` (string).  Specifices a comma-separated list of indexes for the store to use (optional).
- `ns:forceSync` (boolean). Specifies if an OS-level force sync should be executed after every update (optional).
- `ns:valueCacheSize` (integer). Specifies the size of the value cache (optional).
- `ns:valueIDCacheSize` (integer). Specifices the size of the value ID cache (optional).
- `ns:namespaceCacheSize` (integer). Specifies the size of the namespace cache (optional).
- `ns:namespaceIDCacheSize` (integer). Specifies the size of the namespace ID cache (optional).

##### Native store indexes

The native store uses on-disk indexes to speed up querying. It uses B-Trees for indexing statements, where the index key consists of four fields: subject (s), predicate (p), object (o) and context (c). The order in which each of these fields is used in the key determines the usability of an index on a specify statement query pattern: searching statements with a specific subject in an index that has the subject as the first field is significantly faster than searching these same statements in an index where the subject field is second or third. In the worst case, the ‘wrong’ statement pattern will result in a sequential scan over the entire set of statements.

By default, the native repository only uses two indexes, one with a subject-predicate-object-context (spoc) key pattern and one with a predicate-object-subject-context (posc) key pattern. However, it is possible to define more or other indexes for the native repository, using the `ns:tripleIndex` parameter. This can be used to optimize performance for query patterns that occur frequently.

The subject, predicate, object and context fields are represented by the characters ‘s’, ‘p’, ‘o’ and ‘c’ respectively. Indexes can be specified by creating 4-letter words from these four characters. Multiple indexes can be specified by separating these words with commas, spaces and/or tabs. For example, the string “spoc, posc” specifies two indexes; a subject-predicate-object-context index and a predicate-object-subject-context index.

Creating more indexes potentially speeds up querying (a lot), but also adds overhead for maintaining the indexes. Also, every added index takes up additional disk space.

The native store automatically creates/drops indexes upon (re)initialization, so the parameter can be adjusted and upon the first refresh of the configuration the native store will change its indexing strategy, without loss of data.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ns: <http://www.openrdf.org/config/sail/native#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Native store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "openrdf:NativeStore" ;
         sail:iterationCacheSyncThreshold "10000";
         sb:evaluationStrategyFactory "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory" ;
         ns:tripleIndexes "spoc,posc"
      ]
   ].
```

#### Elasticsearch Store

The Elasticsearch Store is an RDF4J database that persists all data directly in Elasticsearch (not to be confused with the Elasticsearch Fulltext Search Sail, which is an adapter Sail implementation to provided full-text search indexing on top of other RDF databases). Its `sail:sailType` value is `"rdf4j:ElasticsearchStore"`.

The parameter namespace for Elasticsearch Store is `http://rdf4j.org/config/sail/elasticsearchstore`, commonly abbreviated to `ess`. It takes the following configuration options:

- `ess:hostname` (string). Specifies the hostname to use for connecting to Elasticsearch (required).
- `ess:port` (int). Specifies the port number to use for connecting to Elasticsearch (optional).
- `ess:clusterName` (string). Specifies the Elasticsearch cluster name (optional).
- `ess:index` (string). Specifies the index name to use for storage and  retrieval of data (optional).

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ess: <http://rdf4j.org/config/sail/elasticsearchstore#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Elasticsearch store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "rdf4j:ElasticsearchStore" ;
         sail:iterationCacheSyncThreshold "10000";
         ess:hostname "localhost";
         ess:port 9200;
         ess:clusterName "myCluster";
         ess:index "myIndex";
         sb:evaluationStrategyFactory "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory"
      ]
   ].
```

### Sail Adapter implementations

Sail Adapters, or "Stackable Sails", are Sail implementations that implement intercepting behavior for operations on RDF databases before delegating down to the wrapped Sail implementation (ultimately a [base Sail](#base-sails), though of course multiple Stackable Sails can be stacked on top of each other as well).

Every Sail Adapter has at least one required parameter:

- `sail:delegate`. Specifies and configures the wrapped Sail implementation (required). This is typically supplied as a nested blank node, which in turns has the implementation-specific configuration parameter.

#### RDF Schema inferencer

The RDF Schema inferencer is an Sail Adapter that performs rule-based entailment as defined in the [RDF 1.1 Semantics](https://www.w3.org/TR/rdf11-mt/) recommendation. Reasoning happens in a forward-chaining manner on update operations, and the entailed statements are sent to be persisted by the wrapped Sail. The inferencer also caches RDF Schema statements to speed up entailment and retrieval operations.

The `sail:sailType` value for the RDF Schema inferencer is `"rdf4j:SchemaCachingRDFSInferencer"`.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example memory store with RDF Schema entailment" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "rdf4j:SchemaCachingRDFSInferencer";
         sail:delegate [
             sail:sailType "openrdf:MemoryStore" ;
             sail:iterationCacheSyncThreshold "10000";
             ms:persist true;
             ms:syncDelay 0;
             sb:evaluationStrategyFactory "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory"
         ];
      ]
   ].
```

#### Direct Type inferencer

The Direct Type inferencer is an Sail Adapter that performs entailment on the
the class and instance inheritance hierarchy, asserting specific "shortcut"
properties (`sesame:directType`, `sesame:directSubClassOf` and
`sesame:directSubPropertyOf`) that allow querying for the _direct_ type or
subclass of a resource.  Reasoning happens in a forward-chaining manner on
update operations, and the entailed statements are sent to be persisted by the
wrapped Sail.

The `sail:sailType` value is `"openrdf:DirectTypeHierarchyInferencer"`.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Memory store with direct type entailment" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "openrdf:DirectTypeHierarchyInferencer";
         sail:delegate [
             sail:sailType "openrdf:MemoryStore" ;
             ...
         ];
      ]
   ].
```

#### SHACL Sail

The SHACL Sail is a Sail Adapter that performs transaction validation using the Shapes Constraint Language [SHACL](https://www.w3.org/TR/shacl/). More information about the use of the SHACL Sail can be found in [Validation with SHACL](/documentation/programming/shacl).

The `sail:sailType` value of the SHACL Sail is `"rdf4j:ShaclSail"`. Its parameter namespace is `http://rdf4j.org/config/sail/shacl#`, commonly abbreviated to `ssc`. It takes the following configuration options:

- `ssc:parallelValidation` (boolean): Enables parallel validation (optional).
- `ssc:undefinedTargetValidatesAllSubjects` (boolean):
Specifies if an undefined target in a shape leads to validating all subjects (optional) {{< tag "deprecated" >}} .
- `ssc:logValidationPlans` (boolean): Specifies if validation plans are sent to the logging framework (optional).
- `ssc:logValidationViolations` (boolean): Specifies if shape violations are sent to the logging framework (optional).
- `ssc:ignoreNoShapesLoadedException` (boolean): Specifies if the "no shapes loaded" error is ignored (optional) {{< tag "deprecated" >}}.
- `ssc:validationEnabled` (boolean): Specifies if transaction valudation is enabled by default (optional).
- `ssc:cacheSelectNodes` (boolean): Specifies if select nodes are cached (optional).
- `ssc:globalLogValidationExecution` (boolean): Specifies if validation execution details are sent to the logging framework (optional).
- `ssc:rdfsSubClassReasoning` (boolean): Enables RDF Schema Subclass entailment (optional).
- `ssc:performanceLogging` (boolean): Enables performance logging (optional).
- `ssc:serializableValidation` (boolean): When enabled, combined with transactions using isolation level `SNAPSHOT` the validation guarantee is equivalent to using `SERIALIZABLE` without the performance impact.
- `ssc:eclipseRdf4jShaclExtensions` (boolean): Enables [RDF4J-specific SHACL extensions](/shacl-extensions/)  (optional).
- `ssc:dashDataShapes` (boolean): Enables use of [DASH data shapes](https://datashapes.org/dash) (optional).
- `ssc:validationResultsLimitTotal` (integer): Specifies a limit on the total number of validation results sent in a validation report (optional). A values of -1 indicates no limit.
- `ssc:validationResultsLimitPerConstraint` (integer): Specifies a limit on the number of validation results sent per constraint in a validation report (optional). A values of -1 indicates no limit.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.
@prefix ssc: <http://rdf4j.org/config/sail/shacl#> .

[] a rep:Repository ;
   rep:repositoryID "example" ;
   rdfs:label "Example Memory store with SHACL validation" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
         sail:sailType "rdf4j:ShaclSail";
         ssc:parallelValidation true;
         ssc:validationResultsLimitTotal 300;
         sail:delegate [
             sail:sailType "openrdf:MemoryStore" ;
             ...
         ];
      ]
   ].
```

