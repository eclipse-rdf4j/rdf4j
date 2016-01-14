/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.base;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Base class for {@link Repository} implementation, offering common functionality. 
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractRepository implements Repository {

	// "volatile" guarantees access ordering in JDK5+
	private volatile boolean initialized = false;

	private final Object initLock = new Object();

	public final void initialize()
		throws RepositoryException
	{
		if (!initialized) {
			synchronized (initLock) {
				if (!initialized) {
					initializeInternal();
					initialized = true;
				}
			}
		}
	}

	protected abstract void initializeInternal()
		throws RepositoryException;

	public final void shutDown()
		throws RepositoryException
	{
		synchronized (initLock) {
			shutDownInternal();
			initialized = false;
		}
	}

	public final boolean isInitialized() {
		return initialized;
	}

	protected abstract void shutDownInternal()
		throws RepositoryException;

}
