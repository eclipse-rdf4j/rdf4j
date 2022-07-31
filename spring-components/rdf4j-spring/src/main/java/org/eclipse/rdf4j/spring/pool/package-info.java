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
 * <H1>Rdf4j-Spring Pool</H1>
 *
 * Provides pooling of {@link org.eclipse.rdf4j.repository.RepositoryConnection RepositoryConnection}s.
 *
 * <p>
 * Enable via <code>rdf4j.spring.pool.enabled=true</code>.
 *
 * <p>
 * If enabled, the {@link org.eclipse.rdf4j.spring.RDF4JConfig Rdf4JConfig} will wrap its
 * {@link org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory RepositoryConnectionFactory} in
 * a {@link org.eclipse.rdf4j.spring.pool.PooledRepositoryConnectionFactory PooledRepositoryConnectionFactory}.
 *
 * <p>
 * For more information on configuration of the pool, see {@link org.eclipse.rdf4j.spring.pool.PoolProperties
 * PoolProperties}.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
package org.eclipse.rdf4j.spring.pool;
