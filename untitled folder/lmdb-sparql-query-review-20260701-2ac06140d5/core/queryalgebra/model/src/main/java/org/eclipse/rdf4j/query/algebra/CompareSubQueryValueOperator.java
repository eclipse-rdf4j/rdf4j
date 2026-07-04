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

public abstract class CompareSubQueryValueOperator extends SubQueryValueOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected ValueExpr arg;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected CompareSubQueryValueOperator() {
	}

	protected CompareSubQueryValueOperator(ValueExpr valueExpr, TupleExpr subQuery) {
		super(subQuery);
		setArg(valueExpr);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public ValueExpr getArg() {
		return arg;
	}

	public void setArg(ValueExpr arg) {
		assert arg != null : "arg must not be null";
		arg.setParentNode(this);
		this.arg = arg;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		arg.visit(visitor);
		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (arg == current) {
			setArg((ValueExpr) replacement);
		} else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CompareSubQueryValueOperator && super.equals(other)) {
			CompareSubQueryValueOperator o = (CompareSubQueryValueOperator) other;
			return arg.equals(o.getArg());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ arg.hashCode();
	}

	@Override
	public CompareSubQueryValueOperator clone() {
		CompareSubQueryValueOperator clone = (CompareSubQueryValueOperator) super.clone();
		clone.setArg(getArg().clone());
		return clone;
	}
}
