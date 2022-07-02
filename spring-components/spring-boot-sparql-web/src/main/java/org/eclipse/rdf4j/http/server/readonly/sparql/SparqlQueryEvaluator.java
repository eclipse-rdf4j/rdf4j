/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly.sparql;

import java.io.IOException;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.Repository;

public interface SparqlQueryEvaluator {
	/**
	 * Evaluates/Execute the passed query against the passed repository usimg the passed arguments.
	 * 
	 * @param result          in/out parameter for returning the contentType and the result stream.
	 * @param repository      the repository against which the query is to be executed
	 * @param query           The query to be evaluated
	 * @param acceptHeader    needed to find the best response format.
	 * @param defaultGraphUri see {@link Dataset#getDefaultGraphs()}
	 * @param namedGraphUris  see {@link Dataset#getNamedGraphs()}
	 * @throws MalformedQueryException If the supplied query is malformed.
	 * @throws IOException             if there is a problem with the {@link EvaluateResult#getOutputstream()}
	 * @throws IllegalStateException   if no acceptHeader is present
	 */
	void evaluate(EvaluateResult result, Repository repository, String query, String acceptHeader,
			String defaultGraphUri,
			String[] namedGraphUris) throws MalformedQueryException, IllegalStateException, IOException;
}
