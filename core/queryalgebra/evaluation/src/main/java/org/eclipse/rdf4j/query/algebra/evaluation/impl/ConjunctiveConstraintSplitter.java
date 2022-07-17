/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.VarNameCollector;

/**
 * Splits conjunctive constraints into seperate constraints.
 *
 * @author Arjohn Kampman
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class ConjunctiveConstraintSplitter implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new ConstraintVisitor(tupleExpr));
	}

	protected static class ConstraintVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		protected final TupleExpr tupleExpr;

		public ConstraintVisitor(TupleExpr tupleExpr) {
			this.tupleExpr = tupleExpr;
		}

		@Override
		public void meet(Filter filter) {
			super.meet(filter);

			List<ValueExpr> conjunctiveConstraints = new ArrayList<>(16);
			getConjunctiveConstraints(filter.getCondition(), conjunctiveConstraints);

			TupleExpr filterArg = filter.getArg();

			for (int i = conjunctiveConstraints.size() - 1; i >= 1; i--) {
				Filter newFilter = new Filter(filterArg, conjunctiveConstraints.get(i));
				filterArg = newFilter;
			}

			filter.setCondition(conjunctiveConstraints.get(0));
			filter.setArg(filterArg);
		}

		@Override
		public void meet(LeftJoin node) {
			super.meet(node);

			if (node.getCondition() != null) {
				List<ValueExpr> conjunctiveConstraints = new ArrayList<>(16);
				getConjunctiveConstraints(node.getCondition(), conjunctiveConstraints);

				TupleExpr arg = node.getRightArg();
				ValueExpr condition = null;

				for (ValueExpr constraint : conjunctiveConstraints) {
					if (isWithinBindingScope(constraint, arg)) {
						arg = new Filter(arg, constraint);
					} else if (condition == null) {
						condition = constraint;
					} else {
						condition = new And(condition, constraint);
					}
				}

				node.setCondition(condition);
				node.setRightArg(arg);
			}
		}

		protected void getConjunctiveConstraints(ValueExpr valueExpr, List<ValueExpr> conjunctiveConstraints) {
			if (valueExpr instanceof And) {
				And and = (And) valueExpr;
				getConjunctiveConstraints(and.getLeftArg(), conjunctiveConstraints);
				getConjunctiveConstraints(and.getRightArg(), conjunctiveConstraints);
			} else {
				conjunctiveConstraints.add(valueExpr);
			}
		}

		private boolean isWithinBindingScope(ValueExpr condition, TupleExpr node) {
			return node.getBindingNames().containsAll(VarNameCollector.process(condition));
		}
	}
}
