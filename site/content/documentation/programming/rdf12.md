---
title: "RDF 1.2 triple terms and SPARQL 1.2"
toc: true
weight: 9
autonumbering: true
---
RDF4J supports RDF 1.2 triple terms, reification, and SPARQL 1.2 query syntax.
<!--more-->

RDF 1.2 adds triple terms as a fourth kind of RDF term. Triple terms let you
refer to a triple as the object of another triple, typically by using
`rdf:reifies` or the RDF 1.2 annotation syntax available in Turtle and TriG.
SPARQL 1.2 adds the corresponding query syntax and result serializations.

The main W3C standards related to this page are
[RDF 1.2 Concepts and Abstract Data Model](https://www.w3.org/TR/rdf12-concepts/),
[RDF 1.2 Turtle](https://www.w3.org/TR/rdf12-turtle/),
[RDF 1.2 TriG](https://www.w3.org/TR/rdf12-trig/),
[SPARQL 1.2 Query Language](https://www.w3.org/TR/sparql12-query/),
[SPARQL 1.2 Query Results JSON Format](https://www.w3.org/TR/sparql12-results-json/),
[SPARQL 1.2 Query Results XML Format](https://www.w3.org/TR/sparql12-results-xml/),
and [SPARQL 1.2 Query Results CSV and TSV Formats](https://www.w3.org/TR/sparql12-results-csv-tsv/).

RDF4J support for these features includes:

- creating and inspecting triple terms in the model API
- reading and writing RDF 1.2 data with Rio using the regular RDF format constants
- persisting RDF 1.2 data in stores that support triple terms
- querying triple terms with SPARQL 1.2, regular Repository API calls, or both
- converting between RDF 1.2 reification and RDF 1.1 standard reification

## The RDF 1.2 data model in RDF4J

RDF4J represents RDF 1.2 triple terms with
{{< javadoc "TripleTerm" "model/TripleTerm.html" >}}.
A `TripleTerm` is a `Value`, not a
{{< javadoc "Statement" "model/Statement.html" >}}: it captures
the subject, predicate, and object of a triple so that the triple can be used
as the object of an `rdf:reifies` statement.

This matches the RDF 1.2 data model defined by
[RDF 1.2 Concepts and Abstract Data Model](https://www.w3.org/TR/rdf12-concepts/).

You can create `TripleTerm` objects using a `ValueFactory` or through the static `Values` factory methods:

```java
ValueFactory vf = SimpleValueFactory.getInstance();
IRI bob = vf.createIRI("http://example.org/bob");
IRI assertion = Values.iri("http://example.org/assertions/bobs-age");
IRI certainty = Values.iri("http://example.org/certainty");

TripleTerm byFactoryTripleTerm = vf.createTripleTerm(bob, FOAF.AGE, vf.createLiteral(23));

TripleTerm byValuesTripleTerm = Values.tripleTerm(
                Values.iri("http://example.org/bob"),
                FOAF.AGE,
                Values.literal(23)
        );

Statement reifyingStatement = Statements.statement(assertion, RDF.REIFIES, byValuesTripleTerm, null);
Statement certaintyStatement = Statements.statement(assertion, certainty, Values.literal(0.9), null);
```

The {{< javadoc "Statements" "model/util/Statements.html" >}} and
{{< javadoc "Values" "model/util/Values.html" >}} utility classes also offer
helpers to move between an asserted statement and a triple term:

```java
IRI bob = Values.iri("http://example.org/bob");
Statement ageStatement = Statements.statement(bob, FOAF.AGE, Values.literal(23), null);

TripleTerm bobsAge = Values.tripleTerm(ageStatement);
Statement backToStatement = Statements.toStatement(bobsAge);
```

## Reading and writing RDF 1.2 data

Rio parsers and writers handle RDF 1.2 data through the regular format
constants, including Turtle, TriG, N-Triples, N-Quads, and RDF/XML. When a
syntax supports RDF 1.2 version announcements, RDF4J emits them automatically
when the output uses RDF 1.2-specific features such as triple terms or
annotation syntax.

For the syntax details, see the W3C specifications for
[RDF 1.2 Turtle](https://www.w3.org/TR/rdf12-turtle/),
[RDF 1.2 TriG](https://www.w3.org/TR/rdf12-trig/),
[RDF 1.2 N-Triples](https://www.w3.org/TR/rdf12-n-triples/),
[RDF 1.2 N-Quads](https://www.w3.org/TR/rdf12-n-quads/),
and [RDF 1.2 XML Syntax](https://www.w3.org/TR/rdf12-xml/).

### Reading and writing a Turtle file with annotations

A Turtle file that annotates the statement "Bob's age is 23" with a certainty
score can be written like this:

```turtle
VERSION "1.2"
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://example.org/>

ex:bob foaf:age 23 {| ex:certainty 0.9 |} .
```

The annotation syntax above asserts `ex:bob foaf:age 23` and introduces a
reifier that can carry the extra metadata.

To read this data into an RDF4J `Model`, use the regular Turtle parser:

```java
Model model = Rio.parse(new FileInputStream("/path/to/file.ttl"), "", RDFFormat.TURTLE);
```

Similarly, Rio can write RDF 1.2 data back to Turtle:

```java
Rio.write(model, new FileOutputStream("/path/to/file.ttl"), RDFFormat.TURTLE);
```

The same pattern applies to TriG, N-Triples, N-Quads, and RDF/XML using
`RDFFormat.TRIG`, `RDFFormat.NTRIPLES`, `RDFFormat.NQUADS`, and
`RDFFormat.RDFXML`.

## Storing and retrieving RDF 1.2 data in a Repository

Not every store can handle triple terms natively. Attempting to upload an RDF
1.2 model to a Repository that does not support them can result in errors or
compatibility encodings instead of native storage.

The RDF4J MemoryStore accepts RDF 1.2 data. You can add the model created above
directly to an in-memory Repository:

```java
try(RepositoryConnection conn = repo.getConnection()) {
   conn.add(model);
}
```

You can inspect reifying statements through the Repository API like any other
statement:

```java
try(RepositoryConnection conn = repo.getConnection()) {
  RepositoryResult<Statement> result = conn.getStatements(null, RDF.REIFIES, null);
  Statement st = result.next();
  TripleTerm tripleTerm = (TripleTerm) st.getObject();
  System.out.println(tripleTerm.getSubject()); // http://example.org/bob
}
```

### SPARQL query results containing triple terms

SPARQL 1.2 query result formats can serialize triple terms as binding values.
This includes SPARQL/JSON, SPARQL/XML, the binary query result format, and TSV.

#### SPARQL JSON format

The [SPARQL 1.2 Query Results JSON format](https://www.w3.org/TR/sparql12-results-json/)
uses `"type": "triple"` for such bindings:

```json
{
 "head" : {
   "vars" : [
     "triple",
     "certainty"
   ]
 },
 "results" : {
   "bindings": [
     { "triple" : {
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
       "certainty" : {
         "datatype" : "http://www.w3.org/2001/XMLSchema#decimal",
         "type" : "literal",
         "value" : "0.9"
        }
      }
    ]
  }
}
```

#### SPARQL XML format

The [SPARQL 1.2 Query Results XML format](https://www.w3.org/TR/sparql12-results-xml/)
uses a `<triple>` element:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<sparql xmlns='http://www.w3.org/2005/sparql-results#'>
  <head>
    <variable name='triple'/>
    <variable name='certainty'/>
  </head>
  <results>
    <result>
      <binding name='triple'>
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
      <binding name='certainty'>
        <literal datatype='http://www.w3.org/2001/XMLSchema#decimal'>0.9</literal>
      </binding>
    </result>
  </results>
</sparql>
```

For RDF 1.1 interoperability, writers that do not support triple terms natively
can encode them as special IRIs. In RDF4J this behavior is controlled by
{{< javadoc "BasicWriterSettings ENCODE_TRIPLE_TERMS" "rio/helpers/BasicWriterSettings.html" >}}.

#### TSV format

The [SPARQL 1.2 Query Results CSV and TSV format](https://www.w3.org/TR/sparql12-results-csv-tsv/)
uses the RDF 1.2 triple-term syntax:

```
?triple	?certainty
<<( <http://example.org/bob> <http://xmlns.com/foaf/0.1/age> 23 )>>	0.9
```

### SPARQL 1.2 queries

The SPARQL engine in RDF4J supports SPARQL 1.2 triple-term syntax. Executing
these queries relies on the underlying store supporting RDF 1.2 data storage.

The query syntax follows the
[SPARQL 1.2 Query Language](https://www.w3.org/TR/sparql12-query/) specification.

For example, after loading the annotated age statement shown above into a
MemoryStore, you can query for the reifier and its metadata like this:

```sparql
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://example.org/>

SELECT ?person ?age ?certainty WHERE {
   ?assertion rdf:reifies <<( ?person foaf:age ?age )>> ;
              ex:certainty ?certainty .
}
```

The result will be:

    ?person ?age ?certainty
    ex:bob  23   0.9

## Converting RDF 1.2 reification to RDF 1.1 reification and back

RDF4J offers functions to convert between RDF 1.2 reification and RDF 1.1
standard reification. For example:


```turtle
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://example.org/>

ex:bobsAge rdf:reifies <<( ex:bob foaf:age 23 )>> ;
           ex:certainty 0.9 .
```

becomes:

```turtle
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://example.org/>

ex:bobsAge a rdf:Statement ;
           rdf:subject ex:bob ;
           rdf:predicate foaf:age ;
           rdf:object 23 ;
           ex:certainty 0.9 .
```

You can find the conversion functions in the
{{< javadoc "Models" "model/util/Models.html" >}} utility class. To convert an
RDF 1.2 model to RDF 1.1 standard reification:

```java
Model rdf12Model; // model containing rdf:reifies triples
Model rdf11Model = Models.convertRDF12ReificationToRDF11(rdf12Model);
```

To convert RDF 1.1 standard reification back to RDF 1.2:

```java
Model rdf11Model; // model containing rdf:Statement / rdf:subject / rdf:predicate / rdf:object
Model rdf12Model = Models.convertReificationToRDF12(rdf11Model);
```
