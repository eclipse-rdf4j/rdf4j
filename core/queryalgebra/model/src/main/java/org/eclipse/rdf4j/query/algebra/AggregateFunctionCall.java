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
 * Custom {@link AggregateOperator} function call that can be defined to take an argument and can apply distinct
 * filtering on it.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class AggregateFunctionCall extends AbstractAggregateOperator {

	protected String iri;

	public AggregateFunctionCall(String iri, boolean distinct) {
		super(null, distinct);
		this.iri = iri;
	}

	public AggregateFunctionCall(ValueExpr arg, String iri, boolean distinct) {
		super(arg, distinct);
		this.iri = iri;
	}

	public String getURI() {
		return iri;
	}

	public void setURI(String uri) {
		this.iri = uri;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(super.getSignature());

		sb.append(" (").append(iri);

		sb.append(" distinct=").append(isDistinct());

		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AggregateFunctionCall) {
			AggregateFunctionCall o = (AggregateFunctionCall) other;
			return iri.equals(o.getURI()) && arg.equals(o.getArg()) && isDistinct() == o.isDistinct();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return iri.hashCode() ^ arg.hashCode();
	}

	@Override
	public AggregateFunctionCall clone() {
		return (AggregateFunctionCall) super.clone();
	}
}
