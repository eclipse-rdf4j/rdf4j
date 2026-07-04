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
package org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.variance;

import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.StatisticalAggregateFunction;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateCollector;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunction;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunctionFactory;

/**
 * {@link AggregateFunctionFactory} implementation that provides {@link AggregateFunction} used for processing
 * population variance.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@Experimental
public class PopulationVarianceAggregateFactory implements AggregateFunctionFactory {

	@Override
	public String getIri() {
		return "http://rdf4j.org/aggregate#variance_population";
	}

	@Override
	public AggregateFunction buildFunction(Function<BindingSet, Value> evaluationStep) {
		return new StatisticalAggregateFunction(evaluationStep);
	}

	@Override
	public AggregateCollector getCollector() {
		return new VarianceCollector(true);
	}
}
