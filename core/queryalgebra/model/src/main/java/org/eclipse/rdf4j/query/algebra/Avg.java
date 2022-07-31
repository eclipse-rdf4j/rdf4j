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
 * The AVG operator as defined in http://www.w3.org/TR/sparql11-query/#aggregates.
 * <P>
 * Note that we introduce AVG as a first-class object into the algebra, despite it being defined as a compound of other
 * operators (namely, SUM and COUNT). This allows us to more easily optimize evaluation.
 *
 * @author Jeen Broekstra
 */
public class Avg extends AbstractAggregateOperator {

	public Avg(ValueExpr arg) {
		super(arg);
	}

	public Avg(ValueExpr arg, boolean distinct) {
		super(arg, distinct);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Avg && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Avg".hashCode();
	}

	@Override
	public Avg clone() {
		return (Avg) super.clone();
	}
}
