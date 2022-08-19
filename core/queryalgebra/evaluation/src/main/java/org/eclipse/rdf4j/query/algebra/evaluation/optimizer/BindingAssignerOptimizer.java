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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * Assigns values to variables based on a supplied set of bindings.
 *
 * @author Arjohn Kampman
 */
public class BindingAssignerOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		if (bindings.size() > 0) {
			tupleExpr.visit(new VarVisitor(bindings));
		}
	}

	private static class VarVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		protected BindingSet bindings;

		public VarVisitor(BindingSet bindings) {
			super(true);
			this.bindings = bindings;
		}

		@Override
		public void meet(Var var) {
			if (!var.hasValue() && bindings.hasBinding(var.getName())) {
				Value value = bindings.getValue(var.getName());
				Var replacement = new Var(var.getName(), value, var.isAnonymous(), var.isConstant());
				var.replaceWith(replacement);
			}
		}
	}
}
