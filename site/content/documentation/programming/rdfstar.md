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

{{< highlight java >}}
IRI bob = valueFactory.createIRI("http://example.org/bob");
Triple bobsAge = valueFactory.createTriple(bob, FOAF.AGE, valueFactory.createLiteral(23));

IRI certainty = valueFactory.createIRI("http://example.org/certainty");
Statement aboutBobsAge = valueFactory.createStatement(bobsAge, certainty, valueFactory.createLiteral(0.9));
{{< / highlight >}}

# Reading and writing RDF\* data

RDF4J currently provides several Rio parser/writers for RDF\*-enabled syntax formats: the Turtle\* format, the TriG* format. As the names suggest, both are extended versions of existing RDF format (Turtle and TriG, respectively). In addition, RDF4J's binary RDF format parser has also been extended to be able to read and write RDF\* data.

## Reading / writing a Turtle\* file

A Turtle\* file that contains an annotation with a certainty score, on a statement saying "Bob's age is 23", would look like this:

     @prefix foaf: <http://xmlns.com/foaf/0.1/> .
     @prefix ex: <http://example.org/> .

     <<ex:bob foaf:age 23>> ex:certainty 0.9 .

If we wish to read this data into an RDF4J Model object, we can do so using the Rio Turtle\* parser:

{{< highlight java >}}
Model model = Rio.parse(new FileInputStream("/path/to/file.ttls"), "", RDFFORMAT.TURTLESTAR);
{{< / highlight >}}

Similarly, Rio can be used to write RDF\* models to file:
{{< highlight java >}}
Rio.write(model, new FileOuputStream("/path/to/file.ttls"), "", RDFFORMAT.TURTLESTAR);
{{< / highlight >}}

# Storing and retrieving RDF\* in a Repository 

Note: not every store can handle RDF\* data. Attempting to upload an RDF\* model directly to a Repository that does not support it will result in errors.

The RDF4J MemoryStore accepts RDF\* data. You can add the RDF\* model we created above directly to an in-memory Repository:

{{< highlight java >}}
try(RepositoryConnection conn = repo.getConnection()) {
    conn.add(model);
}
{{< / highlight >}}

You can query this data via the Repository API, like any "normal" RDF data. For example:

{{< highlight java >}}
try(RepositoryConnection conn = repo.getConnection()) {
   RepositoryResult<Statement> result = conn.getStatements(null, null, null); 
   result.forEach(System.out::println);
}
{{< / highlight >}}

will output:

    <<<http://example.org/bob> <http://xmlns.com/foaf/0.1/name> 23>> <http://example.org/certainty> 0.9 

and of course the subject triple can be inspected in code as well:

{{< highlight java >}}
try(RepositoryConnection conn = repo.getConnection()) {
   RepositoryResult<Statement> result = conn.getStatements(null, null, null); 
   Statement st = result.next(); 
   Triple rdfStarTriple = (Triple)st.getSubject();
   System.out.println(rdfStarTriple.getSubject()); // will output http://example.org/bob 
}
{{< / highlight >}}

Likewise, regular SPARQL queries may include the triple as a result. The SPARQL/JSON and TSV query result formats have been extended to handle RDF\* triples as possible value bindings (support in other query result formats is under development). For example, if you run a simple SPARQL like `SELECT * where { ?a ?b ?c }` and serialize the result as SPARQL/JSON, it would look like this:

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
                  "value" : "http://xmlns.com/foaf/0.1/name"
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

## SPARQL\* queries

The SPARQL engine in RDF4J has been extended to allow for SPARQL\* queries. Executing a SPARQL\* query relies on the underlying store supporting RDF\* data storage. 

SPARQL\* allows accessing the RDF\* triple patterns directly in the query. For example, after you have uploaded the above simple RDF\* model to a MemoryStore, you can execute a query like this:

    PREFIX foaf: <http://xmlns.com/foaf/0.1/>
    PREFIX ex: <http://example.org/>
    SELECT ?p ?a ?c WHERE {
       <<?p foaf:age ?a>> ex:certainty ?c .
    }

The result will be:

    ?p      ?a     ?c
    ex:bob  23     0.9

# Converting RDF\* to regular RDF and back

RDF4J offers functions to convert between RDF\* data and regular RDF. In the converted regular RDF, the RDF\* triple is replaced with a reified statement using the [RDF Reification vocabulary](https://www.w3.org/wiki/RdfReification). For example:


     <<ex:bob foaf:age 23>> ex:certainty 0.9 .

becomes:

    _:node1 a rdf:Statement; 
            rdf:subject ex:bob ;
            rdf:predicate foaf:age ;
            rdf:object 23 ;
            ex:certainty 0.9 .

You can find the the conversion functions in the {{< javadoc "Models" "model/util/Models.html" >}} utility class. There are several variants, but the simplest form just takes a `Model` containing RDF\* data and creates a new `Model` containing the same data, but with all RDF\* annotation converted to reified statements:

{{< highlight java >}}
Model rdfStarModel; // model containing RDF\* annotations
Model convertedModel = Models.convertRDFStarToReification(rdfStarModel);
{{< / highlight >}}

Likewise, you can convert back:

{{< highlight java >}}
Model reificiationModel; // model containing RDF reification statements
Model rdfStarModel = Models.convertReificiationtoRDFStar(reificiationModel);
{{< / highlight >}}

Note: since the RDF\* functionality is currently in experimental stage, it is possible that the names or precise signatures of these functions will be changed in a future release.

