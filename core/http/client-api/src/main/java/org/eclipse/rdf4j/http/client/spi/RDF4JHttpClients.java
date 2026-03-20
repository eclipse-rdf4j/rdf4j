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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for obtaining {@link RDF4JHttpClientFactory} instances and creating {@link RDF4JHttpClient} objects.
 * <p>
 * Factories are discovered via {@link ServiceLoader}. The system property {@code rdf4j.http.client.factory} can be used
 * to select a specific factory by name. If multiple factories are found and no preference is set, the {@code "apache5"}
 * factory is preferred.
 */
public final class RDF4JHttpClients {

	private static final Logger logger = LoggerFactory.getLogger(RDF4JHttpClients.class);

	/**
	 * System property for selecting a specific HTTP client factory by name.
	 */
	public static final String FACTORY_PROPERTY = "rdf4j.http.client.factory";

	private RDF4JHttpClients() {
	}

	/**
	 * Returns the default {@link RDF4JHttpClientFactory}.
	 * <p>
	 * The factory is chosen as follows:
	 * <ol>
	 * <li>If the system property {@code rdf4j.http.client.factory} is set, use the factory with that name.</li>
	 * <li>If a factory named {@code "apache5"} is available, prefer it.</li>
	 * <li>Otherwise, use the first factory found.</li>
	 * </ol>
	 *
	 * @return the default factory.
	 * @throws IllegalStateException if no factory is found on the classpath.
	 */
	public static RDF4JHttpClientFactory defaultFactory() {
		String preferred = System.getProperty(FACTORY_PROPERTY);
		if (preferred != null) {
			return factory(preferred);
		}
		List<RDF4JHttpClientFactory> all = allFactories();
		if (all.isEmpty()) {
			throw new IllegalStateException(
					"No RDF4JHttpClientFactory found on the classpath. Add rdf4j-http-client-jdk or rdf4j-http-client-apache5 as a dependency.");
		}
		// prefer "apache5" if available
		for (RDF4JHttpClientFactory f : all) {
			if ("apache5".equals(f.getName())) {
				logger.debug("Using Apache HttpComponents 5 HTTP client factory");
				return f;
			}
		}
		logger.debug("Using HTTP client factory: {}", all.get(0).getName());
		return all.get(0);
	}

	/**
	 * Returns the factory with the given name.
	 *
	 * @param name the factory name.
	 * @return the matching factory.
	 * @throws IllegalArgumentException if no factory with the given name is found.
	 */
	public static RDF4JHttpClientFactory factory(String name) {
		for (RDF4JHttpClientFactory f : ServiceLoader.load(RDF4JHttpClientFactory.class)) {
			if (name.equals(f.getName())) {
				return f;
			}
		}
		throw new IllegalArgumentException("No RDF4JHttpClientFactory named '" + name + "' found on the classpath.");
	}

	/**
	 * Returns all available {@link RDF4JHttpClientFactory} instances.
	 *
	 * @return a list of all factories, possibly empty.
	 */
	public static List<RDF4JHttpClientFactory> allFactories() {
		List<RDF4JHttpClientFactory> list = new ArrayList<>();
		for (RDF4JHttpClientFactory f : ServiceLoader.load(RDF4JHttpClientFactory.class)) {
			list.add(f);
		}
		return list;
	}

	/**
	 * Creates a new {@link RDF4JHttpClient} using the default factory and default configuration.
	 *
	 * @return a new client.
	 */
	public static RDF4JHttpClient newDefaultClient() {
		return defaultFactory().create();
	}

	/**
	 * Creates a new {@link RDF4JHttpClient} using the default factory with the given configuration.
	 *
	 * @param config the client configuration.
	 * @return a new client.
	 */
	public static RDF4JHttpClient newDefaultClient(HttpClientConfig config) {
		return defaultFactory().create(config);
	}

	/**
	 * Creates a new {@link RDF4JHttpClient} using the default factory with the given configuration and authentication.
	 *
	 * @param config     the client configuration.
	 * @param authConfig the authentication configuration.
	 * @return a new client.
	 */
	public static RDF4JHttpClient newDefaultClient(HttpClientConfig config, HttpAuthConfig authConfig) {
		return defaultFactory().create(config, authConfig);
	}
}
