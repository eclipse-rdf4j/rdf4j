---
title: "RDF-star and SPARQL-star"
toc: true
weight: 9
autonumbering: true
---
RDF4J has (experimental) support for RDF-star and SPARQL-star.
<!--more-->

RDF-star and its companion SPARQL-star are proposed extensions to the RDF and
SPARQL standards (see [RDF-star W3C Community
Group](https://w3c.github.io/rdf-star/)) to provide a more convenient way to
annotate RDF statements and to query such annotations. In essence, RDF-star
attempts to bridge the gap between the RDF world and the Property Graph world.

RDF4J support for these extensions currently covers:

 - reading and writing RDF-star data in a variety of syntax formats (including Turtle-star and TriG-star)
 - converting between an RDF-star Model using annotations and a regular RDF Model (translating the annotations to regular RDF reification)
 - persisting RDF-star data in the Memory Memory Store and querying with SPARQL-star, regular SPARQL and / or API calls
 - adding extension hooks for third-party triplestores that implement the SAIL API to allow persistence and querying of RDF-star annotations

Note: these features are currently in the experimental/beta stage. While we'll do our best to not make breaking changes in future releases unless necessary, we make no guarantees.

## The RDF-star data model in RDF4J

To support RDF-star annotations, the core RDF model in RDF4J has been extended with a new type of Resource: the {{< javadoc "Triple" "model/Triple.html" >}} (not to be confused with the pre-existing {{< javadoc "Statement" "model/Statement.html" >}}, which is the representation of a "regular" RDF statement).

You can create `Triple` objects using a `ValueFactory` or through the static `Values` factory methods, and then use them as the subject (or object) of another statement, for example:

```java
IRI bob = Values.iri("http://example.org/bob");
Triple bobsAge = Values.triple(bob, FOAF.AGE, Values.literal(23));

IRI certainty = Values.iri("http://example.org/certainty");
Statement aboutBobsAge = Statements.statement(bobsAge, certainty, Values.literal(0.9), null);
```

The {{< javadoc "Statements" "model/util/Statements.html" >}} and {{< javadoc "Values" "model/util/Values.html" >}}   utility classes offers several functions to easily transform Statements into Triples, vice versa:

```java
IRI bob = Values.iri("http://example.org/bob");
Triple bobsAge = Values.triple(bob, FOAF.AGE, valueFactory.createLiteral(23));

Statement ageStatement = Statements.toStatement(bobsAge);
Triple backToTriple = Values.triple(ageStatement);
```

## Reading and writing RDF-star data

RDF4J currently provides several Rio parser/writers for RDF-star enabled syntax formats: the Turtle-star format, and the TriG-star format. As the names suggest, both are extended versions of existing RDF format (Turtle and TriG, respectively). In addition, RDF4J's [binary RDF format](/documentation/reference/rdf4j-binary/) parser has also been extended to be able to read and write RDF-star data.

### Reading / writing a Turtle-star file

A Turtle-star file that contains an annotation with a certainty score, on a statement saying "Bob's age is 23", would look like this:

```turtle
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix ex: <http://example.org/> .

<<ex:bob foaf:age 23>> ex:certainty 0.9 .
```

If we wish to read this data into an RDF4J Model object, we can do so using the Rio Turtle-star parser:

```java
Model model = Rio.parse(new FileInputStream("/path/to/file.ttls"), "", RDFFormat.TURTLESTAR);
```

Similarly, Rio can be used to write RDF-star models to file:
```java
Rio.write(model, new FileOutputStream("/path/to/file.ttls"), "", RDFFormat.TURTLESTAR);
```

## Storing and retrieving RDF-star in a Repository

Note: not every store can handle RDF-star data. Attempting to upload an RDF-star model directly to a Repository that does not support it will result in errors.

The RDF4J MemoryStore accepts RDF-star data. You can add the RDF-star model we created above directly to an in-memory Repository:

```java
try(RepositoryConnection conn = repo.getConnection()) {
    conn.add(model);
}
```

You can query this data via the Repository API, like any "normal" RDF data. For example:

```java
try(RepositoryConnection conn = repo.getConnection()) {
   RepositoryResult<Statement> result = conn.getStatements(null, null, null);
   result.forEach(System.out::println);
}
```
will output:

```turtle
<<<http://example.org/bob> <http://xmlns.com/foaf/0.1/age> 23>> <http://example.org/certainty> 0.9
```

and of course the subject triple can be inspected in code as well:

```java
try(RepositoryConnection conn = repo.getConnection()) {
   RepositoryResult<Statement> result = conn.getStatements(null, null, null);
   Statement st = result.next();
   Triple rdfStarTriple = (Triple)st.getSubject();
   System.out.println(rdfStarTriple.getSubject()); // will output http://example.org/bob
}
```

### SPARQL query results containing RDF-star

When querying a store that contains RDF-star data, even regular SPARQL queries may include RDF-star triples as a result. To make it possible to serialize such query results (e.g. for network transfer), the SPARQL/JSON, SPARQL/XML, Binary and TSV query result formats have all been extended to handle RDF-star triples as possible value bindings.

#### Extended SPARQL JSON format

The default [SPARQL 1.1 Query Results JSON format](https://www.w3.org/TR/sparql11-results-json/) has been extended as in the following example:

```json
{
  "head" : {
    "vars" : [
      "a",
      "b",
      "c"
    ]
  },
  "results" : {
    "bindings": [
      { "a" : {
          "type" : "triple",
          "value" : {
            "subject" : {
              "type" : "uri",
              "value" : "http://example.org/bob"
            },
            "predicate" : {
              "type" : "uri",
              "value" : "http://xmlns.com/foaf/0.1/age"
            },
            "object" : {
              "datatype" : "http://www.w3.org/2001/XMLSchema#integer",
              "type" : "literal",
              "value" : "23"
            }
          }
        },
        "b": {
          "type": "uri",
          "value": "http://example.org/certainty"
        },
        "c" : {
          "datatype" : "http://www.w3.org/2001/XMLSchema#decimal",
          "type" : "literal",
          "value" : "0.9"
        }
      }
    ]
  }
}
```

RDF4J introduces a new, custom content type `application/x-sparqlstar-results+json` to specifically request this extended content. By default, if a client requests the standard content type (`application/sparql-results+json` or `application/json`), the response serializer will instead "compress" the RDF-star triple into a single IRI, using Base64 encoding:

```json
{
  "head" : {
    "vars" : [
      "a",
      "b",
      "c"
    ]
  },
  "results" : {
    "bindings": [
      { "a" : {
          "type" : "uri",
          "value" : "urn:rdf4j:triple:PDw8aHR0cDovL2V4YW1wbGUub3JnL2JvYj4gPGh0dHA6Ly94bWxucy5jb20vZm9hZi8wLjEvYWdlPiAiMjMiXl48aHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjaW50ZWdlcj4-Pg=="
        },
        "b": {
          "type": "uri",
          "value": "http://example.org/certainty"
        },
        "c" : {
          "datatype" : "http://www.w3.org/2001/XMLSchema#decimal",
          "type" : "literal",
          "value" : "0.9"
        }
      }
    ]
  }
}
```

This ensures that by default, an RDF4J-based endpoint will always use standards-compliant serialization to avoid breaking clients that have not yet been updated.

It is possible to override this behavior by explicitly configuring the writer to _not_ encode RDF-star triples. Setting the {{< javadoc "BasicWriterSetting ENCODE_RDF_STAR" "rio/helpers/BasicWriterSettings.html" >}} to `false` will ensure that even when requesting the standard content type, the serializer will use the extended syntax.

#### Extended SPARQL XML format

The default [SPARQL Query Results XML format](https://www.w3.org/TR/rdf-sparql-XMLres/) has been extended as in the following example:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<sparql xmlns='http://www.w3.org/2005/sparql-results#'>
  <head>
    <variable name='a'/>
    <variable name='b'/>
    <variable name='c'/>
  </head>
  <results>
    <result>
      <binding name='a'>
        <triple>
          <subject>
            <uri>http://example.org/bob</uri>
          </subject>
          <predicate>
            <uri>http://xmlns.com/foaf/0.1/age</uri>
          </predicate>
          <object>
            <literal datatype='http://www.w3.org/2001/XMLSchema#integer'>23</literal>
          </object>
        </triple>
      </binding>
      <binding name='b'>
        <uri>http://example.org/certainty</uri>
      </binding>
      <binding name='c'>
        <literal datatype='http://www.w3.org/2001/XMLSchema#decimal'>0.9</literal>
      </binding>
    </result>
  </results>
</sparql>
```

As with the extended SPARQL Results JSON format, this format is sent by default only when specifically requesting a custom content type: `application/x-sparqlstar-results+xml`. And as with the JSON format, the XML writer can be reconfigured to also send the extended format on requesting the regular SPARQL/XML content type, by setting the {{< javadoc "BasicWriterSetting ENCODE_RDF_STAR" "rio/helpers/BasicWriterSettings.html" >}} to `false`.

#### Extended TSV format

The [SPARQL 1.1 Query Results TSV format](https://www.w3.org/TR/sparql11-results-csv-tsv/) has been extended as follows:

```
?a	?b	?c
<<<http://example.org/bob> <http://xmlns.com/foaf/0.1/age> 23>>	<http://example.org/certainty>	0.9
```

### SPARQL-star queries

The SPARQL engine in RDF4J has been extended to allow for SPARQL-star queries. Executing a SPARQL-star query relies on the underlying store supporting RDF-star data storage.

SPARQL-star allows accessing the RDF-star triple patterns directly in the query. For example, after you have uploaded the above simple RDF-star model to a MemoryStore, you can execute a query like this:

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://example.org/>
SELECT ?p ?a ?c WHERE {
   <<?p foaf:age ?a>> ex:certainty ?c .
}
```

The result will be:

    ?p      ?a     ?c
    ex:bob  23     0.9

## Converting RDF-star to regular RDF and back

RDF4J offers functions to convert between RDF-star data and regular RDF. In the converted regular RDF, the RDF-star triple is replaced with a reified statement using the [RDF Reification vocabulary](https://www.w3.org/wiki/RdfReification). For example:


```turtle
<<ex:bob foaf:age 23>> ex:certainty 0.9 .
```

becomes:

```turtle
_:node1 a rdf:Statement;
        rdf:subject ex:bob ;
        rdf:predicate foaf:age ;
        rdf:object 23 ;
        ex:certainty 0.9 .
```

You can find the the conversion functions in the {{< javadoc "Models" "model/util/Models.html" >}} utility class. There are several variants, but the simplest form just takes a `Model` containing RDF-star data and creates a new `Model` containing the same data, but with all RDF-star annotation converted to reified statements:

```java
Model rdfStarModel; // model containing RDF-star annotations
Model convertedModel = Models.convertRDFStarToReification(rdfStarModel);
```

Likewise, you can convert back:

```java
Model reificiationModel; // model containing RDF reification statements
Model rdfStarModel = Models.convertReificiationtoRDFStar(reificiationModel);
```

Note: since the RDF-star functionality is currently in experimental stage, it is possible that the names or precise signatures of these functions will be changed in a future release.

