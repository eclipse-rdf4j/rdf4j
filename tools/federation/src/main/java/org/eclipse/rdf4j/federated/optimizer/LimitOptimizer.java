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
package org.eclipse.rdf4j.federated.optimizer;

import org.eclipse.rdf4j.federated.algebra.FedXStatementPattern;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * An optimizer that attempts to push upper limits into BGPs of the query.
 *
 * Currently upper limits are only pushed for simple queries consisting of a single BGP.
 *
 *
 * @author Andreas Schwarte
 *
 */
public class LimitOptimizer extends AbstractSimpleQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	/**
	 * Helper variable that contains an applicable limit for the current scope. Set to -1 if no limit is applicable.
	 */
	private long applicableLimitInScope = -1;

	public LimitOptimizer() {
		super(true);
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {

		try {
			tupleExpr.visit(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public void meetOther(QueryModelNode node) throws OptimizationException {

		super.meetOther(node);
	}

	@Override
	public void meet(Slice node) throws OptimizationException {
		if (!node.hasOffset()) {
			applicableLimitInScope = node.getLimit();
		}
		super.meet(node);
		applicableLimitInScope = -1;

	}

	@Override
	public void meet(Projection proj) throws OptimizationException {

		TupleExpr expr = proj.getArg();
		// if the top most element is a statement, i.e. no join, union or
		// any other complex pattern, we can push the limit
		// => this case typically represents a query with a single BGP
		if (expr instanceof FedXStatementPattern) {
			if (applicableLimitInScope > 0) {
				pushLimit((FedXStatementPattern) expr, applicableLimitInScope);
			}
		}

		// currently no need to traverse further. Might be added if we do further
		// optimizations
	}

	protected void pushLimit(FedXStatementPattern stmt, long upperLimit) {
		stmt.setUpperLimit(upperLimit);
	}
}
