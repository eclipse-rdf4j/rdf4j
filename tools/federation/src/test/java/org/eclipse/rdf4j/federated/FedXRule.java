/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class FedXRule implements BeforeEachCallback, AfterEachCallback {

	private final File configurationPreset;

	protected FedXRepository repository;

	// settings that get applied in the actual config
	protected Map<String, String> configSettings = new HashMap<>();

	public FedXRule(File configurationPreset) {
		this.configurationPreset = configurationPreset;
	}

	public FedXRule() {
		this(null);
	}

	public FedXRule withMonitoring() {
		configSettings.put("enableMonitoring", "true");
		return this;
	}

	@Override
	public void beforeEach(ExtensionContext ctx) throws Exception {
		Config.initialize();
		for (Entry<String, String> config : configSettings.entrySet()) {
			Config.getConfig().set(config.getKey(), config.getValue());
		}
		List<Endpoint> endpoints;
		if (configurationPreset != null)
			endpoints = EndpointFactory.loadFederationMembers(configurationPreset);
		else
			endpoints = Collections.<Endpoint>emptyList();
		repository = FedXFactory.createFederation(endpoints);
		repository.init();
		getFederationContext().getManager().getCache().clear();
	}

	@Override
	public void afterEach(ExtensionContext ctx) {
		repository.shutDown();
	}

	public void addEndpoint(Endpoint e) {
		getFederationContext().getManager().addEndpoint(e);
	}

	public void removeEndpoint(Endpoint e) {
		getFederationContext().getManager().removeEndpoint(e, true);
	}

	public void enableDebug() {
		setConfig("debugQueryPlan", "true");
	}

	public void setConfig(String key, String value) {
		Config.getConfig().set(key, value);
	}

	public Repository getRepository() {
		return repository;
	}

	public FederationContext getFederationContext() {
		return repository.getFederationContext();
	}

}
