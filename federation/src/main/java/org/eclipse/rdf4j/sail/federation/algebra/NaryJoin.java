/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.algebra;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A natural join between two tuple expressions.
 */
public class NaryJoin extends AbstractNaryTupleOperator {

	private static final long serialVersionUID = -1501013589230065874L;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NaryJoin() {
		super();
	}

	/**
	 * Creates a new natural join operator.
	 */
	public NaryJoin(TupleExpr... args) {
		super(args);
	}

	/**
	 * Creates a new natural join operator.
	 */
	public NaryJoin(List<TupleExpr> args) {
		super(args);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meetOther(this);
	}

	@Override
	public NaryJoin clone() { // NOPMD
		return (NaryJoin)super.clone();
	}
}
