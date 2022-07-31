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

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPQuery;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Parses boolean query response from remote stores.
 *
 * @author James Leigh
 */
public class SPARQLBooleanQuery extends AbstractHTTPQuery implements BooleanQuery {

	public SPARQLBooleanQuery(SPARQLProtocolSession httpClient, String baseURI, String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseURI);
	}

	@Override
	public boolean evaluate() throws QueryEvaluationException {

		SPARQLProtocolSession client = getHttpClient();

		try {
			return client.sendBooleanQuery(queryLanguage, getQueryString(), baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), getBindingsArray());
		} catch (IOException | RepositoryException | MalformedQueryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}

	private String getQueryString() {
		return QueryStringUtil.getBooleanQueryString(queryString, getBindings());
	}

	@Override
	public Explanation explain(Explanation.Level level) {
		throw new UnsupportedOperationException();
	}
}
