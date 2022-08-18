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

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks {@link QueryModelNode#getParentNode()} references that have become inconsistent with the actual algebra tree
 * structure due to optimization operations. Used during testing.
 *
 * @author Jeen Broekstra
 * @author Håvard Ottestad
 */
@InternalUseOnly
public class ParentReferenceChecker implements QueryOptimizer {

	private static final Logger logger = LoggerFactory.getLogger(ParentReferenceChecker.class);

	private final QueryOptimizer previousOptimizerInPipeline;

	public ParentReferenceChecker(QueryOptimizer previousOptimizerInPipeline) {
		this.previousOptimizerInPipeline = previousOptimizerInPipeline;
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new ParentCheckingVisitor());
	}

	private class ParentCheckingVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final ArrayDeque<QueryModelNode> ancestors = new ArrayDeque<>();

		@Override
		protected void meetNode(QueryModelNode node) throws RuntimeException {
			QueryModelNode expectedParent = ancestors.peekLast();
			if (node.getParentNode() != expectedParent) {
				String previousOptimizer;
				if (ParentReferenceChecker.this.previousOptimizerInPipeline != null) {
					previousOptimizer = ParentReferenceChecker.this.previousOptimizerInPipeline.getClass()
							.getSimpleName();
				} else {
					previousOptimizer = "query parsing";
				}
				String message = "After " + previousOptimizer + " there was an unexpected parent for node " + node
						+ ": " + node.getParentNode() + " (expected " + expectedParent + ")";

				assert node.getParentNode() == expectedParent : message;
			}

			ancestors.addLast(node);
			super.meetNode(node);
			ancestors.pollLast();
		}
	}
}
