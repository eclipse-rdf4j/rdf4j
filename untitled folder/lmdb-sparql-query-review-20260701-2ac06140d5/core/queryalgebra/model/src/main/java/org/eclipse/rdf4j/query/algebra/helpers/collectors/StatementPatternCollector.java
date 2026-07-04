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

package org.eclipse.rdf4j.query.algebra.helpers.collectors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * An efficient QueryModelVisitor that collects StatementPattern's from a query model.
 */
public class StatementPatternCollector extends AbstractSimpleQueryModelVisitor<RuntimeException> {

	public StatementPatternCollector() {
		super(true);
	}

	public static List<StatementPattern> process(QueryModelNode node) {
		StatementPatternCollector collector = new StatementPatternCollector();
		node.visit(collector);
		return collector.getStatementPatterns();
	}

	private final List<StatementPattern> statementPatterns = new ArrayList<>();

	public List<StatementPattern> getStatementPatterns() {
		return statementPatterns;
	}

	@Override
	public void meet(Filter node) {
		// Skip boolean constraints
		node.getArg().visit(this);
	}

	@Override
	public void meet(Join node) throws RuntimeException {
		if (!(node.getLeftArg() instanceof Join || node.getRightArg() instanceof Join)) {
			super.meet(node);
			return;
		}

		Deque<TupleExpr> stack = new ArrayDeque<>();
		TupleExpr current = node;

		while (true) {
			// Drill down the leftmost spine, pushing right branches onto the stack
			while (current instanceof Join) {
				Join join = (Join) current;
				stack.push(join.getRightArg()); // defer right side
				current = join.getLeftArg(); // continue with left side
			}

			// current is a leaf (not a Join)
			current.visit(this);

			// When the stack is empty, we have visited every deferred right branch
			if (stack.isEmpty()) {
				return;
			}
			// Pop the next right branch to process
			current = stack.pop();
		}
	}

	@Override
	public void meet(StatementPattern node) {
		statementPatterns.add(node);
	}
}
