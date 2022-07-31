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
 * @author David Huynh
 * @author Jeen Broekstra
 */
public class Count extends AbstractAggregateOperator {

	public Count(ValueExpr arg) {
		super(arg);
	}

	public Count(ValueExpr arg, boolean distinct) {
		super(arg, distinct);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Count && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "Count".hashCode();
	}

	@Override
	public Count clone() {
		return (Count) super.clone();
	}

	@Override
	public String getSignature() {
		String signature = super.getSignature();
		if (isDistinct()) {
			signature += " (Distinct)";
		}
		return signature;
	}
}
