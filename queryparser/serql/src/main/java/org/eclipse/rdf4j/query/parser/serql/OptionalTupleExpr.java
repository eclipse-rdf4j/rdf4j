/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;

/**
 * @author Arjohn Kampman
 */
class OptionalTupleExpr {

	private final TupleExpr tupleExpr;

	private final ValueExpr constraint;

	public OptionalTupleExpr(TupleExpr tupleExpr) {
		this(tupleExpr, null);
	}

	public OptionalTupleExpr(TupleExpr tupleExpr, ValueExpr constraint) {
		this.tupleExpr = tupleExpr;
		this.constraint = constraint;
	}

	public TupleExpr getTupleExpr() {
		return tupleExpr;
	}

	public ValueExpr getConstraint() {
		return constraint;
	}

	public boolean hasConstraint() {
		return constraint != null;
	}
}
