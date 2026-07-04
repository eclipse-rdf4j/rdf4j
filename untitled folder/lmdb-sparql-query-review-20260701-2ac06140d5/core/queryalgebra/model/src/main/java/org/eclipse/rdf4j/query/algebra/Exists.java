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
 * Checks whether the wrapped Query produces any results.
 */
public class Exists extends SubQueryValueOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Exists() {
	}

	public Exists(TupleExpr subQuery) {
		super(subQuery);
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
		return other instanceof Exists && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Exists".hashCode();
	}

	@Override
	public Exists clone() {
		return (Exists) super.clone();
	}
}
