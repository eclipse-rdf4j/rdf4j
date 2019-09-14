/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.api;

import java.util.List;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;

public interface FedXApi {

	public TupleQueryResult evaluate(String query) throws QueryEvaluationException;

	public TupleQueryResult evaluate(String query, List<Endpoint> endpoints)
			throws FedXException, QueryEvaluationException;

	public TupleQueryResult evaluateAt(String query, List<String> endpointIds)
			throws FedXException, QueryEvaluationException;

	public RepositoryResult<Statement> getStatements(Resource subject, IRI predicate, Value object,
			Resource... contexts);

	public void addEndpoint(Endpoint e);

	public void removeEndpoint(Endpoint e);

	public void removeEndpoint(String endpointId);

	public void shutdown();

}
