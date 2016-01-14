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
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Parses tuple results in the background.
 * 
 * @author James Leigh
 */
public class SPARQLTupleQuery extends AbstractHTTPQuery implements TupleQuery {

	// TODO there was some magic going on in SparqlOperation to get baseURI
	// directly replaced within the query using BASE

	public SPARQLTupleQuery(SparqlSession httpClient, String baseUri, String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseUri);
	}

	public TupleQueryResult evaluate()
		throws QueryEvaluationException
	{

		SparqlSession client = getHttpClient();
		try {
			return client.sendTupleQuery(QueryLanguage.SPARQL, getQueryString(), baseURI, dataset,
					getIncludeInferred(), getMaxExecutionTime(), getBindingsArray());
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

	public void evaluate(TupleQueryResultHandler handler)
		throws QueryEvaluationException, TupleQueryResultHandlerException
	{

		SparqlSession client = getHttpClient();
		try {
			client.sendTupleQuery(QueryLanguage.SPARQL, getQueryString(), baseURI, dataset,
					getIncludeInferred(), getMaxExecutionTime(), handler, getBindingsArray());
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
