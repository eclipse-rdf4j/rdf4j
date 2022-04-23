package org.eclipse.rdf4j.sparqlbuilder.constraint;

public class In extends Function {
	private final Operand searchTerm;

	In(Operand searchTerm, Operand... expressions) {
		this(searchTerm, true, expressions);
	}

	In(Operand searchTerm, boolean in, Operand... expressions) {
		super(in ? SparqlFunction.IN : SparqlFunction.NOT_IN);
		this.searchTerm = searchTerm;
		addOperand(expressions);
	}

	@Override
	public String getQueryString() {
		StringBuilder inExpression = new StringBuilder();
		inExpression.append(searchTerm.getQueryString()).append(" ");
		inExpression.append(super.getQueryString());

		return inExpression.toString();
	}
}
