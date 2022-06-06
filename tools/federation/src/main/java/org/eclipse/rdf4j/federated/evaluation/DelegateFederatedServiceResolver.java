/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.federated.EndpointManager;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;

/**
 * A {@link FederatedServiceResolver} which redirects SERVICE requests to the federation member (if the SERVICE IRI
 * correspond to a member) or to the provided delegate.
 *
 * @author as
 *
 */
public class DelegateFederatedServiceResolver extends AbstractFederatedServiceResolver {

	private final EndpointManager endpointManager;

	private SPARQLServiceResolver defaultImpl;
	private FederatedServiceResolver delegate;

	public DelegateFederatedServiceResolver(EndpointManager endpointManager) {
		super();
		this.endpointManager = endpointManager;
	}

	public void initialize() {

		if (delegate == null) {
			// use a managed resolver if no explicit resolver is provided
			defaultImpl = new SPARQLServiceResolver();
			delegate = defaultImpl;
		}
	}

	@Override
	public void shutDown() {
		super.shutDown();

		// shutdown the managed resolver
		if (defaultImpl != null) {
			defaultImpl.shutDown();
		}

	}

	public void setDelegate(FederatedServiceResolver federatedServiceResolver) {
		if (delegate != null) {
			throw new IllegalStateException("Delegate already initialized.");
		}
		this.delegate = federatedServiceResolver;
	}

	@Override
	public FederatedService getService(String serviceUrl) throws QueryEvaluationException {
		if (isFedXEndpoint(serviceUrl)) {
			return super.getService(serviceUrl);
		} else {
			return delegate.getService(serviceUrl);
		}
	}

	@Override
	protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
		Endpoint ep = getFedXEndpoint(serviceUrl);
		if (ep != null) {
			return new RepositoryFederatedService(ep.getRepository(), false);
		}
		throw new IllegalStateException("External service URL should be managed by delegate.");
	}

	protected boolean isFedXEndpoint(String serviceUrl) {
		return getFedXEndpoint(serviceUrl) != null;
	}

	/**
	 * Return the FedX endpoint corresponding to the given service URI. If there is no such endpoint in FedX, this
	 * method returns null.
	 *
	 * Note that this method compares the endpoint URL first, however, that the name of the endpoint can be used as
	 * identifier as well. Note that the name must be a valid URI, i.e. start with http://
	 *
	 * @param serviceUri
	 * @return
	 */
	private Endpoint getFedXEndpoint(String serviceUri) {
		Endpoint e = endpointManager.getEndpointByUrl(serviceUri);
		if (e != null) {
			return e;
		}
		e = endpointManager.getEndpointByName(serviceUri);
		return e;
	}
}
