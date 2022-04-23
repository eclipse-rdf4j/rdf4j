/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.pool;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.apache.commons.pool2.ObjectPool;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class PooledRepositoryConnection extends RepositoryConnectionWrapper {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final RepositoryConnection delegate;

	private final ObjectPool<RepositoryConnection> pool;

	public PooledRepositoryConnection(
			RepositoryConnection delegate, ObjectPool<RepositoryConnection> pool) {
		super(delegate.getRepository(), delegate);
		Objects.requireNonNull(delegate);
		Objects.requireNonNull(pool);
		this.delegate = delegate;
		this.pool = pool;
	}

	@Override
	public void close() throws RepositoryException {
		logger.debug("Close called on pooled RepositoryConnection, returning it to pool");
		try {
			pool.returnObject(this);
		} catch (Exception e) {
			throw new RepositoryException("Error returning connection to pool", e);
		}
	}
}
