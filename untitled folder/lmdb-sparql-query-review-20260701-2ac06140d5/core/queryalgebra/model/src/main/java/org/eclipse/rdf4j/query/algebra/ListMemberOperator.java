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
 * ValueOperator that verifies if the first of its arguments occurs in any of the subsequent arguments.
 *
 * @author Jeen Broekstra
 */
public class ListMemberOperator extends NAryValueOperator {

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ListMemberOperator && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "ListMemberOperator".hashCode();
	}

	@Override
	public ListMemberOperator clone() {
		return (ListMemberOperator) super.clone();
	}
}
