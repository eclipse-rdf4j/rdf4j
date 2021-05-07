/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;

/**
 * Organizes a query algebra model in a semi-canonical form to allow comparison.
 * 
 * @implNote this class relies on the (now deprecated) {@link NaryJoin} as a means of organizing the join tree in a
 *           canonical form.
 * 
 * @author Andriy Nikolov
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 *
 */
class TestTupleExprCanonicalizer implements QueryOptimizer {

	protected class JoinVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meetOther(QueryModelNode node) throws RuntimeException {
			if (node instanceof NaryJoin) {
				meetJoin((NaryJoin) node);
			} else {
				super.meetOther(node);
			}
		}

		@Override
		public void meet(Join node) throws RuntimeException {
			meetJoin(node);
		}

		public void meetJoin(TupleExpr node) {
			List<TupleExpr> joinArgs = getJoinArgs(node, new ArrayList<TupleExpr>());

			if (joinArgs.size() > 2) {
				TupleExpr replacement = new NaryJoin(joinArgs);
				if (node != TestTupleExprCanonicalizer.this.getOptimized()) {
					node.replaceWith(replacement);
				} else {
					TestTupleExprCanonicalizer.this.setOptimized(replacement);
				}

			}
		}

		protected <L extends List<TupleExpr>> L getJoinArgs(TupleExpr tupleExpr, L joinArgs) {
			if (tupleExpr instanceof NaryJoin) {
				NaryJoin join = (NaryJoin) tupleExpr;
				for (TupleExpr arg : join.getArgs()) {
					getJoinArgs(arg, joinArgs);
				}
			} else if (tupleExpr instanceof Join) {
				Join join = (Join) tupleExpr;
				getJoinArgs(join.getLeftArg(), joinArgs);
				getJoinArgs(join.getRightArg(), joinArgs);
			} else {
				joinArgs.add(tupleExpr);
			}

			return joinArgs;
		}

	}

	protected TupleExpr optimized;

	public TestTupleExprCanonicalizer() {

	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		optimized = tupleExpr;
		tupleExpr.visit(new JoinVisitor());
	}

	public TupleExpr getOptimized() {
		return optimized;
	}

	protected void setOptimized(TupleExpr optimized) {
		this.optimized = optimized;
	}

}
