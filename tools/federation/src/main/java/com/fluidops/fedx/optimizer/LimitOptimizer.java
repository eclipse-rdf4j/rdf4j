/*
 * Copyright (C) 2019 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.optimizer;

import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

import com.fluidops.fedx.algebra.FedXStatementPattern;
import com.fluidops.fedx.exception.OptimizationException;

/**
 * An optimizer that attempts to push upper limits into BGPs of the query.
 * 
 * Currently upper limits are only pushed for simple queries consisting of a
 * single BGP.
 * 
 * 
 * @author Andreas Schwarte
 *
 */
public class LimitOptimizer extends AbstractQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	/**
	 * Helper variable that contains an applicable limit for the current scope. Set
	 * to -1 if no limit is applicable.
	 */
	private long applicableLimitInScope = -1;

	@Override
	public void optimize(TupleExpr tupleExpr) {

		try {
			tupleExpr.visit(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
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
