package com.fluidops.fedx;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.EndpointFactory;

public class FedXRule implements BeforeEachCallback, AfterEachCallback {

	
	private final File configurationPreset;

	protected Repository repository;

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
		if (configurationPreset!=null)
			endpoints = EndpointFactory.loadFederationMembers(configurationPreset);
		else
			endpoints = Collections.<Endpoint>emptyList();
		repository = FedXFactory.initializeFederation(endpoints);
		FederationManager.getInstance().getCache().clear();
	}
	
	@Override
	public void afterEach(ExtensionContext ctx) {
		repository.shutDown();
	}

	public void addEndpoint(Endpoint e) {
		FederationManager.getInstance().addEndpoint(e);
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
	
}
