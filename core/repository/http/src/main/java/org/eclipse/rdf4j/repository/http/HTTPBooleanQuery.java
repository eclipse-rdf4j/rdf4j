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
package org.eclipse.rdf4j.repository.http;

import java.io.IOException;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPQuery;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * TupleQuery specific to the HTTP protocol. Methods in this class may throw the specific RepositoryException subclass
 * UnautorizedException, the semantics of which is defined by the HTTP protocol.
 *
 * @see org.eclipse.rdf4j.http.protocol.UnauthorizedException
 * @author Arjohn Kampman
 */
public class HTTPBooleanQuery extends AbstractHTTPQuery implements BooleanQuery {

	private final HTTPRepositoryConnection conn;

	public HTTPBooleanQuery(HTTPRepositoryConnection conn, QueryLanguage ql, String queryString, String baseURI) {
		super(conn.getSesameSession(), ql, queryString, baseURI);
		this.conn = conn;
	}

	@Override
	public boolean evaluate() throws QueryEvaluationException {
		SPARQLProtocolSession client = getHttpClient();

		try {
			conn.flushTransactionState(Protocol.Action.QUERY);
			return client.sendBooleanQuery(queryLanguage, queryString, baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
	}

	@Override
	public Explanation explain(Explanation.Level level) {
		throw new UnsupportedOperationException();
	}
}
