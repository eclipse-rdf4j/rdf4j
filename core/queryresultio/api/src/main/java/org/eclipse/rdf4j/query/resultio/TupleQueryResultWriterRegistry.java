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
 * A registry that keeps track of the available {@link TupleQueryResultWriterFactory}s.
 *
 * @author Arjohn Kampman
 */
public class TupleQueryResultWriterRegistry
		extends FileFormatServiceRegistry<QueryResultFormat, TupleQueryResultWriterFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class TupleQueryResultWriterRegistryHolder {

		public static final TupleQueryResultWriterRegistry instance = new TupleQueryResultWriterRegistry();
	}

	/**
	 * Gets the default TupleQueryResultWriterRegistry.
	 *
	 * @return The default registry.
	 */
	public static TupleQueryResultWriterRegistry getInstance() {
		return TupleQueryResultWriterRegistryHolder.instance;
	}

	public TupleQueryResultWriterRegistry() {
		super(TupleQueryResultWriterFactory.class);
	}

	@Override
	protected QueryResultFormat getKey(TupleQueryResultWriterFactory factory) {
		return factory.getTupleQueryResultFormat();
	}
}
