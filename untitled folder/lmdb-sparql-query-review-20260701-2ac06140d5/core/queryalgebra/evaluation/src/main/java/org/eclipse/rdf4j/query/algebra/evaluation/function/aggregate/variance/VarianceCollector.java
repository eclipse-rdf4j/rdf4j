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

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.StatisticCollector;

/**
 * {@link org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateCollector} that can compute both sample and
 * population variance based on input of numeric {@link Literal}s.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@Experimental
public class VarianceCollector extends StatisticCollector {

	public VarianceCollector(boolean population) {
		super(population);
	}

	@Override
	protected Literal computeValue() {
		double variance;
		if (population) {
			variance = statistics.getPopulationVariance();
		} else {
			variance = statistics.getVariance();
		}
		if (Double.isNaN(variance)) {
			// no value has been added
			return ZERO;
		}
		return SimpleValueFactory.getInstance().createLiteral(variance);
	}
}
