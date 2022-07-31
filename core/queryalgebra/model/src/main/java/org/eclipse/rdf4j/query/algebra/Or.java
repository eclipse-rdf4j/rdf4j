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

/**
 * A boolean OR operator operating on two boolean expressions.
 */
public class Or extends BinaryValueOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Or() {
	}

	public Or(ValueExpr leftArg, ValueExpr rightArg) {
		super(leftArg, rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Or && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Or".hashCode();
	}

	@Override
	public Or clone() {
		return (Or) super.clone();
	}
}
