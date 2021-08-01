package org.eclipse.rdf4j.sparqlbuilder.constraint;

import org.eclipse.rdf4j.sparqlbuilder.core.Assignable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

public class Bind implements GraphPattern {
	private static final String AS = " AS ";
	private Assignable expression;
	private Variable var;

	Bind(Assignable exp, Variable var) {
		this.expression = exp;
		this.var = var;
	}

	@Override
	public String getQueryString() {
		return "BIND"
				+ SparqlBuilderUtils.getParenthesizedString(
						expression.getQueryString() + AS + var.getQueryString());
	}
}
