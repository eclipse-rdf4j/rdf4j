/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * @author Arjohn Kampman
 */
public class OrderElem extends AbstractQueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 *
	 */
	private static final long serialVersionUID = -6573481604435459287L;

	private ValueExpr expr;

	private boolean ascending = true;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public OrderElem() {
	}

	public OrderElem(ValueExpr expr) {
		this(expr, true);
	}

	public OrderElem(ValueExpr expr, boolean ascending) {
		setExpr(expr);
		setAscending(ascending);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public ValueExpr getExpr() {
		return expr;
	}

	public void setExpr(ValueExpr expr) {
		assert expr != null : "expr must not be null";
		expr.setParentNode(this);
		this.expr = expr;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		expr.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (expr == current) {
			setExpr((ValueExpr) replacement);
		}
	}

	@Override
	public String getSignature() {
		return super.getSignature() + " (" + (ascending ? "ASC" : "DESC") + ")";
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof OrderElem) {
			OrderElem o = (OrderElem) other;
			return ascending == o.isAscending() && expr.equals(o.getExpr());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = expr.hashCode();
		if (ascending) {
			result = ~result;
		}
		return result;
	}

	@Override
	public OrderElem clone() {
		OrderElem clone = (OrderElem) super.clone();
		clone.setExpr(getExpr().clone());
		return clone;
	}
}
