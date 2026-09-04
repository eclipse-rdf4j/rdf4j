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

public class TripleComponent extends AbstractQueryModelNode implements ValueExpr {

	public enum Role {
		SUBJECT,
		PREDICATE,
		OBJECT
	}

	private Var tripleRefVar;
	private final Role role;

	public TripleComponent(Var tripleRefVar, Role role) {
		this.setTripleRefVar(tripleRefVar);
		this.role = role;
	}

	public Role getRole() {
		return role;
	}

	public Var getTripleRefVar() {
		return tripleRefVar;
	}

	private void setTripleRefVar(Var s) {
		tripleRefVar = s;
		tripleRefVar.setParentNode(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (tripleRefVar != null) {
			tripleRefVar.visit(visitor);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof TripleComponent) {
			TripleComponent o = (TripleComponent) other;
			return tripleRefVar.equals(o.getTripleRefVar());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return tripleRefVar.hashCode();
	}

	@Override
	public TripleComponent clone() {
		return new TripleComponent(tripleRefVar.clone(), this.role);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (tripleRefVar == current) {
			setTripleRefVar((Var) replacement);
		} else {
			throw new IllegalArgumentException("Node is not a child node: " + current);
		}
	}
}
