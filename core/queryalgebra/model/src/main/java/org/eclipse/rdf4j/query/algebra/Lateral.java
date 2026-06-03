/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This operator evaluates the right-hand side for each row from the left-hand side, with variables from the left-hand
 * side in scope for the right-hand side evaluation.
 */
public class Lateral extends BinaryTupleOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Lateral() {
	}

	public Lateral(TupleExpr leftArg, TupleExpr rightArg) {
		super(leftArg, rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(16);
		bindingNames.addAll(getLeftArg().getBindingNames());
		bindingNames.addAll(getRightArg().getBindingNames());
		return bindingNames;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return getLeftArg().getAssuredBindingNames();
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		super.visitChildren(visitor);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Lateral && super.equals(other)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Lateral".hashCode();
	}

	@Override
	public Lateral clone() {
		return (Lateral) super.clone();
	}
}
