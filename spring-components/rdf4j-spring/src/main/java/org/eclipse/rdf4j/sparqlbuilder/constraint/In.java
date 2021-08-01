package org.eclipse.rdf4j.sparqlbuilder.constraint;

import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

public class In extends Expression<In> {
	private List<RdfValue> options;
	private Variable var;

	In(Variable var, RdfValue... options) {
		super(null, ", ");
		setOperatorName(var.getQueryString() + " IN");
		parenthesize(true);
		addOperand(options);
	}
}
