/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Iterator;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Optimizes a query model by inlining {@link BindingSetAssignment} values where possible.
 *
 * @author Jeen Broekstra
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class BindingSetAssignmentInliner implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		BindingSetAssignmentVisitor visitor = new BindingSetAssignmentVisitor();
		tupleExpr.visit(visitor);
	}

	private static class BindingSetAssignmentVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private BindingSet bindingSet;

		@Override
		public void meet(BindingSetAssignment bsa) {
			Iterator<BindingSet> iter = bsa.getBindingSets().iterator();
			if (iter.hasNext()) {
				BindingSet firstBindingSet = iter.next();
				if (!iter.hasNext()) {
					bindingSet = firstBindingSet;
				}
			}
			super.meet(bsa);
		}

		@Override
		public void meet(Var var) {
			if (bindingSet != null && bindingSet.hasBinding(var.getName())) {
				var.setValue(bindingSet.getValue(var.getName()));
			}
		}

		@Override
		public void meet(Filter node) throws RuntimeException {
			// TODO Auto-generated method stub
			super.meet(node);
		}

		@Override
		public void meet(LeftJoin leftJoin) {
			leftJoin.getLeftArg().visit(this);
			// we can not pre-bind values for the optional part of the left-join
		}

		@Override
		protected void meetNode(QueryModelNode node) throws RuntimeException {
			// reset if we encounter a scope change (e.g. a subquery or a union)
			if (node instanceof AbstractQueryModelNode) {
				if (((AbstractQueryModelNode) node).isVariableScopeChange()) {
					bindingSet = null;
				}
			}
			super.meetNode(node);
		}
	}
}
