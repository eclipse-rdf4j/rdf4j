---
title: "The RDF Model API"
weight: 2
toc: true
autonumbering: true
---
The RDF Model API is the core of the RDF4J framework. It provides the basic building blocks for manipulating RDF data in Java.
<!--more-->

## RDF Building Blocks: IRIs, literals, blank nodes and statements

The core of the RDF4J framework is the RDF Model API (see the [Model API Javadoc](https://rdf4j.eclipse.org/javadoc/latest/index.html?org/eclipse/rdf4j/model/package-summary.html)), defined in package `org.eclipse.rdf4j.model`. This API defines how the building blocks of RDF (statements, IRIs, blank nodes, literals, and models) are represented.

RDF statements are represented by the {{< javadoc "Statement" "model/Statement.html" >}} interface. Each `Statement` has a subject, predicate, object and (optionally) a context. Each of these 4 items is a {{< javadoc "Value" "model/Value.html" >}}. The `Value` interface is further specialized into {{< javadoc "Resource" "model/Resource.html" >}}, and {{< javadoc "Literal" "model/Literal.html" >}}. `Resource` represents any RDF value that is either a {{< javadoc "BNode" "model/BNode.html" >}} or an {{< javadoc "IRI" "model/IRI.html" >}}. `Literal` represents RDF literal values (strings, dates, integer numbers, and so on).

### Creating new building blocks: the `Values` and `Statements` factory methods

{{< tag " New in RDF4J 3.5 " >}}

To create new values and statements, you can use the {{< javadoc "Values" "model/util/Values.html" >}}  and {{< javadoc "Statements" "model/util/Statements.html" >}} static factory methods, which provide easy creation of new `IRI`s, `Literal`s, `BNode`s, `Triple`s and `Statement`s based on a variety of different input objects.

```java
import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

IRI bob = iri("http://example.org/bob");
IRI nameProp = iri("http://example.org/name");
Literal bobsName = literal("Bob");
Literal bobsAge = literal(42);

Statement st = statement(bob, nameProp, bobsName, null);
```

### Using a `ValueFactory`

If you want more control than the static factory methods provide, you can also use a {{< javadoc "ValueFactory" "model/ValueFactory.html" >}} instance. You can obtain one from {{< javadoc "Values" "model/util/Values.html" >}}, or you can directly use a singleton `ValueFactory` implementation called {{< javadoc "SimpleValueFactory" "model/impl/SimpleValueFactory.html" >}}:

```java
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

ValueFactory factory = SimpleValueFactory.getInstance();
```

For performance reasons, the `SimpleValueFactory` provides only basic input validation. The {{< javadoc "ValidatingValueFactory" "model/impl/ValidatingValueFactory.html" >}} is stricter, albeit somewhat slower (though this should not be noticeable unless you are working with very significant amounts of data).

You can also obtain a `ValueFactory` from the {{< javadoc "Repository" "repository/Repository.html" >}} you are working with, and in fact, this is the recommend approach. For more information about this see the [Repository API documentation](/documentation/programming/repository/).

Regardless of how you obtain your `ValueFactory`, once you have it, you can use it to create new `IRI`s, `Literal`s, and `Statement`s:

```java
IRI bob = iri(factory, "http://example.org/bob");
IRI name = iri(factory,"http://example.org/name");
Literal bobsName = literal(factory, "Bob");
Statement nameStatement = statement(factory, bob, name, bobsName);
```
Or if you prefer, using the `Valuefactory` directly:

```java
IRI bob = factory.createIRI("http://example.org/bob");
IRI name = factory.createIRI("http://example.org/name");
Literal bobsName = factory.createLiteral("Bob");
Statement nameStatement = factory.createStatement(bob, name, bobsName);
```

The Model API also provides pre-defined IRIs for several well-known vocabularies, such as RDF, RDFS, OWL, DC (Dublin Core), FOAF (Friend-of-a-Friend), and more. These constants can all be found in the `org.eclipse.rdf4j.model.vocabulary` package, and can be quite handy in quick creation of RDF statements (or in querying a `Repository`, as we shall see later):

```java
Statement typeStatement = Values.statement(bob, RDF.TYPE, FOAF.PERSON);
```

## The Model interface

The above interfaces and classes show how we can create the individual building blocks that make up an RDF model. However, an actual collection of RDF data is just that: a collection. In order to deal with collections of RDF statements, we can use the {{< javadoc "org.eclipse.rdf4j.model.Model" "model/Model.html" >}} interface.

`Model` is an extension of the default Java Collection class `java.util.Set<Statement>`. This means that you can use a `Model` like any other Java collection in your code:

```java
// create a new Model to put statements in
Model model = DynamicModelFactory.createEmptyModel();
// add an RDF statement
model.add(typeStatement);
// add another RDF statement by simply providing subject, predicate, and object.
model.add(bob, name, bobsName);

// iterate over every statement in the Model
for (Statement statement: model) {
	   ...
}
```

In addition, however, `Model` offers a number of useful methods to quickly get subsets of statements and otherwise search/filter your collection of statements. For example, to quickly iterate over all statements that make a resource an instance of the class `foaf:Person`, you can do:

```java
for (Statement typeStatement: model.filter(null, RDF.TYPE, FOAF.PERSON)) {
  // ...
}
```

Even more convenient is that you can quickly retrieve the building blocks that make up the statements. For example, to immediately iterate over all subject-resources that are of type `foaf:Person` and then retrieve each person’s name, you can do something like the following:

```java
for (Resource person: model.filter(null, RDF.TYPE, FOAF.PERSON).subjects()) {
  // get the name of the person (if it exists)
  Optional<Literal> name = Models.objectLiteral(model.filter(person, FOAF.NAME, null));
}
```

The `filter()` method returns a `Model` again. However, the `Model` returned by this method is still backed by the original `Model`. Thus, changes that you make to this returned `Model` will automatically be reflected in the original `Model` as well.

RDF4J provides three default implementations of the `Model` interface: {{< javadoc "org.eclipse.rdf4j.model.impl.DynamicModel" "model/impl/DynamicModel.html" >}}, {{< javadoc "org.eclipse.rdf4j.model.impl.LinkedHashModel" "model/impl/LinkedHashModel.html" >}}, and {{< javadoc "org.eclipse.rdf4j.model.impl.TreeModel" "model/impl/TreeModel.html" >}}. The difference between them is in their performance for different kinds of lookups and insertion patterns (see their respective javadoc entries for details). These differences are only really noticeable when dealing with quite large collections of statements, however. 

## Building RDF Models with the ModelBuilder

Since version 2.1, RDF4J provides a {{< javadoc "ModelBuilder" "model/util/ModelBuilder.html" >}} utility. The `ModelBuilder` provides a fluent API to quickly and efficiently create RDF models programmatically.

Here’s a simple code example that demonstrates how to quickly create an RDF graph with some FOAF data:

```java
ModelBuilder builder = new ModelBuilder();

// set some namespaces
builder.setNamespace("ex", "http://example.org/").setNamespace(FOAF.NS);

builder.namedGraph("ex:graph1")      // add a new named graph to the model
       .subject("ex:john")        // add  several statements about resource ex:john
	 .add(FOAF.NAME, "John")  // add the triple (ex:john, foaf:name "John") to the named graph
	 .add(FOAF.AGE, 42)
	 .add(FOAF.MBOX, "john@example.org");

// add a triple to the default graph
builder.defaultGraph().add("ex:graph1", RDF.TYPE, "ex:Graph");

// return the Model object
Model m = builder.build();
```

The `ModelBuilder` offers several conveniences:

- you can specify a subject/predicate IRI as a prefixed name string (for example “ex:john”), so you don’t have to use a `ValueFactory` to create an IRI object first.
- you can add a literal object as a `String`, an `int`, or several other supported Java primitive types.
- the `subject()` method makes it easier to take a resource-centric view when building an RDF Model.

## Quickly accessing data with the Models utility

The {{< javadoc "Models" "model/util/Models.html" >}} utility class offers a number of useful methods for convenient access and manipulation of data in a `Model` object. We have already shown some examples of its use in previous sections. For example, to retrieve the value of the `foaf:name` properties for all resources of type `foaf:Person`:

```java
for (Resource person: model.filter(null, RDF.TYPE, FOAF.PERSON).subjects()) {
  // get the name of the person (if it exists)
  Optional<Literal> name = Models.objectLiteral(model.filter(person, FOAF.NAME, null));
}
```

The `Models.objectLiteral` method retrieves an arbitrary object literal value from the statements in the supplied `Model`. Since the supplied `Model` is filtered to only contain the `foaf:name` statements for the given person, the resulting object literal value is the name value for this person. Note that if the model happens to contain more than one name value for this person, this will just return an arbitrary one.

The `Models` utility provides variants for retrieving different types of object values: `Models.object()` retrieves a `Value`, `Models.objectResource()` a `Resource`, `Models.objectIRI()` an `IRI`.

### Property-centric access

To provide quicker access to a property’s value(s), the `Models` class offers some further shortcuts that bypass the need to first filter the `Model`. For example, to retrieve the name literal, we can replace the `objectLiteral call from the previous example like so:

```java
for (Resource person: model.filter(null, RDF.TYPE, FOAF.PERSON).subjects()) {
  // get the name of the person (if it exists)
  Optional<Literal> name = Models.getPropertyLiteral(model, person, FOAF.NAME);
}
```

`Models` also provides methods that allow retrieving all values, instead of one arbitrary one:

```java
for (Resource person: model.filter(null, RDF.TYPE, FOAF.PERSON).subjects()) {
  // get all name-values of the person
  Set<Literal> names = Models.getPropertyLiterals(model, person, FOAF.NAME);
}
```

For both retrieval types, `Models` also provides variants that retrieve other value types such as IRIs. The {{< javadoc "Models" "model/util/Models.html" >}} javadoc is worth exploring for a complete overview of all methods.

In addition to retrieving values in a property-centric manner, `Models` also provides a `setProperty` method, which can be used to quickly give a resoure’s property a new value. For example:

```java
Literal newName = vf.createLiteral("John");
Models.setProperty(person, FOAF.NAME, newName);
```

This will remove any existing name-properties for the given person, and set it to the single new value "John".


## RDF Collections

To model closed lists of items, RDF provides a Collection vocabulary . RDF Collections are represented as a list of items using a Lisp-like structure. The list starts with a head resource (typically a blank node), which is connected to the first collection member via the rdf:first relation. The head resource is then connected to the rest of the list via an rdf:rest relation. The last resource in the list is marked using the rdf:nil node.

As an example, a list containing three values, “A”, “B”, and “C” looks like this as an RDF Collection:
![Image](../images/rdf-collection.svg)

Here, the blank node `_:n1` is the head resource of the list. In this example it is declared an instance of `rdf:List`, however this is not required for the collection to be considered well-formed. For each collection member, a new node is added (linked to the previous node via the `rdf:rest` property), and the actual member value is linked to to this node via the `rdf:first` property. The last member member of the list is marked by the fact that the value of its `rdf:rest` property is set to `rdf:nil`.

Working with this kind of structure directly is rather cumbersome. To make life a little easier, rdf4j provides several utilities to convert between Java Collections and RDF Collections.

### Converting to/from Java Collections

As an example, suppose we wish to add the above list of three string literals as a property value for the property `ex:favoriteLetters` of `ex:John` .

The {{< javadoc "RDFCollections" "model/util/RDFCollections.html" >}} utility allows us to do this, as follows:

```java
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

String ns = "http://example.org/";
// IRI for ex:favoriteLetters
IRI favoriteLetters = iri(ns, "favoriteLetters");
// IRI for ex:John
IRI john = iri(ns, "John");
// create a list of letters
List<Literal> letters = Arrays.asList(new Literal[] { literal("A"), literal("B"), literal("C") });
// create a head resource for our list
Resource head = bnode();
// convert our list and add it to a newly-created Model
Model aboutJohn = RDFCollections.asRDF(letters, head, new LinkedHashModel());
// set the ex:favoriteLetters property to link to the head of the list
aboutJohn.add(john, favoriteLetters, head);
```

Of course, we can also convert back:

```java
Model aboutJohn = ... ; // our Model about John
// get the value of the ex:favoriteLetters property
Resource node = Models.objectResource(aboutJohn.filter(john, favoriteLetters, null)).orElse(null);
// Convert its collection back to an ArrayList of values
if(node != null) {
	 List<Value> values = RDFCollections.asValues(aboutJohn, node, new ArrayList<Value>());
	 // you may need to cast back to Literal.
	 Literal a = (Literal)values.get(0);
}
```

### Extracting, copying, or deleting an RDF Collection

To extract an RDF Collection from the model which contains it, we can do the following:

```java
Model aboutJohn = ...; // our model
// get the value of the ex:favoriteLetters property
Resource node = Models.objectResource(aboutJohn.filter(john, favoriteLetters, null)).orElse(null);
// get the RDF Collection in a separate model
if (node != null) {
	 Model rdfList = RDFCollections.getCollection(aboutJohn, node, new LinkedHashModel());
}
```

As you can see, instead of converting the RDF Collection to a Java List of values, we get back another `Model` object from this, containing a copy of the RDF statements that together form the RDF Collection. This is useful in cases where your original `Model` contains more data than just the RDF Collection, and you want to isolate the collection.

Once you have this copy of your Collection, you can use it to add it somewhere else, or to remove the collection from your `Model`:

```java
// remove the collection from our model about John
aboutJohn.removeAll(rdfList);
// finally remove the triple that linked John to the collection
aboutJohn.remove(john, favoriteLetters, node);
```

Actually, deleting can be done more efficiently than this. Rather than first creating a completely new copy of the RDF Collection only to then delete it, we can use a streaming approach instead:

```java
// extract the collection from our model in streaming fashion and remove each triple from the model
RDFCollections.extract(aboutJohn, node, st -> aboutJohn.remove(st));
// remove the statement that linked john to the collection
aboutJohn.remove(john, favoriteLetters, node);
```

## Working with rdf:Alt, rdf:Bag, rdf:Seq

(new since 3.3.0)

The RDF container classes `rdf:Alt`, `rdf:Bag`, and `rdf:Seq` can also be used to model sets or lists of items in RDF. RDF containers look like this:

```
   urn:myBag -rdf:type--> rdf:Bag
     |
     +---rdf:_1--> "A"
     |
     +---rdf:_2--> "B"
     |
     +---rdf:_3--> "C"
```

RDF4J offers utility conversion functions very similar to the utilities for RDF Collections: the  {{< javadoc "RDFContainers" "model/util/RDFContainers.html" >}} class.

For example, to create the above RDF container, we can do this:

```java
List<Literal> letters = Arrays.asList(new Literal[] { literal("A"), literal("B"), literal("C") });
IRI myBag = iri("urn:myBag");
Model letterBag = RDFContainers.toRDF(RDF.BAG, letters, myBag, new TreeModel());
```

and to convert back to a java collection:

```java
List<Value> newList = RDFContainers.toValues(RDF.BAG, letterBag, myBag, new ArrayList<>());
```
