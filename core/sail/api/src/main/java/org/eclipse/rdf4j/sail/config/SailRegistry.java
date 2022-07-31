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
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

/**
 * A registry that keeps track of the available {@link SailFactory}s.
 *
 * @author Arjohn Kampman
 */
public class SailRegistry extends ServiceRegistry<String, SailFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class SailRegistryHolder {

		public static final SailRegistry instance = new SailRegistry();
	}

	/**
	 * Gets the default SailRegistry.
	 *
	 * @return The default registry.
	 */
	public static SailRegistry getInstance() {
		return SailRegistryHolder.instance;
	}

	public SailRegistry() {
		super(SailFactory.class);
	}

	@Override
	protected String getKey(SailFactory factory) {
		return factory.getSailType();
	}
}
