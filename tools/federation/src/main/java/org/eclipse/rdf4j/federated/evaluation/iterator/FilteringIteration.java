/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters iteration according to specified filterExpr.
 *
 * @author Andreas Schwarte
 */
@Deprecated(since = "4.1.0")
public class FilteringIteration extends FilterIteration<BindingSet, QueryEvaluationException> {

	private static final Logger log = LoggerFactory.getLogger(FilteringIteration.class);

	protected final FilterValueExpr filterExpr;
	protected final FederationEvalStrategy strategy;

	public FilteringIteration(FilterValueExpr filterExpr, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			FederationEvalStrategy strategy)
			throws QueryEvaluationException {
		super(iter);
		this.filterExpr = filterExpr;
		this.strategy = strategy;
	}

	@Override
	protected boolean accept(BindingSet bindings) throws QueryEvaluationException {
		try {
			return strategy.isTrue(filterExpr, bindings);
		} catch (ValueExprEvaluationException e) {
			log.warn("Failed to evaluate filter expr: " + e.getMessage());
			// failed to evaluate condition
			return false;
		}
	}
}
