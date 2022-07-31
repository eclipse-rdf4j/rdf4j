/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;

/**
 * ConjunctiveFilterExpr maintains a list of conjunctive (i.e. AND connected) constraints.
 *
 * @author Andreas Schwarte
 *
 */
public class ConjunctiveFilterExpr extends AbstractQueryModelNode implements FilterValueExpr {
	private static final long serialVersionUID = -4016335014136286638L;

	protected List<FilterExpr> expressions;

	public ConjunctiveFilterExpr(FilterExpr expr1, FilterExpr expr2) {
		this.expressions = new ArrayList<>(3);
		addExpression(expr1);
		addExpression(expr2);
	}

	public ConjunctiveFilterExpr(Collection<FilterExpr> expressions) {
		if (expressions.size() < 2) {
			throw new IllegalArgumentException("Conjunctive Expression must have at least two arguments.");
		}
		this.expressions = new ArrayList<>(expressions.size());
		for (FilterExpr expr : expressions) {
			addExpression(expr);
		}
	}

	public void addExpression(FilterExpr expr) {
		// TODO use some priority ordering: selective filters should be evaluated first (shortcuts!)
		expressions.add(expr);
	}

	public List<FilterExpr> getExpressions() {
		return expressions;
	}

	@Override
	public ConjunctiveFilterExpr clone() {
		return (ConjunctiveFilterExpr) super.clone();
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		for (FilterExpr expr : expressions) {
			expr.getExpression().visit(visitor);
		}
	}

}
