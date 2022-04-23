/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Set;

/**
 * An abstract superclass for unary tuple operators which, by definition, has one argument.
 */
public abstract class UnaryTupleOperator extends AbstractQueryModelNode implements TupleExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's argument.
	 */
	protected TupleExpr arg;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected UnaryTupleOperator() {
	}

	/**
	 * Creates a new unary tuple operator.
	 *
	 * @param arg The operator's argument, must not be <var>null</var>.
	 */
	protected UnaryTupleOperator(TupleExpr arg) {
		setArg(arg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the argument of this unary tuple operator.
	 *
	 * @return The operator's argument.
	 */
	public TupleExpr getArg() {
		return arg;
	}

	/**
	 * Sets the argument of this unary tuple operator.
	 *
	 * @param arg The (new) argument for this operator, must not be <var>null</var>.
	 */
	public void setArg(TupleExpr arg) {
		assert arg != null : "arg must not be null";
		assert arg != this : "arg must not be itself";
		arg.setParentNode(this);
		this.arg = arg;
	}

	@Override
	public Set<String> getBindingNames() {
		return getArg().getBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return getArg().getAssuredBindingNames();
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		arg.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (arg == current) {
			setArg((TupleExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof UnaryTupleOperator) {
			UnaryTupleOperator o = (UnaryTupleOperator) other;
			return arg.equals(o.getArg());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return arg.hashCode();
	}

	@Override
	public UnaryTupleOperator clone() {
		UnaryTupleOperator clone = (UnaryTupleOperator) super.clone();
		clone.arg = getArg().clone();
		clone.arg.setParentNode(clone);
		return clone;
	}

}
