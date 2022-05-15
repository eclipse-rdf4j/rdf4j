/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.pool;

import java.lang.invoke.MethodHandles;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.tx.exception.RepositoryConnectionPoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

/**
 * Uses the delegate factory to actually obtain connections and provides these connections, managing an internal pool.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class PooledRepositoryConnectionFactory
		implements DisposableBean, RepositoryConnectionFactory {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ObjectPool<RepositoryConnection> pool;

	public PooledRepositoryConnectionFactory(
			RepositoryConnectionFactory delegateFactory,
			GenericObjectPoolConfig<RepositoryConnection> config) {

		PooledConnectionObjectFactory factory = new PooledConnectionObjectFactory(delegateFactory);
		if (config == null) {
			this.pool = new GenericObjectPool<>(factory);
		} else {
			this.pool = new GenericObjectPool<>(factory, config);
		}
		factory.setPool(pool);
	}

	public PooledRepositoryConnectionFactory(RepositoryConnectionFactory delegateFactory) {
		this(delegateFactory, null);
	}

	@Override
	public void destroy() throws Exception {
		logger.info("shutting down RepositoryConnection pool...");
		pool.close();
		logger.info("\tdone");
	}

	@Override
	public RepositoryConnection getConnection() {
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			throw new RepositoryConnectionPoolException(
					"Cannot obtain RepositoryConnection from pool", e);
		}
	}
}
