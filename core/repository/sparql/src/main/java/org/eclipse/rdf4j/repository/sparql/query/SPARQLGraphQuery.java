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
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
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

	public SPARQLGraphQuery(SparqlSession httpClient, String baseURI, String queryString) {
		super(httpClient, QueryLanguage.SPARQL, queryString, baseURI);
	}

	public GraphQueryResult evaluate()
		throws QueryEvaluationException
	{
		SparqlSession client = getHttpClient();
		try {
			// TODO getQueryString() already inserts bindings, use emptybindingset
			// as last argument?
			return client.sendGraphQuery(queryLanguage, getQueryString(), baseURI, dataset,
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

	public void evaluate(RDFHandler handler)
		throws QueryEvaluationException, RDFHandlerException
	{

		SparqlSession client = getHttpClient();
		try {
			client.sendGraphQuery(queryLanguage, getQueryString(), baseURI, dataset, getIncludeInferred(),
					getMaxExecutionTime(), handler, getBindingsArray());
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
