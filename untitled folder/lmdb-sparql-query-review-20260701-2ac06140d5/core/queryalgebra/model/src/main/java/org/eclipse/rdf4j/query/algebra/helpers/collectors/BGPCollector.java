/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers.collectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Basic graph pattern collector.
 */
public class BGPCollector<X extends Exception> extends AbstractQueryModelVisitor<X> {

	private final QueryModelVisitor<X> visitor;

	private List<StatementPattern> statementPatterns;

	public BGPCollector(QueryModelVisitor<X> visitor) {
		this.visitor = visitor;
	}

	public List<StatementPattern> getStatementPatterns() {
		return (statementPatterns != null) ? statementPatterns : Collections.emptyList();
	}

	@Override
	public void meet(Join node) throws X {
		// by-pass meetNode()
		node.visitChildren(this);
	}

	@Override
	public void meet(StatementPattern sp) throws X {
		if (statementPatterns == null) {
			statementPatterns = new ArrayList<>();
		}
		statementPatterns.add(sp);
	}

	@Override
	protected void meetNode(QueryModelNode node) throws X {
		// resume previous visitor
		node.visit(visitor);
	}
}
