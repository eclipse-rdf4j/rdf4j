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
 * Registry of {@link LanguageHandler}s.
 *
 * @author Peter Ansell
 */
public class LanguageHandlerRegistry extends ServiceRegistry<String, LanguageHandler> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class LanguageHandlerRegistryHolder {

		public static final LanguageHandlerRegistry instance = new LanguageHandlerRegistry();
	}

	/**
	 * Gets the default LanguageHandlerRegistry.
	 *
	 * @return The default registry.
	 */
	public static LanguageHandlerRegistry getInstance() {
		return LanguageHandlerRegistryHolder.instance;
	}

	public LanguageHandlerRegistry() {
		super(LanguageHandler.class);
	}

	@Override
	protected String getKey(LanguageHandler handler) {
		return handler.getKey();
	}

}
