/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.endpoint.provider;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.util.FedXUtil;

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
