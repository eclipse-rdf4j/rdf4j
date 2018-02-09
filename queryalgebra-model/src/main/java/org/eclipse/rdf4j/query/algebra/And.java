/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * A boolean AND operator operating on two boolean expressions.
 */
public class And extends BinaryValueOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public And() {
	}

	public And(ValueExpr leftArg, ValueExpr rightArg) {
		super(leftArg, rightArg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof And && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "And".hashCode();
	}

	@Override
	public And clone() {
		return (And)super.clone();
	}
}
