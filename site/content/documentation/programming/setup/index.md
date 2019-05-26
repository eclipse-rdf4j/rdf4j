---
title: "Setting up your development environment"
layout: "doc"
hide_page_title: "true"
---
# Setup

Before you can get started programming with rdf4j, you will need to set up your development environment, download the necessary, libraries, and so on. This chapter gives you some pointers on how to install the rdf4j libraries and how to initialize your project. 

# Using Apache Maven

By far the most flexible and useful way to include rdf4j in your project, is to use Maven. Apache Maven is a software management tool that helps you by offering things like library version management and dependency management (which is very useful because it means that once you decide you need a particular rdf4j library, Maven automatically downloads all the libraries that your library of choice requires in turn). For details on how to start using Maven, we advise you to take a look at the [Apache Maven website](http://maven.apache.org/). If you are familiar with Maven, here are a few pointers to help set up your maven project.

## Maven Repository

Rdf4j is available from the [Central Repository](https://search.maven.org/), which means you don’t need to add an additional repository configuration to your project.

## The BOM (Bill Of Materials)

A problem in larger projects is a thing called ‘version mismatch’: one part of your project uses version 1.0 of a particular rdf4j artifact, and another part uses 1.0.2 of the same (or a slightly different) artifact, and because they share dependencies you get duplicate libraries on your classpath. 

To help simplify this, rdf4j provides a BOM (Bill Of Materials) for you to include in your project. A BOM is basically a list of related artifacts and their versions. The advantage of including a BOM in your project is that you declare the version of rdf4j only once, and then can rely on all specific rdf4j artifact dependencies to use the correct version.

To include the BOM in your project, add the following to your project root pom:

    <dependencyManagement>
            <dependencies>
                    <dependency>
                            <groupId>org.eclipse.rdf4j</groupId>
                            <artifactId>rdf4j-bom</artifactId>
                            <version>2.5.2</version>
                            <type>pom</type>
                            <scope>import</scope>
                    </dependency>
            </dependencies>
    </dependencyManagement>

After you have done this, you can simply include any rdf4j artifact as a normal dependency, but you can leave out the version number in that dependency. The included BOM ensures that all included rdf4j artifacts throughout your project will use version 2.5.2.

## Maven Artifacts

The groupId for all rdf4j core artifacts is `org.eclipse.rdf4j`. To include a maven dependency in your project that automatically gets you the entire rdf4j core framework, use artifactId rdf4j-runtime: 

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-runtime</artifactId>
    </dependency>

For many projects you will not need the entire rdf4j framework, however. You can fine-tune your dependencies so that you don’t include more than you need.  Here are some typical scenarios and the dependencies that go with it. Of course, it’s up to you to vary on these basic scenarios and figure exactly which components you need (and if you don’t want to bother you can always just use the ‘everything and the kitchen sink’ `rdf4j-runtime` dependency). 

## Simple local storage and querying of RDF

If you require functionality for quick in-memory storage and querying of RDF, you will need to include dependencies on the SAIL repository module (artifactId `rdf4j-repository-sail`) and the in-memory storage backend module (artifactId `rdf4j-sail-memory`):

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-repository-sail</artifactId>
    </dependency>
    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-sail-memory</artifactId>
    </dependency>

A straightforward variation on this scenario is of course if you decide you need a more scalable persistent storage instead of (or alongside) simple in-memory storage. In this case, you can include the native store:

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-sail-nativerdf</artifactId>
    </dependency>

## Parsing / writing RDF files

The rdf4j parser toolkit is called Rio, and it is split in several modules: one for its main API (rdf4j-rio-api), and one for each specific syntax format. If you require functionality to parse or write an RDF file, you will need to include a dependency on any of the parsers for that you will want to use. For example, if you expect to need an RDF/XML syntax parser and a Turtle syntax writer, include the following 2 dependencies (you do not need to include the API dependency explicitly since each parser implementation depends on it already):

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-rio-rdfxml</artifactId>
    </dependency>
    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-rio-turtle</artifactId>
    </dependency>

## Accessing a remote rdf4j Server

If your project only needs functionality to query/manipulate a remotely running Rdf4j Server, you can stick to just including the HTTPRepository module (`rdf4j-repository-http`):

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-repository-http</artifactId>
    </dependency>

## Accessing a SPARQL endpoint

If you want to have functionality to query a remote SPARQL endpoint, such as DBPedia, you can use the SPARQLRepository module  (`rdf4j-repository-sparql`):

    <dependency> 
      <groupId>org.eclipse.rdf4j</groupId> 
      <artifactId>rdf4j-repository-sparql</artifactId> 
    </dependency>

# Using the onejar or SDK distribution

If you are not familiar with Apache Maven, an alternative way to get started with using the rdf4j libraries is to download the rdf4j onejar library and include it in your classpath.

The rdf4j onejar contains all of rdf4j’s own functionality. However, it does not contain any of the third-party libraries on which rdf4j depends, which means that if you use the onejar, you will, in addition, need to download and install these third-party libraries (if your project does not already use them, as most of these libraries are pretty common).

It is important to note that the rdf4j framework consists of a set of libraries: rdf4j is not a monolithic piece of software, you can pick and choose which parts you want and which ones you don’t. In those cases where you don’t care about picking and choosing and just want to get on with it, the onejar is a good choice.

If, however, you want a little more control over what is included, you can download the complete SDK and select (from the lib directory) those libraries that you require. The SDK distribution contains all rdf4j libraries as individual jar files, and in addition it also contains all the third-party libraries you need work with rdf4j.

# Logging: SLF4J initialization

Before you begin using any of the rdf4j libraries, one important configuration step needs to be taken: the initialization and configuration of a logging framework.

rdf4j uses the Simple Logging Facade for Java (SLF4J), which is a framework for abstracting from the actual logging implementation. SLF4J allows you, as a user of the rdf4j framework, to plug in your own favorite logging implementation at deployment time. SLF4J supports the most popular logging implementations such as Java Logging, Apache Commons Logging, Logback, log4j, etc. See the [SLF4J website](http://slf4j.org/) for more info.

What you need to do is to decide which logging implementation you are going to use and include the appropriate SLF4J logger adapter in your classpath. For example, if you decide to use Apache log4j, you need to include the SFL4J-Log4J adapter in your classpath. The SLF4J release packages includes adapters for various logging implementations; just download the SLF4J release package and include the appropriate adapter in your classpath (or, when using Maven, set the appropriate dependency); `slf4j-log4j12-<version>.jar`, for example. 

One thing to keep in mind when configuring logging is that SLF4J expects only a single logger implementation on the classpath. Thus, you should choose only a single logger. In addition, if parts of your code depend on projects that use other logging frameworks directly, you can include a Legacy Bridge which makes sure calls to the legacy logger get redirected to SLF4J (and from there on, to your logger of choice).

In particular, when working with rdf4j’s HTTPRepository or SPARQLRepository libraries, you may want to include the `jcl-over-slf4j` legacy bridge. This is because rdf4j internally uses the Apache Commons HttpClient, which relies on JCL – Jakarta Commons Logging. You can do without this if your own app is a webapp, to be deployed in e.g. Tomcat, but otherwise, your application will probably show a lot of debug log messages on standard output, starting with something like:

    DEBUG httpclient.wire.header

When you set this up correctly, you can have a single logger configuration for your entire project, and you will be able to control both this kind of logging by third party libraries and by rdf4j itself using this single config.

The rdf4j framework itself does not prescribe a particular logger implementation (after all, that’s the whole point of SLF4J, that you get to choose your preferred logger). However, several of the applications included in rdf4j (such as Rdf4j Server, Workbench, and the command line console) do use a logger implementation. The server and console application both use logback, which is the successor to log4j and a native implementation of SLF4J. The Workbench uses `java.util.logging` instead.
