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

/**
 *
 *
 * <H1>Rdf4j-Spring ResultCache</H1>
 *
 * Automatically configures a cache for Rdf4J query results via the {@link org.eclipse.rdf4j.spring.RDF4JConfig
 * Rdf4JConfig}.
 *
 * <p>
 * Enable via <code>rdf4j.spring.resultcache.enabled=true</code>.
 *
 * <p>
 * If enabled, the {@link org.eclipse.rdf4j.spring.RDF4JConfig Rdf4JConfig} wraps the
 * {@link org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory RepositoryConnectionFactory} in
 * a {@link org.eclipse.rdf4j.spring.resultcache.CachingRepositoryConnectionFactory CachingRepositoryConnectionFactory},
 * which wraps {@link org.eclipse.rdf4j.repository.RepositoryConnection RepositoryConnection}s in
 * {@link org.eclipse.rdf4j.spring.resultcache.CachingRepositoryConnection CachingRepositoryConnection}s. These return
 * {@link org.eclipse.rdf4j.spring.resultcache.ResultCachingGraphQuery ResultCachingGraphQuery} and
 * {@link org.eclipse.rdf4j.spring.resultcache.ResultCachingTupleQuery ResultCachingTupleQuery} wrappers when
 * instantiating queries. The <code>
 * ResultCaching(Tuple|Graph)Query</code> returns a <code>Reusable(Tuple|Graph)QueryResult</code>, which records the
 * results as they are read by the client code and keeps them for future use.
 *
 * <p>
 * There are two levels of caching: connection-level and global. The connection-level cache is cleared when the
 * connection is closed (or returned to the pool, if pooling is enabled). The global cache is cleared whenever data is
 * written to the repostitory <b>by the application</b>.
 *
 * <p>
 * <b>Note: global result caching is disabled by default.</b> The reason is that in the general case, we cannot be sure
 * that no other application writes to the repository. If you are <b>really</b> sure that your application is the only
 * one writing to the repository, or if the repository is read-only, you can enable the global result cache using <code>
 * rdf4j.spring.resultcache.assume-no-other-repository-clients=true</code>.
 *
 * <p>
 * For More information on configuration, see {@link org.eclipse.rdf4j.spring.resultcache.ResultCacheProperties
 * ResultCacheProperties}
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
package org.eclipse.rdf4j.spring.resultcache;
