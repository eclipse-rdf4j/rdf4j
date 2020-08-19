---
title: "RDF* and SPARQL*"
toc: true
weight: 9
---
(new in RDF4J 3.2)

RDF4J has (experimental) support for RDF\* and SPARQL\*.
<!--more-->

RDF\*
and its companion SPARQL\* are proposed extensions to the RDF and SPARQL standards (see [Olaf
Hartig's position paper](https://blog.liu.se/olafhartig/2019/01/10/position-statement-rdf-star-and-sparql-star/) )
to provide a more convenient way to annotate RDF statements and to query such
annotations. In essence, RDF\* attempts to bridge the gap between the RDF world
and the Property Graph world. 

RDF4J support for these extensions currently (3.2.0) covers:

 - reading and writing RDF\* data in a variety of syntax formats (including Turtle\* and TriG*)
 - converting between an RDF\* Model using annotations and a regular RDF Model (translating the annotations to regular RDF reification)
 - persisting RDF\* data in the Memory Memory Store and querying with SPARQL\*, regular SPARQL and / or API calls
 - adding extension hooks for third-party triplestores that implement the SAIL API to allow persistence and querying of RDF\* annotations

Note: these features are currently in the experimental/beta stage. While we'll do our best to not make breaking changes in future releases unless necessary, we make no guarantees.

# The RDF\* data model in RDF4J

To support RDF\* annotations, the core RDF model in RDF4J has been extended with a new type of Resource: the {{< javadoc "Triple" "model/Triple.html" >}} (not to be confused with the pre-existing {{< javadoc "Statement" "model/Statement.html" >}}, which is the representation of a "regular" RDF statement). 

You can create `Triple` objects using a `ValueFactory`, and then use them as the subject (or object) of another statement, for example:

```java
IRI bob = valueFactory.createIRI("http://example.org/bob");
Triple bobsAge = valueFactory.createTriple(bob, FOAF.AGE, valueFactory.createLiteral(23));

IRI certainty = valueFactory.createIRI("http://example.org/certainty");
Statement aboutBobsAge = valueFactory.createStatement(bobsAge, certainty, valueFactory.createLiteral(0.9));
```

# Reading and writing RDF\* data

RDF4J currently provides several Rio parser/writers for RDF\*-enabled syntax formats: the Turtle\* format, and the TriG\* format. As the names suggest, both are extended versions of existing RDF format (Turtle and TriG, respectively). In addition, RDF4J's [binary RDF format](/documentation/reference/rdf4j-binary/) parser has also been extended to be able to read and write RDF\* data.

## Reading / writing a Turtle\* file

A Turtle\* file that contains an annotation with a certainty score, on a statement saying "Bob's age is 23", would look like this:

```turtle
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix ex: <http://example.org/> .

<<ex:bob foaf:age 23>> ex:certainty 0.9 .
```

If we wish to read this data into an RDF4J Model object, we can do so using the Rio Turtle\* parser:

```java
Model model = Rio.parse(new FileInputStream("/path/to/file.ttls"), "", RDFFORMAT.TURTLESTAR);
```

Similarly, Rio can be used to write RDF\* models to file:
```java
Rio.write(model, new FileOuputStream("/path/to/file.ttls"), "", RDFFORMAT.TURTLESTAR);
```

# Storing and retrieving RDF\* in a Repository 

Note: not every store can handle RDF\* data. Attempting to upload an RDF\* model directly to a Repository that does not support it will result in errors.

The RDF4J MemoryStore accepts RDF\* data. You can add the RDF\* model we created above directly to an in-memory Repository:

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

## SPARQL query results containing RDF\*

When querying a store that contains RDF\* data, even regular SPARQL queries may include RDF\* triples as a result. To make it possible to serialize such query results (e.g. for network transfer), the SPARQL/JSON, SPARQL/XML, Binary and TSV query result formats have all been extended to handle RDF\* triples as possible value bindings.

### Extended SPARQL JSON format

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
            "s" : {
              "type" : "uri",
              "value" : "http://example.org/bob"
            },
            "p" : {
              "type" : "uri",
              "value" : "http://xmlns.com/foaf/0.1/age"
            },
            "o" : {
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

RDF4J introduces a new, custom content type `application/x-sparqlstar-results+json` to specifically request this extended content. By default, if a client requests the standard content type (`application/sparql-results+json` or `application/json`), the response serializer will instead "compress" the RDF\* triple into a single IRI, using Base64 encoding:

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

It is possible to override this behavior by explicitly configuring the writer to _not_ encode RDF\* triples. Setting the {{< javadoc "BasicWriterSetting ENCODE_RDF_STAR" "rio/helpers/BasicWriterSettings.html" >}} to `false` will ensure that even when requesting the standard content type, the serializer will use the extended syntax.

### Extended SPARQL XML format

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

### Extended TSV format

The [SPARQL 1.1 Query Results TSV format](https://www.w3.org/TR/sparql11-results-csv-tsv/) has been extended as follows:

```
?a	?b	?c
<<<http://example.org/bob> <http://xmlns.com/foaf/0.1/age> 23>>	<http://example.org/certainty>	0.9
```

## SPARQL\* queries

The SPARQL engine in RDF4J has been extended to allow for SPARQL\* queries. Executing a SPARQL\* query relies on the underlying store supporting RDF\* data storage. 

SPARQL\* allows accessing the RDF\* triple patterns directly in the query. For example, after you have uploaded the above simple RDF\* model to a MemoryStore, you can execute a query like this:

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

# Converting RDF\* to regular RDF and back

RDF4J offers functions to convert between RDF\* data and regular RDF. In the converted regular RDF, the RDF\* triple is replaced with a reified statement using the [RDF Reification vocabulary](https://www.w3.org/wiki/RdfReification). For example:


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

You can find the the conversion functions in the {{< javadoc "Models" "model/util/Models.html" >}} utility class. There are several variants, but the simplest form just takes a `Model` containing RDF\* data and creates a new `Model` containing the same data, but with all RDF\* annotation converted to reified statements:

```java
Model rdfStarModel; // model containing RDF\* annotations
Model convertedModel = Models.convertRDFStarToReification(rdfStarModel);
```

Likewise, you can convert back:

```java
Model reificiationModel; // model containing RDF reification statements
Model rdfStarModel = Models.convertReificiationtoRDFStar(reificiationModel);
```

Note: since the RDF\* functionality is currently in experimental stage, it is possible that the names or precise signatures of these functions will be changed in a future release.

# Property Graphs (PG) vs Separate Assertions (SA)

In the [current discussions around RDF\*](https://lists.w3.org/Archives/Public/public-rdf-star/), two "modes" for working with RDF\* have been identified. Simply put, these two modes are:

1. Property Graph (PG) mode - in this mode, when an RDF\* triple is annotated and added, the associated "ground" statement is automatically assumed to also exist. In this mode, asserting `<<ex:bob foaf:age 23>> ex:certainty 0.9 .` implicitly also asserts the statement `ex:bob foaf:age 23.`. 
2. Separate Assertions (SA) mode - in this mode, RDF\* annotation are seen as separate from the statements which they annotate. In this mode, asserting `<<ex:bob foaf:age 23>> ex:certainty 0.9` does not automatically imply that the statement `ex:bob foaf:age 23` is present - the ground fact and the annotation are seen as independent entities, and either one can exist without the other. 

Although RDF4J makes no assumptions about the mode used at the level of its core interfaces, the default implementations in RDF4J's own triplestore implementations currently work only in SA mode.

