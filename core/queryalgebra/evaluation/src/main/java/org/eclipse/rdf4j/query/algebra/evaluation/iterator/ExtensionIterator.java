/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public class ExtensionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	private Consumer<QueryBindingSet> setter;

	public ExtensionIterator(Extension extension, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			EvaluationStrategy strategy) throws QueryEvaluationException {
		super(iter);
		this.setter = buildLambdaToEvaluateTheExpressions(extension, strategy);
	}

	public ExtensionIterator(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Consumer<QueryBindingSet> setter) throws QueryEvaluationException {
		super(iter);
		this.setter = setter;
	}

	public static Consumer<QueryBindingSet> buildLambdaToEvaluateTheExpressions(Extension extension,
			EvaluationStrategy strategy) {
		Consumer<QueryBindingSet> consumer = null;
		for (ExtensionElem extElem : extension.getElements()) {
			ValueExpr expr = extElem.getExpr();
			if (!(expr instanceof AggregateOperator)) {
				QueryValueEvaluationStep prepared = strategy.prepare(extElem.getExpr());
				consumer = andThen(consumer, (targetBindings) -> setValue(extElem.getName(), prepared, targetBindings));
			}
		}
		if (consumer == null) {
			return (bs) -> {

			};
		}
		return consumer;
	}

	private static void setValue(String extElem, QueryValueEvaluationStep prepared, QueryBindingSet targetBindings) {
		try {
			// we evaluate each extension element over the targetbindings, so that bindings from
			// a previous extension element in this same extension can be used by other extension elements.
			// e.g. if a projection contains (?a + ?b as ?c) (?c * 2 as ?d)
			Value targetValue = prepared.evaluate(targetBindings);

			if (targetValue != null) {
				// Potentially overwrites bindings from super
				targetBindings.setBinding(extElem, targetValue);
			}
		} catch (ValueExprEvaluationException e) {
			// silently ignore type errors in extension arguments. They should not cause the
			// query to fail but result in no bindings for this solution
			// see https://www.w3.org/TR/sparql11-query/#assignment
			// use null as place holder for unbound variables that must remain so
			targetBindings.setBinding(extElem, null);
		}
	}

	private static Consumer<QueryBindingSet> andThen(Consumer<QueryBindingSet> consumer,
			Consumer<QueryBindingSet> next) {
		if (consumer == null)
			return next;
		else
			return consumer.andThen(next);
	}

	@Override
	public BindingSet convert(BindingSet sourceBindings) throws QueryEvaluationException {
		QueryBindingSet targetBindings = new QueryBindingSet(sourceBindings);
		setter.accept(targetBindings);
		return targetBindings;
	}
}
