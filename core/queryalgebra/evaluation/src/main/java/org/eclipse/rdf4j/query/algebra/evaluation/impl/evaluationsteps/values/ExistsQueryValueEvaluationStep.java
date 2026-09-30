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
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public final class ExistsQueryValueEvaluationStep implements QueryValueEvaluationStep {
	private final Function<BindingSet, CloseableIteration<BindingSet>> probe;

	public ExistsQueryValueEvaluationStep(QueryEvaluationStep subquery) {
		this((Function<BindingSet, CloseableIteration<BindingSet>>) subquery::evaluate);
	}

	/**
	 * @param probe evaluates the EXISTS body for one outer row; see
	 *              {@link org.eclipse.rdf4j.query.algebra.evaluation.iterator.FilterIterator#existsProbeFunction} for
	 *              the probe that keeps every physical EXISTS path on the same semantics
	 */
	public ExistsQueryValueEvaluationStep(Function<BindingSet, CloseableIteration<BindingSet>> probe) {
		this.probe = Objects.requireNonNull(probe, "probe");
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		try (CloseableIteration<BindingSet> iter = probe.apply(bindings)) {
			return BooleanLiteral.valueOf(iter.hasNext());
		}
	}
}
