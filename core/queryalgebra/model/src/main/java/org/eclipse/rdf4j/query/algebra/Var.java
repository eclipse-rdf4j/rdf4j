/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Objects;

import org.eclipse.rdf4j.model.Value;

/**
 * A variable that can contain a Value.
 */
public final class Var implements ValueExpr, QueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String name;

	private Value value;

	private boolean anonymous = false;

	private boolean constant = false;

	private QueryModelNode parent;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Var() {
	}

	public Var(String name) {
		setName(name);
	}

	public Var(String name, Value value) {
		this(name);
		setValue(value);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean hasValue() {
		return value != null;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public final <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public final <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {

	}

	@Override
	public QueryModelNode getParentNode() {
		return parent;
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		this.parent = parent;
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {

	}

	@Override
	public void replaceWith(QueryModelNode replacement) {
		if (parent == null) {
			throw new IllegalStateException("Node has no parent");
		}

		parent.replaceChildNode(this, replacement);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(this.getClass().getSimpleName());

		sb.append(" (name=").append(name);

		if (value != null) {
			sb.append(", value=").append(value.toString());
		}

		if (anonymous) {
			sb.append(", anonymous");
		}

		sb.append(")");

		return sb.toString();
	}

//	@Override
//	public boolean equals(Object other) {
//		if (this == other)
//			return true;
//		if (other instanceof Var) {
//			Var o = (Var) other;
//			return name.equals(o.getName()) && Objects.equals(value, o.getValue()) && anonymous == o.isAnonymous();
//		}
//		return false;
//	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		if (value != null) {
			result ^= value.hashCode();
		}
		if (anonymous) {
			result = ~result;
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Var var = (Var) o;
		return anonymous == var.anonymous && Objects.equals(name, var.name) && Objects.equals(value, var.value);
	}
//
//	int hashCode = 0;
//
//	@Override
//	public int hashCode() {
//		if(hashCode == 0) {
//			int result = 1;
//			result = 31 * result + (name == null ? 0 : name.hashCode());
//			result = 31 * result + (value == null ? 0 : value.hashCode());
//			result = 31 * result + Boolean.hashCode(anonymous);
//			hashCode = result;
//		}
//		return hashCode;
//	}

	@Override
	public Var clone() {
		try {
			return (Var) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException();
		}

	}

	/**
	 * @return Returns the constant.
	 */
	public boolean isConstant() {
		return constant;
	}

	/**
	 * @param constant The constant to set.
	 */
	public void setConstant(boolean constant) {
		this.constant = constant;
	}
}
