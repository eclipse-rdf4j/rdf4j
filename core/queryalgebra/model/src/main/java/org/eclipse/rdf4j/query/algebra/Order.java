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

import java.util.ArrayList;
import java.util.List;

/**
 * An order operator that can be used to order bindings as specified by a set of value expressions.
 *
 * @author Arjohn Kampman
 */
public class Order extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<OrderElem> elements = new ArrayList<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Order() {
	}

	public Order(TupleExpr arg) {
		super(arg);
	}

	public Order(TupleExpr arg, OrderElem... elements) {
		this(arg);
		addElements(elements);
	}

	public Order(TupleExpr arg, Iterable<OrderElem> elements) {
		this(arg);
		addElements(elements);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public List<OrderElem> getElements() {
		return elements;
	}

	public void setElements(List<OrderElem> elements) {
		this.elements = elements;
	}

	public void addElements(OrderElem... elements) {
		for (OrderElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElements(Iterable<OrderElem> elements) {
		for (OrderElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElement(OrderElem pe) {
		elements.add(pe);
		pe.setParentNode(this);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		for (OrderElem elem : elements) {
			elem.visit(visitor);
		}

		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (replaceNodeInList(elements, current, replacement)) {
			return;
		}
		super.replaceChildNode(current, replacement);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Order && super.equals(other)) {
			Order o = (Order) other;
			return elements.equals(o.getElements());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ elements.hashCode();
	}

	@Override
	public Order clone() {
		Order clone = (Order) super.clone();

		clone.elements = new ArrayList<>(getElements().size());
		for (OrderElem elem : getElements()) {
			clone.addElement(elem.clone());
		}

		return clone;
	}

}
