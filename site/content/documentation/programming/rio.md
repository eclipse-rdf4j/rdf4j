---
title: "Parsing and Writing RDF with Rio"
weight: 4
---
The RDF4J framework includes a set of parsers and writers for RDF called Rio. Rio (“RDF I/O”) is a toolkit that can be used independently from the rest of RDF4J. 
<!--more-->
In this chapter, we will take a look at various ways to use Rio to parse from or write to an RDF document. We will show how to do a simple parse and collect the results, how to count the number of triples in a file, how to convert a file from one syntax format to another, and how to dynamically create a parser for the correct syntax format.

If you use RDF4J via the Repository API, then typically you will not need to use the parsers directly: you simply supply the document (either via a URL, or as a File, InputStream or Reader object) to the RepositoryConnection and the parsing is all handled internally. However, sometimes you may want to parse an RDF document without immediately storing it in a triplestore. For those cases, you can use Rio directly.

# Listening to the parser

The Rio parsers all work with a set of Listener interfaces that they report results to: {{< javadoc "ParseErrorListener" "rio/ParseErrorListener.html" >}}, {{< javadoc "ParseLocationListener" "rio/ParseLocationListener.html" >}}, and {{< javadoc "RDFHandler" "rio/RDFHandler.html" >}}. Of these three, `RDFHandler` is the most interesting one: this is the listener that receives parsed RDF triples. So we will concentrate on this interface here.

The `RDFHandler` interface contains five methods: `startRDF`, `handleNamespace`, `handleComment`, `handleStatement`, and `endRDF`. Rio also provides a number of default implementations of RDFHandler, such as {{< javadoc "StatementCollector" "rio/helpers/StatementCollector.html" >}}, which stores all received RDF triples in a Java Collection. Depending on what you want to do with parsed statements, you can either reuse one of the existing RDFHandlers, or, if you have a specific task in mind, you can simply write your own implementation of RDFHandler. Here, I will show you some simple examples of things you can do with RDFHandlers.

## Parsing a file and collecting all triples

As a simple example of how to use Rio, we parse an RDF document and collect all the parsed statements in a Java Collection object (specifically, in a {{< javadoc "Model" "model/Model.html" >}} object).

Let’s say we have a Turtle file, available at `http://example.org/example.ttl`:

{{< highlight java >}}
java.net.URL documentUrl = new URL("http://example.org/example.ttl");
InputStream inputStream = documentUrl.openStream();
{{< / highlight >}}

We now have an open `InputStream` to our RDF file. Now we need a {{< javadoc "RDFParser" "rio/RDFParser.html" >}} object that reads this InputStream and creates RDF statements out of it. Since we are reading a Turtle file, we create a RDFParser object for the {{< javadoc "RDFFormat.TURTLE" "rio/RDFFormat.html" >}} syntax format:

{{< highlight java >}}
RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
{{< / highlight >}}

Note that all Rio classes and interfaces are in package `org.eclipse.rdf4j.rio` or one of its subpackages.

We also need an `RDFHandler` which can receive RDF statements from the parser.
Since we just want to create a collection of Statements for now, we’ll just use
Rio's `StatementCollector`:

{{< highlight java >}}
Model model = new LinkedHashModel();
rdfParser.setRDFHandler(new StatementCollector(model));
{{< / highlight >}}

Note, by the way, that you can use any standard Java Collection class (such as
`java.util.ArrayList` or `java.util.HashSet`) in place of the `Model` object, if you
prefer.

Finally, we need to set the parser to work:

{{< highlight java >}}
try {
   rdfParser.parse(inputStream, documentURL.toString());
}
catch (IOException e) {
  // handle IO problems (e.g. the file could not be read)
}
catch (RDFParseException e) {
  // handle unrecoverable parse error
}
catch (RDFHandlerException e) {
  // handle a problem encountered by the RDFHandler
}
finally {
  inputStream.close();
}
{{< / highlight >}}

After the `parse()` method has executed (and provided no exception has occurred), the collection model will be filled by the StatementCollector. As an aside: you do not have to provide the StatementCollector with a list in advance, you can also use an empty constructor and then just get the collection, using `StatementCollector.getStatements()`.

The {{< javadoc "Rio" "rio/Rio.html" >}} utility class provides additional helper methods, to make parsing to a `Model` a single API call:

{{< highlight java >}}
Model results = Rio.parse(inputStream, documentUrl.toString(), RDFFormat.TURTLE);
{{< / highlight >}}


## Iterating through all the triples in a file

RDF files can also be parsed in a background thread and iterated through as a query result. This allows files that are too big to fit into memory (at once) to be parsed sequentially using a familiar API (specifically, the {{< javadoc "GraphQueryResult" "query/GraphQueryResult.html" >}} interface).

Using the same Turtle file from above:

{{< highlight java >}}
java.net.URL documentUrl = new URL("http://example.org/example.ttl");
InputStream inputStream = documentUrl.openStream();
{{< / highlight >}}

We now have an open `InputStream` to our RDF file. Instead of using a parser directly, we can use the {{< javadoc "QueryResults.parseGraphBackground()" "query/QueryResults.html" >}} function:

{{< highlight java >}}
String baseURI = documentUrl.toString();
RDFFormat format = RDFFormat.TURTLE;
try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
  while (res.hasNext()) {
    Statement st = res.next();
    // ... do something with the resulting statement here.
  }
}
catch (RDF4JException e) {
  // handle unrecoverable error
}
finally {
  inputStream.close();
}
{{< / highlight >}}

## Using your own RDFHandler: counting statements

Suppose you want to count the number of triples in an RDF file. You could of
course parse the file, add all triples to a Collection, and then check the size
of that Collection. However, this will get you into trouble when you are
parsing very large RDF files: you might run out of memory. And in any case:
creating and storing all these Statement objects just to be able to count them
seems a bit of a waste. So instead, we will create our own RDFHandler
implementation, which just counts the parsed RDF statements and then
immediately throws them away.

To create your own handler, you can of course create a class that implements
the `RDFHandler` interface, but a useful shortcut is to instead create a subclass
of {{< javadoc "AbstractRDFHandler" "rio/helpers/AbstractRDFHandler.html" >}}. This is a base class that provides dummy implementations
of all interface methods. The advantage is that you only have to override the
methods in which you need to do something. Since what we want to do is just
count statements, we only need to override the `handleStatement` method.
Additionaly, we of course need a way to get back the total number of statements
found by our counter:

{{< highlight java >}}
class StatementCounter extends AbstractRDFHandler {

  private int countedStatements = 0;

  @Override
  public void handleStatement(Statement st) {
     countedStatements++;
  }

 public int getCountedStatements() {
   return countedStatements;
 }
}
{{< / highlight >}}

Once we have our custom `RDFHandler` class, we can supply that to the parser
instead of the `StatementCollector` we saw earlier:

{{< highlight java >}}
StatementCounter myCounter = new StatementCounter();
rdfParser.setRDFHandler(myCounter);
try {
   rdfParser.parse(inputStream, documentURL.toString());
}
catch (Exception e) {
  // oh no!
}
finally {
  inputStream.close();
}
int numberOfStatements = myCounter.getCountedStatements();
{{< / highlight >}}

# Detecting the file format

In the examples sofar, we have always assumed that you know what the syntax
format of your input file is: we assumed Turtle syntax and created a new parser
using {{< javadoc "RDFFormat.TURTLE" "rio/RDFFormat.html" >}}. However, you may not always know in advance what
exact format the RDF file is in. What then? Fortunately, Rio has a couple of useful features to help you.

The {{< javadoc "Rio" "rio/Rio.html" >}} utility class has a couple of methods for guessing the correct format,
given either a filename or a MIME-type. For example, to get back the RDF format
for our Turtle file, we could do the following:

{{< highlight java >}}
RDFFormat format = Rio.getParserFormatForFileName(documentURL.toString()).orElse(RDFFormat.RDFXML);
{{< / highlight >}}

This will guess, based on the name of the file, that it is a Turtle file and
return the correct format. We can then use that with the Rio class to create
the correct parser dynamically.

Note the `.orElse(RDFFormat.RDFXML)` bit at the end: if Rio can not guess the
parser format based on the file name, it will simply return `RDFFormat.RDFXML` as
a default value. Of course if setting a default value makes no sense, you could
also choose to return null or even to throw an exception - that’s up to you.

Once we have the format determined, we can create a parser for it like so:

{{< highlight java >}}
RDFParser rdfParser = Rio.createParser(format);
{{< / highlight >}}

As you can see, we still have the same result: we have created an `RDFParser`
object which we can use to parse our file, but now we have not made the
explicit assumption that the input file is in Turtle format: if we would later
use the same code with a different file (say, a .owl file – which is in RDF/XML
format), our program would be able to detect the format at runtime and create
the correct parser for it.

# Writing RDF

Sofar, we’ve seen how to read RDF, but Rio of course also allows you to write
RDF, using {{< javadoc "RDFWriter" "rio/RDFWriter.html" >}}s, which are a subclass of `RDFHandler` that is intended for
writing RDF in a specific syntax format.

As an example, we start with a `Model` containing several RDF statements, and we
want to write these statements to a file. In this example, we’ll write our
statements to a file in RDF/XML syntax:

{{< highlight java >}}
Model model; // a collection of several RDF statements
FileOutputStream out = new FileOutputStream("/path/to/file.rdf");
RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, out);
try {
  writer.startRDF();
  for (Statement st: model) {
    writer.handleStatement(st);
  }
  writer.endRDF();
}
catch (RDFHandlerException e) {
 // oh no, do something!
}
finally {
  out.close();
}
{{< / highlight >}}

Again, the `Rio` helper class provides convenience methods which you can use to
make this a one step process. If the collection is a `Model` and the desired
output format supports namespaces, then the namespaces from the model will also
be serialised.

{{< highlight java >}}
Model model; // a collection of several RDF statements
FileOutputStream out = new FileOutputStream("/path/to/file.rdf")
try {
  Rio.write(model, out, RDFFormat.RDFXML);
}
finally {
  out.close();
}
{{< / highlight >}}

Since we have now seen how to read RDF using a parser and how to write using a
writer, we can now convert RDF files from one syntax to another, simply by
using a parser for the input syntax, collecting the statements, and then
writing them again using a writer for the intended output syntax. However, you
may notice that this approach may be problematic for very large files: we are
collecting all statements into main memory (in a `Model` object).

Fortunately, there is a shortcut. We can eliminate the need for using a Model
altogether. If you’ve paid attention, you might have spotted it already:
`RDFWriter`s are also `RDFHandler`s. So instead of first using a `StatementCollector`
to collect our RDF data and then writing that to our `RDFWriter`, we can 
use the RDFWriter directly. So if we want to convert our input RDF file from
Turtle syntax to RDF/XML syntax, we can do that, like so:

{{< highlight java >}}
// open our input document
java.net.URL documentUrl = new URL(“http://example.org/example.ttl”);
InputStream inputStream = documentUrl.openStream();
// create a parser for Turtle and a writer for RDF/XML
RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
RDFWriter rdfWriter = Rio.createWriter(RDFFormat.RDFXML,
			   new FileOutputStream("/path/to/example-output.rdf");

// link our parser to our writer...
rdfParser.setRDFHandler(rdfWriter);
// ...and start the conversion!
try {
   rdfParser.parse(inputStream, documentURL.toString());
}
catch (IOException e) {
  // handle IO problems (e.g. the file could not be read)
}
catch (RDFParseException e) {
  // handle unrecoverable parse error
}
catch (RDFHandlerException e) {
  // handle a problem encountered by the RDFHandler
}
finally {
  inputStream.close();
}
{{< / highlight >}}

# Configuring the parser / writer

The Rio parsers and writers have several configuration options, allowing you to
tweak their behavior. The configuration of a Rio parser/writer can be modified
in two ways: programmatically, or by specifying Java system properties
(typically done by passing `-D` command line flag).

The available configuration options are available via several helper classes,
listed in the Javadoc documentation:

- {{< javadoc "BasicParserSettings" "rio/helpers/BasicParserSettings.html" >}}
  and {{< javadoc "BasicWriterSettings" "rio/helpers/BasicWriterSettings.html" >}} 
  contains various parser/writers settings that can be used with most Rio parsers/writers. This includes things
  such a IRI syntax validation, datatype verification/normalisation, and
  various other general options;
- {{< javadoc "JSONSettings" "rio/helpers/JSONSettings.html" >}} has configuration options related to JSON-based formats such as JSON-LD and RDF/JSON;
- {{< javadoc "JSONLDSettings" "rio/helpers/JSONLDSettings.html" >}} has configuration options specific to JSON-LD writing;
- {{< javadoc "NTriplesParserSettings" "rio/helpers/NTriplesParserSettings.html" >}} and {{< javadoc "NTriplesWriterSettings" "rio/helpers/NTriplesWriterSettings.html" >}} have additional settings specific to the N-Triples and N-Quads formats;
- {{< javadoc "TurtleParserSettings" "rio/helpers/TurtleParserSettings.html" >}} and {{< javadoc "TurtleWriterSettings" "rio/helpers/TurtleWriterSettings.html" >}} have additional settings
  specific for the Turtle and TriG formats;
- {{< javadoc "XMLParserSettings" "rio/helpers/XMLParserSettings.html" >}} and {{< javadoc "XMLWriterSettings" "rio/helpers/XMLWriterSettings.html" >}} have configuration options specific to XML-based parsing and writing.

The Javadoc documentation shows which settings are available, and what their system property keys and their default values are.

## Programmatic configuration

The Rio parser/writer configuration can be retrieved and modified via `RDFParser.getParserConfig()` / `RDFWriter.getWriterConfig()`. This returns a {{< javadoc "RioConfig" "rio/RioConfig.html" >}} object, which is a collection for the various supported parser/writer settings. Each configuration option can be added to this object with a value of choice.

Each Rio parser/writer can be queried about which settings it supports, using `RDFParser.getSupportedSettings()` / `RDFWriter.getSupportedSettings()`.

Lastly, the `RioConfig` also allows you to mark certain types of error as "non-fatal". Non-fatal errors are still reported by Rio, but will not abort file processing.

Some examples follow:

### Example: IRI syntax validation

By default the Rio parsers validate IRI syntax and produce a fatal error if an IRI can not be parsed. You can disable this as follows:

{{< highlight java >}}
RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
{{< / highlight >}}

If you want to make Rio still report syntax errors, but continue processing the file, you can do so as follows:

{{< highlight java >}}
RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
rdfParser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_URI_SYNTAX);
{{< / highlight >}}

### Example: blank node preservation

If you want to preserve blank node identifiers as found in the source file (by default the parser creates new identifiers to ensure uniqueness across multiple files), you can reconfigure the parser as follows:

{{< highlight java >}}
RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
rdfParser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
{{< / highlight >}}

## Configuration via command line switches

To allow reconfiguring a Rio parser/writer in a runtime deployment (for example in an Rdf4j Server), it is also possible to set certain configuration options through Java system properties. You can specify these by passing `-D` commandline switches to the JRE in which the application runs.

The Javadoc for each parser/writer setting documents the system property name by which it can be reconfigured. For example, `BasicParserSettings.VERIFY_LANGUAGE_TAGS` (which determines if Rio verifies that language tags are standards-compliant) can be disabled by using the following command line switch:

    -Dorg.eclipse.rdf4j.rio.verify_language_tags=false

## Some notes on parsing RDF/XML and JAXP limits

The Rio RDF/XML parser uses the [Java API for XML Processing
(JAXP)](https://www.oracle.com/technetwork/java/intro-140052.html) to process
XML data. Check the [documentation on
limit definitions](https://docs.oracle.com/javase/tutorial/jaxp/limits/limits.html) and
using the `jaxp.properties` file if you get one of the following errors:

- JAXP00010001: The parser has encountered more than "64000" entity expansions in this document
- JAXP00010004: The accumulated size of entities is ... that exceeded the "50,000,000" limit

To disable these limits, you can pass `-Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0` to the JVM.

If you have Apache Xerces on the classpath, it replaces the default JDK XML
parser. Unfortunately, Apache Xerces at the time of writing does not fully
implement the JAXP limit definitions, and the above fix will not work with
Xerces. If you run into this issue, we recommend removing Xerces from the
runtime classpath and relying on the default JDK XML processor instead.
