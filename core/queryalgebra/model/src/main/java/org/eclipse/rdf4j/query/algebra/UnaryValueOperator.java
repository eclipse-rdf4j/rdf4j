/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * An abstract superclass for unary value operators which, by definition, has one argument.
 */
public abstract class UnaryValueOperator extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's argument.
	 */
	protected ValueExpr arg;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new empty unary value operator.
	 */
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

	/*---------*
	 * Methods *
	 *---------*/

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
		if (arg == current) {
			setArg((ValueExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof UnaryValueOperator) {
			UnaryValueOperator o = (UnaryValueOperator) other;
			return (arg == null && o.getArg() == null) || (arg != null && arg.equals(o.getArg()));
		}

		return false;
	}

	@Override
	public int hashCode() {
		return arg.hashCode();
	}

	@Override
	public UnaryValueOperator clone() {
		UnaryValueOperator clone = (UnaryValueOperator) super.clone();
		if (getArg() != null) {
			clone.setArg(getArg().clone());
		}
		return clone;
	}
}
