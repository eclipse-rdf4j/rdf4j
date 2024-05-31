/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * The purpose of this type of tuplexpr is to allow an external source to schedule stopping an iteration/query
 * evaluation.
 */
public class StopableTupleExpr extends AbstractQueryModelNode implements TupleExpr {

	private static final long serialVersionUID = 1L;

	/**
	 * An interface for the code that will accept a stop signal and might return an exception.
	 */
	public static interface ConsumeStop extends Supplier<Optional<RDF4JException>> {

	}

	private TupleExpr child;
	private final BooleanSupplier stop;

	public StopableTupleExpr(TupleExpr child, BooleanSupplier areWeStopped) {
		super();
		this.child = child;
		this.stop = areWeStopped;
	}

	@Override
	public String getSignature() {
		return "StopableTupleExpr (" + child.getSignature() + ')';
	}

	@Override
	public Set<String> getBindingNames() {
		return child.getBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return child.getAssuredBindingNames();
	}

	@Override
	public StopableTupleExpr clone() {
		StopableTupleExpr clone = (StopableTupleExpr) super.clone();
		clone.child = child.clone();
		return clone;
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (child == current && replacement instanceof TupleExpr) {
			child = (TupleExpr) replacement;
		}
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		child.visit(visitor);
	}

	public BooleanSupplier getAreWeStopped() {
		return stop;
	}

	public TupleExpr getChild() {
		return child;
	}
}
