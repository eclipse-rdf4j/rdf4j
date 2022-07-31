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
 * Checks whether a certain value is contained in a set of results produced by a query.
 */
public class In extends CompareSubQueryValueOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public In() {
	}

	public In(ValueExpr valueExpr, TupleExpr subQuery) {
		super(valueExpr, subQuery);
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
		return other instanceof In && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "In".hashCode();
	}

	@Override
	public In clone() {
		return (In) super.clone();
	}
}
