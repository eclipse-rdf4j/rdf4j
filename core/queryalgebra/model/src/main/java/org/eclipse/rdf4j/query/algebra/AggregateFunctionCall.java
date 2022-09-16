/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * Custom {@link AggregateOperator} function call that can be defined to take an argument and can apply distinct
 * filtering on it.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@Experimental
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

	public String getIRI() {
		return iri;
	}

	public void setIRI(String uri) {
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		AggregateFunctionCall that = (AggregateFunctionCall) o;

		return iri.equals(that.iri);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + iri.hashCode();
		return result;
	}

	@Override
	public AggregateFunctionCall clone() {
		return (AggregateFunctionCall) super.clone();
	}
}
