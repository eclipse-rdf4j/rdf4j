---
title: "The Repository API"
weight: 3
toc: true
autonumbering: true
---
The Repository API is the central access point for RDF4J-compatible RDF databases (a.k.a. triplestores), as well as for SPARQL endpoints. This is what you use to execute SPARQL queries and update your data.
<!--more-->

The interfaces for the Repository API can be found in package `org.eclipse.rdf4j.repository`. Several implementations for these interface exist in various sub-packages.

## Creating a Repository object

The central interface of the Repository API is the {{< javadoc "Repository" "repository/Repository.html" >}} interface. There are several implementations available of this interface. The three main ones are:

{{< javadoc "SailRepository" "repository/sail/SailRepository.html" >}} is a Repository that operates directly on top of a {{< javadoc "Sail" "sail/Sail.html" >}} - that is, a particular database. This is the class most commonly used when accessing/creating a local RDF4J repository. SailRepository operates on a (stack of) Sail object(s) for storage and retrieval of RDF data. An important thing to remember is that the behaviour of a repository is determined by the Sail(s) that it operates on; for example, the repository will only support RDF Schema or OWL semantics if the Sail stack includes support for this.

{{< javadoc "HTTPRepository" "repository/http/HTTPRepository.html" >}} is a Repository implementation that acts as a proxy to a repository available on a remote RDF4J Server, accessible through HTTP.

{{< javadoc "SPARQLRepository" "repository/sparql/SPARQLRepository.html" >}} is a Repository implementation that acts as a proxy to any remote SPARQL endpoint (whether that endpoint is implemented using RDF4J or not).

Creating Repository objects can be done in multiple ways. We will first show an easy way to quickly create such an object ‘on the fly’. In the section about the RepositoryManager and RepositoryProvider, we show some more advanced patterns, which are particularly useful in larger applications which have to handle and share references to multiple repositories.

### Main memory RDF Repository

One of the simplest configurations is a repository that just stores RDF data in main memory without applying any inferencing. This is also the fastest type of repository that can be used. The following code creates and initializes a non-inferencing main-memory repository:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
...
Repository repo = new SailRepository(new MemoryStore());
```

The constructor of the SailRepository class accepts any object of type `Sail`, so we pass it a new main-memory store object (which is a `Sail` implementation).

The repository that is created by the above code is volatile: its contents are lost when the object is garbage collected or when your Java program is shut down. This is fine for cases where, for example, the repository is used as a means for manipulating an RDF model in memory.

Different types of Sail objects take parameters in their constructor that change their behaviour. The `MemoryStore` takes a `datadir` parameter that specifies a data directory for persisent storage. If specified, the MemoryStore will write its contents to this directory so that it can restore it when it is re-initialized in a future session:

```java
File dataDir = new File("C:\\temp\\myRepository\\");
Repository repo = new SailRepository( new MemoryStore(dataDir) );
```

We can fine-tune the configuration of our repository by passing parameters to the constructor of the Sail object. Some Sail types may offer additional configuration methods, all of which need to be called before the repository is initialized. The MemoryStore currently has one such method: `setSyncDelay(long)`, which can be used to control the strategy that is used for writing to the data file. For example:

```java
File dataDir = new File("C:\\temp\\myRepository\\");
MemoryStore memStore = new MemoryStore(dataDir);
memStore.setSyncDelay(1000L);
Repository repo = new SailRepository(memStore);
```

 The MemoryStore is designed for datasets with fewer than 100,000 triples.

### Native RDF Repository

The NativeStore saves data to disk in a binary format which is optimized for compact storage and fast retrieval. If there is sufficient physical memory, the Native store will act like the MemoryStore on most operating systems because the read/write commands will be cached by the OS.

It is therefore an efficient, scalable and fast solution for datasets with up to 100 million triples (and probably even more).

The code for creation of a Native RDF repository is almost identical to that of a main memory repository:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
File dataDir = new File("/path/to/datadir/");
Repository repo = new SailRepository(new NativeStore(dataDir));
```

By default, the Native store creates a set of two indexes. To configure which indexes it should create, we can either use the `NativeStore.setTripleIndexes(String)` method, or we can directly supply a index configuration string to the constructor:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
File dataDir = new File("/path/to/datadir/");
String indexes = "spoc,posc,cosp";
Repository repo = new SailRepository(new NativeStore(dataDir, indexes));
```

{{< tag " New in RDF4J 3.7" >}}

If a data directory is not set, a temporary directory will be created when the native store is initialized and (contrary to the previous examples) subsequently deleted when the repository is shut down.

This is convenient when the repository is only used as a means to transform large RDF files.

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
Repository repo = new SailRepository(new NativeStore());
```

In the unlikely event of corruption the system property `org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes` can be set to `true` to
allow the NativeStore to output CorruptValue/CorruptIRI/CorruptIRIOrBNode/CorruptLiteral objects. Take a backup of all data before setting 
this property as it allows the NativeStore to delete corrupt indexes in an attempt to recreate them. Consider this feature experimental and use with caution.

### Elasticsearch RDF Repository

{{< tag " New in RDF4J 3.1" >}}

{{< tag " Experimental " >}}

The ElasticsearchStore stores RDF data in Elasticsearch. Not to be confused with the ElasticsearchSail which uses Elasticsearch for enhanced search.

The ElasticsearchStore is experimental and future releases may be incompatible with the current version. Write-ahead-logging is not supported.
This means that a write operation can appear to have partially succeeded if the ElasticsearchStore looses its connection to Elasticsearch during a commit.

Note that, while RDF4J is licensed under the EDL, Elasticsearch itself is distributed under the Elastic License (with SSPL as an alternative).
The Elasticsearch-backed functionality in RDF4J is optional, and adopters should carefully evaluate the Elasticsearch licensing terms to ensure they meet the needs of their projects before enabling it.
Please consult the ElasticSearch website and [license FAQ](https://www.elastic.co/licensing/elastic-license/faq) for more information.
 
Transaction isolation is not as strong as for the other stores. The highest supported level is READ_COMMITTED, and even this
level is only guaranteed when all other transactions also use READ_COMMITTED.

Performance for the NativeStore is in most cases considerably better than for the ElasticsearchStore.
The read cache in the ElasticsearchStore makes workloads with repetitive reads fast. Storing small, infrequently updated, datasets such as a
reference library or an ontology is a good usecase for the ElasticsearchStore.

The code for creation of an ElasticsearchStore is almost identical to other repositories:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
...
// ElasticsearchStore(String hostname, int port, String clusterName, String index)
Repository repo = new SailRepository(new ElasticsearchStore("localhost", 9300, "elasticsearch", "rdf4j_index"));
```

Remember to call `repo.shutdown()` when you are done with your ElasticsearchStore. This will close the underlying Elasticsearch Client.

### RDF Schema inferencing

As we have seen, we can create Repository objects for any kind of back-end store by passing them a reference to the appropriate Sail object. We can pass any stack of Sails this way, allowing all kinds of repository configurations to be created. For example, to stack an RDF Schema inferencer on top of a memory store, we simply create a repository like so:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
...
Repository repo = new SailRepository(
			  new SchemaCachingRDFSInferencer(
			  new MemoryStore()));
```

Each layer in the Sail stack is created by a constructor that takes the underlying Sail as a parameter. Finally, we create the SailRepository object as a functional wrapper around the Sail stack.

The {{< javadoc "SchemaCachingRDFSInferencer" "sail/inferencer/fc/SchemaCachingRDFSInferencer.html" >}} that is used in this example is a generic RDF Schema inferencer; it can be used on top of any Sail that supports the methods it requires. Both MemoryStore and NativeStore support these methods. However, a word of warning: the RDF4J inferencers add a significant performance overhead when adding and removing data to a repository, an overhead that gets progressively worse as the total size of the repository increases. For small to medium-sized datasets it peforms fine, but for larger datasets you are advised not to use it and to switch to alternatives.

### Custom Inferencing

The previous section showed how to use the built-in RDF schema inferencer. This section will show how to create a repository capable of performing inferences according to a custom rule that you provide.

```java
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;
...
String pre = "PREFIX : <http://foo.org/bar#>\n";
String rule = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } WHERE " +
	      "{ { :Bob ?p :Alice } UNION { :Alice ?p :Bob } }";
String match = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } " +
	       "WHERE { ?p :relatesTo :Cryptography }";
Repository repo = new SailRepository(new CustomGraphQueryInferencer(
		  new MemoryStore(), QueryLanguage.SPARQL, rule, match));
```

Here is a data sample (given in the Turtle format) that serves to illustrate this example:

    @prefix : <http://foo.org/bar#> .
    :Bob   :exchangesKeysWith :Alice .
    :Alice :sendsMessageTo    :Bob .

If the above data is loaded into the repository, the repository will also automatically have the following inferred statements:

    @prefix : <http://foo.org/bar#> .
    :exchangesKeysWith :relatesTo :Cryptography .
    :sendsMessageTo    :relatesTo :Cryptography .

The SPARQL graph query in `rule` defines a pattern to search on, and the inferred statements to add to the repository.

The graph query in `match` is needed to decide what inferred statements already exist that may need to be removed when the normal repository contents change. For example, if the first sample data statement was removed, then the inference layer will automatically remove the inferred statement regarding `:exchangesKeysWith`.

In simple rule cases, such as this one, an empty string could have been provided for `match` instead, and the correct matcher query would have been deduced.

The CustomGraphQueryInferencer used here is fairly limited: it effectively only allows a single inference rule. For more complex custom inferencing or validation needs, RDF4J offers the SPIN Sail or the SHACL Sail.

### Access over HTTP
#### Server-side RDF4J repositories

Working with remote RDF4J repositories is just as easy as working with local ones. We use a different Repository object, the `HTTPRepository`, instead of the SailRepository class.

A requirement is that there is a RDF4J Server running on some remote system, which is accessible over HTTP. For example, suppose that at `http://example.org/rdf4j-server/` a RDF4J Server is running, which has a repository with the identification `example-db`. We can access this repository in our code as follows:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
...
String rdf4jServer = "http://example.org/rdf4j-server/";
String repositoryID = "example-db";
Repository repo = new HTTPRepository(rdf4jServer, repositoryID);
```

#### SPARQL endpoints

We can use the Repository interface to access any SPARQL endpoint as well. This is done as follows:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
...
String sparqlEndpoint = "http://example.org/sparql";
Repository repo = new SPARQLRepository(sparqlEndpoint);
```

After you have done this, you can query the SPARQL endpoint just as you would any other type of Repository.

#### Configuring the HTTP session thread pool

Both the HTTPRepository and the SPARQLRepository use the SPARQL Protocol over
HTTP under the hood (in the case of the HTTPRepository, it uses the extended
RDF4J REST API). The HTTP client session is managed by the {{< javadoc
"HttpClientSessionManager"
"http/client/HttpClientSessionManager.html" >}}, which in turn depends
on the Apache HttpClient.

The session uses a caching thread pool executor to handle multithreaded
access to a remote endpoint, defined by default to use a thread pool with a
core size of 1.

To configure this to use a different core pool size, you can specify the
`org.eclipse.rdf4j.client.executors.corePoolSize` system property with a
different number.

### The RepositoryManager and RepositoryProvider

Using what we’ve seen in the previous section, we can create and use various different types of repositories. However, when developing an application in which you have to keep track of several repositories, sharing references to these repositories between different parts of your code can become complex. The {{< javadoc "RepositoryManager" "repository/manager/RepositoryManager.html" >}} and {{< javadoc "RepositoryProvider" "repository/manager/RepositoryProvider.html" >}} provide one central location where all information on the repositories in use (including id, type, directory for persistent data storage, etc.) is kept.
Using the RepositoryManager for handling repository creation and administration offers a number of advantages, including:

- a single RepositoryManager object can be more easily shared throughout your application than a host of static references to individual repositories;
- you can more easily create and manage repositories ‘on-the-fly’, for example if your application requires creation of new repositories on user input;
- the RepositoryManager stores your configuration, including all repository data, in one central spot on the file system.

The RepositoryManager comes in two flavours: the LocalRepositoryManager and the RemoteRepositoryManager.

A LocalRepositoryManager manages repository handling for you locally, and is always created using a (local) directory. This directory is where all repositories handled by the manager store their data, and also where the LocalRepositoryManager itself stores its configuration data.

You create a new LocalRepositoryManager as follows:

```java
import java.io.File;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
File baseDir = new File("/path/to/storage/dir/");
LocalRepositoryManager manager = new LocalRepositoryManager(baseDir);
manager.init();
```

To use a LocalRepositoryManager to create and manage repositories is slightly different from what we’ve seen before about creating repositories. The LocalRepositoryManager works by providing it with {{< javadoc "RepositoryConfig" "repository/config/RepositoryConfig.html" >}} objects, which are declarative specifications of the repository you want. You add a RepositoryConfig object for your new repository, and then request the actual Repository back from the LocalRepositoryManager:

```java
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

String repositoryId = "test-db";
RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
manager.addRepositoryConfig(repConfig);

Repository repository = manager.getRepository(repositoryId);
```

In the above bit of code, you may have noticed that we provide a variable called repositoryTypeSpec to the constructor of our RepositoryConfig. This variable is an instance of a class called `RepositoryImplConfig`, and this specifies the actual configuration of our new repository: what backends to use, whether or not to use inferencing, and so on.

Creating a RepositoryImplConfig object can be done in two ways: programmatically, or by reading a (RDF) config file. We will show the programmatic way first:

```java
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;

// create a configuration for the SAIL stack
var storeConfig = new MemoryStoreConfig();

// create a configuration for the repository implementation
RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(storeConfig);
```

As you can see, we use a class called `MemoryStoreConfig` for specifying the type of storage backend we want. This class resides in a config sub-package of the memory store package (`org.eclipse.rdf4j.sail.memory.config`). Each particular type of SAIL in RDF4J has such a config class.

As a second example, we create a slightly more complex type of store: still in-memory, but this time we want it to use the memory store’s persistence option, and use `standard` query evaluation mode (instead of the default, which is `strict`). We also want to add RDFS inferencing on top. RDFS inferencing is provided by a separate SAIL implementation, which can be ‘stacked’ on top of another SAIL. We follow that pattern in the creation of our config object:

```java
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;

// create a configuration for the SAIL stack
boolean persist = true;
var storeConfig = new MemoryStoreConfig(persist);
// tweak the store config to use standard query evaluation mode
storeConfig.setDefaultQueryEvaluationMode(QueryEvaluationMode.STANDARD);

// stack an inferencer config on top of our backend-config
var stackConfig = new SchemaCachingRDFSInferencerConfig(storeConfig);

// create a configuration for the repository implementation
SailRepositoryConfig repositoryTypeSpec = new SailRepositoryConfig(stackConfig);
```

#### The RemoteRepositoryManager

A useful feature of RDF4J is that most of its APIs are transparent with respect to whether you are working locally or remote. This is the case for the RDF4J repositories, but also for the RepositoryManager. In the above examples, we have used a LocalRepositoryManager, creating repositories for local use. However, it is also possible to use a RemoteRepositoryManager, using it to create and manage repositories residing on a remotely running RDF4J Server.

A RemoteRepositoryManager is initialized as follows:

```java
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;

// URL of the remote RDF4J Server we want to access
String serverUrl = "http://localhost:8080/rdf4j-server";
RemoteRepositoryManager manager = new RemoteRepositoryManager(serverUrl);
manager.init();
```

Once initialized, the RemoteRepositoryManager can be used in the same fashion as the LocalRepositoryManager: creating new repositories, requesting references to existing repositories, and so on.

#### The RepositoryProvider

Finally, RDF4J also includes a `RepositoryProvider` class. This is a utility class that holds static references to RepositoryManagers, making it easy to share Managers (and the repositories they contain) across your application. In addition, the RepositoryProvider also has a built-in shutdown hook, which makes sure all repositories managed by it are shut down when the JVM exits.

To obtain a RepositoryManager from a RepositoryProvider you invoke it with the location you want a RepositoryManager for. If you provide a HTTP url, it will automatically return a RemoteRepositoryManager, and if you provide a local file URL, it will be a LocalRepositoryManager.

```java
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
String url = "http://localhost:8080/rdf4j-server";
RepositoryManager manager  = RepositoryProvider.getRepositoryManager(url);
```

The RepositoryProvider creates and keeps a singleton instance of RepositoryManager for each distinct location you specify, which means that you invoke the above call in several places in your code without having to worry about creating duplicate manager objects.

### Creating a Federation

RDF4J has the option to create a repository that acts as a federation of stores. For more information about this, see the [FedX federation](/documentation/programming/federation) documentation.

## Using a repository: RepositoryConnections

Now that we have created a Repository, we want to do something with it. In RDF4J, this is achieved through `RepositoryConnection` objects, which can be created by the Repository.

A {{< javadoc "RepositoryConnection" "repository/RepositoryConnection.html" >}} represents a connection to the actual store. We can issue operations over this connection, and close it when we are done to make sure we are not keeping resources unnnecessarily occupied.

In the following sections, we will show some examples of basic operations.

### Adding RDF to a repository

The Repository API offers various methods for adding data to a repository. Data can be added by specifying the location of a file that contains RDF data, and statements can be added individually or in collections.

We perform operations on a repository by requesting a `RepositoryConnection` from the repository. On this RepositoryConnection object we can perform various operations, such as query evaluation, getting, adding, or removing statements, etc.

The following example code adds two files, one local and one available through HTTP, to a repository:

```java
import org.eclipse.RDF4J.RDF4JException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import java.io.File;
import java.net.URL;
...
File file = new File("/path/to/example.rdf");
String baseURI = "http://example.org/example/local";
try {
   RepositoryConnection con = repo.getConnection();
   try {
      con.add(file, baseURI, RDFFormat.RDFXML);
      URL url = new URL("http://example.org/example/remote.rdf");
      con.add(url, url.toString(), RDFFormat.RDFXML);
   }
   finally {
      con.close();
   }
}
catch (RDF4JException e) {
   // handle exception
}
catch (java.io.IOEXception e) {
   // handle io exception
}
```

As you can see, the above code does very explicit exception handling and makes sure resources are properly closed when we are done. A lot of this can be simplified. `RepositoryConnection` implements `AutoCloseable`, so a first simple change is to use a try-with-resources construction for handling proper opening and closing of the `RepositoryConnection`:

```java
File file = new File("/path/to/example.rdf");
String baseURI = "http://example.org/example/local";
try (RepositoryConnection con = repo.getConnection()) {
   con.add(file, baseURI, RDFFormat.RDFXML);
   URL url = new URL("http://example.org/example/remote.rdf");
   con.add(url, url.toString(), RDFFormat.RDFXML);
}
catch (RDF4JException e) {
   // handle exception. This catch-clause is
   // optional since RDF4JException is an unchecked exception
}
catch (java.io.IOEXception e) {
   // handle io exception
}
```

More information on other available methods can be found in the {{< javadoc "RepositoryConnection" "repository/RepositoryConnection.html" >}} javadoc.

### Querying a repository

The Repository API has a number of methods for creating and evaluating queries. Three types of queries are distinguished: tuple queries, graph queries and boolean queries. The query types differ in the type of results that they produce.

The result of a tuple query is a set of tuples (or variable bindings), where each tuple represents a solution of a query. This type of query is commonly used to get specific values (URIs, blank nodes, literals) from the stored RDF data. SPARQL SELECT queries are tuple queries.

The result of graph queries is an RDF graph (or set of statements). This type of query is very useful for extracting sub-graphs from the stored RDF data, which can then be queried further, serialized to an RDF document, etc. SPARQL CONSTRUCT and DESCRIBE queries are graph queries.

The result of boolean queries is a simple boolean value, i.e. true or false. This type of query can be used to check if a repository contains specific information. SPARQL ASK queries are boolean queries.

#### SELECT: tuple queries

To evaluate a (SELECT) tuple query we can do the following:

```java
import java.util.List;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
...
try (RepositoryConnection conn = repo.getConnection()) {
   String queryString = "SELECT ?x ?y WHERE { ?x ?p ?y } ";
   TupleQuery tupleQuery = con.prepareTupleQuery(queryString);
   try (TupleQueryResult result = tupleQuery.evaluate()) {
      for (BindingSet bindingSet: result) {
         Value valueOfX = bindingSet.getValue("x");
         Value valueOfY = bindingSet.getValue("y");
         // do something interesting with the values here...
      }
   }
}
```

This evaluates a SPARQL SELECT query and returns a TupleQueryResult, which consists of a sequence of BindingSet objects. Each BindingSet contains a set of Binding objects. A binding is pair relating a variable name (as used in the query’s SELECT clause) with a value.

We can use the TupleQueryResult to iterate over all results and get each individual result for x and y. We retrieve values by name rather than by an index. The names used should be the names of variables as specified in your query (note that we leave out the ‘?’ or ‘$’ prefixes used in SPARQL). The `TupleQueryResult.getBindingNames()` method returns a list of binding names, in the order in which they were specified in the query. To process the bindings in each binding set in the order specified by the projection, you can do the following:

```java
List<String> bindingNames = result.getBindingNames();
for (BindingSet bindingSet: result) {
   Value firstValue = bindingSet.getValue(bindingNames.get(0));
   Value secondValue = bindingSet.getValue(bindingNames.get(1));
   // do something interesting with the values here...
}
```

Finally, it is important to make sure that both the TupleQueryResult and the RepositoryConnection are properly closed after we are done with them. A TupleQueryResult evaluates lazily and keeps resources (such as connections to the underlying database) open. Closing the TupleQueryResult frees up these resources. You can either expliclty invoke close() in the finally clause, or use a try-with-resources construction (as shown in the above examples) to let Java itself handle proper closing for you. In the following code examples, we will use both ways to handle both result and connection closure interchangeably.

As said: a TupleQueryResult evaluates lazily, and keeps an open connection to the data source while being processed. If you wish to quickly materialize the full query result (for example, convert it to a Java List) and then close the TupleQueryResult, you can do something like this:

```java
List<BindingSet> resultList;
try (TupleQueryResult result = tupleQuery.evaluate()) {
   resultList = QueryResults.asList(result);
}
```

#### A SPARQL SELECT query in a single line of code: the Repositories utility

RDF4J provides a convenience utility class `org.eclipse.rdf4j.repository.util.Repositories`, which allows us to significantly shorten our boilerplate code. In particular, the `Repositories` utility allows us to do away with opening/closing a RepositoryConnection completely. For example, to open a connection, create and evaluate a SPARQL SELECT query, and then put that query’s result in a list, we can do the following:

```java
List<BindingSet> results = Repositories.tupleQuery(rep, "SELECT * WHERE {?s ?p ?o }", r -> QueryResults.asList(r));
```

We make use of Lambda expressions to process the result. In this particular example, the only processing we do is to convert the `TupleQueryResult` object into a `List`. However, you can supply any kind of function to this interface to fully customize the processing that you do on the result.

#### Using TupleQueryResultHandlers

You can also directly process the query result by supplying a `TupleQueryResultHandler` to the query’s `evaluate()` method. The main difference is that when using a return object, the caller has control over when the next answer is retrieved (namely, whenever `next()` is called), whereas with the use of a handler, the connection pushes answers to the handler object as soon as it has them available.

As an example we will use `SPARQLResultsCSVWriter` to directly write the query result to the console. SPARQLResultsCSVWriter is a TupleQueryResultHandler implementation that writes SPARQL Results as comma-separated values.

```java
String queryString = "SELECT * WHERE {?x ?p ?y }";
con.prepareTupleQuery(queryString).evaluate(new SPARQLResultsCSVWriter(System.out));
```

RDF4J provides a number of standard implementations of TupleQueryResultHandler, and of course you can also supply your own application-specific implementation. Have a look in the Javadoc for more details.

#### CONSTRUCT/DESCRIBE: graph queries

The following code evaluates a graph query on a repository:

```java
import org.eclipse.rdf4j.query.GraphQueryResult;
GraphQueryResult graphResult = con.prepareGraphQuery("CONSTRUCT { ?s ?p ?o } WHERE {?s ?p ?o }").evaluate();
```

A GraphQueryResult is similar to TupleQueryResult in that is an object that iterates over the query solutions. However, for graph queries the query solutions are RDF statements, so a GraphQueryResult iterates over Statement objects:

```java
for (Statement st: graphResult) {
   // ... do something with the resulting statement here.
}
```

You can also quickly turn a GraphQueryResult into a Model (that is, a Java Collection of statements), by using the org.eclipse.rdf4j.query.QueryResults utility class:

```java
Model resultModel = QueryResults.asModel(graphQueryResult);
```

#### Doing a graph query in a single line of code

Similarly to how we do this with SELECT queries, we can use the Repositories utility to obtain a result from a SPARQL CONSTRUCT (or DESCRIBE) query in a single line of Java code:

```java
Model m = Repositories.graphQuery(rep, "CONSTRUCT WHERE {?s ?p ?o}", r -> QueryResults.asModel(r));
```

#### Using RDFHandlers

For graph queries, we can supply an `org.eclipse.rdf4j.rio.RDFHandler` to the `evaluate()` method. Again, this is a generic interface, each object implementing it can process the reported RDF statements in any way it wants.

All Rio writers (such as the RDFXmlWriter, TurtleWriter, TriXWriter, etc.) implement the RDFHandler interface. This allows them to be used in combination with querying quite easily. In the following example, we use a TurtleWriter to write the result of a SPARQL graph query to standard output in Turtle format:

```java
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
try (RepositoryConnection conn = repo.getConnection()) {
   RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, System.out);
   conn.prepareGraphQuery(QueryLanguage.SPARQL,
       "CONSTRUCT {?s ?p ?o } WHERE {?s ?p ?o } ").evaluate(writer);
}
```

Note that in the above code we use the `org.eclipse.rdf4j.rio.Rio` utility to quickly create a writer of the desired format. The Rio utility offers a lot of useful functions to quickly create writers and parser for various formats.

#### Preparing and Reusing Queries

In the previous sections we have simply created a query from a string and immediately evaluated it. However, the `prepareTupleQuery` and `prepareGraphQuery` methods return objects of type `Query`, specifically `TupleQuery` and `GraphQuery`.

A Query object, once created, can be reused several times. For example, we can evaluate a Query object, then add some data to our repository, and evaluate the same query again.

The Query object also has a `setBinding()` method, which can be used to specify specific values for query variables. As a simple example, suppose we have a repository containing names and e-mail addresses of people, and we want to do a query for each person, retrieve his/her e-mail address, for example, but we want to do a separate query for each person. This can be achieved using the setBinding() functionality, as follows:

```java
try (RepositoryConnection con = repo.getConnection()){
   // First, prepare a query that retrieves all names of persons
   TupleQuery nameQuery = con.prepareTupleQuery("SELECT ?name WHERE { ?person ex:name ?name . }");

   // Then, prepare another query that retrieves all e-mail addresses of persons:
   TupleQuery mailQuery = con.prepareTupleQuery("SELECT ?mail WHERE { ?person ex:mail ?mail ; ex:name ?name . }");

   // Evaluate the first query to get all names
   try (TupleQueryResult nameResult = nameQuery.evaluate()){
      // Loop over all names, and retrieve the corresponding e-mail address.
      for (BindingSet bindingSet: nameResult) {
         Value name = bindingSet.get("name");

         // Retrieve the matching mailbox, by setting the binding for
         // the variable 'name' to the retrieved value. Note that we
         // can set the same binding name again for each iteration, it will
         // overwrite the previous setting.
         mailQuery.setBinding("name", name);
         try ( TupleQueryResult mailResult = mailQuery.evaluate()) {
            // mailResult now contains the e-mail addresses for one particular person

            ....
         }
      }
   }
}
```

The values with which you perform the `setBinding` operation of course do not necessarily have to come from a previous query result (as they do in the above example). As also shown in [The RDF Model API documentation](/documentation/programming/model/#creating-new-building-blocks-the-values-and-statements-factory-methods), you can create your own value objects. You can use this functionality to, for example, query for a particular keyword that is given by user input:

```java
import static org.eclipse.rdf4j.model.util.Values.literal;

// In this example, we specify the keyword string. Of course, this
// could just as easily be obtained by user input, or by reading from
// a file, or...
String keyword = "foobar";

// We prepare a query that retrieves all documents for a keyword.
// Notice that in this query the 'keyword' variable is not bound to
// any specific value yet.
TupleQuery keywordQuery = con.prepareTupleQuery("SELECT ?document WHERE { ?document ex:keyword ?keyword . }");

// Then we set the binding to a literal representation of our keyword.
// Evaluation of the query object will now effectively be the same as
// if we had specified the query as follows:
//   SELECT ?document WHERE { ?document ex:keyword "foobar". }
keywordQuery.setBinding("keyword", literal(keyword));

// We then evaluate the prepared query and can process the result:
TupleQueryResult keywordQueryResult = keywordQuery.evaluate();
```

#### Tweaking the query evaluation mode

{{< tag " New in RDF4J 4.3" >}}

The SPARQL specification is by nature extensible, allowing query engines to add support for additional operators and operator functions (see [section 17.3.1 "Operator Extensibility"](https://www.w3.org/TR/sparql11-query/#operatorExtensibility)). The SPARQL query engine in RDF4J has two configurable evaluation modes that regulate this: `strict` mode, and `standard` mode.

- in `strict` mode, SPARQL queries are evaluated using the strictest form of minimal compliance to the recommendation. No additional operators have been added. 
- in `standard` mode, SPARQL queries are evaluated using a set of operator extensions that make practical sense, in a way that is still fully compliant with the W3C Recommendation. For example, in `standard` mode, comparisons and math operations between literals using calendar datatypes (`xsd:DateTime`, etc) are supported.

For historic reasons the default evaluation mode for RDF4J repositories is `strict`, but this can be changed in several ways. It's possible to set a repository's default evaluation mode to `standard` at Repository creation/configuration time. But you can also override the default behaviour per query, by wrapping the query in a transaction and using the {{< javadoc "QueryEvaluationMode" "common/transaction/QueryEvaluationMode.html" >}} transaction setting:

```java
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
...

try (RepositoryConnection conn = repo.getConnection()) {
   String queryString = "SELECT ?x ?y WHERE { ?x ?p ?y } ";

   // set query evaluation mode to STANDARD in this transaction
   conn.begin(QueryEvaluationMode.STANDARD);
   TupleQuery tupleQuery = con.prepareTupleQuery(queryString);
   try (TupleQueryResult result = tupleQuery.evaluate()) {
      for (BindingSet bindingSet: result) {
         Value valueOfX = bindingSet.getValue("x");
         Value valueOfY = bindingSet.getValue("y");
         // do something interesting with the values here...
      }
   }
   conn.commit();
}
```

#### Explaining queries

SPARQL queries are translated to query plans and then run through an optimization pipeline before they get evaluated and
the results returned. The query explain feature gives a peek into what decisions are being made and how they affect
the performance of your query.

This feature is currently released as an experimental feature, which means that it may change, be moved or even removed in the future.
Explaining queries currently only works if you are using one of the built in stores directly in your Java code.
If you are connecting to a remote RDF4J Server, using the Workbench or connecting to a third party database then you will get an
UnsupportedException.

In RDF4J 3.2.0, queries have a new method `explain(...)` that returns an `Explanation` explaining how the query will be, or has been, evaluated.

 ```java
 try (SailRepositoryConnection connection = sailRepository.getConnection()) {
    TupleQuery query = connection.prepareTupleQuery("select * where .... ");
    String explanation = query.explain(Explanation.Level.Timed).toString();
    System.out.println(explanation);
}
```

There are 5 explanation levels to choose between:

|             | Parsed | Optimized | Cost and Estimates | Fully evaluated | Real result sizes | Runtime telemetry | Performance timing |
|-------------|--------|-----------|--------------------|-----------------|-------------------|-------------------|--------------------|
| Unoptimized | ✓      |           |                    |                 |                   |                   |                    |
| Optimized   | ✓      | ✓         | ✓                  |                 |                   |                   |                    |
| Executed    | ✓      | ✓         | ✓                  | ✓               | ✓                 |                   |                    |
| Telemetry   | ✓      | ✓         | ✓                  | ✓               | ✓                 | ✓                 |                    |
| Timed       | ✓      | ✓         | ✓                  | ✓               | ✓                 |                   | ✓                  |


Use the `Telemetry` level to inspect runtime telemetry metrics per plan node. Use `Timed` when you need
time-per-node diagnostics. `Timed`, `Telemetry`, and `Executed` all fully evaluate the query and iterate
over all the result sets. Seeing as how this can be very time-consuming there is a default best-effort
timeout of 60 seconds. A different timeout can be set by changing the timeout for the query.

The lower levels `Unoptimized` and `Optimized` are useful for understanding how RDF4J reorders queries in
order to optimize them.

As an example, the following query intends to get everyone in Peter's extended friend graph who is at least 18 years old
and return their node and optionally their name.

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?friend ?name WHERE
{
	BIND(<http://example.com/peter> as ?person)
	?person a foaf:Person ;
		(foaf:knows | ^foaf:knows)* ?friend.
	?friend foaf:age ?age
	OPTIONAL {
		?friend foaf:name ?name
	}
	FILTER(?age >= 18)
}
```

For our test data the query returns the following results:

```
[ friend=http://example.com/steve; name="Steve" ]
[ friend=http://example.com/mary ]
```

Our test data also contains other people, so the query has to evaluate a lot more data than the results lead us to believe.

Explaining the query at the `Timed` level gives us the following plan:

```
01 Projection (resultSizeActual=2, totalTimeActual=0.247ms, selfTimeActual=0.002ms)
02 ╠══ProjectionElemList
03 ║     ProjectionElem "friend"
04 ║     ProjectionElem "name"
05 ╚══LeftJoin (LeftJoinIterator) (resultSizeActual=2, totalTimeActual=0.245ms, selfTimeActual=0.005ms)
06    ├──Join (JoinIterator) (resultSizeActual=2, totalTimeActual=0.238ms, selfTimeActual=0.004ms)
07    │  ╠══Extension (resultSizeActual=1, totalTimeActual=0.002ms, selfTimeActual=0.001ms)
08    │  ║  ├──ExtensionElem (person)
09    │  ║  │     ValueConstant (value=http://example.com/peter)
10    │  ║  └──SingletonSet (resultSizeActual=1, totalTimeActual=0.0ms, selfTimeActual=0.0ms)
11    │  ╚══Join (JoinIterator) (resultSizeActual=2, totalTimeActual=0.231ms, selfTimeActual=0.009ms)
12    │     ├──Filter (resultSizeActual=4, totalTimeActual=0.023ms, selfTimeActual=0.014ms)
13    │     │  ╠══Compare (>=)
14    │     │  ║     Var (name=age)
15    │     │  ║     ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
16    │     │  ╚══StatementPattern (costEstimate=4, resultSizeEstimate=12, resultSizeActual=12, totalTimeActual=0.009ms, selfTimeActual=0.009ms)
17    │     │        Var (name=friend)
18    │     │        Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
19    │     │        Var (name=age)
20    │     └──Join (JoinIterator) (resultSizeActual=2, totalTimeActual=0.199ms, selfTimeActual=0.007ms)
31    │        ╠══ArbitraryLengthPath (costEstimate=24, resultSizeEstimate=2.2K, resultSizeActual=2, totalTimeActual=0.189ms, selfTimeActual=0.189ms)
32    │        ║     Var (name=person)
33    │        ║     Union
34    │        ║     ╠══StatementPattern (resultSizeEstimate=1.0K)
35    │        ║     ║     Var (name=person)
36    │        ║     ║     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
37    │        ║     ║     Var (name=friend)
38    │        ║     ╚══StatementPattern (resultSizeEstimate=1.0K)
39    │        ║           Var (name=friend)
40    │        ║           Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
41    │        ║           Var (name=person)
42    │        ║     Var (name=friend)
43    │        ╚══StatementPattern (costEstimate=1, resultSizeEstimate=101, resultSizeActual=2, totalTimeActual=0.004ms, selfTimeActual=0.004ms)
44    │              Var (name=person)
45    │              Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
46    │              Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
47    └──StatementPattern (resultSizeEstimate=5, resultSizeActual=1, totalTimeActual=0.003ms, selfTimeActual=0.003ms)
48          Var (name=friend)
49          Var (name=_const_23b7c3b6_uri, value=http://xmlns.com/foaf/0.1/name, anonymous)
50          Var (name=name)
```

We start by reading the query top to bottom. The first node we encounter is:

```
Projection (resultSizeActual=2, totalTimeActual=0.247ms, selfTimeActual=0.002ms)
```

The node name is "Projection", which represents the `SELECT` keyword. The values in parentheses
are cost-estimates and actual measured output and timing. You may encounter:

 - **costEstimate**: an internal value that represents the cost for executing this node and is used for ordering the nodes
 - **resultSizeEstimate**: the cardinality estimate of a node, essentially how many results this node would return if it were executed alone
 - **resultSizeActual**: the actual number of results that this node produced
 - **totalTimeActual**: the total time this node took to return all its results, including the time for its children
 - **selfTimeActual**: the time this node took all on its own to produce its results

 In the plan above we can see that `ArbitraryLengthPath` took most of our time by using 0.189ms (~75% of the overall time).
 This node represents the `(foaf:knows | ^foaf:knows)* ?friend` part of our query.

 Joins in RDF4J have a left, and a right node (the pipes in the query plan make it simpler to see which node is the left and which is the right).
The join algorithms will first retrieve a result from the left node before it gets a result from the right node. The left node is the first
node displayed under the join node. For the `Join` on line 06 we have the left node being line 07 and the right being line 11.`Executed`
and `Timed` plans will typically show the algorithm for all join and left join nodes. Our fastest algorithm is usually `JoinIterator`, it will
retrieve a result from the left node and use the results to "query" the right node for the next relevant result.

In our plan above we can see how `Extension` node and the `Filter` node can "inform" the `ArbitraryLengthPath` which values for
`person` and `friend` are relevant because the `Extension` node binds exactly one value for `person` and `Filter` node binds exactly
four values for`friend`. This is why `ArbitraryLengthPath` has a `resultSizeActual` of two, meaning that it only produced two results.

The query above is a very efficient and nicely behaved query. Usually the reason to explain a query is because the query is slow or takes
up a lot of memory.

The following query is a typical example of a scoping issue, which is a very common cause of slow SPARQL queries.

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT * WHERE
{
    BIND(<http://example.com/peter> as ?person)
	?person a foaf:Person .
	{
		?person	(foaf:knows | ^foaf:knows)* ?friend.
	} UNION {
		?friend foaf:age ?age.
		FILTER(?age >= 18)
	}
}
```

The issue with this query is that each of the union clauses introduces a new scope. It's quite easy to see in this example. Both unions define a new
variable `?friend`, however the results should not be the intersection of common values but rather the union between "everyone that knows or is known by someone"
and "everyone 18 or older". The only exception here is that `?person` is used in the outer scope, so results from the inner union would be filtered to match
with bindings for `?person` from the outer scope. SPARQL is designed with bottom-up semantics, which means that inner sections should be evaluated before
outer sections. This precisely so as to make scoping issues unambiguous.

The query plan for the query gives us a lot of hints about how this becomes problematic.

```
Projection (resultSizeActual=9, totalTimeActual=1.6s, selfTimeActual=0.074ms)
╠══ProjectionElemList
║     ProjectionElem "person"
║     ProjectionElem "friend"
║     ProjectionElem "age"
╚══Join (HashJoinIteration) (resultSizeActual=9, totalTimeActual=1.6s, selfTimeActual=5.45ms)
   ├──Extension (resultSizeActual=1, totalTimeActual=0.018ms, selfTimeActual=0.016ms)
   │  ╠══ExtensionElem (person)
   │  ║     ValueConstant (value=http://example.com/peter)
   │  ╚══SingletonSet (resultSizeActual=1, totalTimeActual=0.002ms, selfTimeActual=0.002ms)
   └──Union (new scope) (resultSizeActual=10.5K, totalTimeActual=1.6s, selfTimeActual=4.42ms)
      ╠══Join (HashJoinIteration) (resultSizeActual=10.1K, totalTimeActual=1.6s, selfTimeActual=50.0ms)
      ║  ├──StatementPattern (costEstimate=34, resultSizeEstimate=101, resultSizeActual=101, totalTimeActual=0.524ms, selfTimeActual=0.524ms)
      ║  │     Var (name=person)
      ║  │     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
      ║  │     Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
      ║  └──ArbitraryLengthPath (new scope) (costEstimate=47, resultSizeEstimate=2.2K, resultSizeActual=102.0K, totalTimeActual=1.5s, selfTimeActual=1.5s)
      ║        Var (name=person)
      ║        Union
      ║        ├──StatementPattern (resultSizeEstimate=1.0K)
      ║        │     Var (name=person)
      ║        │     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
      ║        │     Var (name=friend)
      ║        └──StatementPattern (resultSizeEstimate=1.0K)
      ║              Var (name=friend)
      ║              Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
      ║              Var (name=person)
      ║        Var (name=friend)
      ╚══Join (JoinIterator) (resultSizeActual=404, totalTimeActual=0.533ms, selfTimeActual=0.145ms)
         ├──Filter (new scope) (costEstimate=12, resultSizeEstimate=12, resultSizeActual=4, totalTimeActual=0.087ms, selfTimeActual=0.073ms)
         │  ╠══Compare (>=)
         │  ║     Var (name=age)
         │  ║     ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
         │  ╚══StatementPattern (resultSizeEstimate=12, resultSizeActual=12, totalTimeActual=0.014ms, selfTimeActual=0.014ms)
         │        Var (name=friend)
         │        Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
         │        Var (name=age)
         └──StatementPattern (costEstimate=101, resultSizeEstimate=101, resultSizeActual=404, totalTimeActual=0.301ms, selfTimeActual=0.301ms)
               Var (name=person)
               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
               Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
```

The biggest time use and largest result size is produced at line:

```
ArbitraryLengthPath (new scope) (costEstimate=47, resultSizeEstimate=2.2K, resultSizeActual=102.0K, totalTimeActual=1.5s, selfTimeActual=1.5s)
```

This tells us that the query is probably producing all possible results for `?person (foaf:knows | ^foaf:knows)* ?friend.`. In fact running this fragment in a new query
shows that it produces ~102,000 results.

Taking a look at the unoptimized plan we can see where the issue lies:

```
01 Projection
02 ╠══ProjectionElemList
03 ║     ProjectionElem "person"
04 ║     ProjectionElem "friend"
05 ║     ProjectionElem "age"
06 ╚══Join
07    ├──Join
08    │  ╠══Extension
09    │  ║  ├──ExtensionElem (person)
10    │  ║  │     ValueConstant (value=http://example.com/peter)
11    │  ║  └──SingletonSet
12    │  ╚══StatementPattern
13    │        Var (name=person)
14    │        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
15    │        Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
16    └──Union (new scope)
17       ╠══ArbitraryLengthPath (new scope)
18       ║     Var (name=person)
19       ║     Union
20       ║     ╠══StatementPattern
31       ║     ║     Var (name=person)
32       ║     ║     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
33       ║     ║     Var (name=friend)
34       ║     ╚══StatementPattern
35       ║           Var (name=friend)
36       ║           Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
37       ║           Var (name=person)
38       ║     Var (name=friend)
39       ╚══Filter (new scope)
40          ├──Compare (>=)
41          │     Var (name=age)
42          │     ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
43          └──StatementPattern
44                Var (name=friend)
45                Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
46                Var (name=age)
```

The problem is that the `Union` on line 16 introduces a new scope. This means that the `Join` above it (line 6) can't push its binding for `?person` into the `Union`.
This is the reason that the execution of the query was done with the `HashJoinIteration` rather than with the `JoinIterator`.

One way to solve this issue is to copy the `BIND` into all relevant unions.

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?friend ?name WHERE
{
    BIND(<http://example.com/peter> as ?person)
	?person a foaf:Person .
	{
	    BIND(<http://example.com/peter> as ?person)
		?person	(foaf:knows | ^foaf:knows)* ?friend.
	} UNION {
		?friend foaf:age ?age.
		FILTER(?age >= 18)
	}
}
```

This forces the inner union to only consider ex:peter as `?person` meaning we only need to find his friends and not everyone elses friends.
The query plan also agrees that this is better.

```
Projection (resultSizeActual=9, totalTimeActual=0.448ms, selfTimeActual=0.007ms)
╠══ProjectionElemList
║     ProjectionElem "person"
║     ProjectionElem "friend"
║     ProjectionElem "age"
╚══Union (new scope) (resultSizeActual=9, totalTimeActual=0.441ms, selfTimeActual=0.03ms)
   ├──Join (JoinIterator) (resultSizeActual=5, totalTimeActual=0.354ms, selfTimeActual=0.024ms)
   │  ╠══Join (JoinIterator) (resultSizeActual=1, totalTimeActual=0.042ms, selfTimeActual=0.002ms)
   │  ║  ├──Extension (resultSizeActual=1, totalTimeActual=0.037ms, selfTimeActual=0.03ms)
   │  ║  │  ╠══ExtensionElem (person)
   │  ║  │  ║     ValueConstant (value=http://example.com/peter)
   │  ║  │  ╚══SingletonSet (resultSizeActual=1, totalTimeActual=0.007ms, selfTimeActual=0.007ms)
   │  ║  └──Extension (resultSizeActual=1, totalTimeActual=0.003ms, selfTimeActual=0.002ms)
   │  ║     ╠══ExtensionElem (person)
   │  ║     ║     ValueConstant (value=http://example.com/peter)
   │  ║     ╚══SingletonSet (resultSizeActual=1, totalTimeActual=0.001ms, selfTimeActual=0.001ms)
   │  ╚══Join (JoinIterator) (resultSizeActual=5, totalTimeActual=0.289ms, selfTimeActual=0.112ms)
   │     ├──StatementPattern (costEstimate=34, resultSizeEstimate=101, resultSizeActual=1, totalTimeActual=0.013ms, selfTimeActual=0.013ms)
   │     │     Var (name=person)
   │     │     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │     │     Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
   │     └──ArbitraryLengthPath (costEstimate=47, resultSizeEstimate=2.2K, resultSizeActual=5, totalTimeActual=0.164ms, selfTimeActual=0.164ms)
   │           Var (name=person)
   │           Union
   │           ├──StatementPattern (resultSizeEstimate=1.0K)
   │           │     Var (name=person)
   │           │     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
   │           │     Var (name=friend)
   │           └──StatementPattern (resultSizeEstimate=1.0K)
   │                 Var (name=friend)
   │                 Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
   │                 Var (name=person)
   │           Var (name=friend)
   └──Join (JoinIterator) (resultSizeActual=4, totalTimeActual=0.057ms, selfTimeActual=0.005ms)
      ╠══Extension (resultSizeActual=1, totalTimeActual=0.002ms, selfTimeActual=0.001ms)
      ║  ├──ExtensionElem (person)
      ║  │     ValueConstant (value=http://example.com/peter)
      ║  └──SingletonSet (resultSizeActual=1, totalTimeActual=0.001ms, selfTimeActual=0.001ms)
      ╚══Join (JoinIterator) (resultSizeActual=4, totalTimeActual=0.05ms, selfTimeActual=0.011ms)
         ├──Filter (new scope) (costEstimate=12, resultSizeEstimate=12, resultSizeActual=4, totalTimeActual=0.031ms, selfTimeActual=0.021ms)
         │  ╠══Compare (>=)
         │  ║     Var (name=age)
         │  ║     ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
         │  ╚══StatementPattern (resultSizeEstimate=12, resultSizeActual=12, totalTimeActual=0.009ms, selfTimeActual=0.009ms)
         │        Var (name=friend)
         │        Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
         │        Var (name=age)
         └──StatementPattern (costEstimate=101, resultSizeEstimate=101, resultSizeActual=4, totalTimeActual=0.008ms, selfTimeActual=0.008ms)
               Var (name=person)
               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
               Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
```

Notice that `ArbitraryLengthPath` produces 5 results and that the entire query runs in 0.164ms instead of 1.5s.

Another way to visualize the query plan is to use the Graphiz DOT format with `query.explain(Explanation.Level.Timed).toDot()`.
This visualization makes it easier to see which part of the query is slowest by looking at the color coding.

<img src="../images/query-plan-explanation.png" alt="Picture of query explanation visualized with Graphviz." class="img-responsive"/>

[Image produced by Dreampuf GraphvizOnline](https://dreampuf.github.io/GraphvizOnline/#digraph%20Explanation%20%7B%0A%20%20%20UUID_beb1d25d33bd4dcc9e6b42e6fd85fa2b%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCOLOR%3D%22%23FF0000%22%3E%3CU%3EProjection%3C%2FU%3E%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%20%3EResult%20size%20actual%3C%2Ftd%3E%3Ctd%3E9%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%20%3ETotal%20time%20actual%3C%2Ftd%3E%3Ctd%20BGCOLOR%3D%22%23FF0000%22%3E1.47ms%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%20%3ESelf%20time%20actual%3C%2Ftd%3E%3Ctd%20BGCOLOR%3D%22%23FFE9E9%22%3E0.038ms%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E%3E%20shape%3Dplaintext%5D%3B%0A%20%20%20UUID_beb1d25d33bd4dcc9e6b42e6fd85fa2b%20-%3E%20UUID_277b9dba1bb44f4b87f15fdfa32de472%20%5Blabel%3D%22left%22%5D%20%3B%0A%20%20%20UUID_beb1d25d33bd4dcc9e6b42e6fd85fa2b%20-%3E%20UUID_e3dd648f8f684d4fbfd4b7f7a6960947%20%5Blabel%3D%22right%22%5D%20%3B%0A%20%20%20UUID_277b9dba1bb44f4b87f15fdfa32de472%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCOLOR%3D%22%23FFFFFF%22%3E%3CU%3EProjectionElemList%3C%2FU%3E%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E%3E%20shape%3Dplaintext%5D%3B%0A%20%20%20UUID_277b9dba1bb44f4b87f15fdfa32de472%20-%3E%20UUID_3c67ab803c074c60bd1a0c8a0a070a67%20%5Blabel%3D%22index%200%22%5D%20%3B%0A%20%20%20UUID_277b9dba1bb44f4b87f15fdfa32de472%20-%3E%20UUID_c55be31929a046e1acaeee2cd0aebf7b%20%5Blabel%3D%22index%201%22%5D%20%3B%0A%20%20%20UUID_277b9dba1bb44f4b87f15fdfa32de472%20-%3E%20UUID_fb810ed934504076840e795c5fd49493%20%5Blabel%3D%22index%202%22%5D%20%3B%0A%20%20%20UUID_3c67ab803c074c60bd1a0c8a0a070a67%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCOLOR%3D%22%23FFFFFF%22%3E%3CU%3EProjectionElem%20%26quot%3Bperson%26quot%3B%3C%2FU%3E%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E%3E%20shape%3Dplaintext%5D%3B%0A%20%20%20UUID_c55be31929a046e1acaeee2cd0aebf7b%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCOLOR%3D%22%23FFFFFF%22%3E%3CU%3EProjectionElem%20%26quot%3Bfriend%26quot%3B%3C%2FU%3E%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E%3E%20shape%3Dplaintext%5D%3B%0A%20%20%20UUID_fb810ed934504076840e795c5fd49493%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCOLOR%3D%22%23FFFFFF%22%3E%3CU%3EProjectionElem%20%26quot%3Bage%26quot%3B%3C%2FU%3E%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E%3E%20shape%3Dplaintext%5D%3B%0A%20%20%20subgraph%20cluster_UUID_e3dd648f8f684d4fbfd4b7f7a6960947%20%7B%0A%20%20%20color%3Dgrey%0AUUID_e3dd648f8f684d4fbfd4b7f7a6960947%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCOLOR%3D%22%23FF0606%22%3E%3CU%3EUnion%3C%2FU%3E%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%3E%3CB%3ENew%20scope%3C%2FB%3E%3C%2Ftd%3E%3Ctd%3E%3CB%3Etrue%3C%2FB%3E%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%20%3EResult%20size%20actual%3C%2Ftd%3E%3Ctd%3E9%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%20%3ETotal%20time%20actual%3C%2Ftd%3E%3Ctd%20BGCOLOR%3D%22%23FF0606%22%3E1.44ms%3C%2Ftd%3E%3C%2Ftr%3E%20%3Ctr%3E%3Ctd%20%3ESelf%20time%20actual%3C%2Ftd%3E%3Ctd%20BGCOLOR%3D%22%23FFAEAE%22%3E0.14ms%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E%3E%20shape%3Dplaintext%5D%3B%0A%20%20%20UUID_e3dd648f8f684d4fbfd4b7f7a6960947%20-%3E%20UUID_460b578e6230407f986cd444b91ab796%20%5Blabel%3D%22left%22%5D%20%3B%0A%20%20%20UUID_e3dd648f8f684d4fbfd4b7f7a6960947%20-%3E%20UUID_b603ce18210d4c059f2aa6302341fb39%20%5Blabel%3D%22right%22%5D%20%3B%0A%20%20%20UUID_460b578e6230407f986cd444b91ab796%20%5Blabel%3D%3C%3Ctable%20BORDER%3D%220%22%20CELLBORDER%3D%221%22%20CELLSPACING%3D%220%22%20CELLPADDING%3D%223%22%20%3E%3Ctr%3E%3Ctd%20COLSPAN%3D%222%22%20BGCO)

If you want to practice with these examples, the code below produces these three plans.

```java
public class QueryExplainExample {

	public static void main(String[] args) {

		SailRepository sailRepository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			ValueFactory vf = connection.getValueFactory();
			String ex = "http://example.com/";

			IRI peter = vf.createIRI(ex, "peter");
			IRI steve = vf.createIRI(ex, "steve");
			IRI mary = vf.createIRI(ex, "mary");
			IRI patricia = vf.createIRI(ex, "patricia");
			IRI linda = vf.createIRI(ex, "linda");

			connection.add(peter, RDF.TYPE, FOAF.PERSON);

			connection.add(peter, FOAF.KNOWS, patricia);
			connection.add(patricia, FOAF.KNOWS, linda);
			connection.add(patricia, FOAF.KNOWS, steve);
			connection.add(mary, FOAF.KNOWS, linda);

			connection.add(steve, FOAF.AGE, vf.createLiteral(18));
			connection.add(mary, FOAF.AGE, vf.createLiteral(18));

			connection.add(steve, FOAF.NAME, vf.createLiteral("Steve"));

			// Add some dummy data
			for (int i = 0; i < 100; i++) {
				connection.add(vf.createBNode(i + ""), RDF.TYPE, FOAF.PERSON);
			}

			for (int i = 0; i < 1000; i++) {
				connection.add(vf.createBNode(i % 150 + ""), FOAF.KNOWS, vf.createBNode(i + 10 + ""));
			}

			for (int i = 0; i < 10; i++) {
				connection.add(vf.createBNode(i + 3 + ""), FOAF.AGE, vf.createLiteral(i + 10));
			}

			for (int i = 0; i < 4; i++) {
				connection.add(vf.createBNode(i + ""), FOAF.NAME, vf.createLiteral("name" + i));
			}

		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(String.join("\n", "",
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>",
				"SELECT ?friend ?name WHERE ",
				"{",
				"	BIND(<http://example.com/peter> as ?person)",
				"	?person a foaf:Person ;",
				"		(foaf:knows | ^foaf:knows)* ?friend.",
				"	OPTIONAL {",
				"		?friend foaf:name ?name",
				"	}",
				"	?friend foaf:age ?age",
				"	FILTER(?age >= 18) ",

				"}"));

			Explanation explain = query.explain(Explanation.Level.Timed);
			System.out.println(explain);

		}

		System.out.println("\n\n");

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(String.join("\n", "",
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>",
				"SELECT * WHERE ",
				"{",
				"  BIND(<http://example.com/peter> as ?person)",
				"	?person a foaf:Person .",
				"	{",
				"		?person	(foaf:knows | ^foaf:knows)* ?friend.",
				"	} UNION {",
				"		?friend foaf:age ?age",
				"		FILTER(?age >= 18) ",
				"	}",
				"}"));

			Explanation explainUnoptimized = query.explain(Explanation.Level.Unoptimized);
			System.out.println(explainUnoptimized);
			System.out.println("\n\n");

			Explanation explain = query.explain(Explanation.Level.Timed);
			System.out.println(explain);

		}

		System.out.println("\n\n");

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(String.join("\n", "",
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>",
				"SELECT * WHERE ",
				"{",
				"  BIND(<http://example.com/peter> as ?person)",
				"	?person a foaf:Person .",
				"	{",
				"  	BIND(<http://example.com/peter> as ?person)",
				"		?person	(foaf:knows | ^foaf:knows)* ?friend.",
				"	} UNION {",
				"		?friend foaf:age ?age",
				"		FILTER(?age >= 18) ",
				"	}",
				"}"));

			Explanation explain = query.explain(Explanation.Level.Timed);
			System.out.println(explain);
			System.out.println(explain.toDot());

		}

		sailRepository.shutDown();

	}

}
```


### Creating, retrieving, removing individual statements

The RepositoryConnection can also be used for adding, retrieving, removing or otherwise manipulating individual statements, or sets of statements.

To be able to add new statements, we can use either the {{< javadoc "Values" "model/util/Values.html" >}} factory methods or a `ValueFactory` to create the Values out of which the statements consist. For example, we want to add a few statements about two resources, Alice and Bob:

```java
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
...

// create some resources and literals to make statements out of
IRI alice = Values.iri("http://example.org/people/alice");
IRI bob = Values.iri("http://example.org/people/bob");
IRI name = Values.iri("http://example.org/ontology/name");
IRI person = Values.iri("http://example.org/ontology/Person");
Literal bobsName = Values.literal("Bob");
Literal alicesName = Values.literal("Alice");

try (RepositoryConnection con = myRepository.getConnection()) {
  // alice is a person
  conn.add(alice, RDF.TYPE, person);
  // alice's name is "Alice"
  conn.add(alice, name, alicesName);
  // bob is a person
  conn.add(bob, RDF.TYPE, person);
  // bob's name is "Bob"
  conn.add(bob, name, bobsName);
}
```

Of course, it will not always be necessary to create IRI objects. In practice, you will find that you quite often retrieve existing IRIs from the repository (for example, by evaluating a query) and then use those values to add new statements. Also, for several well-knowns vocabularies we can simply reuse the predefined constants found in the org.eclipse.rdf4j.model.vocabulary package, and using the ModelBuilder utility you can very quickly create collections of statements without ever touching a ValueFactory.

Retrieving statements works in a very similar way. One way of retrieving statements we have already seen actually: we can get a GraphQueryResult containing statements by evaluating a graph query. However, we can also use direct method calls to retrieve (sets of) statements. For example, to retrieve all statements about Alice, we could do:

```java
RepositoryResult<Statement> statements = con.getStatements(alice, null, null);
```

Similarly to the TupleQueryResult object and other types of query results, the RepositoryResult is an iterator-like object that lazily retrieves each matching statement from the repository when its next() method is called. Note that, like is the case with QueryResult objects, iterating over a RepositoryResult may result in exceptions which you should catch to make sure that the RepositoryResult is always properly closed after use:

```java
RepositoryResult<Statement> statements = con.getStatements(alice, null, null, true);
try {
   while (statements.hasNext()) {
      Statement st = statements.next();
      ... // do something with the statement
   }
}
finally {
   statements.close(); // make sure the result object is closed properly
}
```

Or alternatively, using try-with-resources and the fact that (since RDF4J 3.1.0) a RepositoryResult is an `Iterable`:

```java
try (RepositoryResult<Statement> statements = con.getStatements(alice, null, null, true)) {
   for (Statement st: statements) {
      ... // do something with the statement
   }
}
```

In the above `getStatements()` invocation, we see four parameters being passed. The first three represent the subject, predicate and object of the RDF statements which should be retrieved. A null value indicates a wildcard, so the above method call retrieves all statements which have as their subject Alice, and have any kind of predicate and object. The optional fourth parameter indicates whether or not inferred statements should be included or not (you can leave this parameter out, in which case it defaults to `true`).

Removing statements again works in a very similar fashion. Suppose we want to retract the statement that the name of Alice is “Alice”):

```java
con.remove(alice, name, alicesName);
```

Or, if we want to erase all statements about Alice completely, we can do:

```java
con.remove(alice, null, null);
```

### Using named graphs/context

RDF4J supports the notion of _context_, which you can think of as a way to group sets of statements together through a single group identifier (this identifier can be a blank node or a URI).

A very typical way to use context is tracking provenance of the statements in a repository, that is, which file these statements originate from. For example, consider an application where you add RDF data from different files to a repository, and then one of those files is updated. You would then like to replace the data from that single file in the repository, and to be able to do this you need a way to figure out which statements need to be removed. The context mechanism gives you a way to do that.

Another typical use case is to support _named graphs_: in the SPARQL query language, named graphs can be queried as subsets of the dataset over which the query is evaluated. In RDF4J, named graphs are implemented via the context mechanism. This means that if you put data in RDF4J in a context, you can query that context as a named graph in SPARQL.

We will start by showing some simple examples of using context in the API. In the following example, we add an RDF document from the Web to our repository, in a context. In the example, we make the context identifier equal to the Web location of the file being uploaded.

```java
String location = "http://example.org/example/example.rdf";
String baseURI = location;
URL url = new URL(location);
IRI context = f.createIRI(location);
conn.add(url, baseURI, RDFFormat.RDFXML, context);
```

We can now use the context mechanism to specifically address these statements in the repository for retrieve and remove operations:

```java
// Get all statements in the context
try (RepositoryResult<Statement> result = conn.getStatements(null, null, null, context)) {
   while (result.hasNext()) {
      Statement st = result.next();
      ... // do something interesting with the result
   }
}
// Export all statements in the context to System.out, in RDF/XML format
RDFHandler writer = Rio.createWriter(RDFFormat.RDFXML, System.out);
conn.export(context, writer);
// Remove all statements in the context from the repository
conn.clear(context);
```

In most methods in the Repository API, the context parameter is a vararg, meaning that you can specify an arbitrary number (zero, one, or more) of context identifiers. This way, you can combine different contexts together. For example, we can very easily retrieve statements that appear in either `context1` or `context2`.

In the following example we add information about Bob and Alice again, but this time each has their own context. We also create a new property called `creator` that has as its value the name of the person who is the creator a particular context. The knowledge about creators of contexts we do not add to any particular context, however:

```java
IRI context1 = f.createIRI("http://example.org/context1");
IRI context2 = f.createIRI("http://example.org/context2");
IRI creator = f.createIRI("http://example.org/ontology/creator");

// Add stuff about Alice to context1
conn.add(alice, RDF.TYPE, person, context1);
conn.add(alice, name, alicesName, context1);

// Alice is the creator of context1
conn.add(context1, creator, alicesName);

// Add stuff about Bob to context2
conn.add(bob, RDF.TYPE, person, context2);
conn.add(bob, name, bobsName, context2);

// Bob is the creator of context2
conn.add(context2, creator, bobsName);
```

Once we have this information in our repository, we can retrieve all statements about either Alice or Bob by using the context vararg:

```java
// Get all statements in either context1 or context2
RepositoryResult<Statement> result = con.getStatements(null, null, null, context1, context2);
```

You should observe that the above RepositoryResult will not contain the information that `context1` was created by Alice and context2 by Bob. This is because those statements were added without any context, thus they do not appear in context1 or `context2`, themselves.

To explicitly retrieve statements that do not have an associated context, we do the following:

```java
// Get all statements that do not have an associated context
RepositoryResult<Statement> result = con.getStatements(null, null, null, (Resource)null);
```

This will give us only the statements about the creators of the contexts, because those are the only statements that do not have an associated context. Note that we have to explicitly cast the null argument to Resource, because otherwise it is ambiguous whether we are specifying a single value or an entire array that is null (a vararg is internally treated as an array). Simply invoking `getStatements(s, p, o, null)` without an explicit cast will result in an `IllegalArgumentException`.

We can also get everything that either has no context or is in context1:

```java
// Get all statements that do not have an associated context, or that are in context1
RepositoryResult<Statement> result = con.getStatements(null, null, null, (Resource)null, context1);
```

So as you can see, you can freely combine contexts in this fashion.

Note:

```java
getStatements(null, null, null);
```

is not the same as:

```java
getStatements(null, null, null, (Resource)null);
```

The former (without any context id parameter) retrieves all statements in the repository, ignoring any context information. The latter, however, only retrieves statements that explicitly do not have any associated context.

### Working with Models, Collections and Iterations

Most of these examples sofar have been on the level of individual statements. However, the Repository API offers several methods that work with Java Collections of statements, allowing more batch-like update operations.

For example, in the following bit of code, we first retrieve all statements about Alice, put them in a Model (which, as we have seen in the previous sections, is an implementation of `java.util.Collection`) and then remove them:

```java
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.model.Model;

// Retrieve all statements about Alice and put them in a Model
RepositoryResult<Statement> statements = con.getStatements(alice, null, null);
Model aboutAlice = QueryResults.asModel(statements);

// Then, remove them from the repository
con.remove(aboutAlice);
```

As you can see, the `QueryResults` class provides a convenient method that takes a `CloseableIteration` (of which `RepositoryResult` is a subclass) as input, and returns the `Model` with the contents of the iterator added to it. It also automatically closes the result object for you.

In the above code, you first retrieve all statements, put them in a `Model`, and then remove them. Although this works fine, it can be done in an easier fashion, by simply supplying the resulting object directly:

```java
con.remove(con.getStatements(alice, null, null));
```

The RepositoryConnection interface has several variations of add, retrieve and remove operations. See the Javadoc for a full overview of the options.

### RDF Collections and RepositoryConnections

In the [Model API documentation](../model/) we have already seen how we can use the RDFCollections utility on top of a Model. This makes it very easy to insert any RDF Collection into your Repository - after all a Model can simply be added as follows:

```java
Model rdfList = ... ;
try (RepositoryConnection conn = repo.getConnection()) {
       conn.add(rdfList);
}
```

In addition to this the Repository API offers the `Connections` utility class, which contains some useful utility functions specifically for retrieving RDF Collections from a Repository.

For example, to retrieve all statements corresponding to an RDF Collection identified by the resource node from our Repository, we can do the following:

```java
// retrieve all statements forming our RDF Collection from the Repository and put
// them in a Model
try(RepositoryConnection conn = rep.getConnection()) {
   Model rdfList = Connections.getRDFCollection(conn, node, new LinkedHashModel());
}
```

Or instead, you can retrieve them in streaming fashion as well:

```java
try(RepositoryConnection conn = repo.getConnection()) {
    Connections.consumeRDFCollection(conn, node,
		 st -> { // ... do something with the triples forming the collection });
}
```

## Transactions

So far, we have shown individual operations on repositories: adding statements, removing them, etc. By default, each operation on a `RepositoryConnection` is immediately sent to the store and committed.

The `RepositoryConnection` interface supports a full transactional mechanism that allows one to group modification operations together and treat them as a single update: before the transaction is committed, none of the operations in the transaction has taken effect, and after, they all take effect. If something goes wrong at any point during a transaction, it can be rolled back so that the state of the repository is the same as before the transaction started.

Bundling update operations in a single transaction often also improves update performance compared to multiple smaller transactions. This may not be noticeable when adding a few thousand statements, but it can make a big difference when loading millions of statements into a repository.

We can indicate that we want to begin a transaction by using the `RepositoryConnection.begin()` method. In the following example, we use a connection to bundle two file addition operations in a single transaction:

```java
File inputFile1 = new File("/path/to/example1.rdf");
String baseURI1 = "http://example.org/example1/";
File inputFile2 = new File("/path/to/example2.rdf");
String baseURI2 = "http://example.org/example2/";

try (RepositoryConnection con = myRepository.getConnection()) {
   // start a transaction
   con.begin();
   try {
      // Add the first file
      con.add(inputFile1, baseURI1, RDFFormat.RDFXML);
      // Add the second file
      con.add(inputFile2, baseURI2, RDFFormat.RDFXML);
      // If everything went as planned, we can commit the result
      con.commit();
   }
   catch (RepositoryException e) {
      // Something went wrong during the transaction, so we roll it back
      con.rollback();
   }
}
```

In the above example, we use a transaction to add two files to the repository. Only if both files can be successfully added will the repository change. If one of the files can not be added (for example because it can not be read), then the entire transaction is cancelled and none of the files is added to the repository.

As you can see, we open a new try block after calling the begin() method (line 9 and further). The purpose of this is to catch any errors that happen during transaction execution, so that we can explicitly call `rollback()` on the transaction. If you prefer your code shorter, you can leave this out, and just do this:

```java
try (RepositoryConnection con = myRepository.getConnection()) {
   // start a transaction
   con.begin();
   // Add the first file
   con.add(inputFile1, baseURI1, RDFFormat.RDFXML);
   // Add the second file
   con.add(inputFile2, baseURI2, RDFFormat.RDFXML);
   // If everything went as planned, we can commit the result
   con.commit();
}
```

The `close()` method, which is automatically invoked by Java when the try-with resources block ends, will also ensure that an unfinished transaction is rolled back (it will also log a warning about this).
A `RepositoryConnection` only supports one active transaction at a time. You can check at any time whether a transaction is active on your connection by using the `isActive()` method. If you need concurrent transactions, you will need to use several separate RepositoryConnections.

### Transaction Isolation Levels

Any transaction operates according to a certain transaction isolation level. A transaction isolation level dictates who can 'see' the updates that are perfomed as part of the transaction while that transaction is active, as well as how concurrent transactions interact with each other.

The following transaction isolation levels are available:

- `NONE` The lowest isolation level; transactions can see their own changes, but may not be able to roll them back, and no support isolation among transactions is guaranteed. This isolation level is typically used for things like bulk data upload operations.

- `READ_UNCOMMITTED` Transactions can be rolled back, but are not necessarily isolated: concurrent transactions may be able to see other’s uncommitted data (so-called ‘dirty reads’).

- `READ_COMMITTED` In this transaction isolation level, only data from concurrent transactions that has been committed can be seen by the current transaction. However, consecutive reads within the same transaction may see different results. This isolation level is typically used for long-lived operations.

- `SNAPSHOT_READ` In addition to being `READ_COMMITTED`, query results in this isolation level will observe a consistent snapshot. Changes occurring to the data while a query is evaluated will not affect that query’s result. This isolation level is typically used in scenarios where there multiple concurrent transactions that do not conflict with each other.

- `SNAPSHOT` In addition to being `SNAPSHOT_READ`, succesful transactions in this isolation level will operate against a particular dataset snapshot. Transactions in this isolation level will either see the complete effects of other transactions (consistently throughout) or not at all. This isolation level is typically used in scenarios where a write operation depends on the result of a previous read operation.

- `SERIALIZABLE` In addition to `SNAPSHOT`, this isolation level requires that all other transactions must appear to occur either completely before or completely after a succesful serializable transaction. This isolation is typically used when multiple concurrent transactions are likely to conflict.

Which transaction isolation level is active is dependent on the actual store the action is performed upon. In addition, not all the transaction isolation levels listed above are by necessity supported by every store.

By default, both the memory store and the native store use the `SNAPSHOT_READ` transaction isolation level. In addition, both of them support the `NONE`, `READ_COMMITTED`, `SNAPSHOT`, and `SERIALIZABLE` levels.

The native and memory store use an optimistic locking scheme. This means that these stores allow multiple concurrent write operations, and set transaction locks 'optimistically', that is, they assume that no conflicts will occur. If a conflict does occur, an exception is thrown on commit, and the calling user has the option to replay the same transaction with the updated state of the store. This setup significantly reduces the risk of deadlocks, and makes a far greater degree of parallel processing possible, with the downside of having to deal with possible errors thrown to prevent inconsistencies. In cases where concurrent transactions are likely to conflict, the user is advised to use the SERIALIZABLE isolation level.

You can specify the transaction isolation level by means of an optional parameter on the begin() method. For example, to start a transaction that uses `SERIALIZABLE` isolation:

```java
try (RepositoryConnection conn = rep.getConnection()) {
    conn.begin(IsolationLevels.SERIALIZABLE);
     ....
    conn.commit();
}
```

A transaction isolation level is a sort of contract, that is, a set of guarantees of what will minimally happen while the transaction is active. A store will make a best effort to honor the guarantees of the requested isolation level. If it does not support the specific isolation level being requested, it will attempt to use a level it does support that offers minimally the same guarantees.

### Automated transaction handling

Although transactions are a convenient mechanism, having to always call `begin()` and `commit()` to explictly start and stop your transactions can be tedious. RDF4J offers a number of convenience utility functions to automate this part of transaction handling, using the `Repositories` utility class.

As an example, consider this bit of transactional code. It opens a connection, starts a transaction, adds two RDF statements, and then commits. It also makes sure that it rolls back the transaction if something went wrong, and it ensures that once we’re done, the connection is closed.

```java
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

IRI bob = iri("urn:bob");
RepositoryConnection conn = myRepository.getConnection();
try {
   conn.begin();
   conn.add(bob, RDF.TYPE, FOAF.PERSON);
   conn.add(bob, FOAF.NAME, literal("Bob"));
   conn.commit();
}
catch (RepositoryException e) {
   conn.rollback();
}
finally {
   conn.close();
}
```

That's an awful lot of code for just inserting two triples. The same thing can be achieved with far less boilerplate code, as follows:

```java
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

IRI bob = iri("urn:bob");
Repositories.consume(myRepository, conn -> {
  conn.add(bob, RDF.TYPE, FOAF.PERSON);
  conn.add(bob, RDFS.LABEL, literal("Bob"));
});
```

As you can see, using `Repositories.consume()`, we do not explicitly begin or commit a transaction. We don’t even open and close a connection explicitly – this is all handled internally. The method also ensures that the transaction is rolled back if an exception occurs.

This pattern is useful for simple transactions, however as we’ve seen above, we sometimes do need to explicitly call `begin()`, especially if we want to modify the transaction isolation level.

## Multithreaded Repository Access

The Repository API supports multithreaded access to a store: multiple concurrent threads can obtain connections to a Repository and query and performs operations on it simultaneously (though, depending on the transaction isolation level, access may occassionally block as a thread needs exclusive access).

The Repository object is thread-safe, and can be safely shared and reused across multiple threads (a good way to do this is via a RepositoryProvider).

{{< warning >}}
<strong>RepositoryConnection is not thread-safe</strong>. This means that you should not try to share a single RepositoryConnection over multiple threads. Instead, ensure that each thread obtains its own RepositoryConnection from a shared Repository object. You can use transaction isolation levels to control visibility of concurrent updates between threads.
{{</ warning >}}
