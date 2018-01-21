package org.eclipse.rdf4j.spanqit.constraint;

/**
 * The SPARQL binary operators
 */
enum BinaryOperator implements SparqlOperator {
	EQUALS("="),
	GREATER_THAN(">"),
	GREATER_THAN_EQUALS(">="),
	LESS_THAN("<"),
	LESS_THAN_EQUALS("<="),
	NOT_EQUALS("!=");
	
	private String operator;
	
	private BinaryOperator(String operator) {
		this.operator = operator;
	}
	
	@Override
	public String getQueryString() {
		return operator;
	}
}