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

package org.eclipse.rdf4j.spring.pool;

import java.lang.invoke.MethodHandles;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.tx.exception.RepositoryConnectionPoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Obtains connections from the delegate factory and manages them in the object pool.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
class PooledConnectionObjectFactory extends BasePooledObjectFactory<RepositoryConnection> {
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private ObjectPool<RepositoryConnection> pool;
	private final RepositoryConnectionFactory delegate;

	public PooledConnectionObjectFactory(RepositoryConnectionFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public RepositoryConnection create() throws Exception {
		logger.debug(
				"Creating pooled connection - obtaining underlying connection from delegate factory");
		try {
			return delegate.getConnection();
		} catch (Exception e) {
			throw new RepositoryConnectionPoolException(
					"Error obtaining RepositoryConnection for pool", e);
		}
	}

	@Override
	public PooledObject<RepositoryConnection> wrap(RepositoryConnection con) {
		return new DefaultPooledObject<>(new PooledRepositoryConnection(con, pool));
	}

	public void setPool(ObjectPool<RepositoryConnection> pool) {
		this.pool = pool;
	}

	@Override
	public void destroyObject(PooledObject<RepositoryConnection> pooledObject) throws Exception {
		logger.debug("destroying pooled connection - closing underlying connection");
		try {
			pooledObject.getObject().close();
			logger.debug("successfully closed underlying connection");
		} catch (Exception e) {
			throw new RepositoryConnectionPoolException("Error closing RepositoryConnection", e);
		}
	}

	@Override
	public boolean validateObject(PooledObject<RepositoryConnection> p) {
		RepositoryConnection con = p.getObject();
		try {
			con.prepareTupleQuery("select (1 as ?one) where {}").evaluate().close();
		} catch (Exception e) {
			logger.info("Test query on pooled connection caused exception - it will be destroyed");
			return false;
		}
		logger.debug("pooled connection still works");
		return true;
	}
}
