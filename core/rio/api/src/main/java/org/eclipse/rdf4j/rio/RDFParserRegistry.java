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

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;

/**
 * A registry that keeps track of the available {@link RDFParserFactory}s.
 *
 * @author Arjohn Kampman
 */
public class RDFParserRegistry extends FileFormatServiceRegistry<RDFFormat, RDFParserFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class RDFParserRegistryHolder {

		public static final RDFParserRegistry instance = new RDFParserRegistry();
	}

	/**
	 * Gets the default RDFParserRegistry.
	 *
	 * @return The default registry.
	 */
	public static RDFParserRegistry getInstance() {
		return RDFParserRegistryHolder.instance;
	}

	public RDFParserRegistry() {
		super(RDFParserFactory.class);
	}

	@Override
	protected RDFFormat getKey(RDFParserFactory factory) {
		return factory.getRDFFormat();
	}
}
