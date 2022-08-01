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

/**
 * The MINUS set operator, which returns the result of the left tuple expression, except for the results that are also
 * returned by the right tuple expression.
 */
public class Difference extends BinaryTupleOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Difference() {
	}

	/**
	 * Creates a new minus operator that operates on the two specified arguments.
	 *
	 * @param leftArg  The left argument of the minus operator.
	 * @param rightArg The right argument of the minus operator.
	 */
	public Difference(TupleExpr leftArg, TupleExpr rightArg) {
		super(leftArg, rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Set<String> getBindingNames() {
		return getLeftArg().getBindingNames();
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
	public boolean equals(Object other) {
		return other instanceof Difference && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Difference".hashCode();
	}

	@Override
	public Difference clone() {
		return (Difference) super.clone();
	}
}
