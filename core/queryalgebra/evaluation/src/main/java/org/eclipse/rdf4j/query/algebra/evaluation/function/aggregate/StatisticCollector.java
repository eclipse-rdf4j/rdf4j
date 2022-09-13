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

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateCollector;

/**
 * {@link AggregateCollector} implementation that processes SPARQL statistical functions based on input {@link Literal}
 * values.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@Experimental
public abstract class StatisticCollector implements AggregateCollector {
	protected static final Literal ZERO = SimpleValueFactory.getInstance().createLiteral("0", CoreDatatype.XSD.INTEGER);

	protected final SummaryStatistics statistics;
	protected final boolean population;
	protected ValueExprEvaluationException typeError;

	public StatisticCollector(boolean population) {
		this.population = population;
		this.statistics = new SummaryStatistics();
	}

	@Override
	public Value getFinalValue() throws ValueExprEvaluationException {
		if (typeError != null) {
			// a type error occurred while processing the aggregate, throw it now.
			throw typeError;
		}
		// for statistical functions at least two literal values are needed
		return computeValue();
	}

	public void setTypeError(ValueExprEvaluationException typeError) {
		this.typeError = typeError;
	}

	public boolean hasError() {
		return typeError != null;
	}

	public void addValue(Literal val) {
		var type = val.getCoreDatatype()
				.asXSDDatatype()
				.orElseThrow(() -> new IllegalArgumentException(val + " is not an XSD type literal"));
		if (type == CoreDatatype.XSD.DOUBLE) {
			statistics.addValue(val.doubleValue());
		} else if (type == CoreDatatype.XSD.FLOAT) {
			statistics.addValue(val.floatValue());
		} else {
			statistics.addValue(Double.parseDouble(val.getLabel()));
		}
	}

	protected abstract Literal computeValue();
}
