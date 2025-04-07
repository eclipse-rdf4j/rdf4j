package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

import java.util.Set;

class ScopeBindingsJoinConditionEvaluator {

	/**
	 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that are (only)
	 * included because of the depth-first evaluation strategy in the evaluation of the constraint.
	 */
    private final Set<String> scopeBindingNames;
    private final QueryValueEvaluationStep joinCondition;

    ScopeBindingsJoinConditionEvaluator(Set<String> scopeBindingNames, QueryValueEvaluationStep joinCondition) {
        this.scopeBindingNames = scopeBindingNames;
        this.joinCondition = joinCondition;
    }

	public boolean evaluate(BindingSet bindings) {
		BindingSet scopeBindings = createScopeBindings(scopeBindingNames, bindings);

		return evaluate(joinCondition, scopeBindings);
	}

	private boolean evaluate(QueryValueEvaluationStep joinCondition, BindingSet scopeBindings) {
		try {
			Value value = joinCondition.evaluate(scopeBindings);
			return QueryEvaluationUtility.getEffectiveBooleanValue(value).orElse(false);
		} catch (ValueExprEvaluationException e) {
			// Ignore, condition not evaluated successfully
			return false;
		}
	}

	private BindingSet createScopeBindings(Set<String> scopeBindingNames, BindingSet bindings) {
		QueryBindingSet scopeBindings = new QueryBindingSet(scopeBindingNames.size());
		for (String scopeBindingName : scopeBindingNames) {
			Binding binding = bindings.getBinding(scopeBindingName);
			if (binding != null) {
				scopeBindings.addBinding(binding);
			}
		}

		return scopeBindings;
	}
}
