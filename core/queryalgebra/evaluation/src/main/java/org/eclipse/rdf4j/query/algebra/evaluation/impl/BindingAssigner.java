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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Assigns values to variables based on a supplied set of bindings.
 *
 * @author Arjohn Kampman
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class BindingAssigner implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		if (bindings.size() > 0) {
			tupleExpr.visit(new VarVisitor(bindings));
		}
	}

	protected static class VarVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		protected BindingSet bindings;

		public VarVisitor(BindingSet bindings) {
			this.bindings = bindings;
		}

		@Override
		public void meet(Var var) {
			if (!var.hasValue() && bindings.hasBinding(var.getName())) {
				Value value = bindings.getValue(var.getName());
				var.setValue(value);
			}
		}
	}
}
