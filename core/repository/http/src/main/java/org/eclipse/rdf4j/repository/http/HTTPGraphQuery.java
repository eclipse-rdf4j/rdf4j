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
import java.lang.ref.WeakReference;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPQuery;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * GraphQuery implementation specific to the HTTP protocol. Methods in this class may throw the specific
 * RepositoryException subclass UnautorizedException, the semantics of which is defined by the HTTP protocol.
 *
 * @see org.eclipse.rdf4j.http.protocol.UnauthorizedException
 * @author Arjohn Kampman
 * @author Herko ter Horst
 * @author Andreas Schwarte
 */
public class HTTPGraphQuery extends AbstractHTTPQuery implements GraphQuery {

	private final HTTPRepositoryConnection conn;

	public HTTPGraphQuery(HTTPRepositoryConnection conn, QueryLanguage ql, String queryString, String baseURI) {
		super(conn.getSesameSession(), ql, queryString, baseURI);
		this.conn = conn;
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		SPARQLProtocolSession client = getHttpClient();
		try {
			conn.flushTransactionState(Protocol.Action.QUERY);
			return client.sendGraphQuery(queryLanguage, queryString, baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), ((WeakReference) null), getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
	}

	/*
	 * public GraphQueryResult evaluate() throws QueryEvaluationException { HTTPClient client =
	 * httpCon.getRepository().getHTTPClient(); try { return client.sendGraphQuery(queryLanguage, queryString, baseURI,
	 * dataset, includeInferred, maxQueryTime, getBindingsArray()); } catch (IOException e) { throw new
	 * HTTPQueryEvaluationException(e.getMessage(), e); } catch (RepositoryException e) { throw new
	 * HTTPQueryEvaluationException(e.getMessage(), e); } catch (MalformedQueryException e) { throw new
	 * HTTPQueryEvaluationException(e.getMessage(), e); } }
	 */

	@Override
	public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
		SPARQLProtocolSession client = getHttpClient();
		try {
			conn.flushTransactionState(Protocol.Action.QUERY);
			client.sendGraphQuery(queryLanguage, queryString, baseURI, dataset, includeInferred, getMaxExecutionTime(),
					handler, getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
	}

	@Override
	public Explanation explain(Explanation.Level level) {
		throw new UnsupportedOperationException();
	}
}
