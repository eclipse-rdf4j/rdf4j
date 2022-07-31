/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.support.connectionfactory.DelegatingRepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class CachingRepositoryConnectionFactory extends DelegatingRepositoryConnectionFactory {
	public CachingRepositoryConnectionFactory(
			RepositoryConnectionFactory delegate, ResultCacheProperties properties) {
		super(delegate);
		this.properties = properties;
		this.globalGraphQueryResultCache = new LRUResultCache<>(properties);
		this.globalTupleQueryResultCache = new LRUResultCache<>(properties);
	}

	private final LRUResultCache<ReusableTupleQueryResult> globalTupleQueryResultCache;
	private final LRUResultCache<ReusableGraphQueryResult> globalGraphQueryResultCache;

	private final ResultCacheProperties properties;

	@Override
	public RepositoryConnection getConnection() {
		return RepositoryConnectionWrappingUtils.wrapOnce(
				getDelegate().getConnection(),
				con -> new CachingRepositoryConnection(
						con,
						globalTupleQueryResultCache,
						globalGraphQueryResultCache,
						properties),
				CachingRepositoryConnection.class);
	}
}
