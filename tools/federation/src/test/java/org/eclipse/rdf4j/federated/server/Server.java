/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.server;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.repository.ConfigurableSailRepository;

/**
 * Interface for the server:
 *
 * {@link SPARQLEmbeddedServer} and {@link NativeStoreServer}
 *
 * @author as
 *
 */
public interface Server {

	public void initialize(int nRepositories) throws Exception;

	public void shutdown() throws Exception;

	public Endpoint loadEndpoint(int i) throws Exception;

	/**
	 * Returns the actual {@link ConfigurableSailRepository} instance for the endpoint
	 *
	 * @param i the endpoint index starting with 1
	 * @return
	 */
	public ConfigurableSailRepository getRepository(int i);
}
