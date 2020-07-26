---
title: "Starting a new Maven project in Eclipse"
weight: 2
toc: true
---
If you are new to RDF4J, or to tools like Eclipse IDE or Apache Maven, this tutorial will help you get started.
<!--more-->

NOTE: using Maven or Eclipse is not required if you want to use RDF4J. These are simply very useful tools for quickly getting a Java project started. Maven is good because it allows you to just define which libraries you want to use and never worry about any further third-party libraries you might need, and Eclipse IDE is good because it has good integration with Maven, code completion features, and is just generally a great Java development environment.

In this tutorial, I assume that you have a basic understanding of programming in Java, and have at least an inkling of what RDF is. However, I do not assume that you know how to use either RDF4J, Maven, or Eclipse, so we’ll go through it all one step at a time. If anything is already sufficiently familiar to you, you are of course free to skip ahead!

## Setting up your environment

Before we kick off, you will need to install the [Eclipse IDE](http://eclipse.org/). In this tutorial, we will use Eclipse for Java EE Developers version 4.5.2 (Mars.2), but it shouldn’t matter too much which exact version you have installed. Select either the “Eclipse for Java developers” or “Eclipse for Java EE developers” installation option.

Eclipse IDE comes with a Maven plugin (called M2Eclipse) already installed. We will use this plugin to work with Maven. You are of course free to also install the Maven command line tools, but we won’t be using those directly in this tutorial.

When starting Eclipse for the first time, you will be asked for a workspace directory – this will be where Eclipse stores all project data as well as some configuration information. Feel free to pick/create a directory of your choice, or just accept the default.

Once Eclipse is started (and any welcome messages have been closed), you will be presented with a screen that should look roughly like this:

![Image](../images/eclipse-empty.png)

### Tweaking Eclipse preferences

Before we start creating our actual project in Eclipse, there are a few preferences that we should tweak. This step is not required, but it will make things a little easier in the rest of this tutorial.

From the menu, select `Eclipse -> Preferences`. The Preferences dialog pops up. On the left, select `Maven`. You will see the Maven configuration settings. The options “Download Artifact Sources” and “Download Artifact JavaDoc” are unchecked by default. Check them both, then click OK.

![Image](../images/eclipse-mvn-prefs.png)

The reason we want this, by the way, is that having either the sources or the Javadoc available really helps when you use code autocompletion in Eclipse – Eclipse will automatically read the Javadoc and provide the documentation in a little tooltip. As said: not required, but very useful.

## Creating a new project

Now that we’re all set up, we can kick things off by creating a new project. Select the File menu, then `New -> New Project...` . A dialog will appear. In this dialog, select the option `Maven Project`: 

![Image](../images/eclipse-create-project-mvn.png)

Once you click Next, you will be presented with a screen to create a new Maven Project. Make you sure the option ‘Create a simple project’ is checked:

![Image](../images/eclipse-create-simple.png)

Click Next again. In the following screen you define further details of your Maven project, such as group and artifact ids, version number, and so on. For this tutorial,  you only have to fill in three fields:

- `group id` – typically you use something like a Java package name for this. We will use `org.example`.
- `artifact id` –  name of the maven artifact if you publish our project as one. We will use `rdf4j-getting-started`.
- `name` – the project name. We will use `HelloRDF4J` .

![Image](../images/eclipse-create-rdf4j.png)

Once this has been filled in you can click Finish. We now have our first Eclipse project, huzzah!

## Defining the POM

So we have a project. That’s great, but it’s still empty: we haven’t added any code or necessary libraries (such as the RDF4J libraries) yet.

We will remedy the library-situation first. In Maven, requirements for libraries (we’re talking about jar files here) are specified in a special project configuration file called pom.xml, often referred to as “the POM”. Open your project and you will see that a file with that name is already present. Doubleclick it to open it:

![Image](../images/eclipse-pom.png)

As you can see, the POM already contains some information – in fact the information that we added when we created our project. However, we need to add more information about our project. Specifically, we want to add which external Java libraries (jar files) we want to include in our project. In Maven, you do this by specifying dependencies. Switch to the Dependencies tab, and click on the Add button to the right of the ‘Dependencies’ box.

The first dependency we will add is for RDF4J itself. RDF4J is not a single library, but consists of a large collection of separate modules, allowing you to pick and choose which functionality you need. For this tutorial, however, we’ll be lazy and just add the entire core framework. We do this by adding a dependency with group id `org.eclipse.rdf4j` and with artifact id `rdf4j-storage`. The version number should of course be for the version of RDF4J you want to use – in this tutorial we’ll use version 2.0M2:

![Image](../images/eclipse-rdf4j-dep.png)

Ater you have clicked OK, we repeat the exercise for a second dependency we require, namely for a logging framework. RDF4J uses the SLF4J logging API, which requires a logging implementation, so we’ll provide one, in this case logback. Add a new dependency, with group id `ch.qos.logback`, artifact id `logback-classic`, and version 1.0.13.

Once you have clicked OK and saved your changes to the POM, your project should be automatically refreshed by Eclipse, and you should see a new section called ‘Maven dependencies’ in the Package Explorer. This section, when opened, shows a list of jar files which can now be used in your project:

![Image](../images/eclipse-maven-deps.png)

If your project does not automatically refresh and you don’t see the above section, right-click on your project in the Package Explorer, and select `Maven -> Update Project...`.

## Configuring the Java compiler

Unfortunately the default Maven archetype that we used to create our new project uses a very old Java compiler (1.5) by default. Since RDF4J requires Java 8, we will need to change this.

The simplest way to do this is to switch to the ‘pom.xml’ tab in the POM editor:

![Image](../images/eclipse-pom-xml.png)

Copy-paste (or type if you prefer) the following section into the xml file, underneath the closing </dependencies> element:

```xml
<build>
    <plugins>
	<plugin>
	    <groupId>org.apache.maven.plugins</groupId>
	    <artifactId>maven-compiler-plugin</artifactId>
	    <version>3.5.1</version>
	    <configuration>
		<source>1.8</source>
		<target>1.8</target>
		<encoding>utf8</encoding>
	    </configuration>
	</plugin>
    </plugins>
</build>
```

Once you have done this, and saved your POM, you will need to manually update your Maven project for Eclipse to accept the changes. You do this by right-clicking on your project (in the project explorer), and then selecting ‘Maven’ -> ‘Update Project…’:

![Image](../images/eclipse-mvn-update.png)

## Configuring the logger

One last configuration step before we can get to actual programming: we need to provide our logger with a configuration file. Although this is strictly speaking not required, it is a good idea to do this, as it will make your program’s output easier to read and will allow you to more easily change your logging configuration later on.

We will create a new logger configuration file. Select `src/main/resources` in the Project Explorer, right-click, and select `New -> File`. Name the new file `logback.xml`.

![Image](../images/eclipse-create-file.png)

Once you have created the (empty) file, add the following XML to it:

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
      <encoder>
	 <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n </pattern> 
      </encoder>
  </appender>
  <root level="info"><appender-ref ref="STDOUT" /></root>
</configuration>
```

This configures a logger that prints its messages to the console/standard output, and sets the logging level to ‘info’. The details are not that important right now, but this at least makes sure that we do not get overwhelmed with loads of debug log messages when running our program, later on.

## Programming our first Semantic Web application

We’re done preparing, we can start programming! Right-click on `src/main/java` and select `New -> Class`. In the dialog shown, set the package name to `org.example`, the class name to `HelloRDF4J`, and make sure the option `public static void main (String[] args)` is checked:

![Image](../images/eclipse-new-class.png)

Click ‘Finish’ and Eclipse will create the new class and automatically open it in an editor.

Since we will want to work with RDF data, we will start by creating something to keep that RDF data in. In RDF4J, RDF data is usually stored in a Repository. There many different types of Repository, both for local access as well as for accessing remote services. In this tutorial, we will create a simple local repository, namely one that uses an in-memory store, without any sort of persistent storage functionality (so the data will be lost when the program exits). Add the following code to your main method:

```java
Repository rep = new SailRepository(new MemoryStore());
```

Once you have done this, you will notice some red lines appearing:

![Image](../images/eclipse-red-lines.png)

This is Eclipse telling you that there is something wrong with your code. In this case, the problem is that several import statements are missing, so we’ll need to add those. You can add each import manually of course, but luckily Eclipse has a shortcut. Hit Ctrl-Shift-O and Eclipse should automatically resolve all missing imports for you (it will pop up a dialog if there is more than one possibility for a particular import).

Now that we have created our repository, we should initialize it. This is done by calling the `rep.initialize()` method (or `rep.init()` in recent RDF4J versions). Notice how, when you start typing, Eclipse shows autocompletion candidates for the methods available and how each method is described in the box:

![Image](../images/eclipse-autocomplete-1.png)

Now that we have created and initialized our repository, we are going to put some data in it, and then get that data out again and print it to the console.

Adding data can be done in numerous ways. In this tutorial, we will be creating a few RDF statements directly in Java (rather than, say, loading them from a file). To do this, we need some ingredients: a namespace that we can use to create any new IRIs we need, and a ValueFactory, which we can use to create IRI, BNode, and Literal objects:

```java
String namespace = "http://example.org/"; 
ValueFactory f = rep.getValueFactory();
```

With these ingredients, we can create a new IRI. We will create an identifier for a person called “John”, about whom we will later add some data to our repository:

```java
IRI john = f.createIRI(namespace, "john");
```

We have now created a IRI, but have not yet added any data to our repository. To do this, we first need to open a RepositoryConnection. Such a connection allows us to add, remove, and retrieve data from our Repository. We do this as follows:

```java
RepositoryConnection conn = rep.getConnection();
```

It is important to keep track of open connections and to close them again when we are finished with them, to free up any resources the connection may keep hold of. RDF4J connections are “AutoCloseable”, which means we can let Java handle the closing of the connection when we’re done with it, rather than having to deal with this ourselves. We use a try-with-resources construction for this, like so:
   
```java
try (RepositoryConnection conn = rep.getConnection()) {
}
```

Your code should now look as follows:

![Image](../images/eclipse-code-1.png)

Using the connection, we can start adding statements to our repository. We will add two triples, one to assert that John is a Person, and one to assert that John’s name is “John”:

```java
conn.add(john, RDF.TYPE, FOAF.PERSON); 
conn.add(john, RDFS.LABEL, f.createLiteral("John"));
```

Note how for well-known RDF vocabularies, such as RDF, RDFS, and FOAF, RDF4J provides constants that you can easily reuse to create/read data with. Also, you see another use of the ValueFactory here, this time to create a Literal object.

Once we have added our data, we can retrieve it back from the repository again. You can do this in several ways, with a SPARQL query or using the RepositoryConnection API methods directly. Here we will use the latter:

```java
RepositoryResult<Statement> statements = conn.getStatements(null, null, null);
```

We use the `getStatements()` method to retrieve data from the repository. The three arguments (all null) are for the subject, predicate, and object we wish to match. Since they are all set to null, this will retrieve all statements.

The object returned by `getStatements()` is a `RepositoryResult`, which is a type of `Iteration`: it allows you to process the result in a streaming fashion, using its `hasNext()` and `next()` methods. Almost all the methods that retrieve data from a `Repository` return a type of `Iteration`: they are very useful for processsing very large result sets, because they do not require that the entire result is kept in memory all at the same time.

However, since our `Repository` only contains 2 statements, we can safely just transfer the entire result into a Java Collection. RDF4J has the `Model` interface for this, which is an extension of the standard Java collections that has some special tricks up its sleeve for dealing with RDF data, but which you can also use in the same manner as a Set or a List. We convert our result to a Model as follows:

```java
Model model = QueryResults.asModel(statements);
```

This retrieves all the statements from the supplied `Iteration`, and puts them in a `Model`. It also closes the `Iteration` for you.

Finally, we want to print out our data to the console. This too is something you can do in numerous ways, but let’s just say we would like to print out our data in Turtle format. Here’s how:

```java
Rio.write(model, System.out, RDFFormat.TURTLE);
```

Rio (which stands for “RDF I/O”) is the RDF4J parser/writer toolkit. We can just pass it our model, specify the outputstream, and the format in which we’d like it sent, and Rio does the rest. Your code should now look like this:

![Image](../images/eclipse-app2.png)

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
model.setNamespace("ex", namespace);
```

Add these lines to your code, directly after where you have created the model. Then run your program again. You will now see the following:

![Image](../images/eclipse-app3.png)

That’s it! Obviously there is still loads to learn about how to use RDF4J effectively, but you’ve got the basics under control now: you can set up a new project using RDF4J,  and have seen some basic ways to add, write, and retrieve data. The rest is up to you. Good sources of further documentation are [Programming with RDF4J](../programming), and of course the [API Javadoc](/javadoc/latest).
