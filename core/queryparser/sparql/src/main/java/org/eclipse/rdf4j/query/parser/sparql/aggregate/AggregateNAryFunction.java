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

import java.util.List;
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
public abstract class AggregateNAryFunction<T extends AggregateCollector, D> implements AggregateProcessor<T, List<D>> {

	protected final BiFunction<Integer, BindingSet, Value> evaluationStepByIndex;

	protected AggregateNAryFunction(BiFunction<Integer, BindingSet, Value> evaluationStepByIndex) {
		this.evaluationStepByIndex = evaluationStepByIndex;
	}

	/**
	 * Process an aggregate with tuple-level distinctness for n-ary functions.
	 *
	 * @param bindingSet    the current binding set
	 * @param distinctTuple predicate to check if the tuple of argument values is distinct. the tuple may contain
	 *                      arbitrary amount of arguments, therefore if necessary single argument distinctness can be
	 *                      checked inside the predicate. Mixing argument sizes of tuples is not recommended.
	 * @param agv           the aggregate collector
	 * @throws QueryEvaluationException if evaluation fails
	 */
	public abstract void processAggregate(BindingSet bindingSet, Predicate<List<D>> distinctTuple, T agv)
			throws QueryEvaluationException;

	protected Value evaluate(Integer index, BindingSet s) throws QueryEvaluationException {
		return evaluationStepByIndex.apply(index, s);
	}
}
