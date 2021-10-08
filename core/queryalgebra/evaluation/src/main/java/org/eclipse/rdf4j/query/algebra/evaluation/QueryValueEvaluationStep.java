package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

public interface QueryValueEvaluationStep {
	Value evaluate(BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException;
}
