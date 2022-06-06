/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.server;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointBase;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.endpoint.EndpointType;
import org.eclipse.rdf4j.federated.endpoint.provider.NativeStoreProvideTest;
import org.eclipse.rdf4j.federated.endpoint.provider.RepositoryInformation;
import org.eclipse.rdf4j.federated.repository.ConfigurableSailRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

public class NativeStoreServer implements Server {

	private final List<Repository> repositories = new ArrayList<>();

	private final File dataDir;

	public NativeStoreServer(File dataDir) {
		super();
		this.dataDir = dataDir;
	}

	@Override
	public void initialize(int nRepositories) throws Exception {

		for (int i = 1; i <= nRepositories; i++) {
			NativeStore store = NativeStoreProvideTest.createNativeStore(new File(dataDir, "endpoint" + i));
			ConfigurableSailRepository repo = new ConfigurableSailRepository(
					store, true);
			repo.init();
			repositories.add(repo);
			repo.shutDown();
		}
	}

	@Override
	public void shutdown() throws Exception {

	}

	@Override
	public Endpoint loadEndpoint(int i) throws Exception {
		EndpointBase e = (EndpointBase) EndpointFactory.loadEndpoint("endpoint" + i, repositories.get(i - 1));
		e.setEndpointClassification(EndpointClassification.Local);

		Field repoInfoField = EndpointBase.class.getDeclaredField("repoInfo");
		repoInfoField.setAccessible(true);

		RepositoryInformation repoInfo = (RepositoryInformation) repoInfoField.get(e);
		repoInfo.setType(EndpointType.NativeStore);
		return e;
	}

	@Override
	public ConfigurableSailRepository getRepository(int i) {
		return (ConfigurableSailRepository) repositories.get(i - 1);
	}

}
