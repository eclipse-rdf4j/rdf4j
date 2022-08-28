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
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Assigns values to variables based on a supplied set of bindings.
 *
 * @author Arjohn Kampman
 * 
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.BindingAssignerOptimizer}
 *             instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class BindingAssigner extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.BindingAssignerOptimizer
		implements QueryOptimizer {

	@Deprecated(forRemoval = true, since = "4.1.0")
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
