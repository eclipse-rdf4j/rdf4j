/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Filters iteration according to specified filterExpr and inserts original bindings into filtered results.
 *
 * @author Andreas Schwarte
 */
@Deprecated(since = "4.1.0")
public class FilteringInsertBindingsIteration extends FilteringIteration {

	protected final BindingSet bindings;

	public FilteringInsertBindingsIteration(FilterValueExpr filterExpr, BindingSet bindings,
			CloseableIteration<BindingSet, QueryEvaluationException> iter, FederationEvalStrategy strategy)
			throws QueryEvaluationException {
		super(filterExpr, iter, strategy);
		this.bindings = bindings;
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		BindingSet next = super.next();
		if (next == null) {
			return null;
		}
		QueryBindingSet res = new QueryBindingSet(bindings.size() + next.size());
		res.addAll(bindings);
		res.addAll(next);
		return res;
	}
}
