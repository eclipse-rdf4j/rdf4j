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
 * The GROUP_CONCAT operator as defined in http://www.w3.org/TR/sparql11-query/#aggregates
 *
 * @author Jeen Broekstra
 */
public class GroupConcat extends AbstractAggregateOperator {

	private ValueExpr separator;

	public GroupConcat(ValueExpr arg) {
		super(arg);
	}

	public GroupConcat(ValueExpr arg, boolean distinct) {
		super(arg, distinct);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof GroupConcat && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Group_Concat".hashCode();
	}

	@Override
	public GroupConcat clone() {
		return (GroupConcat) super.clone();
	}

	public ValueExpr getSeparator() {
		return separator;
	}

	public void setSeparator(ValueExpr separator) {
		this.separator = separator;
	}
}
