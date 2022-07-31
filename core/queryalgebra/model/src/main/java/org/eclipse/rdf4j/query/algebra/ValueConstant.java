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

import org.eclipse.rdf4j.model.Value;

/**
 * A ValueExpr with a constant value.
 */
public class ValueConstant extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Value value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ValueConstant() {
	}

	public ValueConstant(Value value) {
		setValue(value);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		assert value != null : "value must not be null";
		this.value = value;
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
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(super.getSignature());
		sb.append(" (value=");
		sb.append(value.toString());
		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ValueConstant) {
			ValueConstant o = (ValueConstant) other;
			return value.equals(o.getValue());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public ValueConstant clone() {
		return (ValueConstant) super.clone();
	}
}
