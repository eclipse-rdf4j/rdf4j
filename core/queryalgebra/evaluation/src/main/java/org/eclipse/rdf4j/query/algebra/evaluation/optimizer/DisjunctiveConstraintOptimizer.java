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

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * A query optimizer that optimize disjunctive constraints on tuple expressions. Currently, this optimizer {@link Union
 * unions} a clone of the underlying tuple expression with the original expression for each {@link SameTerm} operator,
 * moving the SameTerm to the cloned tuple expression.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class DisjunctiveConstraintOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new OrSameTermOptimizer());
	}

	private static class OrSameTermOptimizer extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		protected OrSameTermOptimizer() {
			super(false);
		}

		@Override
		public void meet(Filter filter) {
			if (filter.getCondition() instanceof Or && containsSameTerm(filter.getCondition())) {
				Or orNode = (Or) filter.getCondition();
				TupleExpr filterArg = filter.getArg();

				ValueExpr leftConstraint = orNode.getLeftArg();
				ValueExpr rightConstraint = orNode.getRightArg();

				// remove filter
				filter.replaceWith(filterArg);

				// Push UNION down below other filters to avoid cloning them
				TupleExpr node = findNotFilter(filterArg);

				Filter leftFilter = new Filter(node.clone(), leftConstraint);
				Filter rightFilter = new Filter(node.clone(), rightConstraint);
				Union union = new Union(leftFilter, rightFilter);
				node.replaceWith(union);

				filter.getParentNode().visit(this);
			} else {
				super.meet(filter);
			}
		}

		private TupleExpr findNotFilter(TupleExpr node) {
			if (node instanceof Filter) {
				return findNotFilter(((Filter) node).getArg());
			}
			return node;
		}

		private boolean containsSameTerm(ValueExpr node) {
			if (node instanceof SameTerm) {
				return true;
			}
			if (node instanceof Or) {
				Or or = (Or) node;
				boolean left = containsSameTerm(or.getLeftArg());
				return left || containsSameTerm(or.getRightArg());
			}
			if (node instanceof And) {
				And and = (And) node;
				boolean left = containsSameTerm(and.getLeftArg());
				return left || containsSameTerm(and.getRightArg());
			}
			return false;
		}
	}
}
