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

public abstract class SubQueryValueOperator extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected TupleExpr subQuery;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected SubQueryValueOperator() {
	}

	protected SubQueryValueOperator(TupleExpr subQuery) {
		setSubQuery(subQuery);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public TupleExpr getSubQuery() {
		return subQuery;
	}

	public void setSubQuery(TupleExpr subQuery) {
		assert subQuery != null : "subQuery must not be null";
		subQuery.setParentNode(this);
		this.subQuery = subQuery;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		subQuery.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (subQuery == current) {
			setSubQuery((TupleExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SubQueryValueOperator) {
			SubQueryValueOperator o = (SubQueryValueOperator) other;
			return subQuery.equals(o.getSubQuery());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return subQuery.hashCode();
	}

	@Override
	public SubQueryValueOperator clone() {
		SubQueryValueOperator clone = (SubQueryValueOperator) super.clone();
		clone.setSubQuery(getSubQuery().clone());
		return clone;
	}
}
