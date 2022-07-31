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

import java.util.HashSet;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.ValueExpr;

/**
 * FilterExpr maintains information for a particular FILTER expression.
 *
 * @author Andreas Schwarte
 *
 */
public class FilterExpr extends AbstractQueryModelNode implements FilterValueExpr {

	private static final long serialVersionUID = -6594037345260846807L;

	protected ValueExpr expr;
	protected HashSet<String> vars;

	public FilterExpr(ValueExpr expr, HashSet<String> vars) {
		super();
		this.expr = expr;
		this.vars = vars;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		expr.visit(visitor);
	}

	public ValueExpr getExpression() {
		return expr;
	}

	public HashSet<String> getVars() {
		return vars;
	}

	@Override
	public FilterExpr clone() {
		return (FilterExpr) super.clone();
	}

	public boolean isCompareEq() {
		return expr instanceof Compare && ((Compare) expr).getOperator() == CompareOp.EQ;
	}
}
