---
title: "Validation with SHACL"
layout: "doc"
hide_page_title: "true"
---

# Validation with SHACL
  
(new in RDF4J 2.5)

The SHapes Constraint Language (SHACL) is a language for validating RDF graphs.

This documentation is for RDF4J 2.5 and onwards, as the SHACL engine was experimental until this release.

# How does the SHACL engine work

The SHACL engine works by analyzing the changes made in a transaction and creating a set of validation 
plans (similar to query plans) and executing these as part of the transaction `commit()` call.

In many cases the SHACL engine can validate your changes based on your changes alone. 
In some cases your changes may affect or be affected by data already in the database, in which case the 
engine will query the database for that particular data.

Here is an example of when data in the database affects the validation plans:

```turtle
ex:PersonShape
    a sh:NodeShape  ;
    sh:targetClass ex:Person ;
    sh:property [
        sh:path ex:age ;
        sh:datatype xsd:integer ;
    ] .
```

Initial data in the database.

```
ex:pete a ex:Person.
```
Data added by a transaction.

```
ex:pete ex:age "eighteen".
```

For this example the SHACL engine will match the predicate `ex:age` with `ex:PersonShape` and realise that `ex:pete` 
might already be defined as a `ex:Person` in the database. The validation plan will then include checking if `ex:pete` 
is type `ex:Person`.

# How to load and update SHACL shapes

The ShaclSail uses a reserved graph (`http://rdf4j.org/schema/rdf4j#SHACLShapeGraph`) for storing the SHACL shapes. 
Utilize a normal connection to load your shapes into this graph. SPARQL is not supported.

{{< highlight java >}}
ShaclSail shaclSail = new ShaclSail(new MemoryStore());
SailRepository sailRepository = new SailRepository(shaclSail);

try (SailRepositoryConnection connection = sailRepository.getConnection()) {
    connection.begin();

    Reader shaclRules = ....

    connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
    connection.commit();
}
{{< / highlight >}}

You can at any point update your shapes. Updating shapes will cause your data to be re-validated. The transaction
will fail if the data is not valid according to the changed shapes.

The simplest way to update your shapes is within a transaction, drop the shapes graph and load your updated shapes. 
The ShaclSail will only apply the actual changes. Modifying shapes in parallel should not be a problem, but may cause 
slowdowns.

Remember that a full validation is run for all the affect shapes. Depending on the amount of
data already in your store this may take a significant amount of time. If you are sure that your
data will not violate the shapes, or otherwise need to skip validation, you can 
temporarily disable validation for your entire ShaclSail with `setValidationEnabled(false)`. Keep
in mind that this affects all transactions.

Do not use SPARQL to update your shapes!

# Supported SHACL features

The SHACL W3C Recommendation defines the SHACL features that should be supported and RDF4J is working hard to 
support them all. At the moment a fairly large subset of these features are supported by the ShaclSail, however quite a 
few are not yet implemented.

An always-up-to-date list of features can be found by calling the static method `ShaclSail.getSupportedShaclPredicates()`.

As of writing this documentation the following features are supported.

- `sh:targetClass`
- `sh:targetNode`
- `sh:targetSubjectsOf`
- `sh:targetObjectsOf`
- `sh:path`
- `sh:property`
- `sh:or`
- `sh:and`
- `sh:not`
- `sh:minCount`
- `sh:maxCount`
- `sh:minLength`
- `sh:maxLength`
- `sh:pattern` and `sh:flags`
- `sh:nodeKind`
- `sh:languageIn`
- `sh:uniqueLang`
- `sh:datatype`
- `sh:class`
- `sh:minExclusive`
- `sh:minInclusive`
- `sh:maxExclusive`
- `sh:maxInclusive`
- `sh:in`
- `sh:deactivated`

Implicit `sh:targetClass` is supported for nodes that are `rdfs:Class` and either of `sh:PropertyShape` or `sh:NodeShape`. Validation for all nodes, equivalent to `owl:Thing` or `rdfs:Resource` in an environment with a reasoner, can be enabled by setting `setUndefinedTargetValidatesAllSubjects(true)`.

`sh:path` is limited to single predicate paths, eg. `ex:age`. Sequence paths, alternative paths, inverse paths and the like are not supported.

# Validation results

On `commit()` the ShaclSail will validate your changes and throw an exception if there are violations. The exception contains a validation report and can be retrieved like this:

{{< highlight java >}}
try {
    connection.commit();
} catch (RepositoryException exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof ShaclSailValidationException) {
        ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
        Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
        // use validationReport or validationReportModel to understand validation violations

        Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
    }
    throw exception;
}
{{< / highlight >}}

The `validationReportModel` follows the report format specified by the W3C SHACL recommendation. It does not provide all the information specified in the recommendation. Example report:

```turtle
[]
    a sh:ValidationReport ;
    sh:conforms false ;
    sh:result [
        a sh:ValidationResult ;
        sh:focusNode <http://example.com/ns#pete> ;
        sh:resultPath <http://example.com/ns#age> ;
        sh:sourceConstraintComponent sh:DatatypeConstraintComponent ;
        sh:sourceShape <http://example.com/ns#PersonShapeAgeProperty> ;
    ] .
```

The `ValidationReport` class provides the same information as the validationReportModel, but as a Java object with getters for accessing the report data.

There is no support for `sh:severity`, all violations will trigger an exception.

## Retrieving violated shapes

Since all shapes are stored in the SHACL shapes graph, the actual shape that was violated can be retrieved from the
ShaclSail when a transaction fails.

{{< highlight java >}}
try {
    connection.commit();
} catch (RepositoryException exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof ShaclSailValidationException) {
        Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
        
        validationReportModel
            .filter(null, SHACL.SOURCE_SHAPE, null);
            .forEach(s -> {
                Value object = s.getObject();
            
                try (SailRepositoryConnection connection = shaclSail.getConnection()) {
            
                    try (Stream<Statement> stream = connection.getStatements((Resource) object, null, null, RDF4J.SHACL_SHAPE_GRAPH).stream()) {
                        List<Statement> collect = stream.collect(Collectors.toList());
            
                        // collect contains the shape!
                    }
            
                }
        
            });
    }
    throw exception;
}
{{< / highlight >}}

# Transactional support

When running multiple transactions in parallel, the results from two transactions may jointly cause the validation to fail. 
A good example is as follows:

```turtle
ex:PersonShape
    a sh:NodeShape  ;
    sh:targetClass ex:Person ;
    sh:property [
        sh:path ex:age ;
        sh:datatype xsd:integer ;
    ] .
```

One transaction adds:

```
ex:pete a ex:Person.
```
In parallel another transaction adds:

```
ex:pete ex:age "eighteen".
```

Neither of these transactions will by themselves cause the validation to fail, but together they will. 

Typically in order to handle this scenario a user would need to use SERIALIZABLE transactions, which are slow and
prone to failure. The ShaclSail instead uses locking to run transactions one-after-the-other if the isolation level is set to
SNAPSHOT. This is typically 2-4x faster than using SERIALIZABLE.

Locking only affects transactions that write to the ShaclSail, and locking is only applied when calling commit(). 

It is possible to disable this type of validation with `setSerializableValidation(false)`.

# Performance
The ShaclSail is built for performance. Each transaction is analyzed so that only the minimal set of shapes need to be
validated, and for each of those shapes only the least amount of data is retrieved in order to perform the validation.

Parallel validation further increases performance. This can be disabled with `setParallelValidation(false)`.

The initial commit to an empty ShaclSail is further optimized if the underlying sail is a MemoryStore.

Some workloads will not fit in memory and need to be validated while stored on disk. This can be achieved by using a 
NativeStore and temporarily disabling the SHACL validation while loading data. After loading data there is a special
method to trigger a full validation against your shapes. The process is illustrated in the following example:

{{< highlight java >}}
ShaclSail shaclSail = new ShaclSail(new NativeStore(new File(...), "spoc,ospc,psoc"));

// significantly reduce required memory
shaclSail.setCacheSelectNodes(false);

// further reduce required memory by not running validation in parallel
shaclSail.setParallelValidation(false);

SailRepository sailRepository = new SailRepository(shaclSail);

shaclSail.disableValidation();

try (SailRepositoryConnection connection = sailRepository.getConnection()) {
    // load shapes
    connection.begin(IsolationLevels.NONE);
    try (InputStream inputStream = new FileInputStream("shacl.ttl")) {
        connection.add(inputStream, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
    }
    connection.commit();

    // load data
    connection.begin(IsolationLevels.NONE);
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream("data.ttl"))) {
        connection.add(inputStream, "", RDFFormat.TURTLE);
    }
    connection.commit();
}
shaclSail.enableValidation();

try (SailRepositoryConnection connection = sailRepository.getConnection()) {
    connection.begin(IsolationLevels.NONE);
    ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
    connection.commit();

    if (!revalidate.conforms()) {
        Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);
    }

}

sailRepository.shutDown();
{{< / highlight >}}

# Reasoning
By default the ShaclSail supports the simple rdfs:subClassOf reasoning required by the W3C recommendation. There is no
support for `sh:entailment`, however the entire reasoner can be disabled with `setRdfsSubClassReasoning(false)`.


# Logging and debugging

By default there is no logging enabled in the ShaclSail. There are four methods for enabling logging:

- `shaclSail.setLogValidationPlans(true);`
- `shaclSail.setGlobalLogValidationExecution(true);`
- `shaclSail.setLogValidationViolations(true);`
- `shaclSail.setPerformanceLogging(true);`

All these will log as `INFO`.

First step to debugging and understanding an unexpected violation is to enable `shaclSail.setLogValidationViolations(true);`.

## Log validation plans

Validation plans are logged as Graphviz DOT. Validations plans are a form of query plan.

Here is the validation plan for the example above: [Link](https://dreampuf.github.io/GraphvizOnline/#digraph%20%20%7B%0Alabelloc%3Dt%3B%0Afontsize%3D30%3B%0Alabel%3D%22DatatypePropertyShape%22%3B%0A1866229258%20%5Blabel%3D%22Base%20sail%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1555990397%20%5Blabel%3D%22Added%20statements%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1544078442%20%5Blabel%3D%22Removed%20statements%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1337866219%20%5Blabel%3D%22Previous%20state%20connection%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1291367132%20%5Blabel%3D%22DirectTupleFromFilter%22%5D%3B%0A1887699190%20%5Blabel%3D%22DatatypeFilter%7Bdatatype%3Dhttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23integer%7D%22%5D%3B%0A1479140596%20-%3E%201887699190%0A1887699190%20-%3E%201291367132%20%5Blabel%3D%22false%20values%22%5D%0A1479140596%20%5Blabel%3D%22UnionNode%22%5D%3B%0A1108889615%20-%3E%201479140596%0A1108889615%20%5Blabel%3D%22UnionNode%22%5D%3B%0A1275028674%20-%3E%201108889615%0A455888635%20%5Blabel%3D%22BufferedSplitter%22%5D%3B%0A204805934%20-%3E%20455888635%0A204805934%20%5Blabel%3D%22TrimTuple%7BnewLength%3D1%7D%22%5D%3B%0A204322447%20-%3E%20204805934%0A204322447%20%5Blabel%3D%22Select%7Bquery%3D%E2%80%99select%20*%20where%20%7B%20BIND(rdf%3Atype%20as%20%3Fb)%20%5Cn%20BIND(%3Chttp%3A%2F%2Fexample.com%2Fns%23Person%3E%20as%20%3Fc)%20%5Cn%20%3Fa%20%3Fb%20%3Fc.%7D%20order%20by%20%3Fa'%7D%22%5D%3B%0A1555990397%20-%3E%20204322447%0A1275028674%20%5Blabel%3D%22InnerJoin%22%5D%3B%0A455888635%20-%3E%201275028674%20%5Blabel%3D%22left%22%5D%3B%0A1019484860%20-%3E%201275028674%20%5Blabel%3D%22right%22%5D%3B%0A1019484860%20%5Blabel%3D%22DirectTupleFromFilter%22%5D%3B%0A1164365897%20%5Blabel%3D%22DatatypeFilter%7Bdatatype%3Dhttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23integer%7D%22%5D%3B%0A1640899500%20-%3E%201164365897%0A1164365897%20-%3E%201019484860%20%5Blabel%3D%22false%20values%22%5D%0A1640899500%20%5Blabel%3D%22Select%7Bquery%3D%E2%80%99select%20*%20where%20%7B%20%3Fa%20%3Chttp%3A%2F%2Fexample.com%2Fns%23age%3E%20%3Fc.%20%7D%20order%20by%20%3Fa'%7D%22%5D%3B%0A1555990397%20-%3E%201640899500%0A1275028674%20-%3E%203565780%20%5Blabel%3D%22discardedRight%22%5D%3B%0A473666452%20-%3E%201108889615%0A473666452%20%5Blabel%3D%22ExternalTypeFilterNode%7BfilterOnType%3Dhttp%3A%2F%2Fexample.com%2Fns%23Person%7D%22%5D%3B%0A3565780%20-%3E%20473666452%0A1337866219%20-%3E%20473666452%20%5Blabel%3D%22filter%20source%22%5D%0A3565780%20%5Blabel%3D%22BufferedTupleFromFilter%22%5D%3B%0A1865219266%20-%3E%201479140596%0A1865219266%20%5Blabel%3D%22BulkedExternalInnerJoin%7Bpredicate%3Dnull%2C%20query%3D'%3Fa%20%3Chttp%3A%2F%2Fexample.com%2Fns%23age%3E%20%3Fc.%20'%7D%22%5D%3B%0A455888635%20-%3E%201865219266%20%5Blabel%3D%22left%22%5D%0A1337866219%20-%3E%201865219266%20%5Blabel%3D%22right%22%5D%0A%7D%0A8)

The structure of this log and its contents may change in the future, without warning.

## Log validation execution

The execution of the validation plan shows what data was requested during the exeuction and how that data was joined together and filtered.

Enabling this logging will enable it for all ShaclSails on all threads.

Enabling this logging will make your validation considerably slower and take up considerably more memory.

Following on from the example above

    01. [main] INFO   Select.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    02. [main] INFO    DatatypeFilter;falseNode:  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    03. [main] INFO     DirectTupleFromFilter.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    04. [main] INFO      InnerJoin;discardedRight:  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    05. [main] INFO       BufferedTupleFromFilter.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    06. [main] INFO         ExternalTypeFilterNode.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    07. [main] INFO           UnionNode.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    08. [main] INFO             UnionNode.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    09. [main] INFO              DatatypeFilter;falseNode:  Tuple{line=[http://example.com/ns#pete, "eighteen"]}
    10. [main] INFO               DirectTupleFromFilter.next():  Tuple{line=[http://example.com/ns#pete, "eighteen"]}

The log is best read in conjunction with the validation plan. By taking the bottom log line (10.) as the bottom node in the plan and for each indentation following the plan upwards. Multiple lines at a given indentation mean that that node produced multiple tuples.

Line 6 shows a query to the underlying database. Line 1 is the query for everything matching the path (ex:age) against the added data in the transaction.

The indentation is best-effort.

The structure of this log and its contents may change in the future, without warning.

## Log validation violations

As the commit() call iterates over the shapes it can log the results (tuples) from the execution of each validation plan.

Following on from the example above:


    1. [main] INFO  SHACL not valid. The following experimental debug results were produced:
    2. 	NodeShape: http://example.com/ns#PersonShape
    3. 		Tuple{line=[http://example.com/ns#pete, "eighteen"], propertyShapes= DatatypePropertyShape <_:node1d285h2ktx1>} -cause->  [ Tuple{line=[http://example.com/ns#pete, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://example.com/ns#Person]} ]


Line 2 shows the shape that triggered this violation. Line 3 shows the ultimate tuple produced and which PropertyShape produced the exception followed by a cause listing other tuples that caused the violation. In this case the existing type statement.

The structure of this log and its contents may change in the future, without warning.

# Full working example

{{< highlight java >}}
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

public class ShaclSampleCode {

    public static void main(String[] args) throws IOException {

        ShaclSail shaclSail = new ShaclSail(new MemoryStore());

        //Logger root = (Logger) LoggerFactory.getLogger(ShaclSail.class.getName());
        //root.setLevel(Level.INFO);

        //shaclSail.setLogValidationPlans(true);
        //shaclSail.setGlobalLogValidationExecution(true);
        //shaclSail.setLogValidationViolations(true);

        SailRepository sailRepository = new SailRepository(shaclSail);
        sailRepository.init();

        try (SailRepositoryConnection connection = sailRepository.getConnection()) {

            connection.begin();

            StringReader shaclRules = new StringReader(
                String.join("\n", "",
                    "@prefix ex: <http://example.com/ns#> .",
                    "@prefix sh: <http://www.w3.org/ns/shacl#> .",
                    "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
                    "@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

                    "ex:PersonShape",
                    "  a sh:NodeShape  ;",
                    "  sh:targetClass foaf:Person ;",
                    "  sh:property ex:PersonShapeProperty .",

                    "ex:PersonShapeProperty ",
                    "  sh:path foaf:age ;",
                    "  sh:datatype xsd:int ;",
                    "  sh:maxCount 1 ;",
                    "  sh:minCount 1 ."
                ));

            connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
            connection.commit();

            connection.begin();

            StringReader invalidSampleData = new StringReader(
                String.join("\n", "",
                    "@prefix ex: <http://example.com/ns#> .",
                    "@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
                    "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

                    "ex:peter a foaf:Person ;",
                    "  foaf:age 20, \"30\"^^xsd:int  ."

                ));

            connection.add(invalidSampleData, "", RDFFormat.TURTLE);
            try {
                connection.commit();
            } catch (RepositoryException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof ShaclSailValidationException) {
                    ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
                    Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
                    // use validationReport or validationReportModel to understand validation violations

                    Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
                }
                throw exception;
            }
        }
    }
}
{{< / highlight >}}

# Further reading

Here are some useful links to learn more about SHACL:

- [W3C SHACL specification](http://www.w3.org/TR/shacl/)
- [Validating RDF Data](http://book.validatingrdf.com/) (various authors)


