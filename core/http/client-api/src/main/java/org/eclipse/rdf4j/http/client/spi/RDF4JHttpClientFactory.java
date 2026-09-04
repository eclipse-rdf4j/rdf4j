/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.client.spi;

/**
 * SPI for creating {@link RDF4JHttpClient} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 *
 * @see RDF4JHttpClients
 */
public interface RDF4JHttpClientFactory {

	/**
	 * @return a short identifier for this factory, e.g. {@code "jdk"} or {@code "apache5"}.
	 */
	String getName();

	/**
	 * Creates a new {@link RDF4JHttpClient} with default configuration.
	 *
	 * @return a new client.
	 */
	RDF4JHttpClient create();

	/**
	 * Creates a new {@link RDF4JHttpClient} with the given configuration.
	 *
	 * @param config the client configuration.
	 * @return a new client.
	 */
	RDF4JHttpClient create(RDF4JHttpClientConfig config);

}
