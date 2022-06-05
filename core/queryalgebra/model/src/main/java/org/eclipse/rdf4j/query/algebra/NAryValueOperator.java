/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract superclass for N-ary value operators.
 *
 * @author Jeen
 */
public abstract class NAryValueOperator extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's arguments.
	 */
	protected List<ValueExpr> args;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected NAryValueOperator() {
	}

	/**
	 * Creates a new N-Ary value operator.
	 *
	 * @param args The operator's list of arguments, must not be <var>null</var>.
	 */
	protected NAryValueOperator(List<ValueExpr> args) {
		setArguments(args);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public void setArguments(List<ValueExpr> args) {
		this.args = args;
	}

	public List<ValueExpr> getArguments() {
		return this.args;
	}

	public void addArgument(ValueExpr arg) {
		if (args == null) {
			args = new ArrayList<>();
		}
		args.add(arg);
		arg.setParentNode(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		for (ValueExpr arg : args) {
			arg.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		for (int i = 0; i < args.size(); i++) {
			ValueExpr arg = args.get(i);
			if (arg == current) {
				args.set(i, (ValueExpr) replacement);
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof NAryValueOperator) {
			NAryValueOperator o = (NAryValueOperator) other;

			return getArguments().equals(o.getArguments());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getArguments().hashCode();
	}

	@Override
	public NAryValueOperator clone() {
		NAryValueOperator clone = (NAryValueOperator) super.clone();

		clone.setArguments(new ArrayList<>());

		for (ValueExpr arg : getArguments()) {
			clone.addArgument(arg.clone());
		}

		return clone;
	}
}
