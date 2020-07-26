---
title: "Sesame to Eclipse RDF4J migration"
toc: true
weight: 4
---

Eclipse RDF4J is the successor of the OpenRDF Sesame project. The RDF4J framework and tools offer the same functionality, and will continue to be maintained and improved by the same team of developers as Sesame was, under Eclipse stewardship. For any users who wish to migrate their existing projects from Sesame to RDF4J (and we certainly urge you to do so quickly), here’s an overview of what has changed.
<!--more->

# Migrating from Sesame 4

The RDF4J 2.0 code is based off of the latest stable Sesame release, 4.1.2. This means that migrating from Sesame 4 to RDF4J 2 is fairly straightforward.

## Changes for Java Programmers
### Maven artifacts

All RDF4J Maven artifacts have a new groupId as well as a new artifactId. Fortunately, the rename is very simple:

- the groupId for all artifacts has changed from `org.openrdf.sesame` to `org.eclipse.rdf4j`;
- the artifactIds all have a different name prefix: `sesame-<...>` has become `rdf4j-<...>`.

For example, the Maven artifact for the Repository API in Sesame was:

    <dependency>
       <groupId>org.openrdf.sesame</groupId>
       <artifactId>sesame-repository-api</artifactId>
    </dependency>

In RDF4J, this same artifact is identified as follows:

    <dependency>
       <groupId>org.eclipse.rdf4j</groupId>
       <artifactId>rdf4j-repository-api</artifactId>
    </dependency>

### Package renaming

Although Java class and method names have (almost) all remained the same, the package names of all RDF4J code has been changed. To upgrade your Java code to use these new package names, you will need to do the following replacements:

- `org.openrdf.*` becomes `org.eclipse.rdf4j.*`
- `info.aduna.*` becomes `org.eclipse.rdf4j.common.*`

If you are using the Eclipse IDE, a simple way to achieve this is to make sure your project has the new RDF4J libraries on the build path (and the old Sesame libraries are removed), and then use “Source” -> “Organize imports” (Ctrl+Shift+O) to replace old package names with new ones.

Note that in some cases Eclipse may need you to decide between two or more possibilities. In this case a dialog will pop up, and you can pick the correct candidate.

Alternatively, you can of course just run a global search-and-replace over your code to update your import statements.

## Upgrading Sesame Server to RDF4J Server

Sesame Server is now called RDF4J Server. When upgrading Sesame Server to RDF4J Server, you need to take the following into account.

### Server URL change

The default server URL for the RDF4J Server is `http://localhost:8080/RDF4J-server`. This is different from the default server URL for Sesame Server, which was `http://localhost:8080/openrdf-sesame`.

### Migrating your data: Server Data Directory change

Sesame Server by default stores its data (configuration, log files, as well as the actual databases) in a directory `%APPDATA%\Aduna\OpenRDF Sesame` (on Windows), `$HOME/.aduna/openrdf-sesame` (on Linux), or `$HOME/Library/Application Support/Aduna/OpenRDF Sesame` (on Mac OSX).

RDF4J server stores its data in a different location: `%APPDATA%\RDF4J\Server` (Windows), `$HOME/.RDF4J/Server` (Linux), or `$HOME/Library/Application Support/RDF4J/Server` (on Mac OSX). Please note that RDF4J Server will not automatically detect existing data from an old Sesame Server installation.

This means that if you wish to migrate your data, you will have to manually copy over the data. This can be done quite easily as follows:

1. Start your new RDF4J Server instance, make sure it creates its initial data directory, then stop it again;
2. Stop your old Sesame Server;
3. Delete the RDF4J/Server/repositories subdirectory that was just created by RDF4J Server;
4. Copy the repositories directory from your old Sesame Server installation to replace the directory you deleted in the previous step;
5. Restart RDF4J Server.

## Upgrading to RDF4J Workbench

Like RDF4J Server, RDF4J Workbench also uses a different data directory from its predecessor. The only thing RDF4J Workbench stores in its data directory is saved queries, so if you haven’t used this functionality you can safely skip this.

To migrate your saved queries from Sesame Workbench to RDF4J Workbench, do the following:

1. Start your new RDF4J Workbench instance, make sure it creates its initial data directory, then stop it again;
2. Stop your old Sesame Workbench;
3. Delete the RDF4J/Workbench/queries subdirectory that was just created by RDF4J Workbench;
4. Copy the queries directory from your old Sesame Workbench installation to replace the directory you deleted in the previous step;
5. Restart RDF4J Workbench.

# Migrating from Sesame 2

RDF4J is based on the Sesame 4 code base, which is a major new release of the framework with significant changes in its core APIs, compared to Sesame 2. Here we give a quick overview of the major changes that you will need to take into account (in addition to the changes documented in the previous sections) when upgrading your project from Sesame 2 to RDF4J.

## RDF4J 2.0 requires Java 8

If you use Sesame components in your own Java application and you wish to upgrade, you will have to make sure you are using a Java 8 compiler and runtime environment.

If you are using the Sesame Server and/or Sesame Workbench applications, you will need to upgrade the Java Runtime Environment (JRE) to Java 8 before upgrading to the new versions.

If you, for whatever reason, really can not upgrade to Java 8, you can use RDF4J 1.0, instead of 2.0. RDF4J 1.0 is a backport of the RDF4J code compatible with Java 7. However, to be clear: RDF4J 1.0 is not identical to Sesame 2! Many of the changes described here for RDF4J 2.0 also hold for RDF4J 1.0 – we merely adapted the code to take out Java 8-specific features such as Optionals and lambda expressions.

## Statement equals method now includes context

In Sesame 2, the method `Statement.equals()` was defined to consider two statements equal if their subject, predicate, and object were identical, disregarding any context information. In RDF4J, this was changed: the context field is now included in the equality check.  As an example of what this means, see the following code:

{{< highlight java >}}
  Statement st1 = vf.createStatement(s1, p1, o1);
  Statement st2 = vf.createStatement(s1, p1, o1, c1);
  System.out.println(st1.equals(st2));
{{< / highlight >}}

In Sesame 2, the above code would print out `true`. In RDF4J, it prints out `false`.

## Use of `java.util.Optional` in Model API

In several places in the Model API, RDF4J (2.0) uses `java.util.Optional` return types, where it previously returned either some value or `null`.

As one example of this, `Literal.getLanguage()`, which return the language tag of an RDF literal (if any is defined) now has `Optional<String>` as its return type, instead of just `String`. As a consequence, if you previously had code that retrieved the language tag like this:

{{< highlight java >}}
  String languageTag = literal.getLanguage(); 
  if (languageTag != null) {
        System.out.println("literal language tag is " + languageTag);
  }
{{< / highlight >}}

You will need to modify slightly, for example, like this:

{{< highlight java >}}
  String languageTag = literal.getLanguage().orElse(null); 
  if (languageTag != null) {
        System.out.println("literal language tag is " + languageTag);
  }
{{< / highlight >}}

Or more drastically:

{{< highlight java >}}
  literal.getLanguage().ifPresent(tag -> System.out.println("literal language tag is " + tag));
{{< / highlight >}}

For more information about how to effectively make use of Optional, see this article by Oracle.

## RDBMS Sail removed

The RDBMS Sail (that is, Sesame storage support for PostgreSQL and MySQL), which was deprecated since Sesame release 2.7.0, has been completely removed in RDF4J. If you were still using this storage backend as part of your project, you will need to switch to a different database type before upgrading or look into third-party implementations that may still support those databases.

## Deprecation, Deprecation, Deprecation

Since Sesame 2, we have cleaned up interfaces, renamed methods and classes, and just generally streamlined a lot of stuff. In most cases we have done this in a way that is backward-compatible: old class/method/interface names have been preserved and can still be used, so you will not immediately need to completely change your code.

However, they have been marked deprecated. This means that we intend to drop support for these older names in future RDF4J releases. In every case, however, we have extensively documented what you should do in the API Javadoc: each deprecated method or class points to the new alternative that you should be using. Upgrade at your leisure, just remember: better sooner than later.

## API Changes in RDF4J compared to Sesame 2

Compared to Sesame 2, RDF4J is a significantly improved version of the framework. Several of the core APIs and interfaces have been improved to make them easier to use, and to make full use of Java 7/8 features. Here, we outline some of these improvements in more detail.

### Unchecked Exceptions

All exceptions thrown by RDF4J are unchecked: they all inherit from `java.lang.RuntimeException`. This means that you as a developer you are now longer forced to catch (or throw) any exceptions that inherit from `org.eclipse.rdf4j.RDF4JException`, such as `RepositoryException`, `QueryEvaluationException`, or `RDFParseException` .

Of course, you still can catch these exceptions if you want, but it means that in cases where you as a programmer are certain that an exception would never occur in practice, you can just ignore the exception, instead of having to write verbose `try-catch-finally` blocks everywhere. In other words: we are shifting the responsibility to you as a programmer to take care you write robust code. Be assured that all exceptions that can be thrown will still be properly documented in the Javadoc.

### `AutoCloseable` results and connections

In RDF4J, all instances of both `CloseableIteration` (the root interface of all query result types, including resource and statement iterators, SPARQL query results) and `RepositoryConnectio`n (the interface for communicating with a `Repository`) inherit from `java.lang.AutoCloseable`.

This means that instead of explicitly having to call the `close()` method on these resources when you’re done with them, you can now use the so-called `try-with-resources` construction. For example, instead of:

{{< highlight java >}}
  RepositoryConnection conn = repo.getConnection();
  try {
      // do something with the connection here
  }
  finally {
      conn.close();
  }
{{< / highlight >}}

You can now do:

{{< highlight java >}}
  try (RepositoryConnection conn = repo.getConnection()) {
          // do something with the connection
  }
{{< / highlight >}}

### Use of long instead of int for parser line and column references

The `ParseErrorListener` and `ParseLocationListener` parser interfaces, and the `QueryResultParseException` and `RDFParseException` exception classes, now accept and return `long` instead of `int`. This enables the parsing of much larger files without being concerned about numeric overflow in tracking.

### Fluent APIs for RDFParser and QueryResultParser

The `RDFParser` and `QueryResultParser` APIs have been enhanced to provide fluent construction and configuration:

{{< highlight java >}}
  Model rdfStatements = new LinkedHashModel();
  ParseErrorCollector errorCollector = new ParseErrorCollector();
  RDFParser aParser = Rio.createParser(RDFFormat.TURTLE)
                                 .setRDFHandler(new StatementCollector(rdfStatements))
                                 .setParseErrorListener(errorCollector);
  try {
      aParser.parse(myInputStream);
  } catch (RDFParseException e) {
      // log or rethrow RDFParseException
  } finally {
      // Examine any parse errors that occurred before moving on
      System.out.println("There were " 
              + errorCollector.getWarnings().size() 
              + " warnings during parsing");
      System.out.println("There were " 
              + errorCollector.getErrors().size() 
              + " errors during parsing");
      System.out.println("There were " 
              + errorCollector.getFatalErrors().size() 
              + " fatal errors during parsing");
  }

  QueryResultCollector handler = new QueryResultCollector();
  ParseErrorCollector errorCollector = new ParseErrorCollector();
  QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.SPARQL)
                                         .setQueryResultHandler(handler)
                                         .setParseErrorListener(errorCollector);
  try {
      aParser.parseQueryResult(myInputStream);
  } catch (QueryResultParseException e) {
      // log or rethrow QueryResultParseException
  } finally {
      // Optionally examine any parse errors that occurred before moving on
      System.out.println("There were " 
              + errorCollector.getWarnings().size() 
              + " warnings during parsing");
      System.out.println("There were " 
              + errorCollector.getErrors().size() 
              + " errors during parsing");
      System.out.println("There were " 
              + errorCollector.getFatalErrors().size() 
              + " fatal errors during parsing");
  }
{{< / highlight >}}

The `QueryResultParser` has also been updated to allow many of the operations that `RDFParser` allows including methods such as `setParseErrorListener` and `setParseLocationListener`, and the set method that is a shortcut for `getParserConfig().set`. Although `RDFParser` and `QueryResultParser` are still distinct at this point, in the future they may either be merged or have a common ancestor and these changes are aimed at making that process smoother for both developers and users.

### Lambdas and the Stream API (RDF4J 2.0 only)

Java 8 offers two powerful new features in the core language: lambda expressions, and the Stream API. RDF4J 2.0 offers a number of improvements and new utilities that allow you to leverage these features. In this section we show a few simple usage examples of these new utilities.

#### Stream-based processing of results

In RDF4J,  results from queries (or from any Repository API retrieval operation) are returned in a lazily-evaluating iterator-like object named a `CloseableIteration`. It has specific subclasses (such as `RepositoryResult`, `GraphQueryResult`, and `TupleQueryResult`), but the basis for them all is identical. RDF4J 2.0 offers a number of ways to more easily interact with such results, by converting the source iteration to a Stream object.

For example, when post-processing the result of a SPARQL CONSTRUCT query (for example to filter out all triples with ‘a’ as the subject), the classic way to do it would be something like this:

{{< highlight java >}}
  GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT ... ");
  GraphQueryResult gqr = gq.evaluate();
  List<Statement> aboutA = new ArrayList<Statement>();
  try {
    while (gqr.hasNext()) {
        Statement st = grq.next();
        if (st.getSubject().equals(a)) {
              aboutA.add(st);
        }
    }
  } finally {
     gqr.close();
  }
{{< / highlight >}}

In RDF4J, we can do this shorter, and more elegantly, using the new `QueryResults.stream()` method and its support for lambda-expressions:

{{< highlight java >}}
  GraphQuery gq = conn.prepareGraphQuery("CONSTRUCT ... ");
  GraphQueryResult gqr = gq.evaluate();
  List<Statement> aboutA = QueryResults.stream(gqr)
                     .filter(s -> s.getSubject().equals(a))
                     .collect(Collectors.toList());
{{< / highlight >}}

Note that by using `QueryResults.stream()` instead of manually iterating over our result, we no longer have to worry about closing the query result when we’re done with it, either: the provided Stream automatically takes care of this when it is either fully exhausted or when some exception occurs.

The same trick also works for results of SELECT queries:

{{< highlight java >}}
  TupleQuery query = conn.prepareTupleQuery("SELECT ?x ?c WHERE { ... } ");
  TupleQueryResult result = query.evaluate();
  // only get those results where c is equal to foaf:Person
  List<BindingSet> filteredResults = QueryResults.stream(result)
                        .filter(bs -> bs.getValue("c").equals(FOAF.PERSON))
                        .collect(Collectors.toList());
{{< / highlight >}}

#### Stream-based querying and transaction handling

In addition to additional Stream-based utilities for results, RDF4J also offers various utilities for more convenient handling of queries and transactions, by means of the `Repositories` utility class.

As a simple example, suppose we want to open a connection, fire off a SPARQL CONSTRUCT query, collect the results in a `Model`, and then close the connection. The ‘classic’ way to do this is as follows:

{{< highlight java >}}
  RepositoryConnection conn = rep.getConnection();
  try {
     GraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT WHERE {?s ?p ?o }");
     Model results = QueryResults.asModel(query.evaluate());
  }
  finally {
     conn.close();
  }
{{< / highlight >}}

Using RDF4J lambda/Stream support, we can now do all of this in a single line of code:

{{< highlight java >}}
  Model results = Repositories.graphQuery(rep, "CONSTRUCT WHERE {?s ?p ?o} ", 
                   gqr -> QueryResults.asModel(gqr));
{{< / highlight >}}
 
Note that although in this example we only do some very basic processing on the result (we convert the result of the query to a Model object), we can write an arbitrarily complex function here to fully customize how the result is processed, and fully control the type of the returned object as well.

The `Repositories` utility really comes into its own when used for update transactions. It takes care of opening a connection, starting a transaction, and properly committing (or rolling back if an error occurs).

As an example, this is the ‘classic’ way to add two new statements to the repository, using a single transaction:

{{< highlight java >}}
  RepositoryConnection conn = rep.getConnection();
  try {
     conn.begin();
     conn.add(st1);
     conn.add(st2);  
     conn.commit();
  }
  catch (RepositoryException e) {
     conn.rollback();
     throw e;
  }
  finally {
     conn.close();
  }
{{< / highlight >}}

And this is the new lambda-based equivalent:

{{< highlight java >}}
  Repositories.consume(rep, conn -> { 
      conn.add(st1); 
      conn.add(st2); 
  });
{{< / highlight >}}

