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
package org.eclipse.rdf4j.query.resultio;

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;

/**
 * A registry that keeps track of the available {@link BooleanQueryResultWriterFactory}s.
 *
 * @author Arjohn Kampman
 */
public class BooleanQueryResultWriterRegistry
		extends FileFormatServiceRegistry<QueryResultFormat, BooleanQueryResultWriterFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class BooleanQueryResultWriterRegistryHolder {

		public static final BooleanQueryResultWriterRegistry instance = new BooleanQueryResultWriterRegistry();
	}

	/**
	 * Gets the default BooleanQueryResultWriterRegistry.
	 *
	 * @return The default registry.
	 */
	public static BooleanQueryResultWriterRegistry getInstance() {
		return BooleanQueryResultWriterRegistryHolder.instance;
	}

	public BooleanQueryResultWriterRegistry() {
		super(BooleanQueryResultWriterFactory.class);
	}

	@Override
	protected QueryResultFormat getKey(BooleanQueryResultWriterFactory factory) {
		return factory.getBooleanQueryResultFormat();
	}
}
