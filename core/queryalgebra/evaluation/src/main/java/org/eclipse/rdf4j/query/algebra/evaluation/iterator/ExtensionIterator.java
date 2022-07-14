/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

@Deprecated(since = "4.1.0")
public class ExtensionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	private final Consumer<MutableBindingSet> setter;
	private final QueryEvaluationContext context;

	public ExtensionIterator(Extension extension, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			EvaluationStrategy strategy, QueryEvaluationContext context) throws QueryEvaluationException {
		super(iter);
		this.context = context;
		this.setter = buildLambdaToEvaluateTheExpressions(extension, strategy, context);
	}

	public ExtensionIterator(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Consumer<MutableBindingSet> setter, QueryEvaluationContext context) throws QueryEvaluationException {
		super(iter);
		this.setter = setter;
		this.context = context;
	}

	public static Consumer<MutableBindingSet> buildLambdaToEvaluateTheExpressions(Extension extension,
			EvaluationStrategy strategy, QueryEvaluationContext context) {
		Consumer<MutableBindingSet> consumer = null;
		for (ExtensionElem extElem : extension.getElements()) {
			ValueExpr expr = extElem.getExpr();
			if (!(expr instanceof AggregateOperator)) {
				QueryValueEvaluationStep prepared = strategy.precompile(extElem.getExpr(), context);
				BiConsumer<Value, MutableBindingSet> setBinding = context.setBinding(extElem.getName());
				consumer = andThen(consumer, (targetBindings) -> setValue(setBinding, prepared, targetBindings));
			}
		}
		if (consumer == null) {
			return (bs) -> {

			};
		}
		return consumer;
	}

	private static void setValue(BiConsumer<Value, MutableBindingSet> setBinding, QueryValueEvaluationStep prepared,
			MutableBindingSet targetBindings) {
		try {
			// we evaluate each extension element over the targetbindings, so that bindings from
			// a previous extension element in this same extension can be used by other extension elements.
			// e.g. if a projection contains (?a + ?b as ?c) (?c * 2 as ?d)
			Value targetValue = prepared.evaluate(targetBindings);

			if (targetValue != null) {
				// Potentially overwrites bindings from super
				setBinding.accept(targetValue, targetBindings);
			}
		} catch (ValueExprEvaluationException e) {
			// silently ignore type errors in extension arguments. They should not cause the
			// query to fail but result in no bindings for this solution
			// see https://www.w3.org/TR/sparql11-query/#assignment
			// use null as place holder for unbound variables that must remain so
			setBinding.accept(null, targetBindings);
		}
	}

	private static Consumer<MutableBindingSet> andThen(Consumer<MutableBindingSet> consumer,
			Consumer<MutableBindingSet> next) {
		if (consumer == null)
			return next;
		else
			return consumer.andThen(next);
	}

	@Override
	public BindingSet convert(BindingSet sourceBindings) throws QueryEvaluationException {
		MutableBindingSet targetBindings = context.createBindingSet(sourceBindings);
		setter.accept(targetBindings);
		return targetBindings;
	}
}
