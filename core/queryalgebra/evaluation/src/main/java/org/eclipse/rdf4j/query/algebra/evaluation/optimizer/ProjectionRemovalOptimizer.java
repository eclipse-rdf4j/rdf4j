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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * If a projection node in the algebra does not contribute or change the results it can be removed from the tree.
 *
 * For example <code>
 * SELECT ?s ?p ?o
 * WHERE {?s ?p ?o }
 * </code> Does not need a projection as the inner statement pattern returns the same result.
 *
 * While <code>
 *  * SELECT ?s ?p
 * WHERE {?s ?p ?o }
 * </code> Does as the statement pattern has one more variable in use than the projection.
 *
 * Note: this optimiser should run after optimisations ran that depend on Projections. e.g.
 *
 * @see UnionScopeChangeOptimizer
 *
 * @author Jerven Bolleman
 */
public class ProjectionRemovalOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new ProjectionFinder());
	}

	private static class VariableFinder extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		private Set<String> vars;

		private VariableFinder() {
			super(true);
		}

		@Override
		public void meet(Var node) throws RuntimeException {
			if (node.getName() != null) {
				if (vars == null) {
					vars = new HashSet<>();
				}
				vars.add(node.getName());
			}
		}

		public Set<String> getVars() {
			return vars != null ? vars : Collections.emptySet();
		}

	}

	private static class ProjectionFinder extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		private ProjectionFinder() {
			super(true);
		}

		@Override
		public void meet(Projection node) throws RuntimeException {
			super.meet(node);
			VariableFinder findVariables = new VariableFinder();
			node.visit(findVariables);

			Set<String> foundChildVariableNames = findVariables.getVars();

			if (!foundChildVariableNames.isEmpty() && foundChildVariableNames.equals(node.getBindingNames())) {
				TupleExpr child = node.getArg();
				node.getParentNode().replaceChildNode(node, child);
			}
		}
	}
}
