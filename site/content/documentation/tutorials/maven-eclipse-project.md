---
title: "Starting a new Maven project in Eclipse"
weight: 2
toc: true
autonumbering: true
---
If you are new to RDF4J, or to tools like Eclipse IDE or Apache Maven, this tutorial will help you get started.
<!--more-->

{{< info >}}
You don't <i>have</i> to use Apache Maven or Eclipse IDE if you want to work with RDF4J. These are simply very useful tools for quickly getting a Java project started. Maven is good because it allows you to just define which libraries you want to use and never worry about any further third-party libraries you might need, and Eclipse IDE is good because it has good integration with Maven, code completion features, and is just generally a great Java development environment. But you can work with RDF4J just as well in a different build tool or IDE.
{{</ info >}}

In this tutorial, we assume that you have a basic understanding of programming in Java, and have at least an inkling of what RDF is. However, we do not assume that you know how to use either RDF4J, Maven, or Eclipse, so we'll go through it all one step at a time. If anything is already sufficiently familiar to you, you are of course free to skip ahead!

## Setting up your environment

Before we kick off, you will need to install the [Eclipse IDE](http://eclipse.org/). In this tutorial, we will use Eclipse for Java Developers version 4.18.0 (2020-12), but it shouldn’t matter too much which exact version you have installed. Select either the "Eclipse for Java developers" or "Eclipse for Java EE developers" installation option.

Eclipse IDE comes with a Maven plugin (called m2e) already installed. We will use this plugin to work with Maven. You are of course free to also install the Maven command line tools, but we won’t be using those directly in this tutorial.

When starting Eclipse for the first time, you will be asked for a workspace directory – this will be where Eclipse stores all project data as well as some configuration information. Feel free to pick/create a directory of your choice, or just accept the default.

Once Eclipse is started (and any welcome messages have been closed), you will be presented with a screen that should look roughly like this:


<a href="../images/eclipse-empty.png" target="new"><img src="../images/eclipse-empty.png" alt="Eclipse IDE started" class="img-responsive"/></a>

### Tweaking Eclipse preferences

Before we start creating our actual project in Eclipse, there are a few preferences that we should tweak. This step is not required, but it will make things a little easier in the rest of this tutorial.

From the menu, select `Eclipse -> Preferences`. The Preferences dialog pops up. On the left, select `Maven`. You will see the Maven configuration settings. The options "Download Artifact Sources" and "Download Artifact JavaDoc" are unchecked by default. Check them both, then click OK.

<a href="../images/eclipse-mvn-prefs.png" target="new"><img src="../images/eclipse-mvn-prefs.png" alt="Eclipse Maven preferences" class="img-responsive"/></a>

The reason we want this, by the way, is that having either the sources or the Javadoc available really helps when you use code autocompletion in Eclipse – Eclipse will automatically read the Javadoc and provide the documentation in a little tooltip. As said: not required, but very useful.

## Creating a new project

Now that we're all set up, we can kick things off by creating a new project. Select the File menu, then `New -> New Project...` . A dialog will appear. In this dialog, select the option `Maven Project`:

<a href="../images/eclipse-create-project-mvn.png" target="new"><img src="../images/eclipse-create-project-mvn.png" alt="New project wizard" class="img-responsive"/></a>

Once you click Next, you will be presented with a screen to create a new Maven Project. Make you sure the option 'Create a simple project' is checked:

<a href="../images/eclipse-create-simple.png" target="new"><img src="../images/eclipse-create-simple.png" alt="New Maven project" class="img-responsive"/></a>

Click Next again. In the following screen you define further details of your Maven project, such as group and artifact ids, version number, and so on. For this tutorial,  you only have to fill in three fields:

- `group id` – typically you use something like a Java package name for this. We will use `org.example`.
- `artifact id` –  name of the maven artifact if you publish our project as one. We will use `rdf4j-getting-started`.
- `name` – the project name. We will use `HelloRDF4J` .

<a href="../images/eclipse-create-rdf4j.png" target="new"><img src="../images/eclipse-create-rdf4j.png" alt="Hello RDF4J" class="img-responsive"/></a>

Once this has been filled in you can click Finish. We now have our first Eclipse project, huzzah!

## Defining the POM

So we have a project. That's great, but it's still empty: we haven't added any code or necessary libraries (such as the RDF4J libraries) yet.

We will remedy the library-situation first. In Maven, requirements for libraries (we're talking about jar files here) are specified in a special project configuration file called `pom.xml`, often referred to as "the POM". Open your project and you will see that a file with that name is already present. Doubleclick it to open it in the POM editor:

<a href="../images/eclipse-pom.png" target="new"><img src="../images/eclipse-pom.png" alt="The POM editor" class="img-responsive"/></a>

As you can see, the POM already contains some information – in fact the information that we added when we created our project. However, we need to add more information about our project. Specifically, we want to add which external Java libraries we want to include in our project. In Maven, you do this by specifying dependencies.

The first dependency we will add is for RDF4J itself. RDF4J is not a single library, but consists of a large collection of separate modules, allowing you to pick and choose which functionality you need. To make handling of all the various modules and the dependencies they all need easier, we will first add a "Bill Of Materials" (BOM) dependency. The easiest way to do this is to edit the XML source code directly. Switch to the 'pom.xml' tab of the POM editor, and add the following XML fragment:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-bom</artifactId>
      <version>3.6.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

It should look like this after you've added this and saved the file:

<a href="../images/eclipse-rdf4j-bom.png" target="new"><img src="../images/eclipse-rdf4j-bom.png" alt="The RDF4J BOM" class="img-responsive"/></a>

Next we need to add the actual RDF4J modules we want to use. As said, this is where you could pick and choose the specific modules (parsers, databases, etc) that you want. For this tutorial, however, we'll be lazy and just add the entire core framework. We do this by adding the `rdf4j-storage` dependency, as follows:

```xml
<dependencies>
  <dependency>
    <groupId>org.eclipse.rdf4j</groupId>
    <artifactId>rdf4j-storage</artifactId>
    <type>pom</type>
  </dependency>
</dependencies>
```

Notice that because we already have a BOM added, we don't need to specify a version number for this dependency. Once you have done this and save the file, Eclipse will automatically begin downloading the RDF4J libraries. This may take a little while, just let it do its thing. Once it's complete, your POM should look like this:

<a href="../images/eclipse-rdf4j-dep.png" target="new"><img src="../images/eclipse-rdf4j-dep.png" alt="The RDF4J dependencies" class="img-responsive"/></a>

We are going to add one more dependency, namely for a logging framework. RDF4J uses the [SLF4J logging API](https://slf4j.org/), which requires a logging implementation, so we’ll provide one, in this case slf4j-simple (which is, as the name implies, a very simple logging library that by default just logs to the console). Add a new dependency, with group id `org.slf4j`, artifact id `slf4j-simple`. Again we don't need to specify a version number: the RDF4J Bill of Materials already specifies which version of slf4j is compatible with RDF4J:

```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <scope>runtime</scope>
</dependency>
```

Once you have saved your changes to the POM, your project should be automatically refreshed by Eclipse again, and you should see a new section called 'Maven dependencies' in the Package Explorer (If your project does not automatically refresh and you don’t see the above section, right-click on your project in the Package Explorer, and select `Maven -> Update Project...`). This section, when opened, shows a list of jar files which can now be used in your project:

<a href="../images/eclipse-maven-deps.png" target="new"><img src="../images/eclipse-maven-deps.png" alt="Maven dependencies" class="img-responsive"/></a>

As you can see, this list contains not just all the RDF4J libraries, but also a lot of other libraries that RDF4J depends on.

{{< info >}}
If you are interested in having more control over which libraries are and aren't included, please see <a href="https://rdf4j.org/documentation/programming/setup/">Setting up your development environment</a>.
{{< / info >}}

## Configuring the Java compiler

Unfortunately the default Maven archetype that we used to create our new project uses a very old Java compiler (1.5) by default. Since RDF4J requires Java 8 at a minimum (we actually recommend Java 11 for better performance), we will need to change this.

Copy-paste (or type if you prefer) the following section into the xml file (put it just above the `dependencyManagement` section we added in earlier). This will set the version to Java 11.

```xml
<properties>
  <maven.compiler.source>11</maven.compiler.source>
  <maven.compiler.target>11</maven.compiler.target>
</properties>
```

Once you have done this, and saved your POM, you will need to manually update your Maven project for Eclipse to accept the changes. You do this by right-clicking on your project (in the project explorer), and then selecting 'Maven' -> 'Update Project…':

<a href="../images/eclipse-mvn-update.png" target="new"><img src="../images/eclipse-mvn-update.png" alt="Update Maven" class="img-responsive"/></a>

## Programming our first Semantic Web application

We're done preparing, we can start programming! Right-click on `src/main/java` and select `New -> Class`. In the dialog shown, set the package name to `org.example`, the class name to `HelloRDF4J`, and make sure the option `public static void main (String[] args)` is checked:

<a href="../images/eclipse-new-class.png" target="new"><img src="../images/eclipse-new-class.png" alt="Creating a new class" class="img-responsive"/></a>

Click 'Finish' and Eclipse will create the new class and automatically open it in an editor.

Since we will want to work with RDF data, we will start by creating something to keep that RDF data in. In RDF4J, RDF data is usually stored in a Repository. There many different types of Repository, both for local access as well as for accessing remote services. In this tutorial, we will create a simple local repository, namely one that uses an in-memory store, without any sort of persistent storage functionality (so the data will be lost when the program exits). Add the following code to your main method:

```java
Repository rep = new SailRepository(new MemoryStore());
```

Once you have done this and saved the file, you will notice some red lines appearing:

<a href="../images/eclipse-red-lines.png" target="new"><img src="../images/eclipse-red-lines.png" alt="Red lines in the editor" class="img-responsive"/></a>

This is Eclipse telling you that there is something wrong with your code. In this case, the problem is that several import statements are missing (note that Eclipse tells you what is wrong in detail as well in the 'Problems' tab underneath the editor). We'll need to add those import statements. You can add each import manually of course, but luckily Eclipse has a shortcut. Hit Ctrl+Shift+O and Eclipse should automatically resolve all missing imports for you. It will pop up a dialog if there is more than one possibility for a particular import, which will like happen for the `Repository` interface. Just pick the one from `org.eclipse.rdf4j`:

<a href="../images/eclipse-import-resolve.png" target="new"><img src="../images/eclipse-import-resolve.png" alt="Resolve imports" class="img-responsive"/></a>

Hit 'Finish', then save your code.

Now that we have created our repository, we are going to put some data in it, and then get that data out again and print it to the console.

Adding data can be done in numerous ways. In this tutorial, we will be creating a few RDF statements directly in Java (rather than, say, loading them from a file). To do this, we need a namespace that we can use to create any new IRIs we need. With this namespace, we can create a new IRI. We will create an identifier for a person called "John", about whom we will later add some data to our repository:

```java
Namespace ex = Values.namespace("ex", "http://example.org/");
IRI john = Values.iri(ex, "john");
```

We use a static factory(the `Values` class) to easily create new Namespaces, IRIs and other types of values.

We have now created a IRI, but have not yet added any data to our repository. To do this, we first need to open a RepositoryConnection. Such a connection allows us to add, remove, and retrieve data from our Repository. We do this as follows:

```java
RepositoryConnection conn = rep.getConnection();
```

It is important to keep track of open connections and to close them again when we are finished with them, to free up any resources the connection may keep hold of. RDF4J connections are "AutoCloseable", which means we can let Java handle the closing of the connection when we’re done with it, rather than having to deal with this ourselves. We use a [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) construction for this, like so:

```java
try (RepositoryConnection conn = rep.getConnection()) {
}
```

Your code should now look as follows:

<a href="../images/eclipse-code-1.png" target="new"><img src="../images/eclipse-code-1.png" alt="Java code 1" class="img-responsive"/></a>

Using the connection, we can start adding statements to our repository. We will add two triples, one to assert that John is a Person, and one to assert that John's name is "John":

```java
conn.add(john, RDF.TYPE, FOAF.PERSON);
conn.add(john, RDFS.LABEL, Values.literal("John"));
```

Note how for well-known RDF vocabularies, such as RDF, RDFS, and FOAF, RDF4J provides constants that you can easily reuse to create/read data with. Also, you see another use of the `Values` static factory here, this time to create a Literal object.

Once we have added our data, we can retrieve it back from the repository again. You can do this in several ways, with a SPARQL query or using the RepositoryConnection API methods directly. Here we will use the latter:

```java
RepositoryResult<Statement> statements = conn.getStatements(null, null, null);
```

We use the `getStatements()` method to retrieve data from the repository. The three arguments (all null) are for the subject, predicate, and object we wish to match. Since they are all set to null, this will retrieve all statements.

The object returned by `getStatements()` is a `RepositoryResult`, which implements both `Iteration` and `Iterable`: it allows you to process the result in a streaming fashion, using its `hasNext()` and `next()` methods, as well as using Java's for-each loop to iterate over it. Almost all the methods that retrieve data from a `Repository` return such a streaming/iterating object: they are very useful for processsing very large result sets, because they do not require that the entire result is kept in memory all at the same time.

It is important to always call `close()` on this kind of result object when you are done with them, to avoid memory leaks and other problems. Since the `RepositoryResult` is _also_ a `AutoCloseable`, you can use a try-with-resources construction (similar to what we used for opening the connection) to handle this.

However, since our `Repository` only contains 2 statements, we can safely just transfer the entire result into a Java Collection. RDF4J has the `Model` interface for this, which is an extension of the standard Java collections that has some special tricks up its sleeve for dealing with RDF data, but which you can also use in the same manner as a Set or a List. We convert our result to a Model as follows:

```java
Model model = QueryResults.asModel(statements);
```

The `QueryResults` utility retrieves all the statements from the supplied result, and puts them in a `Model`. It also automatically closes the result object for you.

Finally, we want to print out our data to the console. This too is something you can do in numerous ways, but let's just say we would like to print out our data in [Turtle](https://www.w3.org/TR/turtle/) format. Here's how:

```java
Rio.write(model, System.out, RDFFormat.TURTLE);
```

Rio (which stands for "RDF I/O") is the RDF4J parser/writer toolkit. We can just pass it our model, specify the outputstream, and the format in which we'd like it written, and Rio does the rest. Your code should now look like this:

<a href="../images/eclipse-code-2.png" target="new"><img src="../images/eclipse-code-2.png" alt="Java code 2" class="img-responsive"/></a>

You now have created your first Semantic Web application! After saving, just right-click on `HelloRDF4J.java` (in the project explorer) and select `Run as -> Java application`. You will see the following output in the Console:

```turtle
<http://example.org/john> a <http://xmlns.com/foaf/0.1/Person> ;
         <http://www.w3.org/2000/01/rdf-schema#label> "John" .
```

As you can see, it contains exactly the two statements that we added earlier, in Turtle syntax.

However, it is a bit ugly, using all those long IRIs. We can make the output a bit prettier, by adding some namespace definitions to our Model:

```java
model.setNamespace(RDF.NS);
model.setNamespace(RDFS.NS);
model.setNamespace(FOAF.NS);
model.setNamespace(ex);
```

Add these lines to your code, directly after where you have created the model. Then run your program again. You will now see the following:

<a href="../images/eclipse-code-3.png" target="new"><img src="../images/eclipse-code-3.png" alt="Java code 3" class="img-responsive"/></a>

That’s it! Obviously there is still loads to learn about how to use RDF4J effectively, but you’ve got the basics under control now: you can set up a new project using RDF4J,  and have seen some basic ways to add, write, and retrieve data. The rest is up to you. Good sources of further documentation are the [Getting Started tutorial](/documentation/tutorials/getting-started/), [Programming with RDF4J](/documentation/programming), and of course the [API Javadoc](/javadoc/latest).
