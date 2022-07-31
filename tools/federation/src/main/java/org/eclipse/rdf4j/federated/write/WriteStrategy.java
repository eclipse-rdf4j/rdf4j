/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.write;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
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
@Experimental
public interface WriteStrategy extends AutoCloseable {
	/**
	 * Close this write strategy (e.g. close a shared {@link RepositoryException}).
	 *
	 * @throws RepositoryException
	 */
	@Override
	void close() throws RepositoryException;

	/**
	 * Assign {@link TransactionSetting}s to be used for the next transaction.
	 *
	 * @param transactionSettings one or more {@link TransactionSetting}s
	 * @throws RepositoryException
	 */
	void setTransactionSettings(TransactionSetting... transactionSettings) throws RepositoryException;

	/**
	 * Begin a transaction.
	 *
	 * @throws RepositoryException
	 */
	void begin() throws RepositoryException;

	/**
	 * Commit a transaction.
	 *
	 * @throws RepositoryException
	 */
	void commit() throws RepositoryException;

	/**
	 * Rollback a transaction.
	 *
	 * @throws RepositoryException
	 */
	void rollback() throws RepositoryException;

	/**
	 * Add a statement
	 *
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @throws RepositoryException
	 */
	void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException;

	/**
	 * Remove a statement
	 *
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @throws RepositoryException
	 */
	void removeStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws RepositoryException;

	void clear(Resource... contexts) throws RepositoryException;

	void clearNamespaces() throws RepositoryException;
}
