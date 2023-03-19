---
title: "Repository and SAIL configuration"
toc: true
weight: 4
autonumbering: true
---

RDF4J repositories and SAIL configuration can be set up and changed by means of a configuration file in Turtle syntax. Here, we document the way the various components (repositories and SAILs) work together and how a full database configuration can be defined by "stacking" SAILs and wrapping in a Repository implementation.

<!--more-->

{{< info >}}
Since RDF4J 4.3.0, the configuration vocabulary for repositories and Sail implementations has been unified into a single namespace: <code>tag:rdf4j.org,2023:config/</code>. While currently RDF4J can still read configurations using the legacy vocabulary (using various namespaces starting with <code>http://www.openrdf.org/config/</code>), we strongly urge you to update existing configuration files to use the new vocabulary. See the section on <a href="#migrating-old-configurations">Migrating old configurations</a> for some pointers.
{{< / info >}}

## Repository configuration

A Repository configuration consists of a single RDF subject of type `config:Repository`. Typically this subject is a blank node (`[]` in Turtle syntax), and is assigned its configuration parameters through use of RDF properties.

The configuration namespace is `tag:rdf4j.org,2023:config/`, commonly abbreviated to `config`.
A Repository takes a following configuration parameters:

- `config:rep.id` (String): the repository identifier (required). This must be unique within the running system.
- `rdfs:label` (String): a human-readable name or short description of the repository (optional).
- `config:rep.impl`: this specifies and configures the specific repository implementation (required). This is typically supplied as a nested blank node, which in turns has the implementation-specific configuration parameter. Every repository implemenation _must_ specify a `config:rep.type`.

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.


[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "SPARQL endpoint at http://example.org/" ;
   config:rep.impl [ config:rep.type "example:repositoryType";
                        # your implementation config here
   ].
```

### SPARQLRepository

The `SPARQLRepository` repository implementation is a client interface for a remote SPARQL endpoint.
Its `config.rep.type` value is `"openrdf:SPARQLRepository"`.

It takes the following configuration parameters:

- `config:sparql.queryEndpoint` (IRI): the SPARQL _query_ endpoint URL (required).
- `config:sparql.updateEndpoint` (IRI): the SPARQL _update_ endpoint URL (optional). Only needs to be defined if different from the query endpoint.

#### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "SPARQL endpoint at http://example.org/" .
   config:rep.impl [
      config:repositoryType "openrdf:SPARQLRepository" ;
      config:sparql.queryEndpoint <http://example.org/sparql> ;
      config:sparql.updateEndpoint <http://example.org/sparql/update> ;
   ];
```

### HTTPRepository

The `HTTPRepository` repository implementation is a client interface for a
store on a (remote) RDF4J Server. It differs from `SPARQLRepository` in that it
implements several RDF4J-specific  extensions to the standard SPARQL Protocol
which enhance transactional support and general performance.  Its
`config:rep.type` value is `"openrdf:HTTPRepository"`.

It takes the following configuration parameters:

- `config:http.url` (IRI): the location of the repository on an RDF4J Server (required).
- `config:http.username` (string): username for basic authentication (optional).
- `config:http.password` (string): password for basic authentication (optional).

#### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "example repository on locally running RDF4J Server" .
   config:rep.impl [
      config:rep.type "openrdf:HTTPRepository" ;
      config:http.url <http://localhost:8080/rdf4j-server/repositories/test>
   ];
```

### SailRepository

The {{< javadoc "SailRepository" "repository/sail/SailRepository.html" >}} repository implementation is the main implementation for direct access to a local RDF database (a SAIL implementation). Its `config:rep.type` is `openrdf:SailRepository`.

It takes the following configuration parameters:

- `config:sail.impl`: this specifies and configures the specific SAIL implementation (required). This is typically supplied as a nested blank node, which in turns has the SAIL-specific configuration parameters. Every SAIL implementation _must_ specify a `config:sail.type` property.

#### Example configuration

In this example we configure a simple SailRepository using a MemoryStore SAIL. See the section [SAIL Configuration](#sail-configuration) for more details.

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Memory store" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore" ;
         config:sail.iterationCacheSyncThreshold "10000";
         config:mem.persist true;
         config:mem.syncDelay 0;
         config:sail.defaultQueryEvaluationMode "STANDARD"
      ]
   ].
```

### DatasetRepository

The {{< javadoc "DatasetRepository" "repository/dataset/DatasetRepository.html" >}} is a wrapper around a `SailRepository` that dynamically loads datasets specified in the `FROM` and `FROM NAMED` clauses in SPARQL queries.

#### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Dataset Repository" ;
   config:rep.impl [
      config:rep.type "openrdf:DatasetRepository" ;
      config:delegate [
        config:rep.type "openrdf:SailRepository" ;
        config:sail.impl [
          ...
        ]
      ]
   ].
```

### ContextAwareRepository

The {{< javadoc "ContextAwareRepository" "repository/contextaware/ContextAwareRepository.html" >}} is a wrapper around any other `Repository`. It can be used to configure the default context(s) on which query, update, or delete operations operate per default.

It takes the following parameters:

- `config:ca.readContext`. Specifies the context(s) used per default when executing read operations (including SPARQL queries). Can be specified with multiple values to include more than one context;
- `config:ca.insertContext`. Specifies the context used for insertion operations.
- `config:ca.removeContext`. Specifies the context(s) used for delete operations. Can be specified with multiple values to include more than one context.

#### Example configuration


```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
@prefix ex: <http://example.org/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Context-aware repository" ;
   config:rep.impl [
      config:rep.type "openrdf:ContextAwareRepository" ;
      config:ca.readContext ex:namedGraph1, ex:namedGraph2;
      config:delegate [
        config:rep.type "openrdf:HTTPRepository" ;
      ]
   ].
```

## Sail Configuration

Sail implementations come in two basic types: _base_ Sails, and Sail wrappers / adapters (sometimes also referred to as "Stackable Sails").

Each Sail configuration identifies one base Sail, which typically functions as the actual database layer. This base Sail configuration can optionally be wrapped in one or more Stackable Sail configurations. The entire "Sail stack" is then usually wrapped as part of a [SailRepository configuration](#SailRepository).

Every Sail in a Sail configuration has at least one required parameter:

- `config:sail.type` - this identifies the type of Sail being configured.
- `config:sail.iterationCacheSyncThreshold` (integer). Specifies the size of the internal cache for query result iterations before on-disk overflow is enabled.

### Base Sails

Base Sail implementations are responsible for persistence of data and handling of queries, transactions and update operations on that data. A base Sail is typically a database.

Base Sails commonly take the following parameter:

- `config:sail.defaultQueryEvaluationMode` (string). Specifies the default query evaluation mode used for SPARQL queries on this store. Expected values are `STRICT` or `STANDARD`. 

#### Memory Store

A Memory Store is an RDF4J database that keeps all data in main memory, with optional persistence on disk. Its `config:sail.type` value is `"openrdf:MemoryStore"`.

It takes the following configuration options:

- `config:mem.persist` (boolean). Specifies if the store persists its data to disk (required). Persistent memory stores write their data to disk before being shut down and read this data back in the next time they are initialized. Non-persistent memory stores are always empty upon initialization.
- `config:mem.syncDelay` (integer). Specifies the amount of time (in milliseconds) between an update operation completing and the store syncing its contents to disk (optional). By default, the memory store persistence mechanism synchronizes the disk backup directly upon any change to the contents of the store. Setting a delay on this synchronization can be useful if your application performs several transactions in sequence and you want to prevent disk synchronization in the middle of this sequence to improve update performance.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Memory store" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore" ;
         config:sail.iterationCacheSyncThreshold "10000";
         config:mem.persist true;
         config:mem.syncDelay 0;
         config:sail.defaultQueryEvaluationMode "STANDARD"
      ]
   ].
```

#### Native Store

A Native Store is an RDF4J database that persists all data directly on disk in an indexed binary format. Its `config:sail.type` value is `"openrdf:NativeStore"`.

It takes the following configuration options:

- `config:native.tripleIndex` (string).  Specifices a comma-separated list of indexes for the store to use (optional).
- `config:native.forceSync` (boolean). Specifies if an OS-level force sync should be executed after every update (optional).
- `config:native.valueCacheSize` (integer). Specifies the size of the value cache (optional).
- `config:native.valueIDCacheSize` (integer). Specifices the size of the value ID cache (optional).
- `config:native.namespaceCacheSize` (integer). Specifies the size of the namespace cache (optional).
- `config:native.namespaceIDCacheSize` (integer). Specifies the size of the namespace ID cache (optional).

##### Native store indexes

The native store uses on-disk indexes to speed up querying. It uses B-Trees for indexing statements, where the index key consists of four fields: subject (s), predicate (p), object (o) and context (c). The order in which each of these fields is used in the key determines the usability of an index on a specify statement query pattern: searching statements with a specific subject in an index that has the subject as the first field is significantly faster than searching these same statements in an index where the subject field is second or third. In the worst case, the ‘wrong’ statement pattern will result in a sequential scan over the entire set of statements.

By default, the native repository only uses two indexes, one with a subject-predicate-object-context (spoc) key pattern and one with a predicate-object-subject-context (posc) key pattern. However, it is possible to define more or other indexes for the native repository, using the `config:native.tripleIndex` parameter. This can be used to optimize performance for query patterns that occur frequently.

The subject, predicate, object and context fields are represented by the characters ‘s’, ‘p’, ‘o’ and ‘c’ respectively. Indexes can be specified by creating 4-letter words from these four characters. Multiple indexes can be specified by separating these words with commas, spaces and/or tabs. For example, the string “spoc, posc” specifies two indexes; a subject-predicate-object-context index and a predicate-object-subject-context index.

Creating more indexes potentially speeds up querying (a lot), but also adds overhead for maintaining the indexes. Also, every added index takes up additional disk space.

The native store automatically creates/drops indexes upon (re)initialization, so the parameter can be adjusted and upon the first refresh of the configuration the native store will change its indexing strategy, without loss of data.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Native store" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
        config:sail.type "openrdf:NativeStore" ;
         config:sail.iterationCacheSyncThreshold "10000";
         config:sail.defaultQueryEvaluationMode "STANDARD";
         config:native.tripleIndexes "spoc,posc"
      ]
   ].
```

#### Elasticsearch Store

The Elasticsearch Store is an RDF4J database that persists all data directly in Elasticsearch (not to be confused with the Elasticsearch Fulltext Search Sail, which is an adapter Sail implementation to provided full-text search indexing on top of other RDF databases). Its `config:sail.type` value is `"rdf4j:ElasticsearchStore"`.

The ElasticsearchStore takes the following configuration options:

- `config:ess.hostname` (string). Specifies the hostname to use for connecting to Elasticsearch (required).
- `config:ess.port` (int). Specifies the port number to use for connecting to Elasticsearch (optional).
- `config:ess.clusterName` (string). Specifies the Elasticsearch cluster name (optional).
- `config:ess.index` (string). Specifies the index name to use for storage and  retrieval of data (optional).

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Elasticsearch store" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "rdf4j:ElasticsearchStore" ;
         config:sail.iterationCacheSyncThreshold "10000";
         config:ess.hostname "localhost";
         config:ess.port 9200;
         config:ess.clusterName "myCluster";
         config:ess.index "myIndex";
         config:sail.defaultQueryEvaluationMode "STANDARD"
      ]
   ].
```

### Sail Adapter implementations

Sail Adapters, or "Stackable Sails", are Sail implementations that implement intercepting behavior for operations on RDF databases before delegating down to the wrapped Sail implementation (ultimately a [base Sail](#base-sails), though of course multiple Stackable Sails can be stacked on top of each other as well).

Every Sail Adapter has at least one required parameter:

- `config:delegate`. Specifies and configures the wrapped Sail implementation (required). This is typically supplied as a nested blank node, which in turns has the implementation-specific configuration parameter.

#### RDF Schema inferencer

The RDF Schema inferencer is an Sail Adapter that performs rule-based entailment as defined in the [RDF 1.1 Semantics](https://www.w3.org/TR/rdf11-mt/) recommendation. Reasoning happens in a forward-chaining manner on update operations, and the entailed statements are sent to be persisted by the wrapped Sail. The inferencer also caches RDF Schema statements to speed up entailment and retrieval operations.

The `config:sail.type` value for the RDF Schema inferencer is `"rdf4j:SchemaCachingRDFSInferencer"`.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example memory store with RDF Schema entailment" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "rdf4j:SchemaCachingRDFSInferencer";
         config:delegate [
             config:sail.type "openrdf:MemoryStore" ;
             config:sail.iterationCacheSyncThreshold "10000";
             config:mem.persist true;
             config:mem.syncDelay 0;
             config:sail.defaultQueryEvaluationMode "STANDARD"
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

The `config:sail.type` value is `"openrdf:DirectTypeHierarchyInferencer"`.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Memory store with direct type entailment" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:DirectTypeHierarchyInferencer";
         config:delegate [
             config:sail.type "openrdf:MemoryStore" ;
             ...
         ];
      ]
   ].
```

#### SHACL Sail

The SHACL Sail is a Sail Adapter that performs transaction validation using the Shapes Constraint Language [SHACL](https://www.w3.org/TR/shacl/). More information about the use of the SHACL Sail can be found in [Validation with SHACL](/documentation/programming/shacl).

The `config:sail.type` value of the SHACL Sail is `"rdf4j:ShaclSail"`. It takes the following configuration options:

- `config:shacl.parallelValidation` (boolean): Enables parallel validation (optional).
- `config:shacl.undefinedTargetValidatesAllSubjects` (boolean):
Specifies if an undefined target in a shape leads to validating all subjects (optional) {{< tag "deprecated" >}} .
- `config:shacl.logValidationPlans` (boolean): Specifies if validation plans are sent to the logging framework (optional).
- `config:shacl.logValidationViolations` (boolean): Specifies if shape violations are sent to the logging framework (optional).
- `config:shacl.ignoreNoShapesLoadedException` (boolean): Specifies if the "no shapes loaded" error is ignored (optional) {{< tag "deprecated" >}}.
- `config:shacl.validationEnabled` (boolean): Specifies if transaction valudation is enabled by default (optional).
- `config:shacl.cacheSelectNodes` (boolean): Specifies if select nodes are cached (optional).
- `config:shacl.globalLogValidationExecution` (boolean): Specifies if validation execution details are sent to the logging framework (optional).
- `config:shacl.rdfsSubClassReasoning` (boolean): Enables RDF Schema Subclass entailment (optional).
- `config:shacl.performanceLogging` (boolean): Enables performance logging (optional).
- `config:shacl.serializableValidation` (boolean): When enabled, combined with transactions using isolation level `SNAPSHOT` the validation guarantee is equivalent to using `SERIALIZABLE` without the performance impact.
- `config:shacl.eclipseRdf4jShaclExtensions` (boolean): Enables [RDF4J-specific SHACL extensions](/shacl-extensions/)  (optional).
- `config:shacl.dashDataShapes` (boolean): Enables use of [DASH data shapes](https://datashapes.org/dash) (optional).
- `config:shacl.validationResultsLimitTotal` (integer): Specifies a limit on the total number of validation results sent in a validation report (optional). A values of -1 indicates no limit.
- `config:shacl.validationResultsLimitPerConstraint` (integer): Specifies a limit on the number of validation results sent per constraint in a validation report (optional). A values of -1 indicates no limit.

##### Example configuration

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "example" ;
   rdfs:label "Example Memory store with SHACL validation" ;
   config:rep.impl [
      config:rep.id "openrdf:SailRepository" ;
      sr:sail.impl [
         config:sail.type "rdf4j:ShaclSail";
         config:shacl.parallelValidation true;
         config:shacl.validationResultsLimitTotal 300;
         config:delegate [
             config:sail.type "openrdf:MemoryStore" ;
             ...
         ];
      ]
   ].
```

## Migrating old configurations

Since RDF4J 4.3.0, the configuration vocabulary has been updated and simplified:

 - we have removed all references to the (now defunct) openrdf.org domain, replacing it with rdf4j.org
 - we have unified the vocabularies for the various modules and components into a single namespace.

To illustrate what changes are required, we first show a legacy configuration for a repository using a native store backend:

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ns: <http://www.openrdf.org/config/sail/native#>.
@prefix sb: <http://www.openrdf.org/config/sail/base#>.

[] a rep:Repository ;
    rep:repositoryID "mystore" ;
    rdfs:label "my native store" ;
    rep:repositoryImpl [
        rep:repositoryType "openrdf:SailRepository" ;
        sr:sailImpl [
            sail:sailType "openrdf:NativeStore" ;
            sail:iterationCacheSyncThreshold "10000";
            ns:tripleIndexes "spoc,posc" ;
            sb:defaultQueryEvaluationMode "STANDARD"
        ]
    ].
```

To migrate this config to the new vocabulary, we first replace all custom namespace prefixes with a single new one:

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.
```
Each attribute and vocabulary value in the actual data needs to be rewritten. The old `rep`, `sr`, `sail`, `ns` and `sb` namespace prefixes all need to be replaced with the `config` prefix. Additionally, the local names of each attribute need to be prepended with the shortname of the component to which they belong - and we have taken the opportunity to shorten/clean up the actual local names as well. For example, `rep:repositoryImpl` has become `config:rep.impl`, and `ns:tripleIndexes` now is `config:native.tripleIndexes`. 

The fully rewritten configuration looks like this:

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
    config:rep.id "mystore" ;
    rdfs:label "my native store" ;
    config:rep.impl [
        config:rep.type "openrdf:SailRepository" ;
        config:sail.impl [
            config:sail.type "openrdf:NativeStore" ;
            config:sail.iterationCacheSyncThreshold "10000";
            config:native.tripleIndexes "spoc,posc" ;
            config:sail.defaultQueryEvaluationMode "STANDARD"
        ]
    ].
```

Note that we have not (yet) renamed the type identifier literals `openrdf:SailRepository` and `openrdf:NativeStore`. For more details we refer you to the {{< javadoc "CONFIG javadoc" "model/vocabulary/CONFIG.html" >}}.

