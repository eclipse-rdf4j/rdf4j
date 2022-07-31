/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EndpointManager is the singleton instance that manages available {@link Endpoint}s. Particular endpoints can be
 * looked up by their id and connection and all relevant information can be used.
 *
 * @author Andreas Schwarte
 *
 */
public class EndpointManager {

	protected static final Logger log = LoggerFactory.getLogger(EndpointManager.class);

	/*
	 * TODO we probably need to make this class thread safe! => synchronized access
	 */

	/**
	 * Initialize the singleton endpoint manager with the provided endpoints
	 *
	 * @param endpoints
	 */
	public static synchronized EndpointManager initialize(List<Endpoint> endpoints) {
		return new EndpointManager(endpoints);
	}

	// map enpoint ids and connections to the corresponding endpoint
	protected HashMap<String, Endpoint> endpoints = new HashMap<>();

	protected boolean inRepair = false;
	protected Long lastRepaired = -1L;

	/**
	 * Construct an EndpointManager without any endpoints
	 */
	private EndpointManager() {
		this(null);
	}

	/**
	 * Construct an EndpointManager with the provided endpoints
	 *
	 * @param endpoints
	 */
	private EndpointManager(List<Endpoint> endpoints) {
		init(endpoints);
	}

	/**
	 * Initialize the endpoint mapping with the provided endpoints
	 *
	 * @param _endpoints a list of (initialized) endpoints or null
	 */
	private void init(List<Endpoint> _endpoints) {
		if (_endpoints != null) {
			for (Endpoint e : _endpoints) {
				addEndpoint(e);
			}
		}
	}

	/**
	 * Add the (initialized) endpoint to this endpoint manager to be used by the {@link FederationManager}.
	 *
	 * @param e the endpoint
	 */
	public void addEndpoint(Endpoint e) {
		endpoints.put(e.getId(), e);
	}

	/**
	 * Remove the provided endpoint from this endpoint manager to be used by the {@link FederationManager}. In addition,
	 * this method unregisters the {@link FederatedService} from Sesame
	 *
	 * @param e the endpoint
	 *
	 * @throws NoSuchElementException if there is no mapping for some endpoint id
	 */
	protected void removeEndpoint(Endpoint e) throws NoSuchElementException {
		if (!endpoints.containsKey(e.getId())) {
			throw new NoSuchElementException("No endpoint avalaible for id " + e.getId());
		}
		endpoints.remove(e.getId());
	}

	/**
	 * @return a collection of available endpoints in this endpoint manager
	 */
	public Collection<Endpoint> getAvailableEndpoints() {
		return endpoints.values();
	}

	/**
	 * @param endpointID
	 * @return the endpoint corresponding to the provided id or null
	 */
	public Endpoint getEndpoint(String endpointID) {
		return endpoints.get(endpointID);
	}

	/**
	 * Return the Endpoint for the provided endpoint url, if it exists. Otherwise return null.
	 *
	 * @param endpointUrl
	 * @return the endpoint by its URL
	 */
	public Endpoint getEndpointByUrl(String endpointUrl) {
		for (Endpoint e : endpoints.values()) {
			if (e.getEndpoint().equals(endpointUrl)) {
				return e;
			}
		}
		return null;
	}

	public Endpoint getEndpointByName(String endpointName) {
		for (Endpoint e : endpoints.values()) {
			if (e.getName().equals(endpointName)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * @param endpointIDs
	 * @return a list of endpoints corresponding to the provided ids
	 *
	 * @throws NoSuchElementException if there is no mapping for some endpoint id
	 */
	public List<Endpoint> getEndpoints(Set<String> endpointIDs) throws NoSuchElementException {
		List<Endpoint> res = new ArrayList<>();
		for (String endpointID : endpointIDs) {
			Endpoint e = endpoints.get(endpointID);
			if (e == null) {
				throw new NoSuchElementException("No endpoint found for " + endpointID + ".");
			}
			res.add(e);
		}
		return res;
	}
}
