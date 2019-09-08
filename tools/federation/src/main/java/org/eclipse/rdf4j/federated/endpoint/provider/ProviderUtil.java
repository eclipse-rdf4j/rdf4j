/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint.provider;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.Config;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Convenience methods for {@link Endpoint} providers
 * 
 * @author Andreas Schwarte
 *
 */
public class ProviderUtil {

	/**
	 * Checks the connection by submitting a SPARQL SELECT query:
	 * 
	 * SELECT * WHERE { ?s ?p ?o } LIMIT 1
	 * 
	 * Throws an exception if the query cannot be evaluated
	 * successfully for some reason (indicating that the 
	 * endpoint is not ok)
	 * 
	 * @param repo
	 * @throws RepositoryException
	 * @throws QueryEvaluationException
	 * @throws MalformedQueryException
	 */
	public static void checkConnectionIfConfigured(Repository repo) throws RepositoryException {
		
		if (!Config.getConfig().isValidateRepositoryConnections())
			return;
		
		RepositoryConnection conn = null;		
		try {
			conn = repo.getConnection();
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o } LIMIT 1");
			FedXUtil.applyMaxQueryExecutionTime(query);
			TupleQueryResult qRes = null;
			try {
				qRes = query.evaluate();
				if (!qRes.hasNext())
					throw new RepositoryException("No data in provided repository");
				// Consume data to avoid pending connections
				Iterations.asList(qRes);
			} finally {
				if (qRes!=null)
					Iterations.closeCloseable(qRes);
			}			
			
		} catch (MalformedQueryException ignore) { 
				;	// can never occur
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		} finally {			
			if (conn!=null)
				conn.close();
		}
	}
}
