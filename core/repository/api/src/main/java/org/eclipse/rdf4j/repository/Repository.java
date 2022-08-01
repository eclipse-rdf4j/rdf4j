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
package org.eclipse.rdf4j.repository;

import java.io.File;

import org.eclipse.rdf4j.model.ValueFactory;

/**
 * An RDF4J repository that contains RDF data that can be queried and updated. Access to the repository can be acquired
 * by opening a connection to it. This connection can then be used to query and/or update the contents of the
 * repository. Depending on the implementation of the repository, it may or may not support multiple concurrent
 * connections.
 * <p>
 * Please note that a repository should be shut down before it is discarded/garbage collected. Forgetting the latter can
 * result in loss of data (depending on the Repository implementation)!
 * <p>
 * Repository implementations are thread-safe unless specifically documented otherwise.
 *
 * @author Arjohn Kampman
 */
public interface Repository {

	/**
	 * Set the directory where data and logging for this repository is stored.
	 *
	 * @param dataDir the directory where data for this repository is stored
	 */
	void setDataDir(File dataDir);

	/**
	 * Get the directory where data and logging for this repository is stored.
	 *
	 * @return the directory where data for this repository is stored.
	 */
	File getDataDir();

	/**
	 * Initializes this repository. A repository needs to be initialized before it can be used, however explicitly
	 * calling this method is not necessary: the repository will automatically initialize itself if an operation is
	 * executed on it that requires it to be initialized.
	 *
	 * @throws RepositoryException If the initialization failed.
	 * @since 2.5
	 */
	void init() throws RepositoryException;

	/**
	 * Indicates if the Repository has been initialized. Note that the initialization status may change if the
	 * Repository is shut down.
	 *
	 * @return true iff the repository has been initialized.
	 */
	boolean isInitialized();

	/**
	 * Shuts the repository down, releasing any resources that it keeps hold of. Once shut down, the repository can no
	 * longer be used until it is re-initialized.
	 */
	void shutDown() throws RepositoryException;

	/**
	 * Checks whether this repository is writable, i.e. if the data contained in this repository can be changed. The
	 * writability of the repository is determined by the writability of the Sail that this repository operates on.
	 */
	boolean isWritable() throws RepositoryException;

	/**
	 * Opens a connection to this repository that can be used for querying and updating the contents of the repository.
	 * Created connections need to be closed to make sure that any resources they keep hold of are released. The best
	 * way to do this is to use a try-with-resources block, as follows:
	 *
	 * <pre>
	 * try (RepositoryConnection conn = repository.getConnection()) {
	 * 	// perform operations on the connection
	 * }
	 * </pre>
	 *
	 * Note that {@link RepositoryConnection} is not guaranteed to be thread-safe! The recommended pattern for
	 * repository access in a multi-threaded application is to share the Repository object between threads, but have
	 * each thread create and use its own {@link RepositoryConnection}s.
	 *
	 * @return A connection that allows operations on this repository.
	 * @throws RepositoryException If something went wrong during the creation of the Connection.
	 */
	RepositoryConnection getConnection() throws RepositoryException;

	/**
	 * Gets a ValueFactory for this Repository.
	 *
	 * @return A repository-specific ValueFactory.
	 */
	ValueFactory getValueFactory();

}
