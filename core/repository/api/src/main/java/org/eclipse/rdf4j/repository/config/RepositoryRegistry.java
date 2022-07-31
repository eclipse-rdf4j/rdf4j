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
package org.eclipse.rdf4j.repository.config;

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

/**
 * A registry that keeps track of the available {@link RepositoryFactory}s.
 *
 * @author Arjohn Kampman
 */
public class RepositoryRegistry extends ServiceRegistry<String, RepositoryFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class RepositoryRegistryHolder {

		public static final RepositoryRegistry instance = new RepositoryRegistry();
	}

	/**
	 * Gets the default RepositoryRegistry.
	 *
	 * @return The default registry.
	 */
	public static RepositoryRegistry getInstance() {
		return RepositoryRegistryHolder.instance;
	}

	public RepositoryRegistry() {
		super(RepositoryFactory.class);
	}

	@Override
	protected String getKey(RepositoryFactory factory) {
		return factory.getRepositoryType();
	}
}
