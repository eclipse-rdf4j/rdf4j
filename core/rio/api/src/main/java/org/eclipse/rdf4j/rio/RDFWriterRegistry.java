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
 * A registry that keeps track of the available {@link RDFWriterFactory}s.
 *
 * @author Arjohn Kampman
 */
public class RDFWriterRegistry extends FileFormatServiceRegistry<RDFFormat, RDFWriterFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class RDFWriterRegistryHolder {

		public static final RDFWriterRegistry instance = new RDFWriterRegistry();
	}

	/**
	 * Gets the default RDFWriterRegistry.
	 *
	 * @return The default registry.
	 */
	public static RDFWriterRegistry getInstance() {
		return RDFWriterRegistryHolder.instance;
	}

	public RDFWriterRegistry() {
		super(RDFWriterFactory.class);
	}

	@Override
	protected RDFFormat getKey(RDFWriterFactory factory) {
		return factory.getRDFFormat();
	}
}
