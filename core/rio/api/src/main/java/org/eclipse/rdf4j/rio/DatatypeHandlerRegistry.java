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
package org.eclipse.rdf4j.rio;

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

/**
 * Registry of {@link DatatypeHandler}s.
 *
 * @author Peter Ansell
 */
public class DatatypeHandlerRegistry extends ServiceRegistry<String, DatatypeHandler> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class DatatypeHandlerRegistryHolder {

		public static final DatatypeHandlerRegistry instance = new DatatypeHandlerRegistry();
	}

	/**
	 * Gets the default DatatypeHandlerRegistry.
	 *
	 * @return The default registry.
	 */
	public static DatatypeHandlerRegistry getInstance() {
		return DatatypeHandlerRegistryHolder.instance;
	}

	public DatatypeHandlerRegistry() {
		super(DatatypeHandler.class);
	}

	@Override
	protected String getKey(DatatypeHandler handler) {
		return handler.getKey();
	}

}
