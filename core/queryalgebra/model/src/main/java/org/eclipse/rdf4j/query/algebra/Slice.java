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
 * The SLICE operator, as defined in <a href="http://www.w3.org/TR/rdf-sparql-query/#defn_algSlice">SPARQL Query
 * Language for RDF</a>. The SLICE operator selects specific results from the underlying tuple expression based on an
 * offset and limit value (both optional).
 *
 * @author Arjohn Kampman
 */
public class Slice extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private long offset;

	private long limit;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Slice() {
		this(null);
	}

	public Slice(TupleExpr arg) {
		this(arg, -1, -1);
	}

	public Slice(TupleExpr arg, long offset2, long limit2) {
		super(arg);
		setOffset(offset2);
		setLimit(limit2);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	/**
	 * Checks whether the row selection has a (valid) offset.
	 *
	 * @return <var>true</var> when <var>offset &gt; 0</var>
	 */
	public boolean hasOffset() {
		return offset > 0L;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	/**
	 * Checks whether the row selection has a (valid) limit.
	 *
	 * @return <var>true</var> when <var>offset &gt;= 0</var>
	 */
	public boolean hasLimit() {
		return limit >= 0L;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(256);

		sb.append(super.getSignature());
		sb.append(" (");

		if (hasLimit()) {
			sb.append("limit=").append(getLimit());
		}
		if (hasOffset()) {
			sb.append("offset=").append(getOffset());
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Slice && super.equals(other)) {
			Slice o = (Slice) other;
			return offset == o.getOffset() && limit == o.getLimit();
		}
		return false;
	}

	@Override
	public int hashCode() {
		// casting long to int is not safe, but shouldn't matter for hashcode, should it?
		return super.hashCode() ^ (int) offset ^ (int) limit;
	}

	@Override
	public Slice clone() {
		return (Slice) super.clone();
	}
}
