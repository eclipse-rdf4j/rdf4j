/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.aggregate;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * N-ary aggregate function processor.
 *
 * @param <T>
 * @param <D>
 *
 * @author Nik Kozlov
 */
@Experimental
public abstract class AggregateNAryFunction<T extends AggregateCollector, D> implements AggregateProcessor<T, D> {

	protected final BiFunction<Integer, BindingSet, Value> evaluationStepByIndex;

	protected AggregateNAryFunction(BiFunction<Integer, BindingSet, Value> evaluationStepByIndex) {
		this.evaluationStepByIndex = evaluationStepByIndex;
	}

	@Override
	public abstract void processAggregate(BindingSet bindingSet, Predicate<D> distinctValue, T agv)
			throws QueryEvaluationException;

	protected Value evaluate(Integer index, BindingSet s) throws QueryEvaluationException {
		return evaluationStepByIndex.apply(index, s);
	}
}
