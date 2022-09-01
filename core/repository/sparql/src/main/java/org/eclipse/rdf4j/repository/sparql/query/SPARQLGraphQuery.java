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
package org.eclipse.rdf4j.repository.sparql.query;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPQuery;
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
 * Parses RDF results in the background.
 *
 * @author James Leigh
 * @author Andreas Schwarte
 */
public class SPARQLGraphQuery extends AbstractHTTPQuery implements GraphQuery {

	public SPARQLGraphQuery(SPARQLProtocolSession httpClient, String baseURI, String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseURI);
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		SPARQLProtocolSession client = getHttpClient();
		try {
			// TODO getQueryString() already inserts bindings, use emptybindingset
			// as last argument?
			return client.sendGraphQuery(queryLanguage, getQueryString(), baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), ((WeakReference) null), getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}

	@Override
	public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {

		SPARQLProtocolSession client = getHttpClient();
		try {
			client.sendGraphQuery(queryLanguage, getQueryString(), baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), handler, getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}

	private String getQueryString() {
		return QueryStringUtil.getGraphQueryString(queryString, getBindings());
	}

	@Override
	public Explanation explain(Explanation.Level level) {
		throw new UnsupportedOperationException();
	}
}
