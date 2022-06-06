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
 *
 * @implNote In the future this class may stop extending AbstractQueryModelNode in favor of directly implementing
 *           ValueExpr and QueryModelNode.
 */
public class Var extends AbstractQueryModelNode implements ValueExpr {

	private String name;

	private Value value;

	private boolean anonymous = false;

	private boolean constant = false;

	private QueryModelNode parent;

	private int cachedHashCode = 0;

	@Deprecated(forRemoval = true, since = "4.1.0")
	public Var() {
	}

	public Var(String name, Value value, boolean anonymous, boolean constant) {
		this.name = name;
		this.value = value;
		this.anonymous = anonymous;
		this.constant = constant;

	}

	public Var(String name) {
		this(name, null, false, false);
	}

	public Var(String name, boolean anonymous) {
		this(name, null, anonymous, false);
	}

	public Var(String name, Value value) {
		this(name, value, false, false);
	}

	@Deprecated(forRemoval = true, since = "4.1.0")
	public void setAnonymous(boolean anonymous) {
		this.cachedHashCode = 0;
		this.anonymous = anonymous;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public String getName() {
		return name;
	}

	@Deprecated(forRemoval = true, since = "4.1.0")
	public void setName(String name) {
		this.cachedHashCode = 0;
		this.name = name;
	}

	@Deprecated(forRemoval = true, since = "4.1.0")
	public void setValue(Value value) {
		this.cachedHashCode = 0;
		this.value = value;
	}

	public boolean hasValue() {
		return value != null;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
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

	@Override
	public int hashCode() {
		if (cachedHashCode == 0) {
			int result = 1;
			result = 31 * result + (name == null ? 0 : name.hashCode());
			result = 31 * result + (value == null ? 0 : value.hashCode());
			result = 31 * result + Boolean.hashCode(anonymous);
			cachedHashCode = result;
		}
		return cachedHashCode;
	}

	@Override
	public Var clone() {
		return new Var(name, value, anonymous, constant);
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
	@Deprecated(forRemoval = true, since = "4.1.0")
	public void setConstant(boolean constant) {
		this.constant = constant;
	}
}
