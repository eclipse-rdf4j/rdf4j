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
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Algebra construct representing an empty union.
 *
 * @author Andreas Schwarte
 *
 */
public class EmptyNUnion extends NTuple implements EmptyResult {

	private static final long serialVersionUID = -1268373891635616169L;

	public EmptyNUnion(List<TupleExpr> args, QueryInfo queryInfo) {
		super(args, queryInfo);
	}

}
