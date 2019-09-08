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
package com.fluidops.fedx.evaluation;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.endpoint.Endpoint;

/**
 * A {@link FederatedServiceResolver} which redirects SERVICE requests to
 * the federation member (if the SERVICE IRI correspond to a member) or to
 * the provided delegate.
 * 
 * @author as
 *
 */
public class DelegateFederatedServiceResolver extends AbstractFederatedServiceResolver implements FederatedServiceResolver {
	
	
	private static DelegateFederatedServiceResolver instance = null;
	
	public static FederatedServiceResolver getInstance() {
		if (instance==null)
			throw new IllegalStateException("Not initialized, call #initialize() first");
		return instance;
	}
	
	public static void initialize() {
		instance = new DelegateFederatedServiceResolver();
	}
	
	public static void shutdown() {
		instance.defaultImpl.shutDown();
		instance.shutDown();
		instance = null;
		
	}
	
	private final SPARQLServiceResolver defaultImpl = new SPARQLServiceResolver();
	private FederatedServiceResolver delegate = defaultImpl;
	

	@Override
	protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
		Endpoint ep = getFedXEndpoint(serviceUrl);
		if (ep != null) {
			return new RepositoryFederatedService(ep.getRepository(), false);
		}
		return delegate.getService(serviceUrl);
	}
	
	/**
	 * Return the FedX endpoint corresponding to the given service URI. If 
	 * there is no such endpoint in FedX, this method returns null.
	 * 
	 * Note that this method compares the endpoint URL first, however, that
	 * the name of the endpoint can be used as identifier as well. Note that
	 * the name must be a valid URI, i.e. start with http://
	 * 
	 * @param serviceUri
	 * @return
	 */
	private Endpoint getFedXEndpoint(String serviceUri) {
		EndpointManager em = EndpointManager.getEndpointManager();
		Endpoint e = em.getEndpointByUrl(serviceUri);
		if (e!=null)
			return e;
		e = em.getEndpointByName(serviceUri);
		return e;
	}
}
