package org.eclipse.rdf4j.sparqlbuilder.constraint;

import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

public class NotIn extends Expression<NotIn> {
	private List<RdfValue> options;
	private Variable var;

	NotIn(Variable var, RdfValue... options) {
		super(null, ", ");
		setOperatorName(var.getQueryString() + " NOT IN");
		parenthesize(true);
		addOperand(options);
	}
}
