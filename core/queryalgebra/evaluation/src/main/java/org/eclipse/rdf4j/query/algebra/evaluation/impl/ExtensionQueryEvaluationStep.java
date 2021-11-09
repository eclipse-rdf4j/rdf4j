package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ExtensionIterator;

public final class ExtensionQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep arg;
	private final Consumer<MutableBindingSet> consumer;

	ExtensionQueryEvaluationStep(QueryEvaluationStep arg, Consumer<MutableBindingSet> consumer) {
		this.arg = arg;
		this.consumer = consumer;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
		CloseableIteration<BindingSet, QueryEvaluationException> result;
		try {
			result = arg.evaluate(bs);
		} catch (ValueExprEvaluationException e) {
			// a type error in an extension argument should be silently ignored
			// and
			// result in zero bindings.
			result = new EmptyIteration<>();
		}
		return new ExtensionIterator(result, consumer);
	}
}