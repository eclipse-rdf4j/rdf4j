/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.base;

import java.io.File;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A {@link DelegatingRepository} implementation that, by default, forwards all
 * method calls to its delegate.
 * 
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class RepositoryWrapper implements DelegatingRepository {

	private volatile Repository delegate;

	/**
	 * Creates a new <tt>RepositoryWrapper</tt>.
	 */
	public RepositoryWrapper() {
	}

	/**
	 * Creates a new <tt>RepositoryWrapper</tt> and calls
	 * {@link #setDelegate(Repository)} with the supplied delegate repository.
	 */
	public RepositoryWrapper(Repository delegate) {
		setDelegate(delegate);
	}

	public void setDelegate(Repository delegate) {
		this.delegate = delegate;
	}

	public Repository getDelegate() {
		return delegate;
	}

	public void setDataDir(File dataDir) {
		getDelegate().setDataDir(dataDir);
	}

	public File getDataDir() {
		return getDelegate().getDataDir();
	}

	public void initialize()
		throws RepositoryException
	{
		getDelegate().initialize();
	}

	public void shutDown()
		throws RepositoryException
	{
		getDelegate().shutDown();
	}

	public boolean isWritable()
		throws RepositoryException
	{
		return getDelegate().isWritable();
	}

	public RepositoryConnection getConnection()
		throws RepositoryException
	{
		return getDelegate().getConnection();
	}

	public ValueFactory getValueFactory() {
		return getDelegate().getValueFactory();
	}

	public String toString() {
		return getDelegate().toString();
	}

	public boolean isInitialized() {
		return getDelegate().isInitialized();
	}
}
