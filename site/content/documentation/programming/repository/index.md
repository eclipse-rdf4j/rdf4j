---
title: "The Repository API"
layout: "doc"
---

The Repository API is the central access point for RDF4J-compatible RDF databases (a.k.a. triplestores), as well as for SPARQL endpoints. Its purpose is to give a developer-friendly access point to RDF repositories, offering various methods for querying and updating the data, while hiding a lot of the nitty gritty details of the underlying machinery.

The interfaces for the Repository API can be found in package `org.eclipse.rdf4j.repository`. Several implementations for these interface exist in various sub-packages.

# Creating a Repository object

The first step in any action that involves repositories is to create a Repository for it.

The central interface of the Repository API is the {{< javadoc "Repository" "repository/Repository.html" >}} interface. There are several implementations available of this interface. The three main ones are:

{{< javadoc "SailRepository" "repository/sail/SailRepository.html" >}} is a Repository that operates directly on top of a {{< javadoc "Sail" "sail/Sail.html" >}} - that is, a particular database. This is the class most commonly used when accessing/creating a local RDF4J repository. SailRepository operates on a (stack of) Sail object(s) for storage and retrieval of RDF data. An important thing to remember is that the behaviour of a repository is determined by the Sail(s) that it operates on; for example, the repository will only support RDF Schema or OWL semantics if the Sail stack includes an inferencer for this.

{{< javadoc "HTTPRepository" "repository/http/HTTPRepository.html" >}} is a Repository implementation that acts as a proxy to a repository available on a remote RDF4J Server, accessible through HTTP.

{{< javadoc "SPARQLRepository" "repository/sparql/SPARQLRepository.html" >}} is a Repository implementation that acts as a proxy to any remote SPARQL endpoint (whether that endpoint is implemented using RDF4J or not).

Creating Repository objects can be done in multiple ways. We will first show an easy way to quickly create such an object ‘on the fly’. In the section about the RepositoryManager and RepositoryProvider, we show some more advanced patterns, which are particularly useful in larger applications which have to handle and share references to multiple repositories.

We will first take a look at the use of the SailRepository class in order to create and use a local repository.

## Main memory RDF Repository

One of the simplest configurations is a repository that just stores RDF data in main memory without applying any inferencing. This is also by far the fastest type of repository that can be used. The following code creates and initializes a non-inferencing main-memory repository:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
...
Repository repo = new SailRepository(new MemoryStore());
{{< / highlight >}}

The constructor of the SailRepository class accepts any object of type `Sail`, so we pass it a new main-memory store object (which is a `Sail` implementation).

The repository that is created by the above code is volatile: its contents are lost when the object is garbage collected or when your Java program is shut down. This is fine for cases where, for example, the repository is used as a means for manipulating an RDF model in memory.

Different types of Sail objects take parameters in their constructor that change their behaviour. The `MemoryStore` takes a `datadir` parameter that specifies a data directory for persisent storage. If specified, the MemoryStore will write its contents to this directory so that it can restore it when it is re-initialized in a future session:

{{< highlight java  >}}
File dataDir = new File("C:\\temp\\myRepository\\");
Repository repo = new SailRepository( new MemoryStore(dataDir) );
{{< / highlight >}}

We can fine-tune the configuration of our repository by passing parameters to the constructor of the Sail object. Some Sail types may offer additional configuration methods, all of which need to be called before the repository is initialized. The MemoryStore currently has one such method: `setSyncDelay(long)`, which can be used to control the strategy that is used for writing to the data file. For example:

{{< highlight java  >}}
File dataDir = new File("C:\\temp\\myRepository\\");
MemoryStore memStore = new MemoryStore(dataDir);
memStore.setSyncDelay(1000L);
Repository repo = new SailRepository(memStore);
{{< / highlight >}}

## Native RDF Repository

A Native RDF Repository does not keep its data in main memory, but instead stores it directly to disk (in a binary format optimized for compact storage and fast retrieval). It is an efficient, scalable and fast solution for RDF storage of datasets that are too large to keep entirely in memory.

The code for creation of a Native RDF repository is almost identical to that of a main memory repository:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
File dataDir = new File("/path/to/datadir/");
Repository repo = new SailRepository(new NativeStore(dataDir));
{{< / highlight >}}

By default, the Native store creates a set of two indexes. To configure which indexes it should create, we can either use the `NativeStore.setTripleIndexes(String)` method, or we can directly supply a index configuration string to the constructor:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
File dataDir = new File("/path/to/datadir/");
String indexes = "spoc,posc,cosp";
Repository repo = new SailRepository(new NativeStore(dataDir, indexes));
{{< / highlight >}}

## Elasticserch RDF Repository

> Experimental! New in RDF4J 3.1

The ElasticsearchStore stores RDF data in Elasticsearch. Not to be confused with the ElasticsearchSail which uses Elasticsearch for enhanced search. 

The ElasticsearchStore is experimental and future releases may be incompatible with the current version. Write-ahead-logging is not supported. 
This means that a write operation can appear to have partially succeeded if the ElasticsearchStore looses its connection to Elasticsearch during a commit. 

Transaction isolation is not as strong as for the other stores. The highest supported level is READ_COMMITTED, and even this 
level is only guaranteed when all other transactions also use READ_COMMITTED.

Performance for the NativeStore is in most cases considerably better than for the ElasticsearchStore. 
The read cache in the ElasticsearchStore makes workloads with repetitive reads fast. Storing small, infrequently updated, datasets such as a 
reference library or an ontology is a good usecase for the ElasticsearchStore.    

The code for creation of an ElasticsearchStore is almost identical to other repositories:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
...
// ElasticsearchStore(String hostname, int port, String clusterName, String index)
Repository repo = new SailRepository(new ElasticsearchStore("localhost", 9300, "elasticsearch", "rdf4j_index"));
{{< / highlight >}}

Remember to call `repo.shutdown()` when you are done with your ElasticsearchStore. This will close the underlying Elasticsearch Client.

## RDF Schema inferencing

As we have seen, we can create Repository objects for any kind of back-end store by passing them a reference to the appropriate Sail object. We can pass any stack of Sails this way, allowing all kinds of repository configurations to be created quite easily. For example, to stack an RDF Schema inferencer on top of a memory store, we simply create a repository like so:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
...
Repository repo = new SailRepository(
			  new SchemaCachingRDFSInferencer(
			  new MemoryStore()));
{{< / highlight >}}

Each layer in the Sail stack is created by a constructor that takes the underlying Sail as a parameter. Finally, we create the SailRepository object as a functional wrapper around the Sail stack.

The {{< javadoc "SchemaCachingRDFSInferencer" "sail/inferencer/fc/SchemaCachingInferencer.html" >}} that is used in this example is a generic RDF Schema inferencer; it can be used on top of any Sail that supports the methods it requires. Both MemoryStore and NativeStore support these methods. However, a word of warning: the RDF4J inferencers add a significant performance overhead when adding and removing data to a repository, an overhead that gets progressively worse as the total size of the repository increases. For small to medium-sized datasets it peforms fine, but for larger datasets you are advised not to use it and to switch to alternatives.

## Custom Inferencing 

The previous section showed how to use the built-in RDF schema inferencer. This section will show how to create a repository capable of performing inferences according to a custom rule that you provide.

{{< highlight java  >}}
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
{{< / highlight >}}

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

## Server-side repository

Working with remote repositories is just as easy as working with local ones. We use a different Repository object, the `HTTPRepository`, instead of the SailRepository class.

A requirement is that there is a RDF4J Server running on some remote system, which is accessible over HTTP. For example, suppose that at `http://example.org/rdf4j-server/` a RDF4J Server is running, which has a repository with the identification `example-db`. We can access this repository in our code as follows:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
...
String rdf4jServer = "http://example.org/rdf4j-server/";
String repositoryID = "example-db";
Repository repo = new HTTPRepository(rdf4jServer, repositoryID);
{{< / highlight >}}

Note: some OpenJDK 8 JVMs have a ScheduledThreadPoolExecutor bug, causing high processor load even when idling. Setting the property `-Dorg.eclipse.rdf4j.client.executors.jdkbug` will use 1 core thread (instead of 0) for clients to remediate this.  

## Accessing a SPARQL endpoint

We can use the Repository interface to access any SPARQL endpoint as well. This is done as follows:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
...
String sparqlEndpoint = "http://example.org/sparql";
Repository repo = new SPARQLRepository(sparqlEndpoint);
{{< / highlight >}}

After you have done this, you can query the SPARQL endpoint just as you would any other type of Repository.

## The RepositoryManager and RepositoryProvider

Using what we’ve seen in the previous section, we can create and use various different types of repositories. However, when developing an application in which you have to keep track of several repositories, sharing references to these repositories between different parts of your code can become complex. Ideal would be one central location where all information on the repositories in use (including id, type, directory for persistent data storage, etc.) is kept. This is the role of the {{< javadoc "RepositoryManager" "repository/manager/RepositoryManager.html" >}} and {{< javadoc "RepositoryProvider" "repository/manager/RepositoryProvider.html" >}}.

Using the RepositoryManager for handling repository creation and administration offers a number of advantages, including:

- a single RepositoryManager object can be more easily shared throughout your application than a host of static references to individual repositories;
- you can more easily create and manage repositories ‘on-the-fly’, for example if your application requires creation of new repositories on user input;
- the RepositoryManager stores your configuration, including all repository data, in one central spot on the file system.

The RepositoryManager comes in two flavours: the LocalRepositoryManager and the RemoteRepositoryManager.

A LocalRepositoryManager manages repository handling for you locally, and is always created using a (local) directory. This directory is where all repositories handled by the manager store their data, and also where the LocalRepositoryManager itself stores its configuration data.

You create a new LocalRepositoryManager as follows:

{{< highlight java  >}}
import java.io.File;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
File baseDir = new File("/path/to/storage/dir/");
LocalRepositoryManager manager = new LocalRepositoryManager(baseDir);
manager.init();
{{< / highlight >}}

To use a LocalRepositoryManager to create and manage repositories is slightly different from what we’ve seen before about creating repositories. The LocalRepositoryManager works by providing it with {{< javadoc "RepositoryConfig" "repository/config/RepositoryConfig.html" >}} objects, which are declarative specifications of the repository you want. You add a RepositoryConfig object for your new repository, and then request the actual Repository back from the LocalRepositoryManager:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

String repositoryId = "test-db";
RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
manager.addRepositoryConfig(repConfig);

Repository repository = manager.getRepository(repositoryId);
{{< / highlight >}}

In the above bit of code, you may have noticed that we provide an innocuous-looking variable called repositoryTypeSpec to the constructor of our RepositoryConfig. This variable is an instance of a class called `RepositoryImplConfig`, and this specifies the actual configuration of our new repository: what backends to use, whether or not to use inferencing, and so on.

Creating a RepositoryImplConfig object can be done in two ways: programmatically, or by reading a (RDF) config file. Here, we will show the programmatic way.

{{< highlight java  >}}
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;

// create a configuration for the SAIL stack
SailImplConfig backendConfig = new MemoryStoreConfig();

// create a configuration for the repository implementation
RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
{{< / highlight >}}

As you can see, we use a class called `MemoryStoreConfig` for specifying the type of storage backend we want. This class resides in a config sub-package of the memory store package (`org.eclipse.rdf4j.sail.memory.config`). Each particular type of SAIL in RDF4J has such a config class.

As a second example, we create a slightly more complex type of store: still in-memory, but this time we want it to use the memory store’s persistence option, and we also want to add RDFS inferencing. In RDF4J, RDFS inferencing is provided by a separate SAIL implementation, which can be ‘stacked’ on top of another SAIL. We follow that pattern in the creation of our config object:

{{< highlight java  >}}
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;

// create a configuration for the SAIL stack
boolean persist = true;
SailImplConfig backendConfig = new MemoryStoreConfig(persist);

// stack an inferencer config on top of our backend-config
backendConfig = new SchemaCachingRDFSInferencerConfig(backendConfig);

// create a configuration for the repository implementation
SailRepositoryConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
{{< / highlight >}}

### The RemoteRepositoryManager

A useful feature of RDF4J is that most its APIs are transparent with respect to whether you are working locally or remote. This is the case for the RDF4J repositories, but also for the RepositoryManager. In the above examples, we have used a LocalRepositoryManager, creating repositories for local use. However, it is also possible to use a RemoteRepositoryManager, using it to create and manage repositories residing on a remotely running RDF4J Server.

A RemoteRepositoryManager is initialized as follows:

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;

// URL of the remote RDF4J Server we want to access
String serverUrl = "http://localhost:8080/rdf4j-server";
RemoteRepositoryManager manager = new RemoteRepositoryManager(serverUrl);
manager.init();
{{< / highlight >}}

Once initialized, the RemoteRepositoryManager can be used in the same fashion as the LocalRepositoryManager: creating new repositories, requesting references to existing repositories, and so on.

### The RepositoryProvider

Finally, RDF4J also includes a `RepositoryProvider` class. This is a utility class that holds static references to RepositoryManagers, making it easy to share Managers (and the repositories they contain) across your application. In addition, the RepositoryProvider also has a built-in shutdown hook, which makes sure all repositories managed by it are shut down when the JVM exits.

To obtain a RepositoryManager from a RepositoryProvider you invoke it with the location you want a RepositoryManager for. If you provide a HTTP url, it will automatically return a RemoteRepositoryManager, and if you provide a local file URL, it will be a LocalRepositoryManager.

{{< highlight java  >}}
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
String url = "http://localhost:8080/rdf4j-server";
RepositoryManager manager  = RepositoryProvider.getRepositoryManager(url);
{{< / highlight >}}

The RepositoryProvider creates and keeps a singleton instance of RepositoryManager for each distinct location you specify, which means that you invoke the above call in several places in your code without having to worry about creating duplicate manager objects.

## Creating a Federation

It is possible to create a virtual repository that is a federation of existing repositories. The following code illustrates how to use the RepositoryManagerFederator class to create a federation. It assumes you already have a reference to a RepositoryManager instance, and is a simplified form of what the RDF4J Console runs when its federate command is invoked:

{{< highlight java  >}}
void federate(RepositoryManager manager, String fedID, String description,
	Collection<String> memberIDs, boolean readonly, boolean distinct)
	throws MalformedURLException, RDF4JException {
    if (manager.hasRepositoryConfig(fedID)) {
	System.err.println(fedID + " already exists.");
    }
    else if (validateMembers(manager, readonly, memberIDs)) {
	RepositoryManagerFederator rmf =
	    new RepositoryManagerFederator(manager);
	rmf.addFed(fedID, description, memberIDs, readonly, distinct);
	System.out.writeln("Federation created.");
    }
}
boolean validateMembers(RepositoryManager manager, boolean readonly,
	 Collection<String> memberIDs)
	 throws RDF4JException {
    boolean result = true;
    for (String memberID : memberIDs) {
	if (manager.hasRepositoryConfig(memberID)) {
	    if (!readonly) {
		if (!manager.getRepository(memberID).isWritable()) {
		    result = false;
		    System.err.println(memberID + " is read-only.");
		}
	    }
	}
	else {
	   result = false;
	   System.err.println(memberID + " does not exist.");
	}
    }
    return result;
}
{{< / highlight >}}

# Using a repository: RepositoryConnections

Now that we have created a Repository, we want to do something with it. In RDF4J, this is achieved through `RepositoryConnection` objects, which can be created by the Repository.

A {{< javadoc "RepositoryConnection" "repository/RepositoryConnection.html" >}} represents a connection to the actual store. We can issue operations over this connection, and close it when we are done to make sure we are not keeping resources unnnecessarily occupied.

In the following sections, we will show some examples of basic operations.

## Adding RDF to a repository

The Repository API offers various methods for adding data to a repository. Data can be added by specifying the location of a file that contains RDF data, and statements can be added individually or in collections.

We perform operations on a repository by requesting a `RepositoryConnection` from the repository. On this RepositoryConnection object we can perform various operations, such as query evaluation, getting, adding, or removing statements, etc.

The following example code adds two files, one local and one available through HTTP, to a repository:

{{< highlight java  >}}
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
{{< / highlight >}}

As you can see, the above code does very explicit exception handling and makes sure resources are properly closed when we are done. A lot of this can be simplified. `RepositoryConnection` implements `AutoCloseable`, so a first simple change is to use a try-with-resources construction for handling proper opening and closing of the `RepositoryConnection`:

{{< highlight java >}}
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
{{< / highlight >}}

More information on other available methods can be found in the {{< javadoc "RepositoryConnection" "repository/RepositoryConnection.html" >}} javadoc.

## Querying a repository

The Repository API has a number of methods for creating and evaluating queries. Three types of queries are distinguished: tuple queries, graph queries and boolean queries. The query types differ in the type of results that they produce.

The result of a tuple query is a set of tuples (or variable bindings), where each tuple represents a solution of a query. This type of query is commonly used to get specific values (URIs, blank nodes, literals) from the stored RDF data. SPARQL SELECT queries are tuple queries.

The result of graph queries is an RDF graph (or set of statements). This type of query is very useful for extracting sub-graphs from the stored RDF data, which can then be queried further, serialized to an RDF document, etc. SPARQL CONSTRUCT and DESCRIBE queries are graph queries.

The result of boolean queries is a simple boolean value, i.e. true or false. This type of query can be used to check if a repository contains specific information. SPARQL ASK queries are boolean queries.

### SELECT: tuple queries

To evaluate a (SELECT) tuple query we can do the following:

{{< highlight java >}}
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
      while (result.hasNext()) {  // iterate over the result
         BindingSet bindingSet = result.next();
         Value valueOfX = bindingSet.getValue("x");
         Value valueOfY = bindingSet.getValue("y");
         // do something interesting with the values here...
      }
   }
}
{{< / highlight >}}

This evaluates a SPARQL SELECT query and returns a TupleQueryResult, which consists of a sequence of BindingSet objects. Each BindingSet contains a set of Binding objects. A binding is pair relating a variable name (as used in the query’s SELECT clause) with a value.




As you can see, we use the TupleQueryResult to iterate over all results and get each individual result for x and y. We retrieve values by name rather than by an index. The names used should be the names of variables as specified in your query (note that we leave out the ‘?’ or ‘$’ prefixes used in SPARQL). The TupleQueryResult.getBindingNames() method returns a list of binding names, in the order in which they were specified in the query. To process the bindings in each binding set in the order specified by the projection, you can do the following:

{{< highlight java  >}}
List<String> bindingNames = result.getBindingNames();
while (result.hasNext()) {
   BindingSet bindingSet = result.next();
   Value firstValue = bindingSet.getValue(bindingNames.get(0));
   Value secondValue = bindingSet.getValue(bindingNames.get(1));
   // do something interesting with the values here...
}
{{< / highlight >}}

Finally, it is important to make sure that both the TupleQueryResult and the RepositoryConnection are properly closed after we are done with them. A TupleQueryResult evaluates lazily and keeps resources (such as connections to the underlying database) open. Closing the TupleQueryResult frees up these resources. You can either expliclty invoke close() in the finally clause, or use a try-with-resources construction (as shown in the above examples) to let Java itself handle proper closing for you. In the following code examples, we will use both ways to handle both result and connection closure interchangeably.

As said: a TupleQueryResult evaluates lazily, and keeps an open connection to the data source while being processed. If you wish to quickly materialize the full query result (for example, convert it to a Java List) and then close the TupleQueryResult, you can do something like this:

{{< highlight java  >}}
List<BindingSet> resultList;
try (TupleQueryResult result = tupleQuery.evaluate()) {
   resultList = QueryResults.asList(result);
}
{{< / highlight >}}

> New in RDF4J 3.1.0

Since RDF4J 3.1.0, query results from a RepositoryConnection also implement `java.lang.Iterable`. This means that it is now possible to iterate over a query result directly, without first materializing the result, as follows:

{{< highlight java  >}}
List<BindingSet> resultList;
try (TupleQueryResult result = tupleQuery.evaluate()) {
  for (BindingSet bindingSet: result) {
     Value firstValue = bindingSet.getValue(bindingNames.get(0));
     Value secondValue = bindingSet.getValue(bindingNames.get(1));
     // do something interesting with the values here...
  }
}
{{< / highlight >}}

### A tuple query in a single line of code: the Repositories utility

RDF4J provides a convenience utility class `org.eclipse.rdf4j.repository.util.Repositories`, which allows us to significantly shorten our boilerplate code. In particular, the `Repositories` utility allows us to do away with opening/closing a RepositoryConnection completely. For example, to open a connection, create and evaluate a SPARQL SELECT query, and then put that query’s result in a list, we can do the following:

{{< highlight java  >}}
List<BindingSet> results = Repositories.tupleQuery(rep, "SELECT * WHERE {?s ?p ?o }", r -> QueryResults.asList(r));
{{< / highlight >}}

We make use of so-called Lambda expressions to process the result. In this particular example, the only processing we do is to convert the `TupleQueryResult` object into a `List`. However, you can supply any kind of function to this interface to fully customize the processing that you do on the result.

### Using TupleQueryResultHandlers

You can also directly process the query result by supplying a `TupleQueryResultHandler` to the query’s `evaluate()` method. The main difference is that when using a return object, the caller has control over when the next answer is retrieved (namely, whenever `next()` is called), whereas with the use of a handler, the connection pushes answers to the handler object as soon as it has them available.

As an example we will use `SPARQLResultsCSVWriter` to directly write the query result to the console. SPARQLResultsCSVWriter is a TupleQueryResultHandler implementation that writes SPARQL Results as comma-separated values.

{{< highlight java  >}}
String queryString = "SELECT * WHERE {?x ?p ?y }";
con.prepareTupleQuery(queryString).evaluate(new SPARQLResultsCSVWriter(System.out));
{{< / highlight >}}

RDF4J provides a number of standard implementations of TupleQueryResultHandler, and of course you can also supply your own application-specific implementation. Have a look in the Javadoc for more details.

### CONSTRUCT/DESCRIBE: graph queries

The following code evaluates a graph query on a repository:

{{< highlight java  >}}
import org.eclipse.rdf4j.query.GraphQueryResult;
GraphQueryResult graphResult = con.prepareGraphQuery("CONSTRUCT { ?s ?p ?o } WHERE {?s ?p ?o }").evaluate();
{{< / highlight >}}

A GraphQueryResult is similar to TupleQueryResult in that is an object that iterates over the query solutions. However, for graph queries the query solutions are RDF statements, so a GraphQueryResult iterates over Statement objects:

{{< highlight java  >}}
for (Statement st: graphResult) {
   // ... do something with the resulting statement here.
}
{{< / highlight >}}

You can also quickly turn a GraphQueryResult into a Model (that is, a Java Collection of statements), by using the org.eclipse.rdf4j.query.QueryResults utility class:

{{< highlight java  >}}
Model resultModel = QueryResults.asModel(graphQueryResult);
{{< / highlight >}}

### Doing a graph query in a single line of code

Similarly to how we do this with SELECT queries, we can use the Repositories utility to obtain a result from a SPARQL CONSTRUCT (or DESCRIBE) query in a single line of Java code:

{{< highlight java  >}}
Model m = Repositories.graphQuery(rep, "CONSTRUCT WHERE {?s ?p ?o}", r -> QueryResults.asModel(r));
{{< / highlight >}}

### Using RDFHandlers

For graph queries, we can supply an `org.eclipse.rdf4j.rio.RDFHandler` to the `evaluate()` method. Again, this is a generic interface, each object implementing it can process the reported RDF statements in any way it wants.

All Rio writers (such as the RDFXmlWriter, TurtleWriter, TriXWriter, etc.) implement the RDFHandler interface. This allows them to be used in combination with querying quite easily. In the following example, we use a TurtleWriter to write the result of a SPARQL graph query to standard output in Turtle format:

{{< highlight java  >}}
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
try (RepositoryConnection conn = repo.getConnection()) {
   RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, System.out);
   conn.prepareGraphQuery(QueryLanguage.SPARQL,
       "CONSTRUCT {?s ?p ?o } WHERE {?s ?p ?o } ").evaluate(writer);
}
{{< / highlight >}}

Note that in the above code we use the `org.eclipse.rdf4j.rio.Rio` utility to quickly create a writer of the desired format. The Rio utility offers a lot of useful functions to quickly create writers and parser for various formats.

### Preparing and Reusing Queries

In the previous sections we have simply created a query from a string and immediately evaluated it. However, the `prepareTupleQuery` and `prepareGraphQuery` methods return objects of type `Query`, specifically `TupleQuery` and `GraphQuery`.

A Query object, once created, can be reused several times. For example, we can evaluate a Query object, then add some data to our repository, and evaluate the same query again.

The Query object also has a `setBinding()` method, which can be used to specify specific values for query variables. As a simple example, suppose we have a repository containing names and e-mail addresses of people, and we want to do a query for each person, retrieve his/her e-mail address, for example, but we want to do a separate query for each person. This can be achieved using the setBinding() functionality, as follows:

{{< highlight java  >}}
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
{{< / highlight >}}

The values with which you perform the `setBinding` operation of course do not necessarily have to come from a previous query result (as they do in the above example). Using a ValueFactory you can create your own value objects. You can use this functionality to, for example, query for a particular keyword that is given by user input:

{{< highlight java  >}}
ValueFactory factory = myRepository.getValueFactory();

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
keywordQuery.setBinding("keyword", factory.createLiteral(keyword));

// We then evaluate the prepared query and can process the result:
TupleQueryResult keywordQueryResult = keywordQuery.evaluate();
{{< / highlight >}}

### Explaining queries

> New in RDF4J 3.2.0 - Experimental feature

SPARQL queries are translated to query plans and then run through an optimization pipeline before they get evaluated and 
the results returned. The query explain feature gives a peek into what decisions are being made and how they affect
the performance of your query.

This feature is currently released as an experimental feature, which means that it may change, be moved or even removed in the future. 
Explaining queries only works if you are using one of the built in stores directly in your Java code. 
If you are connecting to a remote RDF4J Server, using the Workbench or connecting to a third party database then you will get an 
UnsupportedException. 

In 3.2.0 queries have a new method `explain(...)` that returns an `Explanation` explaining how the query will be, or has been, evaluated.

 {{< highlight java  >}}
 try (SailRepositoryConnection connection = sailRepository.getConnection()) {
    TupleQuery query = connection.prepareTupleQuery("select * where .... ");
    String explain = query.explain(Explanation.Level.Timed).toString();
    System.out.println(explain);
}
{{< / highlight >}}

There are 4 explanation levels to choose between:

|             | Parsed | Optimized | Cost and Estimates | Fully evaluated | Real result sizes | Performance timing |
|-------------|--------|-----------|--------------------|-----------------|-------------------|--------------------|
| Unoptimized | ✓      |           |                    |                 |                   |                    |
| Optimized   | ✓      | ✓         | ✓                  |                 |                   |                    |
| Executed    | ✓      | ✓         | ✓                  | ✓               | ✓                 |                    |
| Timed       | ✓      | ✓         | ✓                  | ✓               | ✓                 | ✓                  |


First try to use the `Timed` level, since this is the richest and gives the clearest understanding about 
which part of the query is the slowest. `Timed` and `Executed` both fully evaluate the query and iterate 
over all the result sets. Seeing as how this can be very time-consuming there is a default best-effort 
timeout of 60 seconds. A different timeout can be set by changing the timeout for the query.

The lower levels `Unoptimized` and `Optimized` are useful for understanding how RDF4J reorders queries in 
order to optimize them.

The following query intends to get everyone in Peter's extended friend graph who is at least 18 years old 
and return their node and optionally their name.

{{< highlight sparql  >}}
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
{{< / highlight >}}

For our test data the query returns the following results:

```
[ friend=http://example.com/steve; name="Steve" ]
[ friend=http://example.com/mary ]
```

Our test data also contains other people, so the query has to evaluate a lot more data than the results lead us to believe.

Explaining the query at the `Timed` level gives us the following plan:

```
01 Projection (resultSizeActual=2, totalTimeActual=27.3ms, selfTimeActual=0.118ms)
02    ProjectionElemList
03       ProjectionElem "friend"
04       ProjectionElem "name"
05    LeftJoin (LeftJoinIterator) (resultSizeActual=2, totalTimeActual=27.2ms, selfTimeActual=0.425ms)
06       Join (JoinIterator) (resultSizeActual=2, totalTimeActual=26.4ms, selfTimeActual=0.355ms)
07          Extension (resultSizeActual=1, totalTimeActual=0.115ms, selfTimeActual=0.09ms)
08             ExtensionElem (person)
09                ValueConstant (value=http://example.com/peter)
10             SingletonSet (resultSizeActual=1, totalTimeActual=0.026ms, selfTimeActual=0.026ms)
11          Join (JoinIterator) (resultSizeActual=2, totalTimeActual=26.0ms, selfTimeActual=0.72ms)
12             Filter (resultSizeActual=4, totalTimeActual=3.76ms, selfTimeActual=2.42ms)
13                Compare (>=)
14                   Var (name=age)
15                   ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
16                StatementPattern (costEstimate=4, resultSizeEstimate=12, resultSizeActual=12, totalTimeActual=1.34ms, selfTimeActual=1.34ms)
17                   Var (name=friend)
18                   Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
19                   Var (name=age)
20             Join (JoinIterator) (resultSizeActual=2, totalTimeActual=21.5ms, selfTimeActual=0.697ms)
31                ArbitraryLengthPath (costEstimate=24, resultSizeEstimate=2.2K, resultSizeActual=2, totalTimeActual=20.3ms, selfTimeActual=20.3ms)
32                   Var (name=person)
33                   Union
34                      StatementPattern (resultSizeEstimate=1.0K)
35                         Var (name=person)
36                         Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
37                         Var (name=friend)
38                      StatementPattern (resultSizeEstimate=1.0K)
39                         Var (name=friend)
40                         Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
41                         Var (name=person)
42                   Var (name=friend)
43                StatementPattern (costEstimate=1, resultSizeEstimate=101, resultSizeActual=2, totalTimeActual=0.461ms, selfTimeActual=0.461ms)
44                   Var (name=person)
45                   Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
46                   Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
47       StatementPattern (resultSizeEstimate=5, resultSizeActual=1, totalTimeActual=0.295ms, selfTimeActual=0.295ms)
48          Var (name=friend)
49          Var (name=_const_23b7c3b6_uri, value=http://xmlns.com/foaf/0.1/name, anonymous)
50          Var (name=name)
```

We start by reading the query top to bottom. The first node we encounter is:

```
Projection (resultSizeActual=2, totalTimeActual=27.3ms, selfTimeActual=0.118ms)
``` 

The node name is "Projection", which represents the `SELECT` keyword. The values in parentheses 
are cost-estimates and actual measured output and timing. You may encounter:

 - **costEstimate**: an internal value that represents the cost for executing this node and is used for ordering the nodes
 - **resultSizeEstimate**: the cardinality estimate of a node, essentially how many results this node would return if it were executed alone
 - **resultSizeActual**: the actual number of results that this node produced
 - **totalTimeActual**: the total time this node took to return all its results, including the time for its children
 - **selfTimeActual**: the time this node took all on its own to produce its results
 
 In the plan above we can see that `ArbitraryLengthPath` took most of our time by using 20.3ms (75% of the overall time). 
 This node represents the `(foaf:knows | ^foaf:knows)* ?friend` part of our query.
 
 Joins in RDF4J have a left, and a right node. The join algorithms will first retrieve a result from the left node before it gets a result 
 from the right node. The left node is the first node displayed under the join node. For the `Join` on line 06 we have the left node 
 being line 07 and the right being line 11.`Executed` and `Timed` plans will typically show the algorithm for all join and left join nodes. 
Our fastest algorithm is usually `JoinIterator`, it will retrieve a result from the left node and use the results to "query" the right node 
for the next relevant result. 
 
In our plan above we can see how `Extension` node and the `Filter` node can "tell" the `ArbitraryLengthPath` which values for 
`person` and `friend` are relevant because the `Extension` node binds exactly one value for `person` and `Filter` node binds exactly 
four values for`friend`. This is why `ArbitraryLengthPath` has a `resultSizeActual` of two, meaning that it only produced two results.

The query above is a very efficient and nicely behaved query. Usually the reason to explain a query is because the query is slow or takes 
up a lot of memory.

The following query is a typical example of a scoping issue. A very common issue to have in a slow SPARQL query.

{{< highlight sparql  >}}
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
{{< / highlight >}}

The issue with this query is that each of the union clauses introduce a new scope. It's quite easy to see in this example. Both unions define a new 
variable `?friend`, however the results should not be the intersection of common values but rather union between "everyone that knows or is known by someone" 
and "everyone 18 or older". The only exception here is that `?person` is used in the outer scope, so results from the inner union would be filtered to match 
with bindings for `?person` from the outer scope. SPARQL is designed with bottom-up semantics, which means that inner sections should be evaluated before
outer sections. This precisely so as to make scoping issues unambiguous.

The query plan for the query gives us a lot of hints about how this becomes problematic.

```
Projection (resultSizeActual=9, totalTimeActual=1.1s, selfTimeActual=0.134ms)
   ProjectionElemList
      ProjectionElem "person"
      ProjectionElem "friend"
      ProjectionElem "age"
   Join (HashJoinIteration) (resultSizeActual=9, totalTimeActual=1.1s, selfTimeActual=4.67ms)
      Extension (resultSizeActual=1, totalTimeActual=0.046ms, selfTimeActual=0.036ms)
         ExtensionElem (person)
            ValueConstant (value=http://example.com/peter)
         SingletonSet (resultSizeActual=1, totalTimeActual=0.011ms, selfTimeActual=0.011ms)
      Union (new scope) (resultSizeActual=10.5K, totalTimeActual=1.1s, selfTimeActual=4.68ms)
         Join (HashJoinIteration) (resultSizeActual=10.1K, totalTimeActual=1.1s, selfTimeActual=41.5ms)
            StatementPattern (costEstimate=34, resultSizeEstimate=101, resultSizeActual=101, totalTimeActual=1.14ms, selfTimeActual=1.14ms)
               Var (name=person)
               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
               Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
            ArbitraryLengthPath (new scope) (costEstimate=47, resultSizeEstimate=2.2K, resultSizeActual=102.0K, totalTimeActual=1.0s, selfTimeActual=1.0s)
               Var (name=person)
               Union
                  StatementPattern (resultSizeEstimate=1.0K)
                     Var (name=person)
                     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
                     Var (name=friend)
                  StatementPattern (resultSizeEstimate=1.0K)
                     Var (name=friend)
                     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
                     Var (name=person)
               Var (name=friend)
         Join (JoinIterator) (resultSizeActual=404, totalTimeActual=1.26ms, selfTimeActual=0.275ms)
            Filter (new scope) (costEstimate=12, resultSizeEstimate=12, resultSizeActual=4, totalTimeActual=0.555ms, selfTimeActual=0.463ms)
               Compare (>=)
                  Var (name=age)
                  ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
               StatementPattern (resultSizeEstimate=12, resultSizeActual=12, totalTimeActual=0.092ms, selfTimeActual=0.092ms)
                  Var (name=friend)
                  Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
                  Var (name=age)
            StatementPattern (costEstimate=101, resultSizeEstimate=101, resultSizeActual=404, totalTimeActual=0.428ms, selfTimeActual=0.428ms)
               Var (name=person)
               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
               Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
```

The biggest time use and largest result size is produced at line:

```
ArbitraryLengthPath (new scope) (costEstimate=47, resultSizeEstimate=2.2K, resultSizeActual=102.0K, totalTimeActual=1.0s, selfTimeActual=1.0s)
```

This tells us that the query is probably producing all possible results for `?person (foaf:knows | ^foaf:knows)* ?friend.`. In fact running this fragment in a new query
shows that produces ~102 000 results.

Taking a look at the unoptimized plan we can see where the issue lies:

```
01 Projection
02    ProjectionElemList
03       ProjectionElem "person"
04       ProjectionElem "friend"
05       ProjectionElem "age"
06    Join
07       Join
08          Extension
09             ExtensionElem (person)
10                ValueConstant (value=http://example.com/peter)
11             SingletonSet
12          StatementPattern
13             Var (name=person)
14             Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
15             Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
16       Union (new scope)
17          ArbitraryLengthPath (new scope)
18             Var (name=person)
19             Union
20                StatementPattern
31                   Var (name=person)
32                   Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
33                   Var (name=friend)
34                StatementPattern
35                   Var (name=friend)
36                   Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
37                   Var (name=person)
38             Var (name=friend)
39          Filter (new scope)
40             Compare (>=)
41                Var (name=age)
42                ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
43             StatementPattern
44                Var (name=friend)
45                Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
46                Var (name=age)
```

The problem is that the `Union` on line 16 introduces a new scope. This means that the `Join` above it (line 6) can't push its binding for `?person` into the `Union`. 
This is the reason that the execution of the query was done with the `HashJoinIteration` rather than with the `JoinIterator`.

One way to solve this issue is to copy the `BIND` into all relevant unions.

{{< highlight sparql  >}}
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
{{< / highlight >}}

This forces the inner union to only consider ex:peter as `?person` meaning we only need to find his friends and not everyone elses friends. 
The query plan also agrees that this is better.

 ```
Projection (resultSizeActual=9, totalTimeActual=1.16ms, selfTimeActual=0.029ms)
   ProjectionElemList
      ProjectionElem "person"
      ProjectionElem "friend"
      ProjectionElem "age"
   Union (new scope) (resultSizeActual=9, totalTimeActual=1.13ms, selfTimeActual=0.041ms)
      Join (JoinIterator) (resultSizeActual=5, totalTimeActual=0.411ms, selfTimeActual=0.039ms)
         Join (JoinIterator) (resultSizeActual=1, totalTimeActual=0.056ms, selfTimeActual=0.039ms)
            Extension (resultSizeActual=1, totalTimeActual=0.012ms, selfTimeActual=0.009ms)
               ExtensionElem (person)
                  ValueConstant (value=http://example.com/peter)
               SingletonSet (resultSizeActual=1, totalTimeActual=0.003ms, selfTimeActual=0.003ms)
            Extension (resultSizeActual=1, totalTimeActual=0.005ms, selfTimeActual=0.003ms)
               ExtensionElem (person)
                  ValueConstant (value=http://example.com/peter)
               SingletonSet (resultSizeActual=1, totalTimeActual=0.002ms, selfTimeActual=0.002ms)
         Join (JoinIterator) (resultSizeActual=5, totalTimeActual=0.316ms, selfTimeActual=0.058ms)
            StatementPattern (costEstimate=34, resultSizeEstimate=101, resultSizeActual=1, totalTimeActual=0.007ms, selfTimeActual=0.007ms)
               Var (name=person)
               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
               Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
            ArbitraryLengthPath (costEstimate=47, resultSizeEstimate=2.2K, resultSizeActual=5, totalTimeActual=0.251ms, selfTimeActual=0.251ms)
               Var (name=person)
               Union
                  StatementPattern (resultSizeEstimate=1.0K)
                     Var (name=person)
                     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
                     Var (name=friend)
                  StatementPattern (resultSizeEstimate=1.0K)
                     Var (name=friend)
                     Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
                     Var (name=person)
               Var (name=friend)
      Join (JoinIterator) (resultSizeActual=4, totalTimeActual=0.68ms, selfTimeActual=0.015ms)
         Extension (resultSizeActual=1, totalTimeActual=0.005ms, selfTimeActual=0.003ms)
            ExtensionElem (person)
               ValueConstant (value=http://example.com/peter)
            SingletonSet (resultSizeActual=1, totalTimeActual=0.002ms, selfTimeActual=0.002ms)
         Join (JoinIterator) (resultSizeActual=4, totalTimeActual=0.659ms, selfTimeActual=0.062ms)
            Filter (new scope) (costEstimate=12, resultSizeEstimate=12, resultSizeActual=4, totalTimeActual=0.581ms, selfTimeActual=0.566ms)
               Compare (>=)
                  Var (name=age)
                  ValueConstant (value="18"^^<http://www.w3.org/2001/XMLSchema#integer>)
               StatementPattern (resultSizeEstimate=12, resultSizeActual=12, totalTimeActual=0.014ms, selfTimeActual=0.014ms)
                  Var (name=friend)
                  Var (name=_const_8d89de74_uri, value=http://xmlns.com/foaf/0.1/age, anonymous)
                  Var (name=age)
            StatementPattern (costEstimate=101, resultSizeEstimate=101, resultSizeActual=4, totalTimeActual=0.017ms, selfTimeActual=0.017ms)
               Var (name=person)
               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
               Var (name=_const_e1df31e0_uri, value=http://xmlns.com/foaf/0.1/Person, anonymous)
```

Notice that `ArbitraryLengthPath` produces 5 results and that the entire query runs in 1.16ms instead of 1.1s.

If you want to practice with these examples, the code below produces these three plans.

{{< highlight java  >}}
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
				"  		BIND(<http://example.com/peter> as ?person)",
				"		?person	(foaf:knows | ^foaf:knows)* ?friend.",
				"	} UNION {",
				"		?friend foaf:age ?age",
				"		FILTER(?age >= 18) ",
				"	}",
				"}"));

			Explanation explain = query.explain(Explanation.Level.Timed);
			System.out.println(explain);

		}

		sailRepository.shutDown();

	}
	
}	
{{< / highlight >}}


## Creating, retrieving, removing individual statements

The RepositoryConnection can also be used for adding, retrieving, removing or otherwise manipulating individual statements, or sets of statements.

To be able to add new statements, we can use a ValueFactory to create the Values out of which the statements consist. For example, we want to add a few statements about two resources, Alice and Bob:

{{< highlight java  >}}
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
...

ValueFactory f = myRepository.getValueFactory();

// create some resources and literals to make statements out of
IRI alice = f.createIRI("http://example.org/people/alice");
IRI bob = f.createIRI("http://example.org/people/bob");
IRI name = f.createIRI("http://example.org/ontology/name");
IRI person = f.createIRI("http://example.org/ontology/Person");
Literal bobsName = f.createLiteral("Bob");
Literal alicesName = f.createLiteral("Alice");

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
{{< / highlight >}}

Of course, it will not always be necessary to use a ValueFactory to create IRIs. In practice, you will find that you quite often retrieve existing IRIs from the repository (for example, by evaluating a query) and then use those values to add new statements. Also, for several well-knowns vocabularies we can simply reuse the predefined constants found in the org.eclipse.rdf4j.model.vocabulary package, and using the ModelBuilder utility you can very quickly create collections of statements without ever touching a ValueFactory.

Retrieving statements works in a very similar way. One way of retrieving statements we have already seen actually: we can get a GraphQueryResult containing statements by evaluating a graph query. However, we can also use direct method calls to retrieve (sets of) statements. For example, to retrieve all statements about Alice, we could do:

{{< highlight java  >}}
RepositoryResult<Statement> statements = con.getStatements(alice, null, null);
{{< / highlight >}}

Similarly to the TupleQueryResult object and other types of query results, the RepositoryResult is an iterator-like object that lazily retrieves each matching statement from the repository when its next() method is called. Note that, like is the case with QueryResult objects, iterating over a RepositoryResult may result in exceptions which you should catch to make sure that the RepositoryResult is always properly closed after use:

{{< highlight java  >}}
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
{{< / highlight >}}

Or alternatively, using try-with-resources and the fact that (since RDF4J 3.1.0) a RepositoryResult is an `Iterable`:

{{< highlight java  >}}
try (RepositoryResult<Statement> statements = con.getStatements(alice, null, null, true)) {
   for (Statement st: statements) {
      ... // do something with the statement
   }
}
{{< / highlight >}}

In the above `getStatements()` invocation, we see four parameters being passed. The first three represent the subject, predicate and object of the RDF statements which should be retrieved. A null value indicates a wildcard, so the above method call retrieves all statements which have as their subject Alice, and have any kind of predicate and object. The optional fourth parameter indicates whether or not inferred statements should be included or not (you can leave this parameter out, in which case it defaults to `true`).

Removing statements again works in a very similar fashion. Suppose we want to retract the statement that the name of Alice is “Alice”):

{{< highlight java  >}}
con.remove(alice, name, alicesName);
{{< / highlight >}}

Or, if we want to erase all statements about Alice completely, we can do:

{{< highlight java  >}}
con.remove(alice, null, null);
{{< / highlight >}}

## Using named graphs/context

RDF4J supports the notion of _context_, which you can think of as a way to group sets of statements together through a single group identifier (this identifier can be a blank node or a URI).

A very typical way to use context is tracking provenance of the statements in a repository, that is, which file these statements originate from. For example, consider an application where you add RDF data from different files to a repository, and then one of those files is updated. You would then like to replace the data from that single file in the repository, and to be able to do this you need a way to figure out which statements need to be removed. The context mechanism gives you a way to do that.

Another typical use case is to support _named graphs_: in the SPARQL query language, named graphs can be queried as subsets of the dataset over which the query is evaluated. In RDF4J, named graphs are implemented via the context mechanism. This means that if you put data in RDF4J in a context, you can query that context as a named graph in SPARQL.

We will start by showing some simple examples of using context in the API. In the following example, we add an RDF document from the Web to our repository, in a context. In the example, we make the context identifier equal to the Web location of the file being uploaded.

{{< highlight java  >}}
String location = "http://example.org/example/example.rdf";
String baseURI = location;
URL url = new URL(location);
IRI context = f.createIRI(location);
conn.add(url, baseURI, RDFFormat.RDFXML, context);
{{< / highlight >}}

We can now use the context mechanism to specifically address these statements in the repository for retrieve and remove operations:

{{< highlight java  >}}
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
{{< / highlight >}}

In most methods in the Repository API, the context parameter is a vararg, meaning that you can specify an arbitrary number (zero, one, or more) of context identifiers. This way, you can combine different contexts together. For example, we can very easily retrieve statements that appear in either `context1` or `context2`.

In the following example we add information about Bob and Alice again, but this time each has their own context. We also create a new property called `creator` that has as its value the name of the person who is the creator a particular context. The knowledge about creators of contexts we do not add to any particular context, however:

{{< highlight java  >}}
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
{{< / highlight >}}

Once we have this information in our repository, we can retrieve all statements about either Alice or Bob by using the context vararg:

{{< highlight java  >}}
// Get all statements in either context1 or context2
RepositoryResult<Statement> result = con.getStatements(null, null, null, context1, context2);
{{< / highlight >}}

You should observe that the above RepositoryResult will not contain the information that `context1` was created by Alice and context2 by Bob. This is because those statements were added without any context, thus they do not appear in context1 or `context2`, themselves.

To explicitly retrieve statements that do not have an associated context, we do the following:

{{< highlight java  >}}
// Get all statements that do not have an associated context
RepositoryResult<Statement> result = con.getStatements(null, null, null, (Resource)null);
{{< / highlight >}}

This will give us only the statements about the creators of the contexts, because those are the only statements that do not have an associated context. Note that we have to explicitly cast the null argument to Resource, because otherwise it is ambiguous whether we are specifying a single value or an entire array that is null (a vararg is internally treated as an array). Simply invoking `getStatements(s, p, o, null)` without an explicit cast will result in an `IllegalArgumentException`.

We can also get everything that either has no context or is in context1:

{{< highlight java  >}}
// Get all statements that do not have an associated context, or that are in context1
RepositoryResult<Statement> result = con.getStatements(null, null, null, (Resource)null, context1);
{{< / highlight >}}

So as you can see, you can freely combine contexts in this fashion.

Note: 

{{< highlight java >}}
getStatements(null, null, null);
{{< / highlight >}}

is not the same as:

{{< highlight java >}}
getStatements(null, null, null, (Resource)null);
{{< / highlight >}}

The former (without any context id parameter) retrieves all statements in the repository, ignoring any context information. The latter, however, only retrieves statements that explicitly do not have any associated context.

## Working with Models, Collections and Iterations

Most of these examples sofar have been on the level of individual statements. However, the Repository API offers several methods that work with Java Collections of statements, allowing more batch-like update operations.

For example, in the following bit of code, we first retrieve all statements about Alice, put them in a Model (which, as we have seen in the previous sections, is an implementation of `java.util.Collection`) and then remove them:

{{< highlight java  >}}
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.model.Model;

// Retrieve all statements about Alice and put them in a Model
RepositoryResult<Statement> statements = con.getStatements(alice, null, null);
Model aboutAlice = QueryResults.asModel(statements);

// Then, remove them from the repository
con.remove(aboutAlice);
{{< / highlight >}}

As you can see, the `QueryResults` class provides a convenient method that takes a `CloseableIteration` (of which `RepositoryResult` is a subclass) as input, and returns the `Model` with the contents of the iterator added to it. It also automatically closes the result object for you.

In the above code, you first retrieve all statements, put them in a `Model`, and then remove them. Although this works fine, it can be done in an easier fashion, by simply supplying the resulting object directly:

{{< highlight java  >}}
con.remove(con.getStatements(alice, null, null));
{{< / highlight >}}

The RepositoryConnection interface has several variations of add, retrieve and remove operations. See the Javadoc for a full overview of the options.

## RDF Collections and RepositoryConnections

In the [Model API documentation](../model/) we have already seen how we can use the RDFCollections utility on top of a Model. This makes it very easy to insert any RDF Collection into your Repository - after all a Model can simply be added as follows:

{{< highlight java  >}}
Model rdfList = ... ;
try (RepositoryConnection conn = repo.getConnection()) {
       conn.add(rdfList);
}
{{< / highlight >}}

In addition to this the Repository API offers the `Connections` utility class, which contains some useful utility functions specifically for retrieving RDF Collections from a Repository.

For example, to retrieve all statements corresponding to an RDF Collection identified by the resource node from our Repository, we can do the following:

{{< highlight java  >}}
// retrieve all statements forming our RDF Collection from the Repository and put
// them in a Model
try(RepositoryConnection conn = rep.getConnection()) {
   Model rdfList = Connections.getRDFCollection(conn, node, new LinkedHashModel());
}
{{< / highlight >}}

Or instead, you can retrieve them in streaming fashion as well:

{{< highlight java  >}}
try(RepositoryConnection conn = repo.getConnection()) {
    Connections.consumeRDFCollection(conn, node,
		 st -> { // ... do something with the triples forming the collection });
}
{{< / highlight >}}

# Transactions

So far, we have shown individual operations on repositories: adding statements, removing them, etc. By default, each operation on a `RepositoryConnection` is immediately sent to the store and committed.

The `RepositoryConnection` interface supports a full transactional mechanism that allows one to group modification operations together and treat them as a single update: before the transaction is committed, none of the operations in the transaction has taken effect, and after, they all take effect. If something goes wrong at any point during a transaction, it can be rolled back so that the state of the repository is the same as before the transaction started. Bundling update operations in a single transaction often also improves update performance compared to multiple smaller transactions.

We can indicate that we want to begin a transaction by using the `RepositoryConnection.begin()` method. In the following example, we use a connection to bundle two file addition operations in a single transaction:

{{< highlight java  >}}
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
{{< / highlight >}}

In the above example, we use a transaction to add two files to the repository. Only if both files can be successfully added will the repository change. If one of the files can not be added (for example because it can not be read), then the entire transaction is cancelled and none of the files is added to the repository.

As you can see, we open a new try block after calling the begin() method (line 9 and further). The purpose of this is to catch any errors that happen during transaction execution, so that we can explicitly call `rollback()` on the transaction. If you prefer your code shorter, you can leave this out, and just do this:

{{< highlight java  >}}
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
{{< / highlight >}}

The `close()` method, which is automatically invoked by Java when the try-with resources block ends, will also ensure that an unfinished transaction is rolled back (it will also log a warning about this).
A `RepositoryConnection` only supports one active transaction at a time. You can check at any time whether a transaction is active on your connection by using the `isActive()` method. If you need concurrent transactions, you will need to use several separate RepositoryConnections.

## Transaction Isolation Levels

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

{{< highlight java  >}}
try (RepositoryConnection conn = rep.getConnection()) {
    conn.begin(IsolationLevels.SERIALIZABLE);
     ....
    conn.commit();
}
{{< / highlight >}}

A transaction isolation level is a sort of contract, that is, a set of guarantees of what will minimally happen while the transaction is active. A store will make a best effort to honor the guarantees of the requested isolation level. If it does not support the specific isolation level being requested, it will attempt to use a level it does support that offers minimally the same guarantees.

## Automated transaction handling

Although transactions are a convenient mechanism, having to always call `begin()` and `commit()` to explictly start and stop your transactions can be tedious. RDF4J offers a number of convenience utility functions to automate this part of transaction handling, using the `Repositories` utility class.

As an example, consider this bit of transactional code. It opens a connection, starts a transaction, adds two RDF statements, and then commits. It also makes sure that it rolls back the transaction if something went wrong, and it ensures that once we’re done, the connection is closed.

{{< highlight java  >}}
ValueFactory f = myRepository.getValueFactory();
IRI bob = f.createIRI("urn:bob");
RepositoryConnection conn = myRepository.getConnection();
try {
   conn.begin();
   conn.add(bob, RDF.TYPE, FOAF.PERSON);
   conn.add(bob, FOAF.NAME, f.createLiteral("Bob"));
   conn.commit();
}
catch (RepositoryException e) {
   conn.rollback();
}
finally {
   conn.close();
}
{{< / highlight >}}

That's an awful lot of code for just inserting two triples. The same thing can be achieved with far less boilerplate code, as follows:

{{< highlight java  >}}
ValueFactory f = myRepository.getValueFactory();
IRI bob = f.createIRI("urn:bob");
Repositories.consume(myRepository, conn -> {
  conn.add(bob, RDF.TYPE, FOAF.PERSON);
  conn.add(bob, RDFS.LABEL, f.createLiteral("Bob"));
});
{{< / highlight >}}

As you can see, using `Repositories.consume()`, we do not explicitly begin or commit a transaction. We don’t even open and close a connection explicitly – this is all handled internally. The method also ensures that the transaction is rolled back if an exception occurs.

This pattern is useful for simple transactions, however as we’ve seen above, we sometimes do need to explicitly call `begin()`, especially if we want to modify the transaction isolation level.

# Multithreaded Repository Access

The Repository API supports multithreaded access to a store: multiple concurrent threads can obtain connections to a Repository and query and performs operations on it simultaneously (though, depending on the transaction isolation level, access may occassionally block as a thread needs exclusive access).

The Repository object is thread-safe, and can be safely shared and reused across multiple threads (a good way to do this is via a RepositoryProvider).

NOTE: RepositoryConnection is not thread-safe. This means that you should not try to share a single RepositoryConnection over multiple threads. Instead, ensure that each thread obtains its own RepositoryConnection from a shared Repository object. You can use transaction isolation levels to control visibility of concurrent updates between threads. 
