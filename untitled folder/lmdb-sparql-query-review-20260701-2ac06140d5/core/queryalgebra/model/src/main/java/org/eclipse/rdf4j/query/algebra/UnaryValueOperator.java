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

/**
 * An abstract superclass for unary value operators which, by definition, has one argument. In special cases the
 * argument can be null, for instance to represent the argument * in COUNT(*).
 */
public abstract class UnaryValueOperator extends AbstractQueryModelNode implements ValueExpr {

	/**
	 * The operator's argument.
	 */
	protected ValueExpr arg;

	protected UnaryValueOperator() {
	}

	/**
	 * Creates a new unary value operator.
	 *
	 * @param arg The operator's argument, must not be <var>null</var>.
	 */
	protected UnaryValueOperator(ValueExpr arg) {
		setArg(arg);
	}

	/**
	 * Gets the argument of this unary value operator.
	 *
	 * @return The operator's argument.
	 */
	public ValueExpr getArg() {
		return arg;
	}

	/**
	 * Sets the argument of this unary value operator.
	 *
	 * @param arg The (new) argument for this operator, must not be <var>null</var>.
	 */
	public void setArg(ValueExpr arg) {
		assert arg != null : "arg must not be null";
		arg.setParentNode(this);
		this.arg = arg;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (arg != null) {
			arg.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		assert current != null;
		if (arg == current) {
			setArg((ValueExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UnaryValueOperator)) {
			return false;
		}

		UnaryValueOperator that = (UnaryValueOperator) o;

		return Objects.equals(arg, that.arg);
	}

	@Override
	public int hashCode() {
		return arg != null ? arg.hashCode() : 97;
	}

	@Override
	public UnaryValueOperator clone() {
		UnaryValueOperator clone = (UnaryValueOperator) super.clone();
		if (arg != null) {
			clone.setArg(arg.clone());
		}
		return clone;
	}
}
