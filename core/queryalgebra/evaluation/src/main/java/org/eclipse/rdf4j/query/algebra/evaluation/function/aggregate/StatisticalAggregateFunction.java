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
package org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate;

import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunction;

/**
 * {@link AggregateFunction} used for processing of extended statistical aggregate operations through SPARQL.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@Experimental
public class StatisticalAggregateFunction extends AggregateFunction<StatisticCollector, Value> {

	public StatisticalAggregateFunction(Function<BindingSet, Value> evaluationStep) {
		super(evaluationStep);
	}

	@Override
	public void processAggregate(BindingSet bindingSet, Predicate<Value> distinctValue, StatisticCollector collector)
			throws QueryEvaluationException {
		if (collector.hasError()) {
			// Prevent calculating the aggregate further if a type error has occurred.
			return;
		}
		Value v = evaluate(bindingSet);
		if (distinctValue.test(v)) {
			if (v instanceof Literal) {
				Literal nextLiteral = (Literal) v;
				// check if the literal is numeric.
				if (((Literal) v).getCoreDatatype()
						.asXSDDatatype()
						.orElseThrow(() -> new ValueExprEvaluationException("not an XSD type literal: " + v))
						.isNumericDatatype()) {
					collector.addValue(nextLiteral);
				} else {
					collector.setTypeError(new ValueExprEvaluationException("not a number: " + v));
				}
			} else if (v != null) {
				// we do not actually throw the exception yet, but record it and
				// stop further processing. The exception will be thrown when
				// getValue() is invoked.
				collector.setTypeError(new ValueExprEvaluationException("not a literal: " + v));
			}
		}
	}
}
