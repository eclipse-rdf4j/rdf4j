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

/**
 * A tuple operator that groups tuples that have a specific set of equivalent variable bindings, and that can apply
 * aggregate functions on the grouped results.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class GroupElem extends AbstractQueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String name;

	private AggregateOperator operator;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public GroupElem(String name, AggregateOperator operator) {
		setName(name);
		setOperator(operator);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getName() {
		return name;
	}

	public void setName(String name) {
		assert name != null : "name must not be null";
		this.name = name;
	}

	public AggregateOperator getOperator() {
		return operator;
	}

	public void setOperator(AggregateOperator operator) {
		assert operator != null : "operator must not be null";
		this.operator = operator;
		operator.setParentNode(this);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		operator.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (operator == current) {
			setOperator((AggregateOperator) replacement);
		}
	}

	@Override
	public String getSignature() {
		return super.getSignature() + " (" + name + ")";
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GroupElem) {
			GroupElem o = (GroupElem) other;
			return name.equals(o.getName()) && operator.equals(o.getOperator());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ operator.hashCode();
	}

	@Override
	public GroupElem clone() {
		GroupElem clone = (GroupElem) super.clone();
		clone.setOperator(getOperator().clone());
		return clone;
	}
}
