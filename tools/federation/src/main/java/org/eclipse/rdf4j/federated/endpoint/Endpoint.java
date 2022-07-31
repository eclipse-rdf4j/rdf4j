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
package org.eclipse.rdf4j.federated.endpoint;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.iterator.CloseDependentConnectionIteration;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 *
 * Structure to maintain endpoint information, e.g. {@link Repository} type, location.
 *
 * <p>
 * The {@link Repository} to use can be obtained by calling {@link #getRepository()}
 * </p>
 *
 * <p>
 * A {@link RepositoryConnection} for interacting with the store can be obtained using {@link #getConnection()}. Note
 * that typically the {@link TripleSource} of the endpoint should be used.
 * </p>
 *
 *
 * @author Andreas Schwarte
 * @see ManagedRepositoryEndpoint
 * @see RepositoryEndpoint
 * @see ResolvableEndpoint
 *
 */
public interface Endpoint {

	/**
	 *
	 * @return the initialized {@link Repository}
	 */
	Repository getRepository();

	/**
	 * Return a {@link RepositoryConnection} for the {@link Repository} represented by this endpoint.
	 * <p>
	 * Callers of this method need to ensure to close the connection after use.
	 * </p>
	 *
	 * <p>
	 * Typical pattern:
	 * </p>
	 *
	 * <pre>
	 * try (RepositoryConnection conn = endpoint.getConnection()) {
	 * 	// do something with the connection
	 * }
	 * </pre>
	 *
	 * <p>
	 * If the {@link RepositoryConnection} needs to stay open outside the scope of a method (e.g. for streaming
	 * results), consider using {@link CloseDependentConnectionIteration}.
	 * </p>
	 *
	 * @return the repository connection
	 *
	 * @throws RepositoryException if the repository is not initialized
	 */
	RepositoryConnection getConnection();

	/**
	 *
	 * @return the {@link TripleSource}
	 */
	TripleSource getTripleSource();

	/**
	 *
	 * @return the {@link EndpointClassification}
	 */
	EndpointClassification getEndpointClassification();

	/**
	 *
	 * @return whether this endpoint is writable
	 */
	boolean isWritable();

	/**
	 *
	 * @return the identifier of the federation member
	 */
	String getId();

	/**
	 *
	 * @return the name of the federation member
	 */
	String getName();

	/**
	 * Get the endpoint location, e.g. for SPARQL endpoints the url
	 *
	 * @return the endpoint location
	 */
	String getEndpoint();

	/**
	 * Returns the size of the given repository, i.e. the number of triples.
	 *
	 * @return the size of the endpoint
	 * @throws RepositoryException
	 */
	long size() throws RepositoryException;

	/**
	 * Initialize this {@link Endpoint}
	 *
	 * @param federationContext
	 * @throws RepositoryException
	 */
	void init(FederationContext federationContext) throws RepositoryException;

	/**
	 * Shutdown this {@link Endpoint}
	 *
	 * @throws RepositoryException
	 */
	void shutDown() throws RepositoryException;

	/**
	 *
	 * @return whether this Endpoint is initialized
	 */
	boolean isInitialized();

	/**
	 * Additional endpoint specific configuration.
	 *
	 * @return the endpointConfiguration
	 */
	EndpointConfiguration getEndpointConfiguration();
}
