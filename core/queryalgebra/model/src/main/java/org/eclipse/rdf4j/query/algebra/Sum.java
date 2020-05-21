/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * The SUM operator as defined in http://www.w3.org/TR/sparql11-query/#aggregates
 *
 * @author Jeen Broekstra
 */
public class Sum extends AbstractAggregateOperator {

	public Sum(ValueExpr arg) {
		super(arg);
	}

	public Sum(ValueExpr arg, boolean distinct) {
		super(arg, distinct);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Sum && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Sum".hashCode();
	}

	@Override
	public Sum clone() {
		return (Sum) super.clone();
	}
}
