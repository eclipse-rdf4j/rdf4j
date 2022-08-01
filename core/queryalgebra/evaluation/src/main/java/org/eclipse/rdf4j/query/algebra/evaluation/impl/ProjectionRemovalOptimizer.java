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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

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
@Deprecated(forRemoval = true, since = "4.1.0")
public class ProjectionRemovalOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new ProjectionFinder());
	}

	private class VariableFinder extends AbstractQueryModelVisitor<RuntimeException> {
		private final Set<Var> vars = new HashSet<>();

		@Override
		public void meet(Var node) throws RuntimeException {
			vars.add(node);
			super.meet(node);
		}

		public Set<Var> getVars() {
			return vars;
		}

	}

	private class ProjectionFinder extends AbstractQueryModelVisitor<RuntimeException> {
		@Override
		public void meet(Projection node) throws RuntimeException {
			super.meet(node);
			VariableFinder findVariables = new VariableFinder();
			node.visit(findVariables);
			Set<String> foundChildVariableNames = findVariables.getVars()
					.stream()
					.map(Var::getName)
					.collect(Collectors.toSet());
			if (foundChildVariableNames.equals(node.getBindingNames())) {
				TupleExpr child = node.getArg();
				node.getParentNode().replaceChildNode(node, child);
			}
		}
	}
}
