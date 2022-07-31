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
package org.eclipse.rdf4j.repository.base;

import java.io.File;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A {@link DelegatingRepository} implementation that, by default, forwards all method calls to its delegate.
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class RepositoryWrapper implements DelegatingRepository {

	private volatile Repository delegate;

	/**
	 * Creates a new <var>RepositoryWrapper</var>.
	 */
	public RepositoryWrapper() {
	}

	/**
	 * Creates a new <var>RepositoryWrapper</var> and calls {@link #setDelegate(Repository)} with the supplied delegate
	 * repository.
	 */
	public RepositoryWrapper(Repository delegate) {
		setDelegate(delegate);
	}

	@Override
	public void setDelegate(Repository delegate) {
		this.delegate = delegate;
	}

	@Override
	public Repository getDelegate() {
		return delegate;
	}

	@Override
	public void setDataDir(File dataDir) {
		getDelegate().setDataDir(dataDir);
	}

	@Override
	public File getDataDir() {
		return getDelegate().getDataDir();
	}

	@Override
	public void shutDown() throws RepositoryException {
		getDelegate().shutDown();
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return getDelegate().isWritable();
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return getDelegate().getConnection();
	}

	@Override
	public ValueFactory getValueFactory() {
		return getDelegate().getValueFactory();
	}

	@Override
	public String toString() {
		return getDelegate().toString();
	}

	@Override
	public boolean isInitialized() {
		return getDelegate().isInitialized();
	}

	@Override
	public void init() throws RepositoryException {
		getDelegate().init();
	}
}
