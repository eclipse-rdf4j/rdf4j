/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ExtensionIterator;

public final class ExtensionQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep arg;
	private final Consumer<MutableBindingSet> consumer;
	private final QueryEvaluationContext context;

	ExtensionQueryEvaluationStep(QueryEvaluationStep arg, Consumer<MutableBindingSet> consumer,
			QueryEvaluationContext context) {
		this.arg = arg;
		this.consumer = consumer;
		this.context = context;
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
			result = QueryEvaluationStep.EMPTY_ITERATION;
		}
		return new ExtensionIterator(result, consumer, context);
	}
}
