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
package org.eclipse.rdf4j.query.parser;

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;
import org.eclipse.rdf4j.query.QueryLanguage;

/**
 * A registry that keeps track of the available {@link QueryParserFactory}s.
 *
 * @author Arjohn Kampman
 */
public class QueryParserRegistry extends ServiceRegistry<QueryLanguage, QueryParserFactory> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class QueryParserRegistryHolder {

		public static final QueryParserRegistry instance = new QueryParserRegistry();
	}

	/**
	 * Gets the default QueryParserRegistry.
	 *
	 * @return The default registry.
	 */
	public static QueryParserRegistry getInstance() {
		return QueryParserRegistryHolder.instance;
	}

	public QueryParserRegistry() {
		super(QueryParserFactory.class);
	}

	@Override
	protected QueryLanguage getKey(QueryParserFactory factory) {
		return factory.getQueryLanguage();
	}
}
