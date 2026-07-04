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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An extension operator that can be used to add bindings to solutions whose values are defined by {@link ValueExpr
 * value expressions}.
 */
public class Extension extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<ExtensionElem> elements = new ArrayList<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Extension() {
	}

	public Extension(TupleExpr arg) {
		super(arg);
	}

	public Extension(TupleExpr arg, ExtensionElem... elements) {
		this(arg);
		addElements(elements);
	}

	public Extension(TupleExpr arg, Iterable<ExtensionElem> elements) {
		this(arg);
		addElements(elements);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public List<ExtensionElem> getElements() {
		return elements;
	}

	public void setElements(Iterable<ExtensionElem> elements) {
		this.elements.clear();
		addElements(elements);
	}

	public void addElements(ExtensionElem... elements) {
		for (ExtensionElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElements(Iterable<ExtensionElem> elements) {
		for (ExtensionElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElement(ExtensionElem pe) {
		elements.add(pe);
		pe.setParentNode(this);
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(arg.getBindingNames());

		for (ExtensionElem pe : elements) {
			bindingNames.add(pe.getName());
		}

		return bindingNames;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		super.visitChildren(visitor);
		for (ExtensionElem elem : elements) {
			elem.visit(visitor);
		}
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
		if (other instanceof Extension && super.equals(other)) {
			Extension o = (Extension) other;
			return elements.equals(o.getElements());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ elements.hashCode();
	}

	@Override
	public Extension clone() {
		Extension clone = (Extension) super.clone();

		clone.elements = new ArrayList<>(getElements().size());
		for (ExtensionElem elem : getElements()) {
			clone.addElement(elem.clone());
		}

		return clone;
	}

}
