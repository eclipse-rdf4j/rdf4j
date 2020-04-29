---
title: "Federation with FedX"
layout: "doc"
hide_page_title: "true"
---

# Federation with FedX

(new in RDF4J 3.1)

FedX provides transparent federation of multiple SPARQL endpoints under a single virtual endpoint. As an example, a knowledge graph such as Wikidata can be queried in a federation with endpoints that are linked to Wikidata as an integration hub. In a federated SPARQL query in FedX, one no longer needs to explicitly address specific endpoints using SERVICE clauses. Instead, FedX automatically selects relevant sources, sends statement patterns to these sources for evaluation, and joins the individual results. FedX seamlessly integrates into RDF4J using the Repository API and can be used as a drop-in component in existing applications including the RDF4J Workbench.


## Core Features

* Virtual Integration of heterogeneous Linked Data sources (e.g. as SPARQL endpoints)
* Transparent access to data sources through a federation
* Efficient query processing in federated environments
* On-demand federation setup at query time
* Fast and effective query execution due to new optimization techniques for federated setups
* Practical applicability & easy integration as a RDF4J Repository

## Getting Started

Below we present examples for getting started in using FedX.

The examples query data from http://dbpedia.org/ and join it with data from https://www.wikidata.org/. It turns out that these endpoints are currently the most reliable ones that are publicly accessible.

**Example query**

_Retrieve the European Union countries from DBpedia and join it with the GDP data coming from Wikidata_

```
SELECT * WHERE { 
  ?country a yago:WikicatMemberStatesOfTheEuropeanUnion .
  ?country owl:sameAs ?countrySameAs . 
  ?countrySameAs wdt:P2131 ?gdp .
}
```

Note that the query is a bit artificial, however, it illustrates quite well the powers of federating different data sources.


### Using a Java program

The following Java code can be used to execute our example query against the federation. 

```
Repository repository = FedXFactory.newFederation()
	.withSparqlEndpoint("http://dbpedia.org/sparql")
	.withSparqlEndpoint("https://query.wikidata.org/sparql")
	.create();
		
try (RepositoryConnection conn = repository.getConnection()) {

	String query = 
		"PREFIX wd: <http://www.wikidata.org/entity/> "
		+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/> "
		+ "SELECT * WHERE { "
		+ " ?country a <http://dbpedia.org/class/yago/WikicatMemberStatesOfTheEuropeanUnion> ."
		+ " ?country <http://www.w3.org/2002/07/owl#sameAs> ?countrySameAs . "
		+ " ?countrySameAs wdt:P2131 ?gdp ."
		+ "}";

	TupleQuery tq = conn.prepareTupleQuery(query);
	try (TupleQueryResult tqRes = tq.evaluate()) {

		int count = 0;
		while (tqRes.hasNext()) {
			BindingSet b = tqRes.next();
			System.out.println(b);
			count++;
		}

		System.out.println("Results: " + count);
	}
}
		
repository.shutDown();
```

The full code is also available as source in the "demos" package of the test source folder.

Instead of defining the federation via code, it is also possible to use data configurations. See the following sections for further examples.


## FedX in the RDF4J Workbench

FedX is integrated into the RDF4J server and RDF4J workbench. This allows creation of a managed federation repository and exposing it as a SPARQL repository. In the following provide some details on how this can be achieved.

### Using the workbench UI

The RDF4J workbench offers a UI for creating a federation:

1. Navigate to the workbench UI
2. Create some repositories (the federation members) and prepare them with data
3. Create a new repository and select _federation_ as type
4. Pick the federation members from the list of managed repositories
5. Save and explore the federation

See <a href="/documentation/tools/server-workbench/#federation">RDF4J Workbench Federation</a> for further information.

### Advanced federation using a repository config template

Repositories in the workbench can also be created using <a href="/documentation/tools/repository-configuration/">Repository configuration templates</a>.

Also a FedX federation can be configured using such template and deployed in the <a href="/documentation/tools/server-workbench/#repository-configuration">RDF4J server</a>.

The following snippet depicts an example repository configuration that defines a federation over the repositories _my-repository-1_ and _my-repository-2_. The actual repositories of the federation members are managed by the RDF4J server.

```
#
# RDF4J configuration template for a FedX Repository
#
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix fedx: <http://rdf4j.org/config/federation#>.

[] a rep:Repository ;
   rep:repositoryImpl [
      rep:repositoryType "fedx:FedXRepository" ;
      fedx:member [
         fedx:store "ResolvableRepository" ;
         fedx:repositoryName "my-repository-1" 
      ] ,
      [
         fedx:store "ResolvableRepository" ;
         fedx:repositoryName "my-repository-2" 
      ]
   ];
   rep:repositoryID "my-federation" ;
   rdfs:label "FedX Federation" .
```


In order to deploy a FedX configuration the repository configuration template needs to be placed in a `config.ttl` file in the RDF4J application dir. The full location is `[Rdf4j_DATA]/server/repositories/[REPOSITORY_ID]/config.ttl`, see <a href="/documentation/tools/server-workbench/#repository-configuration">here</a> for further details.

## FedX in Java Applications

FedX is implemented as a RDF4J Repository. To initialize FedX and the underlying federation SAIL, we provide the FedXFactory class, which provides various methods for intuitive configuration. In the following, we present various Java code snippets that illustrate how FedX can be used in an application.

Basically, FedX can be used and accessed using the SAIL architecture (see the [RDF4J SAIL documentation](http://docs.rdf4j.org/sail/) for details). The Repository can be obtained from any FedXFactory initialization method. Besides using the Repository interface for creating queries, we also provide a _QueryManager_ class to conveniently create queries. The advantage of the _QueryManager_ over using the _RepositoryConnection_ to create queries, is that preconfigured PREFIX declarations are added automatically to the query, i.e. the user can use common prefixes (such as rdf, foaf, etc.) without the need to specify them in the prologue of the query. See PREFIX Declarations for a detailed documentation.

**Example 1: Using a simple SPARQL Federation as a Repository**

In the following example, we configure a federation with the publicly available DBpedia and SemanticWebDogFood SPARQL endpoints. Please refer to Configuring FedX for details.

```
Repository repo = FedXFactory.createSparqlFederation(Arrays.asList(
			"http://dbpedia.org/sparql",
			"http://data.semanticweb.org/sparql"));
repo.init();

String q = "PREFIX rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;\n"
	+ "PREFIX dbpedia-owl: &lt;http://dbpedia.org/ontology/&gt;\n"
	+ "SELECT ?President ?Party WHERE {\n"
	+ "?President rdf:type dbpedia-owl:President .\n"
	+ "?President dbpedia-owl:party ?Party . }";

try (RepositoryConnection conn = repo.getConnection()) {
	TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
	try (TupleQueryResult res = query.evaluate()) {

		while (res.hasNext()) {
			System.out.println(res.next());
		}
	}
}

repo.shutDown();
System.out.println("Done.");
System.exit(0);
```



**Example 2: Using a data configuration file**

In this example we use a data configuration file to set up the federation members (see section on member configuration below for more details). Note that in this example we use an initialized Repository to create the query, as well as the connection. 

```
File dataConfig = new File("local/dataSourceConfig.ttl");
Repository repo = FedXFactory.createFederation(dataConfig);
repo.init();

String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
	+ "SELECT ?President ?Party WHERE {\n"
	+ "?President rdf:type dbpedia-owl:President .\n"
	+ "?President dbpedia-owl:party ?Party . }";

try (RepositoryConnection conn = repo.getConnection()) {
	TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
	try (TupleQueryResult res = query.evaluate()) {

		while (res.hasNext()) {
			System.out.println(res.next());
		}
	}
}

repo.shutDown();
System.out.println("Done.");
System.exit(0);
```



**Example 3: Setting up FedX using the Endpoint utilities**

This example shows how to setup FedX using a mechanism to include dynamic endpoints.

```
List<Endpoint> endpoints = new ArrayList<>();
endpoints.add( EndpointFactory.loadSPARQLEndpoint("dbpedia", "http://dbpedia.org/sparql"));
endpoints.add( EndpointFactory.loadSPARQLEndpoint("swdf", "http://data.semanticweb.org/sparql"));

Repository repo = FedXFactory.createFederation(endpoints);
repo.init();

String q = "PREFIX rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;\n"
	+ "PREFIX dbpedia-owl: &lt;http://dbpedia.org/ontology/&gt;\n"
	+ "SELECT ?President ?Party WHERE {\n"
	+ "?President rdf:type dbpedia-owl:President .\n"
	+ "?President dbpedia-owl:party ?Party . }";

TupleQuery query = QueryManager.prepareTupleQuery(q);
try (TupleQueryResult res = query.evaluate()) {

	while (res.hasNext()) {
		System.out.println(res.next());
	}
}

repo.shutDown();
System.out.println("Done.");
System.exit(0);
```

**Example 4: Using repositories from a remote RDF4J server as federation members**

This example shows how repositories from a remote RDF4J server can be easily used as federation members. The main idea is to instruct FedX to use the `RemoteRepositoryManager` as a resolver for the resolvable endpoints.

Similarly of course a `LocalRepositoryManager` or any other construct implementing the `RepositoryResolver` interface can be used.

```
// connection URL of a RDF4J server which manages the repositories
String serverUrl = "http://localhost:8080/rdf4j-server";
RepositoryManager repoManager = new RemoteRepositoryManager(serverUrl);

// assumes that remote repositories exist
Repository repo = FedXFactory.newFederation()
		.withRepositoryResolver(repoManager)
		.withResolvableEndpoint("my-repository-1")
		.withResolvableEndpoint("my-repository-2")
		.create();

repo.init();

try (RepositoryConnection conn = repo.getConnection()) {
	try (RepositoryResult<Statement> repoResult = conn.getStatements(null, RDF.TYPE, FOAF.PERSON)) {
		repoResult.forEach(st -> System.out.println(st));
	}
}

repo.shutDown();
repoManager.shutDown();
```


## Federation Management

FedX federations can be managed both at initialization and at runtime. This is possible since FedX is capable of on-demand federation setup, meaning that we do not require any prior knowledge about data sources.

The federation can be controlled at runtime using the _FederationManager_. This class provides all means for interacting with the federation at runtime, e.g. adding or removing federation members.

Endpoints can be added to the federation using the methods `addEndpoint(Endpoint)` and removed with `removeEndpoint(endpoint)`. Note that new endpoints can be initialized using the endpoint Management facilities.


## Endpoint Management

In FedX any federation member is mapped to an _Endpoint_. The endpoint maintains all relevant information for a particular endpoint, e.g. how triples can be retrieved from the endpoint. Endpoints can be added to the federation at initialization time or at runtime.

In FedX we provide support methods to create _Endpoints_ for SPARQL endpoints, RDF4J _NativeStores_ as well as _Resolvable Endpoints_ (e.g. managed by an RDF4J RepositoryManager). The methods can be used to create endpoints easily.

**Example: Using the endpoint Manager to create endpoints**

```
Config.initialize();
List<Endpoint> endpoints = new ArrayList<>();

// initializing a SPARQL endpoint (with explicit name)
endpoints.add( EndpointFactory.loadSPARQLEndpoint("http://dbpedia", "http://dbpedia.org/sparql"));

// another SPARQL endpoint (name is constructed from url)
endpoints.add( EndpointFactory.loadSPARQLEndpoint("http://data.semanticweb.org/sparql"));

// load a RDF4J NativeStore (path either absolute or relative to Config#getBaseDir)
endpoints.add( EndpointFactory.loadNativeEndpoint("http://mystore", "path/to/myNativeStore"));

FedXFactory.initializeFederation(endpoints);
```

For details about the methods please refer to the javadoc help of the class _EndpointFactory_


**Note:** With the Endpoint mechanism it is basically possible to support any kind of Repository of SAIL implementation as federation member. For documentation consider the javadoc, in particular _EndpointFactory_ and  _EndpointProvider_.


## FedX configuration

FedX provides various means for configuration. Configuration settings can be defined using the `FedXConfig` facility, which can be passed at initialization time. Note that certain settings can also be changed during runtime, please refer to the API documentation for details. 



### Available Properties


<table border=1 style="cellpadding: 4px;">
<tr><th>Property</th><th>Description</th></tr>
<tr><td>prefixDeclarations</td><td>Path to prefix declarations file, see PREFIX Declarations</td></tr>
<tr><td>cacheLocation</td><td>Location where the memory cache gets persisted at shutdown, default <i>cache.db</i></td></tr>
<tr><td>joinWorkerThreads</td><td>The number of join worker threads for parallelization, default <i>20</i></td></tr>
<tr><td>unionWorkerThreads</td><td>The number of union worker threads for parallelization, default <i>20</i></td></tr>
<tr><td>boundJoinBlockSize</td><td>Block size for bound joins, default <i>15</i></td></tr>
<tr><td>enforceMaxQueryTime</td><td>Max query time in seconds, 0 to disable, default <i>30</i></td></tr>
<tr><td>enableServiceAsBoundJoin</td><td>Flag for evaluating a SERVICE expression (contacting non-federation members) using vectored evaluation, default <i>true</i>. For today's endpoints it is more efficient to disable vectored evaluation of SERVICE</td></tr>
<tr><td>debugQueryPlan</td><td>Print the optimized query execution plan to stdout, default <i>false</i></td></tr>
<tr><td>enableMonitoring</td><td>Flag to enable/disable monitoring features, default <i>false</i></td></tr>
<tr><td>logQueryPlan</td><td>Flag to enable/disable query plan logging via Java class <i>QueryPlanLog</i>, default <i>false</i></td></tr>
<tr><td>logQueries</td><td>Flag to enable/disable query logging via <i>QueryLog</i>, default <i>false</i>. The <i>QueryLog</i> facility allows to log all queries to a file</td></tr>
</table>



### Query timeouts

FedX supports to define the maximum execution time for a query. This can be set on query level `Query#setMaxExecutionTime`or globally using the FedX config setting _enforceMaxQueryTime_.

Note that the query engine attempts to abort any running evaluation of a subquery when the maximum execution time has reached.

If a query timeout occurs, a _QueryInterruptedException_ is thrown.


### Prefix declarations

FedX allows to (optionally) define commonly used prefixes (e.g. rdf, foaf, etc.) in a 
configuration file. These configured prefixes are then automatically inserted into a query,
meaning that the user does not have to specify full URIs nor the PREFIX declaration in the 
query.

The prefixes can be either specified in a configuration file as key-value pairs or directly
configured via Java code (see examples below). When using a configuration file, this can be 
configured via the _prefixDeclarations_ property.

**Example: Prefix configuration via configuration file**

```
# this file contains a set of prefix declarations
=http://example.org/
foaf=http://xmlns.com/foaf/0.1/
rdf=http://www.w3.org/1999/02/22-rdf-syntax-ns#
dbpedia=http://dbpedia.org/ontology/
```


**Example: Setting prefixes at runtime**

The QueryManager can be used to define additional prefixes at runtime.

```
QueryManager qm = repo.getQueryManager();
qm.addPrefixDeclaration("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
qm.addPrefixDeclaration("dbpedia", "http://dbpedia.org/ontology/");
```

## Member configuration

Federation members can be added to a federation either directly as a list of endpoints, or using a data configuration file (see section FedX in Java applications). In a data configuration the federation members are specified using turtle syntax.


### Example 1: SPARQL Federation:

```
@prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
@prefix fedx: <http://rdf4j.org/config/federation#> .

<http://DBpedia> a sd:Service ;
	fedx:store "SPARQLEndpoint";
	sd:endpoint "http://dbpedia.org/sparql";
	fedx:supportsASKQueries false .

<http://SWDF> a sd:Service ;
	fedx:store "SPARQLEndpoint" ;
	sd:endpoint "http://data.semanticweb.org/sparql".

<http://LinkedMDB> a sd:Service ;
	fedx:store "SPARQLEndpoint";
	sd:endpoint "http://data.linkedmdb.org/sparql".
```

Note: if a SPARQL endpoint does not support ASK queries, the endpoint can be configured to use SELECT queries instead using `fedx:supportsASKQueries false`. This is for instance useful for Virtuoso based endpoints like
DBpedia. Moreover note that for convenience the public DBpedia endpoint is automatically configured to use SELECT queries.

### Example 2: SPARQL Federation with RDF4J remote repositories

```
@prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
@prefix fedx: <http://rdf4j.org/config/federation#> .

<http://dbpedia> a sd:Service ;
	fedx:store "RemoteRepository";
	fedx:repositoryServer "http://host/rdf4j-server" ;
	fedx:repositoryName "repoName" .
```

### Example 3: Local Federation (NativeStore):

```
@prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
@prefix fedx: <http://rdf4j.org/config/federation#> .

<http://DBpedia> a sd:Service ;
	fedx:store "NativeStore";
	fedx:repositoryLocation "repositories\\native-storage.dbpedia36".

<http://NYTimes> a sd:Service ;
	fedx:store "NativeStore";
	fedx:repositoryLocation "repositories\\native-storage.nytimes".
```


### Example 4: Federation with resolvable endpoints:


FedX supports to use resolvable endpoints as federation members. These resolvable repositories are not managed by FedX, but are resolved using a provided _RepositoryResolver_. An example use case is to reference a repository managed by the RDF4J Server (i.e. from within the RDF4J workbench). Alternatively, any custom resolver can be provided to FedX during the initialization using the _FedXFactory_, e.g. a `LocalRepositoryManager`.

```
@prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
@prefix fedx: <http://rdf4j.org/config/federation#> .

<http://myNativeStore> a sd:Service ;
	fedx:store "ResolvableRepository" ;
	fedx:repositoryName "myNativeStore" .
```

Note that also hybrid combinations are possible.

### Example 5: Federation with writable endpoint:

(new in RDF4J 3.2.0) 

FedX supports nominating a single federation member as being able to receive updates. If enabled, any statement add/remove operations, including SPARQL updates, will be forwarded on top of the nominated member:

```
@prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
@prefix fedx: <http://rdf4j.org/config/federation#> .

<http://myNativeStore> a sd:Service ;
	fedx:store "NativeStore";
	fedx:repositoryLocation "repositories\\my-native-store" ;
	fedx:writable true .

<http://DBpedia> a sd:Service ;
	fedx:store "SPARQLEndpoint";
	sd:endpoint "http://dbpedia.org/sparql";
	fedx:supportsASKQueries false .
```

Notes:

* If more than one endpoint is configured to be writable, FedX will select any at random for write operations
* Any type of endpoint can be configured to be writable. For production settings it is best practice to use external repositories accessed as _ResolvableRepository_ or _SPARQLEndpoint_.

## Monitoring & Logging

FedX does not rely on a specific logging backend implementation at runtime. To integrate with any logging backends it is possible to use any of the SLF4J adapters.

FedX brings certain facilities to monitor the application state. These facilities are described in the following.

Note: for the following features `enableMonitoring` must be set in the FedX configuration.

### Logging queries

By setting _logQueries=true_ in the FedX configuration, all incoming queries are traced
to a logger with the name _QueryLogger_. If a corresponding configuration is added to the logging backend, the queries can for instance be traced to a file. 

### Logging the query plan

There are two ways of seeing the optimized query plan:

a) by setting _debugQueryPlan=true_, the query plan is printed to stdout (which is handy in the CLI or for debugging).

b) by setting _logQueryPlan=true_ the optimized query plan is written to a variable local to the executing thread.The optimized query plan can be retrieved via the _QueryPlanLog_ service, as illustrated in the following abstract snippet.
 
```
FedXConfig config = new FedXConfig().withEnableMonitoring(true).withLogQueryPlan(true);
Repository repo = FedXFactory.newFederation()
		.withSparqlEndpoint("http://dbpedia.org/sparql")
		.withSparqlEndpoint("https://query.wikidata.org/sparql")
		.withConfig(config)
		.create();

TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, <SOME_QUERY>);
.. evaluate query ..

System.out.println("# Optimized Query Plan:");
System.out.println(QueryPlanLog.getQueryPlan());
```

### Monitoring the number of requests

If monitoring is enabled, the number of requests sent to each individual federation member are monitored. All
available information can be retrieved by the _MonitoringService_, which can be retrieved via 

`MonitoringUtil.getMonitoringService()`

The following snippet illustrates a monitoring utility that prints all monitoring information to stdout.

```
FedXConfig config = new FedXConfig().withEnableMonitoring(true).withLogQueryPlan(true);
FedXRepository repo = FedXFactory.newFederation()
		.withSparqlEndpoint("http://dbpedia.org/sparql")
		.withSparqlEndpoint("https://query.wikidata.org/sparql")
		.withConfig(config)
		.create();
repo.init();

TupleQuery query = ...

.. evaluate queries ..

MonitoringUtil.printMonitoringInformation(repo.getFederationContext());
```
