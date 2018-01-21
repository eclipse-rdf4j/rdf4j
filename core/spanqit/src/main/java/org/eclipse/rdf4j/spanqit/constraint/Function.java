package org.eclipse.rdf4j.spanqit.constraint;

/**
 * A SPARQL Function. Consists of a function name and a parenthesized,
 * comma-separated list of arguments.
 * 
 * @see <a href="http://www.w3.org/TR/rdf-sparql-query/#termConstraint">SPARQL
 *      Filters</a>
 *      <br>
 *      <a href=
 *      "http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#SparqlOps">
 *      SPARQL Function Definitions</a>
 */
class Function extends Expression<Function> {
	Function(SparqlFunction function) {
		super(function, ", ");
		parenthesize();
		setOperatorName(operator.getQueryString(), false);
	}
}