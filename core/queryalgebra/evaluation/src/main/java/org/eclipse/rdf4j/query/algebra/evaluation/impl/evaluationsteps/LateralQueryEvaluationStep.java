package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

public class LateralQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep left;
	private final QueryEvaluationStep right;
	private final Lateral node;
	private final QueryEvaluationContext context;

	private LateralQueryEvaluationStep(QueryEvaluationStep left, QueryEvaluationStep right, Lateral node,
			QueryEvaluationContext context) {
		this.left = left;
		this.right = right;
		this.node = node;
		this.context = context;

	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		throw new QueryEvaluationException(new UnsupportedOperationException("LATERAL is not yet supported"));
	}

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, Lateral node,
			QueryEvaluationContext context) {
		QueryEvaluationStep left = strategy.precompile(node.getLeftArg(), context);
		QueryEvaluationStep right = strategy.precompile(node.getRightArg(), context);
		return new LateralQueryEvaluationStep(left, right, node, context);
	}

}
