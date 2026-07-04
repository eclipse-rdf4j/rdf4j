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

import java.util.ArrayList;
import java.util.List;

/**
 * A call to an (external) function that operates on zero or more arguments.
 *
 * @author Arjohn Kampman
 */
public class FunctionCall extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected String uri;

	/**
	 * The operator's argument.
	 */
	protected List<ValueExpr> args = new ArrayList<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FunctionCall() {
	}

	/**
	 * Creates a new unary value operator.
	 *
	 * @param args The operator's argument, must not be <var>null</var>.
	 */
	public FunctionCall(String uri, ValueExpr... args) {
		setURI(uri);
		addArgs(args);
	}

	public FunctionCall(String uri, Iterable<ValueExpr> args) {
		setURI(uri);
		addArgs(args);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getURI() {
		return uri;
	}

	public void setURI(String uri) {
		this.uri = uri;
	}

	public List<ValueExpr> getArgs() {
		return args;
	}

	public void setArgs(Iterable<ValueExpr> args) {
		this.args.clear();
		addArgs(args);
	}

	public void addArgs(ValueExpr... args) {
		for (ValueExpr arg : args) {
			addArg(arg);
		}
	}

	public void addArgs(Iterable<ValueExpr> args) {
		for (ValueExpr arg : args) {
			addArg(arg);
		}
	}

	public void addArg(ValueExpr arg) {
		assert arg != null : "arg must not be null";
		args.add(arg);
		arg.setParentNode(this);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		for (ValueExpr arg : args) {
			arg.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		replaceNodeInList(args, current, replacement);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(super.getSignature());

		sb.append(" (").append(uri);

		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof FunctionCall) {
			FunctionCall o = (FunctionCall) other;
			return uri.equals(o.getURI()) && args.equals(o.getArgs());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return uri.hashCode() ^ args.hashCode();
	}

	@Override
	public FunctionCall clone() {
		FunctionCall clone = (FunctionCall) super.clone();

		clone.args = new ArrayList<>(getArgs().size());
		for (ValueExpr arg : getArgs()) {
			clone.addArg(arg.clone());
		}

		return clone;
	}
}
