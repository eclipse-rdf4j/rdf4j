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

import java.util.Objects;

import org.eclipse.rdf4j.model.Value;

/**
 * A variable that can contain a Value.
 *
 * @implNote In the future this class may stop extending AbstractQueryModelNode in favor of directly implementing
 *           ValueExpr and QueryModelNode.
 */
public class Var extends AbstractQueryModelNode implements ValueExpr {

	private final String name;

	private Value value;

	private final boolean anonymous;

	private boolean constant = false;

	private int cachedHashCode = 0;

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

	public Var(String name, Value value, boolean anonymous) {
		this(name, value, anonymous, false);
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public String getName() {
		return name;
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
	public void setParentNode(QueryModelNode parent) {
		assert getParentNode() == null;
		super.setParentNode(parent);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {

	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(this.getClass().getSimpleName());

		sb.append(" (name=").append(name);

		if (value != null) {
			sb.append(", value=").append(value);
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

		if (cachedHashCode != 0 && var.cachedHashCode != 0 && cachedHashCode != var.cachedHashCode) {
			return false;
		}

		return anonymous == var.anonymous && !(name == null && var.name != null || value == null && var.value != null)
				&& Objects.equals(name, var.name) && Objects.equals(value, var.value);
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

}
