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
package com.fluidops.fedx.api;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.exception.FedXException;

public interface FedXApi {

	
	public TupleQueryResult evaluate(String query) throws QueryEvaluationException;
	
	public TupleQueryResult evaluate(String query, List<Endpoint> endpoints) throws FedXException, QueryEvaluationException;
	
	public TupleQueryResult evaluateAt(String query, List<String> endpointIds) throws FedXException, QueryEvaluationException;
	
	public RepositoryResult<Statement> getStatements(Resource subject, IRI predicate, Value object,
			Resource... contexts);

	public void addEndpoint(Endpoint e);

	public void removeEndpoint(Endpoint e);
	
	public void removeEndpoint(String endpointId);
	
	public void shutdown();
	
	

}
