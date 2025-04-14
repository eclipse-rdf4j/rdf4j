package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

public class LeftJoinPreFilterQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep wrapped;
	private final ScopeBindingsJoinConditionEvaluator joinConditionEvaluator;

    public LeftJoinPreFilterQueryEvaluationStep(QueryEvaluationStep wrapped,
                                                ScopeBindingsJoinConditionEvaluator joinConditionEvaluator) {
		this.wrapped = wrapped;
        this.joinConditionEvaluator = joinConditionEvaluator;
    }

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet leftBindings) {
		if (shouldEvaluate(leftBindings)) {
			return wrapped.evaluate(leftBindings);
		}

		return QueryEvaluationStep.EMPTY_ITERATION;
	}

	private boolean shouldEvaluate(BindingSet leftBindings) {
		return joinConditionEvaluator.evaluate(leftBindings);
	}
}
