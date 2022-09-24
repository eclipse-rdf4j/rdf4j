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

import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Moves the Order node above the Projection when variables are projected.
 *
 * @author James Leigh
 * 
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.OrderLimitOptimizer}
 *             instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class OrderLimitOptimizer extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.OrderLimitOptimizer
		implements QueryOptimizer {

	@Deprecated(forRemoval = true, since = "4.1.0")
	protected static class OrderOptimizer extends AbstractQueryModelVisitor<RuntimeException> {

		private boolean variablesProjected = true;

		private Projection projection;

		@Override
		public void meet(Projection node) {
			projection = node;
			node.getArg().visit(this);
			projection = null;
		}

		@Override
		public void meet(Order node) {
			for (OrderElem e : node.getElements()) {
				e.visit(this);
			}
			if (variablesProjected) {
				QueryModelNode parent = node.getParentNode();
				if (projection == parent) {
					node.replaceWith(node.getArg().clone());
					node.setArg(projection.clone());
					Order replacement = node.clone();
					projection.replaceWith(replacement);
					QueryModelNode distinct = replacement.getParentNode();
					if (distinct instanceof Distinct) {
						distinct.replaceWith(new Reduced(replacement.clone()));
					}
				}
			}
		}

		@Override
		public void meet(Var node) {
			if (projection != null) {
				boolean projected = false;
				for (ProjectionElem e : projection.getProjectionElemList().getElements()) {
					if (node.getName().equals(e.getName())) {
						projected = true;
						break;
					}
				}
				if (!projected) {
					variablesProjected = false;
				}
			}
		}

	}
}
