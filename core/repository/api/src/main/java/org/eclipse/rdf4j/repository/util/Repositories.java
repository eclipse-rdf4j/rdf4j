/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.rio.RDFHandler;

/**
 * Utility for dealing with {@link Repository} and {@link RepositoryConnection} objects.
 * 
 * @author Peter Ansell
 */
public final class Repositories {

	/**
	 * Performs a SPARQL Select query on the given Repository and passes the results to the given
	 * {@link TupleQueryResultHandler}.
	 * 
	 * @param repository
	 *        The {@link Repository} to open a connection to.
	 * @param query
	 *        The SPARQL Select query to execute.
	 * @param handler
	 *        A {@link TupleQueryResultHandler} that consumes the results.
	 * @throws RepositoryException
	 *         If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException
	 *         If the transaction state was not properly recognised. (Optional specific exception)
	 * @throws MalformedQueryException
	 *         If the supplied query is malformed
	 * @throws QueryEvaluationException
	 *         If there was an error evaluating the query
	 */
	public static void tupleQuery(Repository repository, String query, TupleQueryResultHandler handler)
		throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
		QueryEvaluationException
	{
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery preparedQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			preparedQuery.evaluate(handler);
		}
	}

	/**
	 * Performs a SPARQL Construct or Describe query on the given Repository and passes the results to the
	 * given {@link RDFHandler}.
	 * 
	 * @param repository
	 *        The {@link Repository} to open a connection to.
	 * @param query
	 *        The SPARQL Construct or Describe query to execute.
	 * @param handler
	 *        An {@link RDFHandler} that consumes the results.
	 * @throws RepositoryException
	 *         If there was an exception dealing with the Repository.
	 * @throws UnknownTransactionStateException
	 *         If the transaction state was not properly recognised. (Optional specific exception)
	 * @throws MalformedQueryException
	 *         If the supplied query is malformed
	 * @throws QueryEvaluationException
	 *         If there was an error evaluating the query
	 */
	public static void graphQuery(Repository repository, String query, RDFHandler handler)
		throws RepositoryException, UnknownTransactionStateException, MalformedQueryException,
		QueryEvaluationException
	{
		try (RepositoryConnection conn = repository.getConnection()) {
			GraphQuery preparedQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);
			preparedQuery.evaluate(handler);
		}
	}

	/**
	 * Private constructor to prevent instantiation, this is a static helper class.
	 */
	private Repositories() {
	}

}
