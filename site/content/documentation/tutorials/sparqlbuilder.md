---
title: "Creating SPARQL Queries with the SparqlBuilder"
toc: true
weight: 4
autonumbering: true
---
RDF4J SparqlBuilder is a fluent Java API used to programmatically create SPARQL query strings.
<!--more-->
It is based on the Spanqit query builder developed by Anqit Praqash, and has been slightly modified to allow tighter integration with the rest of the RDF4J framework.

SparqlBuilder allows the following SPARQL query:

    PREFIX foaf: <http://xmlns.com/foaf/0.1/>
    SELECT ?name
    WHERE { ?x foaf:name ?name }
    ORDER BY ?name
    LIMIT 5
    OFFSET 10

to be created as simply as:

```java
query
    .prefix(FOAF.NS)
    .select(name)
    .where(x.has(FOAF.NAME, name))
    .orderBy(name)
    .limit(5)
    .offset(10);
```

The RDF4J SparqlBuilder is based on the SPARQL 1.1 Query Recommendation and the
SPARQL 1.1 Update Receommendation. Almost all features of SPARQL 1.1 are
supported, excluding some current known limitations.

This document assumes the reader is already familiar with the SPARQL query language. Please refer to the above specification if not.

## Getting SparqlBuilder

Obtain SparqlBuilder by adding the following dependency to your maven pom file:

```xml
<dependency>
    <groupId>org.eclipse.rdf4j</groupId>
    <artifactId>rdf4j-sparqlbuilder</artifactId>
    <version>${rdf4j.version}</version>
</dependency>
```

## Queries

The Queries class provides static methods to instantiate the various query objects. For example:

```java
SelectQuery selectQuery = Queries.SELECT();
ConstructQuery constructQuery = Queries.CONSTRUCT();
```

Query objects provide methods to set or add the various elements appropriate for the type of query:

```java
Prefix ex;
Variable product;
TriplePattern personWroteBook, personAuthoredBook;

// ...

selectQuery.prefix(ex).select(product).where(product.isA(ex.iri("book"));
constructQuery.prefix(ex).construct(personWroteBook).where(personAuthoredBook);
```

## Elements

SPARQL elements are created using various static factory classes. Most core elements of a query are created by the static SparqlBuilder class:

```java
import org.eclipse.rdf4j.model.vocabulary.FOAF;

Variable price = SparqlBuilder.var("price");
System.out.println(price.getQueryString()); // ==> ?price

Prefix foaf = SparqlBuilder.prefix(FOAF.NS);
System.out.println(foaf.getQueryString()); // ==> PREFIX foaf: <http://xmlns.com/foaf/0.1/>
```

Other factory classes include the Queries class mentioned in the previous section, as well as the Expressions, GraphPatterns, and Rdf classes.
All query elements created by SparqlBuilder implement the QueryElement interface, which provides the getQueryString() method. This can be used to get the String representing the SPARQL syntax of any element.

## Graph Patterns

SparqlBuilder uses three classes to represent the SPARQL graph patterns, all of which implement the GraphPattern interface:
- The TriplePattern class represents triple patterns.
- The GraphPatternNotTriple class represents collections of graph patterns.
- The SubSelect class represents a SPARQL sub query.

Graph patterns are created by the more aptly named GraphPatterns class.

###  Triple Patterns

Use `GraphPatterns#tp()` to create a TriplePattern instance:

```java
Prefix dc = SparqlBuilder.prefix("dc", iri("http://purl.org/dc/elements/1.1/"));
Variable book = SparqlBuilder.var("book");

TriplePattern triple = GraphPatterns.tp(book, dc.iri("author"), Rdf.literalOf("J.R.R. Tolkien"));
System.out.println(triple.getQueryString()); // ==> ?book dc:author "J.R.R. Tolkien"
```

or, using RDF4J Model object and vocabulary constants directly:

```java
Prefix dc = SparqlBuilder.prefix(DC.NS);
Variable book = SparqlBuilder.var("book");

TriplePattern triple = GraphPatterns.tp(book, DC.AUTHOR, Rdf.literalOf("J.R.R. Tolkien"));
System.out.println(triple.getQueryString()); // ==> ?book dc:author "J.R.R. Tolkien"
```

In almost all places, SparqlBuilder allows either RDF4J Model objects or its own interfaces to be used. You can freely mix this.

A TriplePattern instance can also be created from the `has()` and `isA()` shortcut methods of `RdfSubject`, and be expanded to contain multiple triples with the same subject via predicate-object lists and object lists:

```java
Prefix foaf = SparqlBuilder.prefix(FOAF.NS);
Variable x = SparqlBuilder.var("x"), name = SparqlBuilder.var("name");

TriplePattern triple = x.has(FOAF.NICK, Rdf.literalOf("Alice"), Rdf.literalOf("Alice_"))
    .andHas(FOAF.NAME, name);
System.out.println(triple.getQueryString());
// ===> ?x foaf:nick "Alice_", "Alice" ;
//	   foaf:name ?name .
```

### Property Paths

Property paths can be generated with a lambda expression that uses a builder pattern 
wherever a predicate (IRI) can be passed:
```
SelectQuery query = Queries
    .SELECT(name)
    .prefix(FOAF.NS)
    .where(x.has(p -> p.pred(FOAF.ACCOUNT)
                .then(FOAF.MBOX),
            name))
```
yields
```
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name
WHERE { ?x foaf:account / foaf:mbox ?name . }
```
Here are a few examples and the path they yield

| property path builder code          |  property path |
|--------------------------|---------------- |
|`p -> p.pred(FOAF.ACCOUNT).then(FOAF.MBOX)` | `foaf:account / foaf:mbox` |
|`p -> p.pred(RDF.TYPE).then(RDFS.SUBCLASSOF).zeroOrMore()` | `rdf:type / rdfs:subClassOf *` |
|`p -> p.pred(EX.MOTHER_OF).or(EX.FATHER_OF).oneOrMore()` | `( ex:motherOf \| ex:fatherOf ) +` |
|`p -> p.pred(EX.MOTHER_OF).or(p1 -> p1.pred(EX.FATHER_OF).zeroOrOne())` | `ex:motherOf \| ( ex:fatherOf ? )` |
|`p -> p.negProp().pred(RDF.TYPE).invPred(RDF.TYPE)` | `!( rdf:type \| ^ rdf:type )` |

### Compound graph patterns

Three methods in GraphPatterns exist to create GraphPatternNotTriple instances. `GraphPatterns#and()` creates a group graph pattern, consisting of the GraphPattern instances passed as parameters:

```java
Variable mbox = SparqlBuilder.var("mbox"), x = SparqlBuilder.var("x");
GraphPatternNotTriple groupPattern =
GraphPatterns.and(x.has(FOAF.NAME), name), x.has(FOAF.MBOX, mbox);
System.out.println(groupPattern.getQueryString());
// ==> { ?x foaf:mbox ?mbox . ?x foaf:name ?name }
```

`GraphPatterns#union()` creates an alternative graph pattern, taking the union of the provided GraphPattern instances:

```java
Prefix dc10 = SparqlBuilder.prefix("dc10", iri("http://purl.org/dc/elements/1.0/")),
	dc11 = SparqlBuilder.prefix("dc11", iri("http://purl.org/dc/elements/1.1/"));
Variable book = SparqlBuilder.var("book"), title = SparqlBuilder.var("title");

GraphPatternNotTriple union = GraphPatterns.union(book.has(dc10.iri("title"), title),
	book.has(dc11.iri("title"), title);
System.out.println(union.getQueryString());
// ==> { ?book dc10:title ?title } UNION { ?book dc11:title ?title }
```

`GraphPatterns#optional()` creates an optional group graph pattern, consisting of the passed in `GraphPattern`s:

```java
GraphPatternNotTriple optionalPattern = GraphPatterns.optional(GraphPatterns.tp(x, foaf.iri("mbox"), mbox));
System.out.println(optionalPattern.getQueryString());
// ==> OPTIONAL { ?x foaf:mbox ?mbox }
```

### Sub-select

Finally, GraphPatterns#select() creates an instance of a SubSelect, which represents a SPARQL subquery:

```java
SubSelect subQuery = GraphPatterns.select();
```

## Query Constraints

You can create SPARQL query constraints using the Expressions class which provides static methods to create Expression objects representing SPARQL’s built-in expressions:

```java
Variable name = SparqlBuilder.var("name");
Expression<?> regexExpression = Expressions.regex(name, "Smith");
System.out.println(regexExpression.getQueryString());
// ==> REGEX( ?name, "Smith" )
```

`Expression`s take `Operand` instances as arguments. Operand is implemented by the types you would expect (Variable, the RDF model interface RdfValue, and Expression itself). Where possible, Expressions has included wrappers to take appropriate Java primitives as parameters as well:

```java
Variable price = SparqlBuilder.var("price");
Expression<?> priceLimit = Expressions.lt(price, 100);
System.out.println(priceLimit.getQueryString());
// ==> ?price < 100
```

For those places where a wrapper has not (yet) been created, `RdfLiteral`s can be used instead. The `Rdf` class provides factory methods to create StringLiteral, NumericLiteral, and BooleanLiteral instances:

```java
Variable price = SparqlBuilder.var("price");
ExpressionOperand discount = Rdf.literalOf(0.9);
Expression<?> discountedPrice = Expressions.multiply(price, discount);
System.out.println(discountedPrice.getQueryString());
// ==> ( ?price * 0.9 )
```

## The RDF Model

SparqlBuilder provides a collection of interfaces to model RDF objects in the `org.eclipse.rdf4j.sparqlbuilder.rdf` package. Instances of these interfaces can be used when needed and as expected while creating SPARQL queries. The `Rdf` class contains static factory methods to create RDF objects for use with SparqlBuilder, including IRI’s, blank nodes, and RDF literals.
Currently SparqlBuilder’s set of interfaces for RDF model objects is different from the main RDF4J model interfaces. This will be further integrated in future releases.

## Examples

For more detailed examples of how to use SparqlBuilder, check out the JUnit tests located within the project.

## Known Limitations

Currently, the following SPARQL 1.1 features have not yet been implemented in SparqlBuilder:

- Values Block
- RDF Collection Syntax
- DESCRIBE and ASK Queries

