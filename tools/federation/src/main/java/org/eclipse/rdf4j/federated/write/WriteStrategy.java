/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.write;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Interface for the {@link WriteStrategy} that is used for writing data to the federation. The implementation can
 * decided upon how is data written to the underlying federation members (e.g. to a designated federation member)
 * 
 * <p>
 * <b>Note:</b> this is an experimental feature which is subject to change in a future version.
 * </p>
 * 
 * @author Andreas Schwarte
 * @see RepositoryWriteStrategy
 * @see ReadOnlyWriteStrategy
 */
public interface WriteStrategy {

	/**
	 * Initialize the write strategy (e.g. open a shared {@link RepositoryConnection}.
	 * 
	 * @throws RepositoryException
	 */
	public void initialize() throws RepositoryException;

	/**
	 * Returns true if this instance is initialized
	 * 
	 * @return flag indicating the initialization resources
	 */
	public boolean isInitialized();

	/**
	 * Close this write strategy (e.g. close a shared {@link RepositoryException}).
	 * 
	 * @throws RepositoryException
	 */
	public void close() throws RepositoryException;

	/**
	 * Begin a transaction.
	 * 
	 * @throws RepositoryException
	 */
	public void begin() throws RepositoryException;

	/**
	 * Commit a transaction.
	 * 
	 * @throws RepositoryException
	 */
	public void commit() throws RepositoryException;

	/**
	 * Rollback a transaction.
	 * 
	 * @throws RepositoryException
	 */
	public void rollback() throws RepositoryException;

	/**
	 * Add a statement
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @throws RepositoryException
	 */
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException;

	/**
	 * Remove a statement
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @throws RepositoryException
	 */
	public void removeStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException;
}
