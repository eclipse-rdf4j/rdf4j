package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

public class LeftJoinPostFilterQueryEvaluationStep implements QueryEvaluationStep {

    private final QueryEvaluationStep wrapped;
	private final ScopeBindingsJoinConditionEvaluator joinConditionEvaluator;

    public LeftJoinPostFilterQueryEvaluationStep(QueryEvaluationStep wrapped,
                                                 ScopeBindingsJoinConditionEvaluator joinConditionEvaluator) {
		this.wrapped = wrapped;
        this.joinConditionEvaluator = joinConditionEvaluator;
    }

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet leftBindings) {
		var rightIteration = wrapped.evaluate(leftBindings);

		if (rightIteration == QueryEvaluationStep.EMPTY_ITERATION) {
			return rightIteration;
		}

		return new FilterIteration<>(rightIteration) {

			@Override
			protected boolean accept(BindingSet bindings) {
				return joinConditionEvaluator.evaluate(bindings);
			}

			@Override
			protected void handleClose() {
				rightIteration.close();
			}
		};
	}
}
