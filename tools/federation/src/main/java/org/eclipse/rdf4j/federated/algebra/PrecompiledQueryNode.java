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

import org.eclipse.rdf4j.federated.evaluation.SailTripleSource;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Node representing a precompiled query.
 *
 * @author Andreas Schwarte
 * @see SailTripleSource
 */
public class PrecompiledQueryNode extends QueryRoot {

	private static final long serialVersionUID = -5382415823483370751L;

	private TupleExpr query;

	public PrecompiledQueryNode(TupleExpr query) {
		super(query);
		this.query = query;
	}

	public TupleExpr getQuery() {
		return query;
	}

	@Override
	public PrecompiledQueryNode clone() {
		PrecompiledQueryNode clone = (PrecompiledQueryNode) super.clone();
		clone.query = this.query;
		return clone;
	}
}
