/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;

/**
 * A query optimizer that contains a list of other query optimizers, which are called consecutively when the list's
 * {@link #optimize(TupleExpr, Dataset, BindingSet)} method is called.
 *
 * @author Arjohn Kampman
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class QueryOptimizerList implements QueryOptimizer {

	protected List<QueryOptimizer> optimizers;

	public QueryOptimizerList() {
		this.optimizers = new ArrayList<>(8);
	}

	public QueryOptimizerList(List<QueryOptimizer> optimizers) {
		this.optimizers = new ArrayList<>(optimizers);
	}

	public QueryOptimizerList(QueryOptimizer... optimizers) {
		this.optimizers = new ArrayList<>(optimizers.length);
		for (QueryOptimizer optimizer : optimizers) {
			this.optimizers.add(optimizer);
		}
	}

	public void add(QueryOptimizer optimizer) {
		optimizers.add(optimizer);
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		for (QueryOptimizer optimizer : optimizers) {
			optimizer.optimize(tupleExpr, dataset, bindings);
		}
	}
}
