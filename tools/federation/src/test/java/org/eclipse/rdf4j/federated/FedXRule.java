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

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

public class FedXRule implements BeforeEachCallback, AfterEachCallback {

	protected FedXRepository repository;

	// settings that get applied in the actual config
	protected List<Consumer<FedXConfig>> configurations = Lists.newArrayList();

	public FedXRule() {
	}

	public FedXRule withConfiguration(Consumer<FedXConfig> configurator) {
		configurations.add(configurator);
		return this;
	}

	@Override
	public void beforeEach(ExtensionContext ctx) throws Exception {
		FedXConfig fedxConfig = new FedXConfig();
		for (Consumer<FedXConfig> configConsumer : configurations) {
			configConsumer.accept(fedxConfig);
		}
		List<Endpoint> endpoints = Collections.<Endpoint>emptyList();
		repository = FedXFactory.newFederation().withMembers(endpoints).withConfig(fedxConfig).create();
		repository.init();
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
		setConfig(fedxConfig -> fedxConfig.withDebugQueryPlan(true));
	}

	public void setConfig(Consumer<FedXConfig> configurator) {
		configurator.accept(repository.getFederationContext().getConfig());
	}

	public FedXRepository getRepository() {
		return repository;
	}

	public FederationContext getFederationContext() {
		return repository.getFederationContext();
	}

}
