/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.aggregate;

import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 *
 * @param <T>
 * @param <D>
 */
@Experimental
public abstract class AggregateFunction<T extends AggregateCollector, D> {

	protected final Function<BindingSet, Value> evaluationStep;

	public AggregateFunction(Function<BindingSet, Value> evaluationStep) {
		this.evaluationStep = evaluationStep;
	}

	public abstract void processAggregate(BindingSet bindingSet, Predicate<D> distinctValue, T agv)
			throws QueryEvaluationException;

	protected Value evaluate(BindingSet s) throws QueryEvaluationException {
		return evaluationStep.apply(s);
	}
}
