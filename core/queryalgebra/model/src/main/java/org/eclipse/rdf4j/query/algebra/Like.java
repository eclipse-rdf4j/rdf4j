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
 * Compares the string representation of a value expression to a pattern.
 */
@Deprecated(forRemoval = true)
public class Like extends UnaryValueOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String pattern;

	private boolean caseSensitive;

	/**
	 * Operational pattern, equal to pattern but converted to lower case when not case sensitive.
	 */
	private String opPattern;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Like() {
	}

	public Like(ValueExpr expr, String pattern, boolean caseSensitive) {
		super(expr);
		setPattern(pattern, caseSensitive);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public void setPattern(String pattern, boolean caseSensitive) {
		assert pattern != null : "pattern must not be null";
		this.pattern = pattern;
		this.caseSensitive = caseSensitive;
		opPattern = caseSensitive ? pattern : pattern.toLowerCase();
	}

	public String getPattern() {
		return pattern;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public String getOpPattern() {
		return opPattern;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(128);

		sb.append(super.getSignature());
		sb.append(" \"");
		sb.append(pattern);
		sb.append("\"");

		if (caseSensitive) {
			sb.append(" IGNORE CASE");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Like && super.equals(other)) {
			Like o = (Like) other;
			return caseSensitive == o.isCaseSensitive() && opPattern.equals(o.getOpPattern());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ opPattern.hashCode();
	}

	@Override
	public Like clone() {
		return (Like) super.clone();
	}
}
