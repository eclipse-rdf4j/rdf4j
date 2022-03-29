---
title: "Validation with SHACL"
weight: 6
toc: true
autonumbering: true
---
The SHapes Constraint Language (SHACL) is a language for validating RDF graphs.
<!--more-->
This documentation is for RDF4J 2.5 and onwards, as the SHACL engine was experimental until this release.

## How does the SHACL engine work

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

## How to load and update SHACL shapes

By default, the ShaclSail uses a reserved graph (`http://rdf4j.org/schema/rdf4j#SHACLShapeGraph`) for storing the SHACL shapes.
Utilize a normal connection to load your shapes into this graph. SPARQL is not supported.

The [Shapes Graph](#shapes-graph) section explains how to store your shapes in a different named graph.  

```java
ShaclSail shaclSail = new ShaclSail(new MemoryStore());
Repository repo = new SailRepository(shaclSail);

try (RepositoryConnection connection = repo.getConnection()) {

    Reader shaclRules = ....

    // add shapes
    connection.begin();
    connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
    connection.commit();
    
    // clear existing shapes and add new ones in single transaction (eg. update shapes)
    connection.begin();
    connection.clear(RDF4J.SHACL_SHAPE_GRAPH);
    connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
    connection.commit();

    // clear all shapes
    connection.begin();
    connection.clear(RDF4J.SHACL_SHAPE_GRAPH);
    connection.commit();
    
    // connection.clear(); will not clear the Shapes graph!


}
```

You can at any point update your shapes. Updating shapes will cause your data to be re-validated. The transaction
will fail if the data is not valid according to the changed shapes.

The simplest way to update your shapes is within a transaction, drop the shapes graph and load your updated shapes.
The ShaclSail will only apply the actual changes. Modifying shapes in parallel should not be a problem, but may cause
slowdowns.

Remember that a full validation is run for all the affect shapes. Depending on the amount of
data already in your store this may take a significant amount of time. If you are sure that your
data will not violate the shapes, or otherwise need to skip validation then you can read the section on [Performance](#performance).

Do not use SPARQL to update your shapes!

## Supported SHACL features

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
- `sh:inversePath`
- `sh:property`
- `sh:node`
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
- `sh:hasValue`
- `sh:qualifiedMaxCount`
- `sh:qualifiedMinCount`
- `sh:qualifiedValueShape`
- 'sh:shapesGraph'
- `dash:hasValueIn`
- `sh:target` for use with DASH targets
- `rsx:targetShape`

DASH and RSX features need to be explicitly enabled, for instance with `setDashDataShapes(true)` and
`setEclipseRdf4jShaclExtensions(true)`. These are currently experimental features. For more information
about the RSX features, see the [RSX section](#rsx---eclipse-rdf4j-shacl-extensions) of this document.

Implicit `sh:targetClass` is supported for nodes that are `rdfs:Class` and either of `sh:PropertyShape` or `sh:NodeShape`. Validation for all nodes,
equivalent to `owl:Thing` or `rdfs:Resource` in an environment with a reasoner, can be enabled by setting `setUndefinedTargetValidatesAllSubjects(true)`.

`sh:path` is limited to single predicate paths, e.g. `ex:age` or a single inverse path. Sequence paths, alternative paths and the like are not supported.

Nested `sh:property` is not supported.

## Validation results

On `commit()` the ShaclSail will validate your changes and throw an exception if there are violations. The exception contains a validation report and can be retrieved like this:

```java
try {
    connection.commit();
} catch (RepositoryException exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof ValidationException) {
        Model validationReportModel = ((ValidationException) cause).validationReportAsModel();
        // use validationReportModel to understand validation violations

        Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
    }
    throw exception;
}
```

The `validationReportModel` follows the report format specified by the W3C SHACL recommendation. It does not provide all the information specified in the recommendation. Example report:

```turtle
[]
    a sh:ValidationReport ;
    sh:conforms false ;
    rdf4j:truncated false;
    sh:result [
        a sh:ValidationResult ;
        sh:value "eighteen";
        sh:focusNode <http://example.com/ns#pete> ;
        sh:resultPath <http://example.com/ns#age> ;
        sh:sourceConstraintComponent sh:DatatypeConstraintComponent ;
        sh:sourceShape <http://example.com/ns#PersonShapeAgeProperty> ;
    ] .
```

The `ValidationReport` class provides the same information as the validationReportModel, but as a Java object with getters for accessing the report data.

There is no support for `sh:severity`, all violations will trigger an exception.

### Limiting the validation report

Large validation reports take time to generate and can use large amounts of memory.
Limiting the size of the report can be useful to speed up validation and to reduce the number of similar violations.

Limitations can either be configured directly in the ShaclSail or through the configuration files.

 - `setValidationResultsLimitTotal(1000)` limits the total number of validation results per report to 1000. (1 000 000 by default)
     - `<http://rdf4j.org/config/sail/shacl#validationResultsLimitTotal>`
 - `setValidationResultsLimitPerConstraint(10)` limits the number of validation results per constraint component to 10. (1000 by default)
     - `<http://rdf4j.org/config/sail/shacl#validationResultsLimitPerConstraint>`

 Use -1 to remove a limit and 0 to validate but return an empty validation report. 

 A truncated validation report will have `isTruncated()` return true and the model will have `rdf4j:truncated true`.

### Retrieving violated shapes

Since all shapes are stored in the SHACL shapes graph, the actual shape that was violated can be retrieved from the
ShaclSail when a transaction fails.

```java
try {
    connection.commit();
} catch (RepositoryException exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof ValidationException) {
        Model validationReportModel = ((ValidationException) cause).validationReportAsModel();

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
```

## Transactional support

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

Typically, in order to handle this scenario a user would need to use SERIALIZABLE transactions, which are slow and
prone to failure. The ShaclSail instead uses locking to run transactions one-after-the-other if the isolation level is set to
SNAPSHOT. This is typically 2-4x faster than using SERIALIZABLE.

Locking only affects transactions that write to the ShaclSail, and locking is only applied when calling commit().

It is possible to disable this type of validation with `setSerializableValidation(false)`.

## Performance
The ShaclSail is built for performance. Each transaction is analyzed so that only the minimal set of shapes need to be
validated, and for each of those shapes only the least amount of data is retrieved in order to perform the validation.

Parallel validation further increases performance and is enabled by default. This can be disabled with `setParallelValidation(false)`.

Some workloads will not fit in memory and need to be validated while stored on disk. This can be achieved by using a
NativeStore and using the new transaction settings introduced in 3.3.0.

 - `ShaclSail.TransactionSettings.ValidationApproach.Auto`: Let the ShaclSail choose the best approach.
 - `ShaclSail.TransactionSettings.ValidationApproach.Bulk`: Optimized for large transactions, disables caching and parallel validation and runs a full validation step at the end of the transaction.
 - `ShaclSail.TransactionSettings.ValidationApproach.Disabled`: Disable validation.

Disabling validation for a transaction may leave your data in an invalid state. Running a transaction with bulk validation will force a full validation.
This is a useful approach if you need to use multiple transactions to bulk load your data.

As of 3.6.0 there are also a set of experimental transaction settings for hinting about performance aspects of the validation.
- `ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled`: Enable the cache that stores intermediate results so these only need to be computed once.
- `ShaclSail.TransactionSettings.PerformanceHint.CacheDisabled`: Disable the cache.
- `ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation`: Run validation in parallel (multithreaded).
- `ShaclSail.TransactionSettings.PerformanceHint.SerialValidation`:  Run validation in serial (single threaded).


```java
ShaclSail shaclSail = new ShaclSail(new NativeStore(new File(...), "spoc,ospc,psoc"));
SailRepository sailRepository = new SailRepository(shaclSail);

try (SailRepositoryConnection connection = sailRepository.getConnection()) {

	connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Bulk);

//	You can enable parallel validation and the intermediate cache for better performance if you have sufficient memory 
//	connection.begin(
//		IsolationLevels.NONE, 
//		ShaclSail.TransactionSettings.ValidationApproach.Bulk, 
//		ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled, 
//		ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation
//	);	
	
	// load shapes
	try (InputStream inputStream = new FileInputStream("shacl.ttl")) {
		connection.add(inputStream, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
	}

	// load data
	try (InputStream inputStream = new BufferedInputStream(new FileInputStream("data.ttl"))) {
		connection.add(inputStream, "", RDFFormat.TURTLE);
	}

	// commit transaction and catch any exception
	try {
		connection.commit();
	} catch (RepositoryException e){
		if(e.getCause() instanceof ValidationException){
			Model model = ((ValidationException) e.getCause()).validationReportAsModel();
			Rio.write(model, System.out, RDFFormat.TURTLE);
		}
	}

}

sailRepository.shutDown();
```

### Automatic bulk validation
Large transactions will take up significant amounts of memory because the transactional validation needs to analyze the transactional 
changes in order to decide what needs to be validated. Very large transactions could exceed the amount of memory available and cause the 
JVM to crash.

As of 4.0.0 transactions can automatically be switched to bulk validation if they exceed a set limit. 

- `setTransactionalValidationLimit(1000)` will make transactions switch to bulk validation if the transaction size is more than 1000 statements. Default is 500 000.
   - `<http://rdf4j.org/config/sail/shacl#transactionalValidationLimit>`

Automatic bulk validation is not compatible with serializable validation.

## Reasoning
By default, the ShaclSail supports the simple rdfs:subClassOf reasoning required by the W3C recommendation. There is no
support for `sh:entailment`, however the entire reasoner can be disabled with `setRdfsSubClassReasoning(false)`.

## Shapes graph

When shapes are used to validate the contents of a database it makes sense to consider them part of the database schema and as such 
keep them seperated from each other. This separation is achieved by storing shapes in a reserved graph 
(`http://rdf4j.org/schema/rdf4j#SHACLShapeGraph`) which is hidden unless specifically named when loading or removing data.

As of 4.0.0 the ShaclSail can be configured to load shapes from other graphs (both named graphs and the default graph).
- `setShapesGraphs(Set.of(RDF4J.SHACL_SHAPE_GRAPH, Values.iri("http://example.org/myShapeGraph"))` will read shapes from both the normal reserved graph and the named graph `http://example.org/myShapeGraph`.
    - `<http://rdf4j.org/config/sail/shacl#shapesGraph>`

    
Shapes stored in the reserved graph (`http://rdf4j.org/schema/rdf4j#SHACLShapeGraph`) are used to validate the union of all triples
in the default graph and any other named graph. The ShaclSail relies on `sh:shapesGraph` statements to understand how shapes stored
in other graphs should be used for validation.

SHACL uses the term shapes graph to refer to an RDF graph where the shapes are defined, and the term data graph to refer to an RDF
graph where the data to be validated is stored. Data graphs and shapes graphs are linked together using `sh:shapesGraph` statements
which are used by the ShaclSail to decide which shapes should be used to validate which graphs.

For security and performance the ShaclSail ignores `sh:shapesGraph` statements that are not in a graph that has been configured for 
shapes, as explained above. This means that you can always trust data you load into your database to not tamper with your shapes or
with which shapes are used for validation, as long as you limit which graphs your load the data into.

### Example

```trig
ex:shapesGraph1 {
    ex:PersonShape
        a sh:NodeShape  ;
        sh:targetClass ex:Person ;       
}  

ex:shapesGraph2 {
    ex:PersonShape       
        sh:property [
            sh:path ex:age ;
            sh:datatype xsd:integer ;
        ] .

    rdf4j:nil sh:shapesGraph ex:shapesGraph1, ex:shapesGraph2.         
}

ex:shapesGraph3 {
    ex:PersonShape       
        sh:property [
            sh:path ex:age ;
            sh:minCount 1;
        ] .

}
```

The above shapes will result in all the data in the default graph (unnamed graph) being validated against the shapes defined in the union
of both `ex:shapesGraph1` and `ex:shapesGraph2`. The shape defined in `ex:shapesGraph3` is effectively ignored. The resource `rdf4j:nil` is used to refer to the default graph.

The following data is valid.

```trig
{
    ex:steve a ex:Person.
    
    ex:jane a ex:Person;
        ex:age 40.
}  
```

While the following data is invalid.


```trig
{    
    ex:john a ex:Person;
        ex:age "seventy two".
}  
```


## RSX - Eclipse RDF4J SHACL Extensions
RDF4J has seen a need to develop its own extension the W3C SHACL Recommendation in order to support new
and innovative features. We always strive to collaborate with the community when developing these features.

RSX currently contains `rsx:targetShape` which will allow a Shape to be the target for your constraints. This means
that it will be possible to model more complex use-cases like "all norwegian companies with 10 or more employees,
a revenue greater than or equal to 6 million NOK or valued at 23 million or above are required to have a registered
accountant". This also allows for considerably faster implementations than what is currently possible with SPARQL Targets.

The RSX specification will be published soon together with the limited support for `rsx:targetShape` in 3.3.0 (`sh:or`, `sh:and`, `sh:hasValue`, `dash:hasValueIn`, `sh:path`, `sh:property`).

## Logging and debugging

By default, there is no logging enabled in the ShaclSail. There are four methods for enabling logging:

- `shaclSail.setLogValidationPlans(true);`
- `shaclSail.setGlobalLogValidationExecution(true);`
- `shaclSail.setLogValidationViolations(true);`
- `shaclSail.setPerformanceLogging(true);`

All these will log as `INFO`.

First step to debugging and understanding an unexpected violation is to enable `shaclSail.setLogValidationViolations(true);`.

### Log validation plans

Validation plans are logged as Graphviz DOT. Validations plans are a form of query plan.

Here is the validation plan for the example above: [Link](https://dreampuf.github.io/GraphvizOnline/#digraph%20%20%7B%0Alabelloc%3Dt%3B%0Afontsize%3D30%3B%0Alabel%3D%22DatatypePropertyShape%22%3B%0A1866229258%20%5Blabel%3D%22Base%20sail%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1555990397%20%5Blabel%3D%22Added%20statements%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1544078442%20%5Blabel%3D%22Removed%20statements%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1337866219%20%5Blabel%3D%22Previous%20state%20connection%22%20nodeShape%3Dpentagon%20fillcolor%3Dlightblue%20style%3Dfilled%5D%3B%0A1291367132%20%5Blabel%3D%22DirectTupleFromFilter%22%5D%3B%0A1887699190%20%5Blabel%3D%22DatatypeFilter%7Bdatatype%3Dhttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23integer%7D%22%5D%3B%0A1479140596%20-%3E%201887699190%0A1887699190%20-%3E%201291367132%20%5Blabel%3D%22false%20values%22%5D%0A1479140596%20%5Blabel%3D%22UnionNode%22%5D%3B%0A1108889615%20-%3E%201479140596%0A1108889615%20%5Blabel%3D%22UnionNode%22%5D%3B%0A1275028674%20-%3E%201108889615%0A455888635%20%5Blabel%3D%22BufferedSplitter%22%5D%3B%0A204805934%20-%3E%20455888635%0A204805934%20%5Blabel%3D%22TrimTuple%7BnewLength%3D1%7D%22%5D%3B%0A204322447%20-%3E%20204805934%0A204322447%20%5Blabel%3D%22Select%7Bquery%3D%E2%80%99select%20*%20where%20%7B%20BIND(rdf%3Atype%20as%20%3Fb)%20%5Cn%20BIND(%3Chttp%3A%2F%2Fexample.com%2Fns%23Person%3E%20as%20%3Fc)%20%5Cn%20%3Fa%20%3Fb%20%3Fc.%7D%20order%20by%20%3Fa'%7D%22%5D%3B%0A1555990397%20-%3E%20204322447%0A1275028674%20%5Blabel%3D%22InnerJoin%22%5D%3B%0A455888635%20-%3E%201275028674%20%5Blabel%3D%22left%22%5D%3B%0A1019484860%20-%3E%201275028674%20%5Blabel%3D%22right%22%5D%3B%0A1019484860%20%5Blabel%3D%22DirectTupleFromFilter%22%5D%3B%0A1164365897%20%5Blabel%3D%22DatatypeFilter%7Bdatatype%3Dhttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23integer%7D%22%5D%3B%0A1640899500%20-%3E%201164365897%0A1164365897%20-%3E%201019484860%20%5Blabel%3D%22false%20values%22%5D%0A1640899500%20%5Blabel%3D%22Select%7Bquery%3D%E2%80%99select%20*%20where%20%7B%20%3Fa%20%3Chttp%3A%2F%2Fexample.com%2Fns%23age%3E%20%3Fc.%20%7D%20order%20by%20%3Fa'%7D%22%5D%3B%0A1555990397%20-%3E%201640899500%0A1275028674%20-%3E%203565780%20%5Blabel%3D%22discardedRight%22%5D%3B%0A473666452%20-%3E%201108889615%0A473666452%20%5Blabel%3D%22ExternalTypeFilterNode%7BfilterOnType%3Dhttp%3A%2F%2Fexample.com%2Fns%23Person%7D%22%5D%3B%0A3565780%20-%3E%20473666452%0A1337866219%20-%3E%20473666452%20%5Blabel%3D%22filter%20source%22%5D%0A3565780%20%5Blabel%3D%22BufferedTupleFromFilter%22%5D%3B%0A1865219266%20-%3E%201479140596%0A1865219266%20%5Blabel%3D%22BulkedExternalInnerJoin%7Bpredicate%3Dnull%2C%20query%3D'%3Fa%20%3Chttp%3A%2F%2Fexample.com%2Fns%23age%3E%20%3Fc.%20'%7D%22%5D%3B%0A455888635%20-%3E%201865219266%20%5Blabel%3D%22left%22%5D%0A1337866219%20-%3E%201865219266%20%5Blabel%3D%22right%22%5D%0A%7D%0A8)

The structure of this log and its contents may change in the future, without warning.

### Log validation execution

The execution of the validation plan shows what data was requested during the execution and how that data was joined together and filtered.

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

### Log validation violations

As the commit() call iterates over the shapes it can log the results (tuples) from the execution of each validation plan.

Following on from the example above:


    1. [main] INFO  SHACL not valid. The following experimental debug results were produced:
    2. 	NodeShape: http://example.com/ns#PersonShape
    3. 		Tuple{line=[http://example.com/ns#pete, "eighteen"], propertyShapes= DatatypePropertyShape <_:node1d285h2ktx1>} -cause->  [ Tuple{line=[http://example.com/ns#pete, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://example.com/ns#Person]} ]


Line 2 shows the shape that triggered this violation. Line 3 shows the ultimate tuple produced and which PropertyShape produced the exception followed by a cause listing other tuples that caused the violation. In this case the existing type statement.

The structure of this log and its contents may change in the future, without warning.

## Full working example

```java
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
import org.eclipse.rdf4j.sail.shacl.ValidationException;
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
                if (cause instanceof ValidationException) {
                    Model validationReportModel = ((ValidationException) cause).validationReportAsModel();

                    Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
                }
                throw exception;
            }
        }
    }
}
```

## Further reading

Here are some useful links to learn more about SHACL:

- [W3C SHACL specification](http://www.w3.org/TR/shacl/)
- [Validating RDF Data](http://book.validatingrdf.com/) (various authors)

