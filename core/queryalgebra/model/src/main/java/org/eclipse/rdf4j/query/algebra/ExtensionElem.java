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
package org.eclipse.rdf4j.query.algebra;

public class ExtensionElem extends AbstractQueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private ValueExpr expr;

	private String name;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ExtensionElem() {
	}

	public ExtensionElem(ValueExpr expr, String name) {
		setExpr(expr);
		setName(name);
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		return super.getSignature() + " (" + name + ")";
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ExtensionElem) {
			ExtensionElem o = (ExtensionElem) other;
			return name.equals(o.getName()) && expr.equals(o.getExpr());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ expr.hashCode();
	}

	@Override
	public ExtensionElem clone() {
		ExtensionElem clone = (ExtensionElem) super.clone();
		clone.setExpr(getExpr().clone());
		return clone;
	}
}
