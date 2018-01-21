package org.eclipse.rdf4j.spanqit.constraint;

/**
 * The built-in SPARQL aggregates. Keeping this public until
 * {@link Expressions} is completed.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#SparqlOps">
 *      SPARQL Function Definitions</a>
 */
@SuppressWarnings("javadoc") // acceptable, as this won't be public for long
public enum SparqlAggregate implements SparqlOperator {
	AVG("AVG"),
	COUNT("COUNT"),
	GROUP_CONCAT("GROUP_CONCAT"),
	MAX("MAX"),
	MIN("MIN"),
	SAMPLE("SAMPLE"),
	SUM("SUM");

	private String function;
	
	private SparqlAggregate(String function) {
		this.function = function;
	}
	
	@Override
	public String getQueryString() {
		return function;
	}
}