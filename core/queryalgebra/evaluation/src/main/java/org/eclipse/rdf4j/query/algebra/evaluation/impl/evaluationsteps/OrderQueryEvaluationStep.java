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
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Comparator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.OrderIterator;

public class OrderQueryEvaluationStep implements QueryEvaluationStep {

	private final long iterationCacheSyncThreshold;
	private final Comparator<BindingSet> cmp;
	private final long limit;
	private final boolean reduced;
	private final QueryEvaluationStep preparedArg;

	public OrderQueryEvaluationStep(Comparator<BindingSet> cmp, long limit, boolean reduced,
			QueryEvaluationStep preparedArg, long iterationCacheSyncThreshold) {
		super();
		this.cmp = cmp;
		this.limit = limit;
		this.reduced = reduced;
		this.preparedArg = preparedArg;
		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
		return new OrderIterator(preparedArg.evaluate(bs), cmp, limit, reduced, iterationCacheSyncThreshold);
	}
}
