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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The INTERSECT set operator, which returns the intersection of the result sets of two tuple expressions.
 */
public class Intersection extends BinaryTupleOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Intersection() {
	}

	/**
	 * Creates a new intersection operator that operates on the two specified arguments.
	 *
	 * @param leftArg  The left argument of the intersection operator.
	 * @param rightArg The right argument of the intersection operator.
	 */
	public Intersection(TupleExpr leftArg, TupleExpr rightArg) {
		super(leftArg, rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(16);
		bindingNames.addAll(getLeftArg().getBindingNames());
		bindingNames.retainAll(getRightArg().getBindingNames());
		return bindingNames;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<>(16);
		bindingNames.addAll(getLeftArg().getAssuredBindingNames());
		bindingNames.retainAll(getRightArg().getAssuredBindingNames());
		return bindingNames;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Intersection && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Intersection".hashCode();
	}

	@Override
	public Intersection clone() {
		return (Intersection) super.clone();
	}
}
