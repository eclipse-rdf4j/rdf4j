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

import java.util.Set;

import org.eclipse.rdf4j.common.order.AvailableStatementOrder;

/**
 * An abstract superclass for unary tuple operators which, by definition, has one argument.
 */
public abstract class UnaryTupleOperator extends AbstractQueryModelNode implements TupleExpr {

	/**
	 * The operator's argument.
	 */
	protected TupleExpr arg;

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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UnaryTupleOperator)) {
			return false;
		}

		UnaryTupleOperator that = (UnaryTupleOperator) o;

		return arg.equals(that.arg);
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

	@Override
	public Set<Var> getSupportedOrders(AvailableStatementOrder tripleSource) {
		return arg.getSupportedOrders(tripleSource);
	}

	@Override
	public void setOrder(Var var) {
		arg.setOrder(var);
	}

	@Override
	public Var getOrder() {
		return arg.getOrder();
	}
}
