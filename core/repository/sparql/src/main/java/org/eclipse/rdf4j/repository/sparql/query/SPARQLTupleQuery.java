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
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Parses tuple results in the background.
 *
 * @author James Leigh
 */
public class SPARQLTupleQuery extends AbstractHTTPQuery implements TupleQuery {

	// TODO there was some magic going on in SparqlOperation to get baseURI
	// directly replaced within the query using BASE

	public SPARQLTupleQuery(SPARQLProtocolSession httpClient, String baseUri, String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseUri);
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {

		SPARQLProtocolSession client = getHttpClient();
		try {
			return client.sendTupleQuery(QueryLanguage.SPARQL, getQueryString(), baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), new WeakReference<>(this), getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {

		SPARQLProtocolSession client = getHttpClient();
		try {
			client.sendTupleQuery(QueryLanguage.SPARQL, getQueryString(), baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), handler, getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}

	private String getQueryString() {
		return QueryStringUtil.getTupleQueryString(queryString, getBindings());
	}

	@Override
	public Explanation explain(Explanation.Level level) {
		throw new UnsupportedOperationException();
	}
}
