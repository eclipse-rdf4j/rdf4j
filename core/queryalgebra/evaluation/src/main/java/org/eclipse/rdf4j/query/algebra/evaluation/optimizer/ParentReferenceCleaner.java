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

import java.util.ArrayDeque;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleans up {@link QueryModelNode#getParentNode()} references that have become inconsistent with the actual algebra
 * tree structure due to optimization operations. Typically used at the very end of the optimization pipeline.
 *
 * @author Jeen Broekstra
 */
public class ParentReferenceCleaner implements QueryOptimizer {

	private static final Logger logger = LoggerFactory.getLogger(ParentReferenceCleaner.class);

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new ParentFixingVisitor());
	}

	private static class ParentFixingVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final ArrayDeque<QueryModelNode> ancestors = new ArrayDeque<>();

		@Override
		protected void meetNode(QueryModelNode node) throws RuntimeException {
			QueryModelNode expectedParent = ancestors.peekLast();
			if (node.getParentNode() != expectedParent) {
				String message = "unexpected parent for node " + node + ": " + node.getParentNode() + " (expected "
						+ expectedParent + ")";
				assert node.getParentNode() == expectedParent : message;
				logger.debug(message);

				node.setParentNode(expectedParent);
			}

			ancestors.addLast(node);
			super.meetNode(node);
			ancestors.pollLast();
		}
	}
}
