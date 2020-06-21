---
title: "Getting started with RDF4J"
toc: true
weight: 1
---

In this tutorial, we go through the basics of what RDF is, and we show how you can use the Eclipse RDF4J framework to create, process, store, and query RDF data. 
<!--more-->

We assume that you know a little about programming in Java, but no prior knowledge on RDF is assumed.

TIP: The code examples in this tutorial are available for download from the [examples directory in the rdf4j-doc GitHub repository](https://github.com/eclipse/rdf4j-doc/tree/master/examples). We encourage you to download these examples and play around with them. The easiest way to do this is to download the GitHub repository in your favorite Java IDE as an Apache Maven project. 

# Introducing RDF

The [Resource Description Framework (RDF)](https://www.w3.org/TR/rdf11-primer/) is a standard (or more accurately, a "recommendation") formulated by the [World Wide Web Consortium (W3C)](https://www.w3.org/). The purpose of RDF is to provide a framework for expressing information about resources in a machine-processable, interoperable fashion.

A _resource_ can be anything that we can stick an identifier on: a web page, an image, but also more abstract/real-world things like you, me, the concept of "world peace", the number 42, and that library book you never returned. 

RDF is intended for modeling information that needs to processed by applications, rather than just being shown to people. 

In this tutorial, we will be modeling information about artists . Let's start with a simple fact: "Picasso's first name is Pablo". In RDF, this could be expressed as follows:

![Example 1](../images/rdf-graph-1.png)

So what exactly are we looking at here? Well, we have a _resource_ "Picasso", denoted by an IRI (Internationalized _Resource Identifier_): `http://example.org/Picasso`. In RDF, resources have _properties_. Here we are using the `foaf:firstName` property to denote the relation between the resource "Picasso" and the value "Pablo". `foaf:firstName` is also an IRI, though to make things easier to read we use an abbreviated syntax, called _prefixed names_ (more about this later). Finally, the property value, "Pablo", is a _literal_ value: it is not represented using a resource identifier, but simply as a string of characters.

NOTE: the `foaf:firstName` property is part of the FOAF (Friend-of-a-Friend) vocabulary. This is an example of reusing an existing vocabuary to describe our own data. After all, if someone else already defined a property for describing people's first names, why not use it? More about this later.

As you may have noticed, we have depicted our fact about Picasso as a simple *graph*: two nodes, connected by an edge. It is very helpful to think about RDF models as graphs, and a lot of the tools we will be using to create and query RDF data make a lot more sense if you do.

In RDF, each fact is called a *statement*. Each statement consists of three parts (for this reason, it is also often called a _triple_):

* the _subject_ is the starting node of the statement, representing the resource that the fact is "about";
* the _predicate_ is the property that denotes the edge between two nodes;
* the _object_ is the end node of the statement, representing the resource or literal that is the _property value_.

Let's expand our example slightly: we don't just have a single statement about Picasso, we know another fact as well: "Picasso is an artist". We can extend our RDF model as follows:

![Image](../images/rdf-graph-2.png)

Notice how the second statement was added to our graph depiction by simply adding a second edge to an already existing node , labeled with the `rdf:type` property, and the value `ex:Artist`. As you continue to add new facts to your data model, nodes and edges continue to be added to the graph.

## IRIs, namespaces, and prefixed names

IRIs are at the core of what makes RDF powerful. They provide a mechanism that allows global identification of any resource: no matter who authors a dataset or where that data is physically stored, if that data shares an identical IRI with another dataset you know that both datasets are talking about the same thing. 

In many RDF data sets, you will see IRIs that start with 'http://...'. This does not necessarily mean that you can open this link in your browser and get anything meaningful, though. Quite often, IRIs are merely used as unique identifiers, and not as actual addresses. Some RDF sources _do_ make sure that their IRIs can be looked up on the Web, and that you actually get back data (in RDF) that describes the resource identified by the IRI. This is known as a [Linked Data](http://linkeddata.org/) architecture. The ins and outs of Linked Data are beyond the scope of this tutorial, but it's worth exploring once you understand the basics of RDF.

You will often see IRIs in abbreviated form whenever you encounter examples of RDF data: `<prefix>:<name>` This abbreviated form, known as "prefixed names", has no impact on the meaning of the data, but it makes it easier for people to read the data. 

Prefixed names work by defining a _prefix_ that is a replacement for a _namespace_. A namespace is the first part of an IRI that is shared by several resources. For example, the IRIs `http://example.org/Picasso`, `http://example.org/Rodin`, and `http://example.org/Rembrandt` all share the the namespace `http://example.org/`. By defining a new prefix `ex` as the abbreviation for this namespace, we can use the string `ex:Picasso` instead of its full IRI.

## Creating and reusing IRIs

In the running example for this tutorial, we use a namespace prefix `http://example.org/` that we indiscriminately use for various resource and property IRIs we want to use. In a real world scenario, that is not very practical: we don't own the domain 'example.org', for one thing, and moreover it is not very descriptive of what our resources actually are about.

So, how _do_ you pick good IRIs for your resources and properties? There's a lot to be said about this topic, some of it beyond the scope of this tutorial. You should at least keep the following in mind:

1. use a domain name that _you_ own for your own resources. Don't reuse other people's domain, and don't add new resources or properties to existing vocabularies.
2. try and _reuse_ existing vocabularies. Instead of creating new resources and relations to describe all your data, see if somebody else has already published a collection of IRIs (known as a _vocabulary_, or sometimes an _ontology_) that describes the same kind of things you want to describe. Then use _their_ IRIs as part of your own data.

There are several major benefits to reusing existing vocabulary:

* you don't have to reinvent the wheel;
* when the time comes to share your data with a third party, chances are that they _also_ reuse
the existing vocabulary, making data integration easier.

Of course we can't list every possible reusable RDF vocabulary here, but there are several very generic RDF vocabularies that get reused very often:

* [RDF Schema (RDFS)](https://www.w3.org/TR/rdf-schema/) - the RDF Schema vocabulary provides some basic properties and resources that you can use to create class hierarchies, define your own properties in more detail, and so on. One commonly used property from RDFS is `rdfs:label`, which is used to give a resource a human-readable name, as a string value.
* [Web Ontology Language (OWL)](https://www.w3.org/TR/owl2-overview/) - the Web Ontology Language OWL provides an extensive and powerful (but also quite complex) set of resources and properties that can be used to model complex domain models, a.k.a. _ontologies_. It can be used to say things like "this class of things here is exactly the same as that class over there" or "resources of type `BlueCar` must have a property `Color` with value "Blue". Learning about OWL goes beyond the scope of this tutorial. 
* [Simple Knowledge Organization System (SKOS)](https://www.w3.org/TR/skos-primer/) provides a model for expressing the basic structure and content of concept schemes such as thesauri, classification schemes, subject heading lists, taxonomies, folksonomies, and other similar types of controlled vocabulary. It has properties such as `skos:broader`, `skos:narrower` (to indicate that one term is a broader/narrower term than some other term), `skos:prefLabel`, `skos:altLabel` (to give preferred and alternative names for concepts), and more.
* [Friend-Of-A-Friend (FOAF)](http://xmlns.com/foaf/spec/) - the FOAF vocabulary provides resources and properties to model people and their social networks. You can use it to say that some resource describes a `foaf:Person`, and you can use properties such as `foaf:firstName`, `foaf:surname`, `foaf:mbox` to describe all sorts of data about that person.
* [Dublin Core (DC) Elements](http://dublincore.org/documents/dces/) - the Dublin Core Metadata Initiative (DCMI) has a defined a vocabulary of 15 commonly used properties for describing resources from a library/digital archiving perspective. It includes properties such as `dc:creator` (to indicate the creator of a work), `dc:subject`, `dc:title`, and more.

The flexibility of RDF makes it easy to mix and match models as you need them. You will, in practice, often see RDF data sets that have some "home-grown" IRIs, combined with properties and class names from a variety of different other vocabularies. It's not uncommon to see 3 or more different vocabularies all reused in the same dataset.

# Using RDF4J to create RDF models

Enough background, let's get our hands dirty.

Eclipse RDF4J is a Java API for RDF: it allows you to create, parse, write, store, query and reason with RDF data in a highly scalable manner. So let's see two examples of how we can use RDF4J to create the above RDF model in Java.

## Example 01: building a simple Model 

{{< example "Example 01" "model/Example01BuildModel.java" >}} shows how we can create the RDF model we introduced above using RDF4J:

```java {linenos=inline}
// We use a ValueFactory to create the building blocks of our RDF statements: 
// IRIs, blank nodes and literals.
ValueFactory vf = SimpleValueFactory.getInstance();

// We want to reuse this namespace when creating several building blocks.
String ex = "http://example.org/";

// Create IRIs for the resources we want to add.
IRI picasso = vf.createIRI(ex, "Picasso");
IRI artist = vf.createIRI(ex, "Artist");

// Create a new, empty Model object.
Model model = new TreeModel();

// add our first statement: Picasso is an Artist
model.add(picasso, RDF.TYPE, artist);

// second statement: Picasso's first name is "Pablo".
model.add(picasso, FOAF.FIRST_NAME, vf.createLiteral("Pablo"));
```

Let's take a closer look at this. Lines 1-10 are necessary preparation: we use a {{< javadoc "ValueFactory" "model/ValueFactory.html" >}} to create resources , which we will later use to add facts to our model. 

On line 13, we create a new, empty model. RDF4J comes with several {{< javadoc "Model" "model/Model.html" >}} implementations, the ones you will most commonly encounter are {{< javadoc "TreeModel" "model/impl/TreeModel.html" >}} and {{< javadoc "LinkedHashModel" "model/impl/LinkedHashModel.html" >}}. The difference is in how they index data internally - which has a performance impact when working with very large models. For our purposes however, it doesn't really matter which implementation you use.

On lines 16 and 19, we add our two facts that we know about Picasso: that's he's an artist, and that his first name is "Pablo". 

In RDF4J, a {{< javadoc "Model" "model/Model.html" >}} is simply an in-memory collection of RDF statements. We can add statements to an existing model, remove statements from it, and of course iterate over the model to do things with its contents. As an example, let's iterate over all statements in our Model using a `for-each` loop, and print them to the screen:

```java
for (Statement statement: model) {
    System.out.println(statement);
}
```

Or, even shorter:

```java
model.forEach(System.out::println);
```

When you run this, the output will look something like this:

    (http://example.org/Picasso, http://xmlns.com/foaf/0.1/firstName, "Pablo"^^<http://www.w3.org/2001/XMLSchema#string>) [null]
    (http://example.org/Picasso, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://example.org/art/Artist) [null]

Not very pretty perhaps, but at least you should be able to recognize the RDF statements that we originally added to our model. Each line is a single statement, with the subject, predicate, and object value in comma-separated form. The `[null]` behind each statement is a context identifier or named graph identifier, which you can safely ignore for now. The bit `^^<http://www.w3.org/2001/XMLSchema#string>` is a _datatype_ that RDF4J assigned to the literal value we added (in this case, the datatype is simply string). 

## Example 02: using the ModelBuilder

The previous code example shows that you need to do a bit of preparation before actually adding anything to your model: defining common namespaces, creating a ValueFactory, creating IRIs, etc. As a convenience, RDF4J provides a {{< javadoc "ModelBuilder" "model/util/ModelBuilder.html" >}} that simplifies things.

{{< example "Example 02" "model/Example02BuildModel.java" >}} shows how we can create the exact same model using a ModelBuilder:

```java {linenos=inline}
ModelBuilder builder = new ModelBuilder();
Model model = builder
                  .setNamespace("ex", "http://example.org/")
		  .subject("ex:Picasso")
		       .add(RDF.TYPE, "ex:Artist")
		       .add(FOAF.FIRST_NAME, "Pablo")
		  .build();
```

The above bit of code creates the exact same model that we saw in the previous example, but with far less prep code. ModelBuilder accepts IRIs and prefixed names supplied as simple Java strings. On line 3 we define a namespace prefix we want to use, and then on lines 4-6 we use simple prefixed name strings, which the ModelBuilder internally maps to full IRIs.

# Literal values: datatypes and language tags 

We have sofar seen literal values that were just simple strings. However, in RDF, every literal has an associated *datatype* that determines what kind of value the literal is: a string, an integer number, a date, and so on. In addition, a String literal can optionally have a *language tag* that indicates the language the string is in. 

Datatypes are associated with a literal by means of a datatype IRI, usually for a datatype defined in [XML Schema](http://www.w3.org/2001/XMLSchema). Examples are `http://www.w3.org/2001/XMLSchema#string`, `http://www.w3.org/2001/XMLSchema#integer`, `http://www.w3.org/2001/XMLSchema#dateTime` (commonly abbreviated as `xsd:string`, `xsd:integer`, `xsd:dateTime`, respectively). A longer (though not exhaustive) list of supported data types is available in the [RDF 1.1 Concepts](https://www.w3.org/TR/2014/REC-rdf11-concepts-20140225/#section-Datatypes) specification.

Languages are associated with a string literal by means of a "language tag", as identified by [BCP 47](https://tools.ietf.org/html/bcp47). Examples of language tags are "en" (English), "fr" (French), "en-US" (US English), etc. 

We will demonstrate the use of language tags and data types by adding some additional data to our model. Specifically, we will add some information about a painting created by van Gogh, namely "The Potato Eaters".

## Example 03: adding a date and a number 

{{< example "Example 03" "model/Example03LiteralDatatypes.java" >}} shows how we can add the creation date (as an `xsd:dateTime`) and the number of people depicted in the painting (as an `xsd:integer`):

```java {linenos=inline}
  Model model = builder.setNamespace("ex", "http://example.org/")
      .subject("ex:PotatoEaters")
          // this painting was created in April 1885
          .add("ex:creationDate", new GregorianCalendar(1885, Calendar.APRIL, 1).getTime())
          // it depicts 5 people
          .add("ex:peopleDepicted", 5)
      .build();

  // To see what's in our model, let's just print stuff to the screen
  for(Statement st: model) {
      // we want to see the object values of each property
      IRI property = st.getPredicate();
      Value value = st.getObject();
      if (value instanceof Literal) {
          Literal literal = (Literal)value;
          System.out.println("datatype: " + literal.getDatatype());
          
          // get the value of the literal directly as a Java primitive.
          if (property.getLocalName().equals("peopleDepicted")) {
              int peopleDepicted = literal.intValue();
              System.out.println(peopleDepicted + " people are depicted in this painting");
          }
          else if (property.getLocalName().equals("creationDate")) {
              XMLGregorianCalendar calendar = literal.calendarValue();
              Date date = calendar.toGregorianCalendar().getTime();
              System.out.println("The painting was created on " + date);
          }
          
          // You can also just get the lexical value (a string) without 
          // worrying about the datatype
          System.out.println("Lexical value: '" + literal.getLabel() + "'");
      }
  }
```

## Example 04: adding an artwork's title in Dutch and English

{{< example "Example 04" "model/Example04LanguageTags.java" >}} shows how we can add the title of the painting in both Dutch and English, and how we can retrieve this information back from the model:

```java {linenos=inline}
Model model = builder
    .setNamespace("ex", "http://example.org/")
    .subject("ex:PotatoEaters")
	// In English, this painting is called "The Potato Eaters"
	.add(DC.TITLE, vf.createLiteral("The Potato Eaters", "en"))
	// In Dutch, it's called "De Aardappeleters"
	.add(DC.TITLE,  vf.createLiteral("De Aardappeleters", "nl"))
    .build();

// To see what's in our model, let's just print it to the screen
for(Statement st: model) {
    // we want to see the object values of each statement
    Value value = st.getObject();
    if (value instanceof Literal) {
	Literal title = (Literal)value;
	System.out.println("language: " + title.getLanguage().orElse("unknown"));
	System.out.println(" title: " + title.getLabel());
    }
}
```

# Blank nodes

Sometimes, we may want to model some facts without explicitly giving all resources involved in that fact an identifier. For example, consider the following sentence: "Picasso has created a painting depicting cubes, and using a blue color scheme". There's several facts in this sentence:

1. Picasso created some painting;
2. that painting depicts cubes;
3. that painting uses the color blue.

All of the above may be true, but it doesn't involve identifying a specific painting. All we know is that there is _some_ (unknown) painting for which all of this is true. We can express this in RDF using a *blank node*.

When looking at a graph depiction of the RDF, it becomes obvious why it is called a _blank_ node:

![Image](../images/rdf-graph-3.png)

Other possible uses for blank nodes are for modeling a collection of facts that are strongly tied together. For example, "Picasso's home address is '31 Art Gallery, Madrid, Spain'" could be modeled as follows:

![Image](../images/rdf-graph-4.png)

WARNING: Blank nodes can be useful, but they can also complicate things. Because they can not be directly addressed (they have no identifier, after all, hence "blank"), you can only query them via their property values. And since they have no identifier, it's often hard to determine if two blank nodes are really the same resource, or two separate ones. A good rule of thumb is to only use blank nodes if it _really_ conceptually makes no sense to give something its own global identifier.

## Example 05: adding blank nodes to a Model

{{< example "Example 05" "model/Example05BlankNode.java" >}} shows how we can add the address of Picasso to our Model:

```java {linenos=inline}
// To create a blank node for the address, we need a ValueFactory
ValueFactory vf = SimpleValueFactory.getInstance();
BNode address = vf.createBNode();

// First we do the same thing we did in example 02: create a new ModelBuilder,
// and add two statements about Picasso.
ModelBuilder builder = new ModelBuilder();
builder
    .setNamespace("ex", "http://example.org/")
    .subject("ex:Picasso")
	    .add(RDF.TYPE, "ex:Artist")
	    .add(FOAF.FIRST_NAME, "Pablo")
    // this is where it becomes new: we add the address by linking the blank 
    // node to picasso via the `ex:homeAddress` property, and then adding 
    // facts _about_ the address
	    .add("ex:homeAddress", address) // link the blank node
    .subject(address)			    // switch the subject
	    .add("ex:street", "31 Art Gallery")
	    .add("ex:city", "Madrid")
	    .add("ex:country", "Spain");

Model model = builder.build();
```

# Reading and Writing RDF

In the previous sections we saw how to print the contents of a Model to the screen, However, this is of limited use: the format is not easy to read, and certainly not by any other tools that you may wish to share the information with.

Fortunately, RDF4J provides tools for reading and writing RDF models in several syntax formats, all of which are standardized, and therefore can be used to share data between applications. The most commonly used formats are RDF/XML, Turtle, and N-Triples.

## Example 06: Writing to RDF/XML 

{{< example "Example 06" "model/Example06WriteRdfXml.java" >}} shows how we can write our Model as RDF/XML, using the RDF4J {{< javadoc "Rio" "rio/Rio.html" >}} parser/writer tools:

```java
Rio.write(model, System.out, RDFFormat.RDFXML);
```

The output will be similar to this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
	xmlns:ex="http://example.org/"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

<rdf:Description rdf:about="http://example.org/Picasso">
	<rdf:type rdf:resource="http://example.org/Artist"/>
	<firstName xmlns="http://xmlns.com/foaf/0.1/" rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Pablo</firstName>
	<ex:homeAddress rdf:nodeID="node1b4koa8edx1"/>
</rdf:Description>

<rdf:Description rdf:nodeID="node1b4koa8edx1">
	<ex:street rdf:datatype="http://www.w3.org/2001/XMLSchema#string">31 Art Gallery</ex:street>
	<ex:city rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Madrid</ex:city>
	<ex:country rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Spain</ex:country>
</rdf:Description>

</rdf:RDF>
```

The `Rio.write` method takes a `java.io.OutputStream` or a `java.io.Writer` as an argument, so if we wish to write to file instead of to the screen, we can simply use a `FileOutputStream` or a `FileWriter` and point it at the desired file location.

## Example 07: Writing to Turtle and other formats

{{< example "Example 07" "model/Example05WriteTurtle.java" >}} shows how we can write our Model in the {{< javadoc "Turtle" "rio/RDFFormat.html#TURTLE" >}} syntax format:

```java
Rio.write(model, System.out, RDFFormat.TURTLE);
```

To produce other syntax formats, simply vary the supplied RDFFormat. Try out a few different formats yourself, to get a feel for what they look like.

The output in Turtle format looks like this:

    @prefix ex: <http://example.org/> .

    ex:Picasso a ex:Artist ;
            <http://xmlns.com/foaf/0.1/firstName> "Pablo" ;
            ex:homeAddress _:node1b4koq381x1 .

    _:node1b4koq381x1 ex:street "31 Art Gallery" ;
            ex:city "Madrid" ;
            ex:country "Spain" .

If you compare this with the output of writing to RDF/XML, you will notice that the Turtle syntax format is a lot more compact, and also easier to read for a human. Let's quickly go through it:

On the first line, a namespaces prefix is defined. It is one we recognize: the `ex` namespace that we added to our RDF model earlier. Turtle syntax supports using prefixed names to make the format more compact, and easier to read.

Lines 3-5 show three RDF statements, all about `ex:Picasso`. 
The first statement, on line 3, says that Picasso is of type Artist. In Turtle, `a` is a shortcut for the `rdf:type` property. Notice that the line ends with a `;`. This indicates that the _next_ line in the file will be about the same subject. 
Line 4 says that Picasso's first name is "Pablo". Notice that here the full IRI is used for the property - this happens because we didn't set a namespace prefix for it when we created our model. 

TIP: In Turtle syntax, a full IRI always starts with `<` and ends with `>`. This makes them easy to distinguish from prefixed names, and from blank node identifiers. 

Line 5, finally, states that Picasso has a homeAddress, which is some blank node (a blank node identifier in Turtle syntax always starts with `_:`). Note that this line ends with a `.`, which indicates that we are done stating facts about the current subject.

Line 7 and further, finally, state facts about the blank node (the home address of Picasso): its street is "31 Art Gallery", its city is "Madrid", and its Country is "Spain".

## Example 08: Reading a Turtle RDF file

Very similar to how we can write RDF models to files in various syntaxes, we can also use RDF4J Rio to _read_ files to produce an RDF model. 

{{< example "Example 08" "model/Example08ReadTurtle.java" >}} shows how we can read a Turtle file and produce a `Model` object out of it:

```java {linenos=inline}
String filename = "example-data-artists.ttl";

// read the file 'example-data-artists.ttl' as an InputStream.
InputStream input = Example06ReadTurtle.class.getResourceAsStream("/" + filename);

// Rio also accepts a java.io.Reader as input for the parser.
Model model = Rio.parse(input, "", RDFFormat.TURTLE);
```

# Accessing a Model 

Now that we know how to create, read, and save an RDF Models, it is time to look at how we can access the information in a Model. 

We have already seen one simple way of accessing a {{< javadoc "Model" "model/Model.html" >}}: we can iterate over its contents using a `for-each` loop. The reason this works is that `Model` extends the Java Collection API, more particularly it is a `java.util.Set<Statement>`. 

We have more sophisticated options at our disposal, however. 

## Example 09: filtering on a specific subject

{{< example "Example 09" "model/Example09Filter.java" >}} shows how we can use {{< javadoc "Model.filter" "model/Model.html#filter-org.eclipse.rdf4j.model.Resource-org.eclipse.rdf4j.model.IRI-org.eclipse.rdf4j.model.Value-org.eclipse.rdf4j.model.Resource...-" >}} to "zoom in" on a specific subject in our model. We're also using the opportunity to show how you can print out RDF statements in a slightly prettier way:

```java {linenos=inline}
ValueFactory vf = SimpleValueFactory.getInstance();

// We want to find all information about the artist `ex:VanGogh` in our model
IRI vanGogh = vf.createIRI("http://example.org/VanGogh");

// By filtering on a specific subject we zoom in on the data that is about 
// that subject. The filter method takes a subject, predicate, object (and 
// optionally a named graph/context) argument. The more arguments we set to 
// a value, the more specific the filter becomes.
Model aboutVanGogh = model.filter(vanGogh, null, null);

// Iterate over the statements that are about Van Gogh
for (Statement st: aboutVanGogh) {
	// the subject is always `ex:VanGogh`, an IRI, so we can safely cast it
	IRI subject = (IRI)st.getSubject();
	// the property predicate is always an IRI
	IRI predicate = st.getPredicate();

	// the property value could be an IRI, a BNode, or a Literal. In RDF4J,
	// Value is is the supertype of all possible kinds of RDF values.
	Value object = st.getObject();

	// let's print out the statement in a nice way. We ignore the namespaces 
	// and only print the local name of each IRI
	System.out.print(subject.getLocalName() + " " + predicate.getLocalName() + " ");
	if (object instanceof Literal) {
		// It's a literal. print it out nicely, in quotes, and without any ugly
		// datatype stuff
		System.out.println("\"" + ((Literal)object).getLabel() + "\"");
	}
	else if (object instanceof  IRI) {
	        // It's an IRI. Print it out, but leave off the namespace part.
		System.out.println(((IRI)object).getLocalName());
	}
	else {
	        // It's a blank node. Just print out the internal identifier.
		System.out.println(object);
	}
}
```

## Example 10: Getting all property values for a resource

{{< example "Example 10" "model/Example10PropertyValues.java" >}} shows how we can directly get all values of a property, for a given resource, from the model. To simply retrieve all paintings by van Gogh, we can do this:


```java {linenos=inline}
Set<Value> paintings = model.filter(vanGogh, EX.CREATOR_OF, null).objects();
```

TIP: Notice that we are suddenly using a new vocabulary constant for our property: `EX.CREATOR_OF`. It is generally a good idea to create a class containing  constants for your own IRIs when you program with RDF4J: it makes it easier to reuse them and avoids introducing typos (not to mention a lot of hassle if you later decide to rename one of your resources). See {{< example "the EX vocabulary class" "model/vocabulary/EX.java" >}} for an example of how to create your own vocabulary classes.

Once we have selected the values, we can iterate and do something with them. For example, we could try and retrieve further information about each value, like so:

```java {linenos=inline}
for (Value painting: paintings) {
	if (painting instanceof Resource) {
		// our value is either an IRI or a blank node. Retrieve its properties and print.
		Model paintingProperties = model.filter((Resource)painting, null, null);

		// write the info about this painting to the console in Turtle format
		System.out.println("--- information about painting: " + painting);
		Rio.write(paintingProperties, System.out, RDFFormat.TURTLE);
		System.out.println();
	}
}
```

NOTE: The `Model.filter` method does not actually return a _new_ Model object: it returns a filtered view of the original Model. This means that invoking `filter` is very cheap, because it doesn't have to copy the contents into a new Collection. It also means that any modifications to the original Model object will show up in the filter result, and vice versa.

## Example 11: Retrieving a single property value

{{< example "Example 11" "model/Example11SinglePropertyValue.java" >}} shows how we can directly get a single value of a property, from the model. In this example, we retrieve the first name of each known artist, and print it to the console:

```java {linenos=inline}
// iterate over all resources that are of type 'ex:Artist'
for (Resource artist : model.filter(null, RDF.TYPE, EX.ARTIST).subjects()) {
	// get all RDF triples that denote values for the `foaf:firstName` property 
	// of the current artist
	Model firstNameTriples = model.filter(artist, FOAF.FIRST_NAME, null);

	// Get the actual first name by just selecting any property value. If no value 
	// can be found, set the first name to '(unknown)'. 
	String firstName = Models.objectString(firstNameTriples).orElse("(unknown)");

	System.out.println(artist + " has first name '" + firstName + "'");
}
```

In this code example, we use two steps to retrieve the first name for each artist. The first step, on line 5, is that we use `Model.filter` again. This zooms in to select _only_ the `foaf:firstName` statements about the current artist (notice that I say statement**s**, plural: there could very well be an artist with more than one first name).

For the second step, the actual selection of a single property value, we use the {{< javadoc "Models" "model/util/Models.html" >}} utility. This class provides several useful shortcuts for working with data in a model. In this example, we are using the `objectString` method. What this method does is retrieve an arbitrary object-value from the supplied model, and return it converted to a String. Since the model we supply only contains `foaf:firstName` statements about the current artist, we know that the object we get back will be a first name of the current artist.

NOTE: The `Models` utility methods for selecting single values, such as `Models.objectString`, return any one _arbitrary_ suitable value: if there is more than one possible object value in the supplied model, it just picks one. There is no guarantee that it will always pick the same value on consecutive calls.

# Named Graphs and Contexts

As we have seen, the RDF data model can be viewed as a graph. Sometimes it is useful to be able to group together subsets of RDF data as separate graphs. For example, you may want to use several files together, but still keep track of which statements come from which file. An RDF4J {{< javadoc "Model" "model/Model.html" >}} facilitates this by having an optional *context* parameter for most of it methods. This parameter allows you to identify a _named graph_ in the Model, that is a subset of the complete model. In this section, we will look at some examples of this mechanism in action.

## Example 12: Adding statements to two named graphs


{{< example "Example 12" "model/Example12BuildModelWithNamedGraphs.java" >}} shows how we can add information to separate named graphs in a single Model, and using that named graph information to retrieve those subsets again:

```java {linenos=inline}
// We'll use a ModelBuilder to create two named graphs, one containing data about
// Picasso, the other about Van Gogh.
ModelBuilder builder = new ModelBuilder();
builder.setNamespace("ex", "http://example.org/");

// In named graph 1, we add info about Picasso
builder.namedGraph("ex:namedGraph1")
		.subject("ex:Picasso")
			.add(RDF.TYPE, EX.ARTIST)
			.add(FOAF.FIRST_NAME, "Pablo");

// In named graph 2, we add info about Van Gogh.
builder.namedGraph("ex:namedGraph2")
	.subject("ex:VanGogh")
		.add(RDF.TYPE, EX.ARTIST)
		.add(FOAF.FIRST_NAME, "Vincent");


// We're done building, create our Model
Model model = builder.build();

// Each named graph is stored as a separate context in our Model
for (Resource context: model.contexts()) {
	System.out.println("Named graph " + context + " contains: ");

	// write _only_ the statemements in the current named graph to the console, 
	// in N-Triples format
	Rio.write(model.filter(null, null, null, context), System.out, RDFFormat.NTRIPLES);
	System.out.println();
}
```

On line 7 (and 13, respectively), you can see how {{< javadoc "ModelBuilder" "model/util/ModelBuilder.html" >}} can add statements to a specific named graph using the `namedGraph` method. Similarly to how the `subject` method defines what subject each added statement is about (until we set a new subject), `namedGraph` defines what named graph (or 'context') each statement is added to, until either a new named graph is set, or the state is reset using the `defaultGraph` method. 

On lines 23 and further, you can see two examples of how this information can be accessed from the resulting Model. You can explicitly retrieve all available contexts (line 23). You can also use a context identifier as a parameter for the `filter` method, as shown on line 28.

# Databases and SPARQL querying

When RDF models grow larger and more complex, simply keeping all the data in an in-memory collection is no longer an option: large amounts of data will simply not fit, and querying the data will require more sophisticated indexing mechanisms. Moreover, data consistency ensurance mechanisms (transactions, etc) will be necessary. In short: you need a database.

RDF4J has a standardized access API for RDF databases, called the Repository API. This API provides all the things we need from a database: a sophisticated transaction handling mechanism, controls to work efficiently with high data volumes, and, perhaps most importantly: support for querying your data using the [SPARQL query language](https://www.w3.org/TR/sparql11-query/).

In this tutorial, we will show the basics of how to use the Repository API and execute some simple SPARQL queries over your RDF data. Explaining SPARQL or the Repository API in detail is out of scope, however. For more details on how to use the Repository API, have a look at [Programming with RDF4J](/documentation/programming).

## Example 13: Adding an RDF Model to a database

{{< example "Example 13" "repository/Example13AddRDFToDatabase.java" >}} shows how we can add our RDF Model to a database:

```java {linenos=inline}
// First load our RDF file as a Model.
String filename = "example-data-artists.ttl";
InputStream input = Example11AddRDFToDatabase.class.getResourceAsStream("/" + filename);
Model model = Rio.parse(input, "", RDFFormat.TURTLE);

// Create a new Repository. Here, we choose a database implementation
// that simply stores everything in main memory.
Repository db = new SailRepository(new MemoryStore());

// Open a connection to the database
try (RepositoryConnection conn = db.getConnection()) {
	// add the model
	conn.add(model);

	// let's check that our data is actually in the database
	try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
		for (Statement st: result) {
			System.out.println("db contains: " + st);
		}
	}
}
finally {
	// before our program exits, make sure the database is properly shut down.
	db.shutDown();
}
```

In this code example (line 8), we simply create a new {{< javadoc "Repository" "repository/Repository.html" >}} on the fly. We use a {{< javadoc "SailRepository" "repository/sail/SailRepository.html" >}} as the implementing class of the Repository interface, which takes a database implementation (known in RDF4J as a SAIL - "Storage and Inferencing Layer") as its constructor. In this case, we simply use an in-memory database implementation. 

TIP: RDF4J itself provides several database implementations, and many third parties provide full connectivity for their own RDF database product to work with the RDF4J APIs. For a list of third-party databases, see this [list of vendors](http://rdf4j.org/about/rdf4j-databases/) . For more detailed information on how to create and maintain databases, see [Programming with RDF4J](/documentation/programming).

Once we have created and initialized our database, we open a {{< javadoc "RepositoryConnection" "repository/RepositoryConnection.html" >}} to it (line 11). This connection is an `AutoCloseable` resource that offers all sorts of methods for executing commands on the database: adding and removing data, querying, starting transactions, and so on. 

## Example 14: load a file directly into a database

In the code example in the previous section, we first loaded an RDF file into a Model object, and then we added that Model object to our database. This works fine for smaller files of course, but as data gets larger, you really don't want to have to load it completely in main memory before storing it in your database.

{{< example "Example 14" "/repository/Example14AddRDFToDatabase.java" >}} shows how we can add our RDF data to a database directly, without first creating a Model:

```java {linenos=inline}
// Create a new Repository.
Repository db = new SailRepository(new MemoryStore());

// Open a connection to the database
try (RepositoryConnection conn = db.getConnection()) {
    String filename = "example-data-artists.ttl";
    try (InputStream input = 
            Example12AddRDFToDatabase.class.getResourceAsStream("/" + filename)) {
	// add the RDF data from the inputstream directly to our database
	conn.add(input, "", RDFFormat.TURTLE );
    }

    // let's check that our data is actually in the database
    try (RepositoryResult<Statement> result = conn.getStatements(null, null, null)) {
        for(Statement st: result) {
		System.out.println("db contains: " + st);
	}
    }
}
finally {
    // before our program exits, make sure the database is properly shut down.
    db.shutDown();
}
```

The main difference with the previous example is on lines 7-11: we still open an `InputStream` to access our RDF file, but we now provide that stream _directly_ to the Repository, which then takes care of reading the file and adding the data without the need to keep the fully processed model in main memory.

## Example 15: SPARQL SELECT Queries

{{< example "Example 15" "repository/Example15SimpleSPARQLQuery.java" >}} shows how, once we have added data to our database, we can execute a simple SPARQL SELECT-query:

```java {linenos=inline}
// Create a new Repository.
Repository db = new SailRepository(new MemoryStore());

// Open a connection to the database
try (RepositoryConnection conn = db.getConnection()) {
    String filename = "example-data-artists.ttl";
    try (InputStream input = 
           Example13SimpleSPARQLQuery.class.getResourceAsStream("/" + filename)) {
	// add the RDF data from the inputstream directly to our database
	conn.add(input, "", RDFFormat.TURTLE );
    }

    // We do a simple SPARQL SELECT-query that retrieves all resources of 
    // type `ex:Artist`, and their first names.
    String queryString = "PREFIX ex: <http://example.org/> \n";
    queryString += "PREFIX foaf: <" + FOAF.NAMESPACE + "> \n";
    queryString += "SELECT ?s ?n \n";
    queryString += "WHERE { \n";
    queryString += "    ?s a ex:Artist; \n";
    queryString += "       foaf:firstName ?n .";
    queryString += "}";
    TupleQuery query = conn.prepareTupleQuery(queryString); 
    // A QueryResult is also an AutoCloseable resource, so make sure it gets 
    // closed when done.
    try (TupleQueryResult result = query.evaluate()) {
	// we just iterate over all solutions in the result...
	for (BindingSet solution: result) {
	    // ... and print out the value of the variable bindings 
	    // for ?s and ?n
	    System.out.println("?s = " + solution.getValue("s"));
	    System.out.println("?n = " + solution.getValue("n"));
	}
    }
}
finally {
    // Before our program exits, make sure the database is properly shut down.
    db.shutDown();
}
```

On lines 15-21, we define our SPARQL query string, and on line 22 we turn this into a prepared {{< javadoc "Query" "query/Query.html" >}} object. We are using a SPARQL SELECT-query, which will return a result consisting of _tuples_ of variable-bindings (each tuple containing a binding for each variable in the SELECT-clause). Hence, RDF4J calls the constructed query a {{< javadoc "TupleQuery" "query/TupleQuery.html" >}}, and the result of the query a {{< javadoc "TupleQueryResult" "query/TupleQueryResult.html" >}}. Lines 26-34 is where the actual work gets done: on line 25, the query is evaluated, returning a result object. RDF4J QueryResult objects execute lazily: the actual data is not retrieved from the database until we start iterating over the result (as we do on lines 27-33). On line 27 we grab the next solution from the result, which is a {{< javadoc "BindingSet" "query/BindingSet.html" >}}. You can think about a BindingSet as being similar to a row in a table (the binding names are the columns, the binding values the value for each column in this particular row). We then grab the value of the binding of variable `?s` (line 30) and `?n` (line 31) and print them out.

There are a number of variations possible on how you execute a query and process the result. We'll show some of these variations here, and we recommend that you try them out by modifying code example 13 in your own editor and executing the modified code, to see what happens.

One variation is that we can materialize the `TupleQueryResult` iterator into a simple java `List`, containing the entire query result:

```java {linenos=inline}
List<BindingSet> result = QueryResults.asList(query.evaluate()); 
for (BindingSet solution: result) {
     System.out.println("?s = " + solution.getValue("s"));
     System.out.println("?n = " + solution.getValue("n"));
}
```

On line 1, we turn the result of the query into a `List` using the {{< javadoc "QueryResults" "query/QueryResults.html" >}} utility. This utility reads the result completely and also takes care of closing the result (even in case of errors), so there is no need to use a try-with-resources clause in this variation. 

Another variation is that instead of retrieving the query result as an iterator object, we let the query send its result directly to a {{< javadoc "TupleQueryResultHandler" "query/TupleQueryResultHandler.html" >}}. This is a useful way to directly stream a query result to a file on disk. Here, we show how to use this mechanism to write the result to the console in tab-separated-values (TSV) format:

```java {linenos=inline}
TupleQueryResultHandler tsvWriter = new SPARQLResultsTSVWriter(System.out);
query.evaluate(tsvWriter);
```

## Example 16: SPARQL CONSTRUCT Queries

Another type of SPARQL query is the CONSTRUCT-query: instead of returning the result as a sequence of variable bindings, CONSTRUCT-queries return RDF statements. CONSTRUCT queries are very useful for quickly retrieving data subsets from an RDF database, and for transforming that data. 

{{< example "Example 16" "repository/Example16SPARQLConstructQuery.java" >}} shows how we can execute a SPARQL CONSTRUCT query in RDF4J. As you can see, most of the code is quite similar to previous examples:

```java {linenos=inline}
// Create a new Repository.
Repository db = new SailRepository(new MemoryStore());

// Open a connection to the database
try (RepositoryConnection conn = db.getConnection()) {
    String filename = "example-data-artists.ttl";
    try (InputStream input =
	    Example14SPARQLConstructQuery.class.getResourceAsStream("/" + filename)) {
	// add the RDF data from the inputstream directly to our database
	conn.add(input, "", RDFFormat.TURTLE );
    }

    // We do a simple SPARQL CONSTRUCT-query that retrieves all statements 
    // about artists, and their first names.
    String queryString = "PREFIX ex: <http://example.org/> \n";
    queryString += "PREFIX foaf: <" + FOAF.NAMESPACE + "> \n";
    queryString += "CONSTRUCT \n";
    queryString += "WHERE { \n";
    queryString += "    ?s a ex:Artist; \n";
    queryString += "       foaf:firstName ?n .";
    queryString += "}";

    GraphQuery query = conn.prepareGraphQuery(queryString);

    // A QueryResult is also an AutoCloseable resource, so make sure it gets
    // closed when done.
    try (GraphQueryResult result = query.evaluate()) {
	// we just iterate over all solutions in the result...
	for (Statement st: result) {
	    // ... and print them out
	    System.out.println(st);
	}
    }
}
finally {
    // Before our program exits, make sure the database is properly shut down.
    db.shutDown();
}
```

On lines 15-21 we create our SPARQL CONSTRUCT-query. The only real difference is line 17, where we use a CONSTRUCT-clause (instead of the SELECT-clause we saw previously). Line 23 turns the query string into a prepared Query object. Since the result of a CONSTRUCT-query is a set of RDF statements (in other words: a graph), RDF4J calls such a query a {{< javadoc "GraphQuery" "query/GraphQuery.html" >}}, and its result a {{< javadoc "GraphQueryResult" "query/GraphQueryResult.html" >}}.

On line 27 and further we execute the query and iterate over the result. The main difference with previous examples is that this time, the individual solutions in the result are {{< javadoc "Statements" "model/Statement.html" >}}.

As with SELECT-queries, there are a number of variations on how you execute a CONSTRUCT-query and process the result. We'll show some of these variations here, and we recommend that you try them out by modifying code example 14 in your own editor and executing the modified code, to see what happens.

One variation is that we can turn the `GraphQueryResult` iterator into a `Model`, containing the entire query result:

```java {linenos=inline}
Model result = QueryResults.asModel(query.evaluate()); 
for (Statement st: result) {
     System.out.println(st);
}
```

In this particular example, we then iterate over this model to print out the Statements, but obviously we can access the information in this Model in the same ways we have already seen in previous sections.

Another variation is that instead of retrieving the query result as an iterator object, we let the query send its result directly to a {{< javadoc "RDFHandler" "rio/RDFHandler.html" >}}. This is a useful way to directly stream a query result to a file on disk. Here, we show how to use this mechanism to write the result to the console in Turtle format

```java {linenos=inline}
RDFHandler turtleWriter = Rio.createWriter(RDFFormat.TURTLE, System.out);
query.evaluate(turtleWriter);
```

# Further reading

You should now have a basic understanding of the RDF data model, and have a decent grasp on how you can use RDF4J to read, write, create, store, and query RDF data. For more information on how to use RDF4J, we recommend the following sources:

* [Programming with RDF4J](/documentation/programming) - an extensive guide to using the RDF4J framework from Java, covering basics and more advanced configurations.
* [RDF4J API JavaDoc](http://docs.rdf4j.org/javadoc/latest) - the complete API reference. Pay particular attention to the various `util` packages scattered throughout the API, these often contain very useful helper classes and utilities.
* [Getting Started with RDF4J, Maven and Eclipse](/documentation/tutorials/maven-eclipse-project) - a tutorial on how to set up your first RDF4J-based project with the help of Apache Maven and the Eclipse IDE.

For more detailed information about RDF, and SPARQL, consult the following sources:

* The [W3C RDF 1.1 Primer](https://www.w3.org/TR/rdf11-primer/) introduces the basic concepts of RDF and shows concrete examples of the use of RDF.

* The [W3C SPARQL 1.1 Query Language Recommendation](https://www.w3.org/TR/sparql11-query/) is the normative specification of the SPARQL Query Language. It contains a complete overview of all SPARQL operators and capabilities, including many useful examples.

* The [W3C SPARQL 1.1 Update Recommendation](https://www.w3.org/TR/sparql11-update/) is the normative specification for SPARQL Update operations. SPARQL Update allows you to insert, modify, delete, copy and move RDF data. 

If you require any further help, you can [contact us to get support](http://rdf4j.org/support). We welcome your feedback.
