/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * A specialization of {@link ArbitraryLengthPath} to maintain the {@link QueryInfo}
 *
 * @author Andreas Schwarte
 */
public class FedXArbitraryLengthPath extends ArbitraryLengthPath implements FedXTupleExpr {

	private static final long serialVersionUID = -7512248084095130084L;

	private final QueryInfo queryInfo;

	public FedXArbitraryLengthPath(ArbitraryLengthPath path, QueryInfo queryInfo) {
		super(path.getScope(), clone(path.getSubjectVar()), path.getPathExpression(), clone(path.getObjectVar()),
				clone(path.getContextVar()), path.getMinLength());
		this.queryInfo = queryInfo;
	}

	private static Var clone(Var var) {
		if (var == null) {
			return null;
		}
		return var.clone();
	}

	@Override
	public List<String> getFreeVars() {
		return List.copyOf(QueryAlgebraUtil.getFreeVars(getPathExpression()));
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

}
