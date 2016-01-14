/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

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
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public class ExtensionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	private final Extension extension;

	private final EvaluationStrategy strategy;

	public ExtensionIterator(Extension extension,
			CloseableIteration<BindingSet, QueryEvaluationException> iter, EvaluationStrategy strategy)
		throws QueryEvaluationException
	{
		super(iter);
		this.extension = extension;
		this.strategy = strategy;
	}

	@Override
	public BindingSet convert(BindingSet sourceBindings)
		throws QueryEvaluationException
	{
		QueryBindingSet targetBindings = new QueryBindingSet(sourceBindings);

		for (ExtensionElem extElem : extension.getElements()) {
			ValueExpr expr = extElem.getExpr();
			if (!(expr instanceof AggregateOperator)) {
				try {
					// we evaluate each extension element over the targetbindings, so that bindings from
					// a previous extension element in this same extension can be used by other extension elements. 
					// e.g. if a projection contains (?a + ?b as ?c) (?c * 2 as ?d)
					Value targetValue = strategy.evaluate(extElem.getExpr(), targetBindings);

					if (targetValue != null) {
						// Potentially overwrites bindings from super
						targetBindings.setBinding(extElem.getName(), targetValue);
					}
				}
				catch (ValueExprEvaluationException e) {
					// silently ignore type errors in extension arguments. They should not cause the 
					// query to fail but just result in no additional binding.
				}
			}
		}

		return targetBindings;
	}
}
