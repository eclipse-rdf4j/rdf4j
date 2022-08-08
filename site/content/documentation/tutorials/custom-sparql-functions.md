---
title: "Creating custom SPARQL functions"
weight: 3
toc: true
---
In this short tutoral, we'll create a simple custom function and add it RDF4J's SPARQL engine.

<!--more-->
The SPARQL query language is extensible by nature: it allows you to add your own custom functions if the standard set of operators is not sufficient for your needs. The RDF4J SPARQL engine has been designed with this extensibility in mind: you can define your own custom function and use it as part of your SPARQL queries.

Here, we are going to implement a boolean function that detects if some string literal is a [palindrome](https://en.wikipedia.org/wiki/Palindrome).

## The palindrome function

Suppose we have the following RDF data:

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix ex: <http://example.org/> .

ex:a rdfs:label "step on no pets" .
ex:b rdfs:label "go on, try it" .
```

We would like to be able to formulate a SPARQL query that allows us to retrieve all resources that have a palindrome as their label:

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX cfn: <http://example.org/custom-function/>
SELECT ?x ?label
WHERE {
   ?x rdfs:label ?label .
   FILTER(cfn:palindrome(str(?label)))
}
```

The expected result of this query, given the above data, would be:

| x      | label               |
|--------|---------------------|
| `ex:a` | `"step on no pets"` |

Unfortunately, the function `cfn:palindrome` is not a standard SPARQL function, so this query won’t work: the RDF4J SPARQL engine will simply report an error.

We could of course retrieve all label values in the database and then do some checking ourselves on these values, to detect if they’re palindromes. However if we add a custom function instead, we remove the need to scan over the entire database: the SPARQL engine itself can determine if a value is a valid palindrome or not, which removes the need for us to loop over all possible values.

There’s two basic steps in adding custom functions to RDF4J:

1. implementing a Java class for the function;
2. creating a JAR file with your function code in it and an Service Provider Interface (SPI) configuration.

## Implementing the custom function as a Java class

In the RDF4J SPARQL engine, functions are expected to implement the {{< javadoc "Function" "/query/algebra/evaluation/Function.html" >}} interface.

```java
package org.eclipser.rdf4j.examples.function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

public class PalindromeFunction implements Function { }
```

The `Function` interface defines two methods: `evaluate()` and `getURI()`. The latter of these is a simple method that returns a string representation of the URI of the function:

```java
// define a constant for the namespace of our custom function
public static final String NAMESPACE = "http://example.org/custom-function/";

/**
 * return the URI 'http://example.org/custom-function/palindrome' as a String
 */
public String getURI() {
    return NAMESPACE + "palindrome";
}
```

The real proof of the pudding is in the `evaluate()` method: this is where the function logic is implemented. In other words, in this method we check the incoming value to see if it is, first of all, a valid argument for the function, and second of all, a palindrome, and return the result.

{{< example "Example 1" "function/PalindromeFunction.java" >}} show how we put everything together:

```java
package org.eclipse.rdf4j.examples.function;

import static org.eclipse.rdf4j.model.util.Values.literal;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * An example custom SPARQL function that detects palindromes
 *
 * @author Jeen Broekstra
 */
public class PalindromeFunction implements Function {

    // define a constant for the namespace of our custom function
    public static final String NAMESPACE = "http://example.org/custom-function/";

    /**
     * return the URI 'http://example.org/custom-function/palindrome' as a
     * String
     */
    public String getURI() {
	return NAMESPACE + "palindrome";
    }

    /**
     * Executes the palindrome function.
     *
     * @return A boolean literal representing true if the input argument is a
     *         palindrome, false otherwise.
     * @throws ValueExprEvaluationException
     *         if more than one argument is supplied or if the supplied argument
     *         is not a literal.
     */
    public Value evaluate(TripleSource tripleSource, Value... args)
	throws ValueExprEvaluationException
	{
	    // our palindrome function expects only a single argument, so throw an error
	    // if there's more than one
	    if (args.length != 1) {
		throw new ValueExprEvaluationException(
			"palindrome function requires"
			+ "exactly 1 argument, got "
			+ args.length);
	    }
	    Value arg = args[0];
	    // check if the argument is a literal, if not, we throw an error
	    if (!(arg instanceof Literal)) {
		throw new ValueExprEvaluationException(
			"invalid argument (literal expected): " + arg);
	    }

	    // get the actual string value that we want to check for palindrome-ness.
	    String label = ((Literal)arg).getLabel();
	    // we invert our string
	    String inverted = "";
	    for (int i = label.length() - 1; i >= 0; i--) {
		inverted += label.charAt(i);
	    }
	    // a string is a palindrome if it is equal to its own inverse
	    boolean palindrome = inverted.equalsIgnoreCase(label);

	    // a function is always expected to return a Value object, so we
	    // return our boolean result as a Literal
	    return literal(palindrome);
	}
}
```

You are completely free to implement your function logic: in the above example, we have created a function that only returns `true` or `false`, but since the actual return type of an RDF4J function is {{< javadoc "Value" "model/Value.html" >}}, you can create functions that return string literals, numbers, dates, or even IRIs or blank nodes.

In addition, the `evaluate` method accepts a `TripleSource` as input parameter, which you can use to inspect the underlying database, and query it for further information (for a simple/silly example see the {{< example "Existing Palindrome function" "function/ExstingPalindromeFunction.java" >}}, which in addition to checking that the argument is a palindrome, also checks if that palindrome already exists in the database).

There are two important things to keep in mind though:

- the `evaluate()` method is invoked for every single solution in the query result. So you should make sure that the implementation of your function is not overly complex and memory-intensive.
- RDF4J treats functions as singletons. This means that you should not "keep state" as part of your function; for example storing intermediate results in a private object field. This state will carry over between different uses of the function and even between different queries using it, making your results inconsistent.

Once we have created the Java class for our function, we need some way to make the RDF4J SPARQL engine aware of it. This is where the Service Provider Interface (SPI) comes into play.

## Creating an SPI configuration

RDF4J's set of SPARQL functions is dynamically determined through the use of a `java.util.ServiceLoader` class. Specifically, RDF4J has a class called {{< javadoc "FunctionRegistry" "/query/algebra/evaluation/function/FunctionRegistry.html" >}} which keeps track of all implementations of the `Function` interface. Java’s SPI mechanism depends on the presence of configuration files in the JAR files that contain service implementations. This configuration file is expected to be present in the directory `META-INF/services` in your JAR file.

In the case of the SPARQL function registry, the name of this configuration file should be `org.eclipse.rdf4j.query.algebra.evaluation.function.Function` (in other words, the file name is equal to the fully-qualified name of the service interface we are providing an implementation for). The contents are really quite simple: an SPI configuration is a text file, containing the fully-qualified names of each Java class that provides an SPI implementation, one on each line. So in our case, the contents of the file would be:

   org.eclipse.rdf4j.example.function.PalindromeFunction

Apart from this configuration file, your JAR file should of course also contain the actual compiled class. All of this is fairly easy to do, for example from your Eclipse project:

1. create a directory `META-INF` and a subdirectory `META-INF/services` within the `src` directory of your project (or, if you use Maven, within `src/main/resources`) See [our example resources dir](https://github.com/eclipse/rdf4j/tree/main/examples/src/main/resources) for an example;
2. Add a text file named `org.eclipse.rdf4j.query.algebra.evaluation.function.Function` to this new directory. Make sure it contains a single line with the fully qualified name of your custom function class (in our example, that’s `org.eclipse.rdf4j.example.function.PalindromeFunction`);
3. Use Eclipse’s export function (or alternatively Maven’s `package` command) to create a JAR file (select the project, click ‘File’ -> ‘Export’ -> ‘JAR file’). Make sure the JAR file produced contains your compiled code and the sevice registry config file.

Once you have a proper JAR file, you need to add it the runtime classpath of your RDF4J project (or if you're aiming to use this in an RDF4J Server, add it to the RDF4J Server webapp classpath and restart). After that, you’re done: RDF4J should automatically pick up your new custom function, you can from now on use it in your SPARQL queries.

## Further reading

* [Introduction to the Service Provider Interface](http://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) - Oracle Java documentation.

If you require any further help, you can [contact us to get support](http://rdf4j.org/support). We welcome your feedback.
