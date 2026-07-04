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
 * Checks whether a language tag (e.g. "en-US") matches some language range (e.g. "en" or "*").
 */
public class LangMatches extends BinaryValueOperator {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LangMatches() {
	}

	public LangMatches(ValueExpr leftArg, ValueExpr rightArg) {
		super(leftArg, rightArg);
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
		return other instanceof LangMatches && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "LangMatches".hashCode();
	}

	@Override
	public LangMatches clone() {
		return (LangMatches) super.clone();
	}
}
