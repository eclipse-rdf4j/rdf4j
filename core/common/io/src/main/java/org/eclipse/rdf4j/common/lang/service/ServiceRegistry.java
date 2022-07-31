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
package org.eclipse.rdf4j.common.lang.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry that stores services by some key. Upon initialization, the registry searches for service description files
 * at <var>META-INF/services/&lt;service class name&gt;</var> and initializes itself accordingly.
 *
 * @see javax.imageio.spi.ServiceRegistry
 * @author Arjohn Kampman
 */
public abstract class ServiceRegistry<K, S> {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected Map<K, S> services = new ConcurrentHashMap<>(16, 0.75f, 1);

	protected ServiceRegistry(Class<S> serviceClass) {
		ServiceLoader<S> loader = java.util.ServiceLoader.load(serviceClass, serviceClass.getClassLoader());

		Iterator<S> services = loader.iterator();
		while (true) {
			try {
				if (services.hasNext()) {
					S service = services.next();

					Optional<S> oldService = add(service);

					if (oldService.isPresent()) {
						logger.warn("New service {} replaces existing service {}", service.getClass(),
								oldService.get().getClass());
					}

					logger.debug("Registered service class {}", service.getClass().getName());
				} else {
					break;
				}
			} catch (Error e) {
				logger.error("Failed to instantiate service", e);
			}
		}
	}

	/**
	 * Adds a service to the registry. Any service that is currently registered for the same key (as specified by
	 * {@link #getKey(Object)}) will be replaced with the new service.
	 *
	 * @param service The service that should be added to the registry.
	 * @return The previous service that was registered for the same key, or {@link Optional#empty()} if there was no
	 *         such service.
	 */
	public Optional<S> add(S service) {
		return Optional.ofNullable(services.put(getKey(service), service));
	}

	/**
	 * Removes a service from the registry.
	 *
	 * @param service The service be removed from the registry.
	 */
	public void remove(S service) {
		services.remove(getKey(service));
	}

	/**
	 * Gets the service for the specified key, if any.
	 *
	 * @param key The key identifying which service to get.
	 * @return The service for the specified key, or {@link Optional#empty()} if no such service is avaiable.
	 */
	public Optional<S> get(K key) {
		return Optional.ofNullable(services.get(key));
	}

	/**
	 * Checks whether a service for the specified key is available.
	 *
	 * @param key The key identifying which service to search for.
	 * @return <var>true</var> if a service for the specific key is available, <var>false</var> otherwise.
	 */
	public boolean has(K key) {
		return services.containsKey(key);
	}

	/**
	 * Gets all registered services.
	 *
	 * @return An unmodifiable collection containing all registered servivces.
	 */
	public Collection<S> getAll() {
		return Collections.unmodifiableCollection(services.values());
	}

	/**
	 * Gets the set of registered keys.
	 *
	 * @return An unmodifiable set containing all registered keys.
	 */
	public Set<K> getKeys() {
		return Collections.unmodifiableSet(services.keySet());
	}

	/**
	 * Gets the key for the specified service.
	 *
	 * @param service The service to get the key for.
	 * @return The key for the specified service.
	 */
	protected abstract K getKey(S service);
}
