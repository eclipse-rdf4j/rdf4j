/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.event;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Interceptor interface for connection modification. 
 * 
 * @author Herko ter Horst
 */
public interface RepositoryConnectionInterceptor {

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean close(RepositoryConnection conn);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean begin(RepositoryConnection conn);

	/**
	 * @deprecated since 2.7.0. Use {@link #begin(RepositoryConnection)} instead.
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @param autoCommit
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	@Deprecated
	public abstract boolean setAutoCommit(RepositoryConnection conn, boolean autoCommit);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean commit(RepositoryConnection conn);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean rollback(RepositoryConnection conn);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean clear(RepositoryConnection conn, Resource... contexts);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean setNamespace(RepositoryConnection conn, String prefix, String name);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean removeNamespace(RepositoryConnection conn, String prefix);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean clearNamespaces(RepositoryConnection conn);

	/**
	 * @param conn
	 *        the RepositoryConnection to perform the operation on.
	 * @return true if the interceptor has been denied access to the operation, false
	 *         otherwise.
	 */
	public abstract boolean execute(RepositoryConnection conn, QueryLanguage ql, String update,
			String baseURI, Update operation);
}
