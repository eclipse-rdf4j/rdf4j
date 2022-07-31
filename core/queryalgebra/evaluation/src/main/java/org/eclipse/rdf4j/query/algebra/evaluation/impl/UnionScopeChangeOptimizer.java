/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Inspect Union clauses to check if scope change can be avoided (allowing injection of pre-bound vars into union
 * arguments).
 *
 * @author Jeen Broekstra
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class UnionScopeChangeOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new UnionScopeChangeFixer());
	}

	private static class UnionScopeChangeFixer extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meet(Union union) {
			super.meet(union);
			if (union.isVariableScopeChange()) {
				UnionArgChecker checker = new UnionArgChecker();
				union.getLeftArg().visit(checker);
				if (checker.containsBindOrValues) {
					return;
				}

				checker.containsBindOrValues = false;
				union.getRightArg().visit(checker);

				if (checker.containsBindOrValues) {
					return;
				}

				// Neither argument of the union contains a BIND or VALUES clause, we can safely ignore scope change
				// for binding injection
				union.setVariableScopeChange(false);
			}
		}
	}

	private static class UnionArgChecker extends AbstractQueryModelVisitor<RuntimeException> {

		boolean containsBindOrValues = false;

		@Override
		public void meet(Union union) {
			if (!union.isVariableScopeChange()) {
				super.meet(union);
			}
		}

		@Override
		public void meet(Projection subselect) {
			// do not check deeper in the tree
			return;
		}

		@Override
		public void meet(Extension node) throws RuntimeException {
			containsBindOrValues = true;
		}

		@Override
		public void meet(BindingSetAssignment bsa) {
			containsBindOrValues = true;
		}
	}
}
