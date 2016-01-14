/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.query;

import java.io.IOException;

import org.eclipse.rdf4j.http.client.SparqlSession;
import org.eclipse.rdf4j.http.client.query.AbstractHTTPQuery;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Parses boolean query response from remote stores.
 * 
 * @author James Leigh
 * 
 */
public class SPARQLBooleanQuery extends AbstractHTTPQuery implements BooleanQuery {

	public SPARQLBooleanQuery(SparqlSession httpClient, String baseURI,
			String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseURI);
	}

	public boolean evaluate() throws QueryEvaluationException {
		
		SparqlSession client = getHttpClient();

		try {
			return client.sendBooleanQuery(queryLanguage, getQueryString(), baseURI, dataset, getIncludeInferred(), getMaxExecutionTime(),
					getBindingsArray());
		}
		catch (IOException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
		catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}		
	}
	
	private String getQueryString() {
		return QueryStringUtil.getQueryString(queryString, getBindings());
	}
}
