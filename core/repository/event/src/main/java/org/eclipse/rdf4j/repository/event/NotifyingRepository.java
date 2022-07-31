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
package org.eclipse.rdf4j.repository.event;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * @author Herko ter Horst
 */
public interface NotifyingRepository extends Repository {

	/**
	 * Registers a <var>RepositoryListener</var> that will receive notifications of operations that are performed on
	 * this repository.
	 */
	void addRepositoryListener(RepositoryListener listener);

	/**
	 * Removes a registered <var>RepositoryListener</var> from this repository.
	 */
	void removeRepositoryListener(RepositoryListener listener);

	/**
	 * Registers a <var>RepositoryConnectionListener</var> that will receive notifications of operations that are
	 * performed on any< connections that are created by this repository.
	 */
	void addRepositoryConnectionListener(RepositoryConnectionListener listener);

	/**
	 * Removes a registered <var>RepositoryConnectionListener</var> from this repository.
	 */
	void removeRepositoryConnectionListener(RepositoryConnectionListener listener);

	/**
	 * Opens a connection to this repository that can be used for querying and updating the contents of the repository.
	 * Created connections need to be closed to make sure that any resources they keep hold of are released. The best
	 * way to do this is to use a try-finally-block as follows:
	 *
	 * <pre>
	 * Connection con = repository.getConnection();
	 * try {
	 * 	// perform operations on the connection
	 * } finally {
	 * 	con.close();
	 * }
	 * </pre>
	 *
	 * Note that {@link RepositoryConnection} is not guaranteed to be thread-safe! The recommended pattern for
	 * repository access in a multithreaded application is to share the Repository object between threads, but have each
	 * thread create and use its own {@link RepositoryConnection}s.
	 *
	 * @return A connection that allows operations on this repository.
	 * @throws RepositoryException If something went wrong during the creation of the Connection.
	 */
	@Override
	NotifyingRepositoryConnection getConnection() throws RepositoryException;

}
