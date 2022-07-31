/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.List;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A tuple expression that represents an nary-Union.
 *
 * @author Andreas Schwarte
 *
 */
public class NUnion extends NTuple {

	private static final long serialVersionUID = 7891644349783459781L;

	/**
	 * Construct an nary-tuple. Note that the parentNode of all arguments is set to this instance.
	 *
	 * @param args
	 */
	public NUnion(List<TupleExpr> args, QueryInfo queryInfo) {
		super(args, queryInfo);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public NUnion clone() {
		return (NUnion) super.clone();
	}
}
