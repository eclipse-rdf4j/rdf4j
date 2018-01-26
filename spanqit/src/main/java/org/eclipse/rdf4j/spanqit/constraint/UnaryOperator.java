package org.eclipse.rdf4j.spanqit.constraint;

/**
 * The SPARQL unary operators
 */
enum UnaryOperator implements SparqlOperator {
	NOT("!"),
	UNARY_PLUS("+"),
	UNARY_MINUS("-");

	private String operator;

	private UnaryOperator(String operator) {
		this.operator = operator;
	}
	
	@Override
	public String getQueryString() {
		return operator;
	}
}