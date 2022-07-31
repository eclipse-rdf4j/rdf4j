/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Base class for {@link FederatedServiceResolver} which takes care for lifecycle management of produced
 * {@link FederatedService}s.
 * <p>
 * Specific implementation can implement {@link #createService(String)}.
 *
 * @author Andreas Schwarte
 */
public abstract class AbstractFederatedServiceResolver implements FederatedServiceResolver {

	/**
	 * Map service URL to the corresponding initialized {@link FederatedService}
	 */
	protected Map<String, FederatedService> endpointToService = new HashMap<>();

	/**
	 * Register the specified service to evaluate SERVICE expressions for the given url.
	 *
	 * @param serviceUrl
	 * @param service
	 */
	public void registerService(String serviceUrl, FederatedService service) {
		synchronized (endpointToService) {
			endpointToService.put(serviceUrl, service);
		}
	}

	/**
	 * Unregister a service registered to serviceURl
	 *
	 * @param serviceUrl
	 */
	public void unregisterService(String serviceUrl) {
		FederatedService service;
		synchronized (endpointToService) {
			service = endpointToService.remove(serviceUrl);
		}
		if (service != null && service.isInitialized()) {
			try {
				service.shutdown();
			} catch (QueryEvaluationException e) {
				// TODO issue a warning, otherwise ignore
			}
		}
	}

	/**
	 * Retrieve the {@link FederatedService} registered for serviceUrl. If there is no service registered for
	 * serviceUrl, a new {@link FederatedService} is created and registered.
	 *
	 * @param serviceUrl locator for the federation service
	 * @return the {@link FederatedService}, created fresh if necessary
	 * @throws QueryEvaluationException
	 */
	@Override
	public FederatedService getService(String serviceUrl) throws QueryEvaluationException {
		FederatedService service;
		synchronized (endpointToService) {
			service = endpointToService.get(serviceUrl);
			if (service == null) {
				service = createService(serviceUrl);
				endpointToService.put(serviceUrl, service);
			}
		}
		if (!service.isInitialized()) {
			service.initialize();
		}
		return service;
	}

	/**
	 * Verify if a registered {@link FederatedService} exists for the given serviceUrul.
	 *
	 * @param serviceUrl locator for the federation service.
	 * @return {@code true} iff the FederatedService has been registered, {@code false} otherwise.
	 */
	public boolean hasService(String serviceUrl) {
		synchronized (endpointToService) {
			return endpointToService.containsKey(serviceUrl);
		}
	}

	/**
	 * Create a new {@link FederatedService} for the given serviceUrl. This method is invoked, if no
	 * {@link FederatedService} has been created yet for the serviceUrl.
	 *
	 * @param serviceUrl the service IRI
	 * @return a non-null {@link FederatedService}
	 * @throws QueryEvaluationException
	 */
	protected abstract FederatedService createService(String serviceUrl) throws QueryEvaluationException;

	public void unregisterAll() {
		synchronized (endpointToService) {
			for (FederatedService service : endpointToService.values()) {
				try {
					service.shutdown();
				} catch (QueryEvaluationException e) {
					// TODO issue a warning, otherwise ignore
				}
			}
			endpointToService.clear();
		}
	}

	public void shutDown() {
		unregisterAll();
	}
}
