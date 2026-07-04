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
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;

/**
 * A specialization of {@link ZeroLengthPath} that keeps track of {@link QueryInfo} and statement sources.
 *
 * @author Andreas Schwarte
 */
public class FedXZeroLengthPath extends ZeroLengthPath implements QueryRef {

	private static final long serialVersionUID = 2241037911187178861L;

	private final QueryInfo queryInfo;

	private final List<StatementSource> statementSources;

	public FedXZeroLengthPath(Scope scope, Var subjVar, Var objVar, Var conVar, QueryInfo queryInfo,
			List<StatementSource> statementSources) {
		super(scope, subjVar, objVar, conVar);
		this.queryInfo = queryInfo;
		this.statementSources = statementSources;
	}

	public List<StatementSource> getStatementSources() {
		return statementSources;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

}
