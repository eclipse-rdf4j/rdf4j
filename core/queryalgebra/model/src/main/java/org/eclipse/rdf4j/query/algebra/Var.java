/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import org.eclipse.rdf4j.model.Value;

/**
 * A variable that can contain a Value.
 */
public class Var extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String name;

	private Value value;

	private boolean anonymous = false;

	private boolean constant = false;
	
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

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(super.getSignature());

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
	public boolean equals(Object other) {
		if (other instanceof Var) {
			Var o = (Var)other;
			return name.equals(o.getName()) && nullEquals(value, o.getValue()) && anonymous == o.isAnonymous();
		}
		return false;
	}

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
	public Var clone() {
		return (Var)super.clone();
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
