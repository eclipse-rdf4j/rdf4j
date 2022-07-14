/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * A QueryModelVisitor that collects StatementPattern's from a query model. StatementPatterns thet are part of
 * filters/constraints are not included in the result.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class StatementPatternCollector extends AbstractQueryModelVisitor<RuntimeException> {

	public static List<StatementPattern> process(QueryModelNode node) {
		StatementPatternCollector collector = new StatementPatternCollector();
		node.visit(collector);
		return collector.getStatementPatterns();
	}

	private final List<StatementPattern> stPatterns = new ArrayList<>();

	public List<StatementPattern> getStatementPatterns() {
		return stPatterns;
	}

	@Override
	public void meet(Filter node) {
		// Skip boolean constraints
		node.getArg().visit(this);
	}

	@Override
	public void meet(StatementPattern node) {
		stPatterns.add(node);
	}
}
