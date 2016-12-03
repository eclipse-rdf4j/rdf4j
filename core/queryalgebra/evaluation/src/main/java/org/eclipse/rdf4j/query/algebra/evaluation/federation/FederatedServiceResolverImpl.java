/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SesameClient;
import org.eclipse.rdf4j.http.client.SesameClientDependent;
import org.eclipse.rdf4j.http.client.SesameClientImpl;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * The {@link FederatedServiceResolverImpl} is used to manage a set of {@link FederatedService} instances,
 * which are used to evaluate SERVICE expressions for particular service Urls.
 * <p>
 * Lookup can be done via the serviceUrl using the method {@link #getService(String)}. If there is no service
 * for the specified url, a {@link SPARQLFederatedService} is created and registered for future use.
 * 
 * @author Andreas Schwarte
 * @author James Leigh
 */
public class FederatedServiceResolverImpl extends AbstractFederatedServiceResolver
		implements FederatedServiceResolver, HttpClientDependent, SesameClientDependent
{

	public FederatedServiceResolverImpl() {
		super();
	}

	/** independent life cycle */
	private volatile SesameClient client;

	/** dependent life cycle */
	private volatile SesameClientImpl dependentClient;

	public SesameClient getSesameClient() {
		SesameClient result = client;
		if (result == null) {
			synchronized (this) {
				result = client;
				if (result == null) {
					result = client = dependentClient = new SesameClientImpl();
				}
			}
		}
		return result;
	}

	public void setSesameClient(SesameClient client) {
		synchronized (this) {
			this.client = client;
			// If they set a client, we need to check whether we need to
			// shutdown any existing dependentClient
			SesameClientImpl toCloseDependentClient = dependentClient;
			dependentClient = null;
			if (toCloseDependentClient != null) {
				toCloseDependentClient.shutDown();
			}
		}
	}

	public HttpClient getHttpClient() {
		return getSesameClient().getHttpClient();
	}

	public void setHttpClient(HttpClient httpClient) {
		SesameClientImpl toSetDependentClient = dependentClient;
		if (toSetDependentClient == null) {
			getSesameClient();
			toSetDependentClient = dependentClient;
		}
		// The strange lifecycle results in the possibility that the
		// dependentClient will be null due to a call to setSesameClient, so add
		// a null guard here for that possibility
		if (toSetDependentClient != null) {
			toSetDependentClient.setHttpClient(httpClient);
		}
	}

	@Override
	protected FederatedService createService(String serviceUrl)
		throws QueryEvaluationException
	{
		return new SPARQLFederatedService(serviceUrl, getSesameClient());
	}

	@Override
	public void shutDown() {
		super.shutDown();
		if (dependentClient != null) {
			dependentClient.shutDown();
			dependentClient = null;
		}
	}
}
