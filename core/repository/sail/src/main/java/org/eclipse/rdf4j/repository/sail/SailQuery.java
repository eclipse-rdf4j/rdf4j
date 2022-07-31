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
package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.impl.AbstractParserQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * @author Arjohn Kampman
 */
public abstract class SailQuery extends AbstractParserQuery {

	private final SailRepositoryConnection con;

	protected SailQuery(ParsedQuery parsedQuery, SailRepositoryConnection con) {
		super(parsedQuery);
		this.con = con;
	}

	protected SailRepositoryConnection getConnection() {
		return con;
	}

	@Override
	public Explanation explain(Explanation.Level level) {

		int timeout = DEFAULT_EXPLANATION_EXECUTION_TIMEOUT;
		if (getMaxExecutionTime() > 0) {
			timeout = getMaxExecutionTime();
		}

		TupleExpr tupleExpr = getParsedQuery().getTupleExpr();

		// That query has a root does not add to the explanation.
		if (tupleExpr instanceof QueryRoot) {
			tupleExpr = ((QueryRoot) tupleExpr).getArg();
		}
		SailConnection sailCon = getConnection().getSailConnection();

		return sailCon.explain(level, tupleExpr, getActiveDataset(), getBindings(), getIncludeInferred(), timeout);

	}
}
