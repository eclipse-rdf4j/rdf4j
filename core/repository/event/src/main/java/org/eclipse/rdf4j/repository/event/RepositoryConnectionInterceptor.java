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
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#close()} operation on.
	 * @return true if the interceptor has been denied access to the close operation, false otherwise.
	 */
	boolean close(RepositoryConnection conn);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#begin()} or
	 *             {@link RepositoryConnection#begin(org.eclipse.rdf4j.IsolationLevel)} operation on.
	 * @return true if the interceptor has been denied access to the begin operation, false otherwise.
	 */
	boolean begin(RepositoryConnection conn);

	/**
	 * @deprecated since 2.0. Use {@link #begin(RepositoryConnection)} instead.
	 * @param conn       the RepositoryConnection to perform the
	 *                   {@link RepositoryConnectionInterceptor#setAutoCommit(RepositoryConnection, boolean) }operation
	 *                   on.
	 * @param autoCommit
	 * @return true if the interceptor has been denied access to the setAutoCommit operation, false otherwise.
	 */
	@Deprecated
	boolean setAutoCommit(RepositoryConnection conn, boolean autoCommit);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#commit()} operation on.
	 * @return true if the interceptor has been denied access to the commit operation, false otherwise.
	 */
	boolean commit(RepositoryConnection conn);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#rollback()} operation on.
	 * @return true if the interceptor has been denied access to the rollback operation, false otherwise.
	 */
	boolean rollback(RepositoryConnection conn);

	/**
	 * @param conn the RepositoryConnection to perform the
	 *             {@link RepositoryConnection#add(Resource, IRI, Value, Resource...)} operation on.
	 * @return true if the interceptor has been denied access to the add operation, false otherwise.
	 */
	boolean add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	/**
	 * @param conn the RepositoryConnection to perform the
	 *             {@link RepositoryConnection#remove(Resource, IRI, Value, Resource...)} operation on.
	 * @return true if the interceptor has been denied access to the remove operation, false otherwise.
	 */
	boolean remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#clear(Resource...)} operation on.
	 * @return true if the interceptor has been denied access to the clear operation, false otherwise.
	 */
	boolean clear(RepositoryConnection conn, Resource... contexts);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#setNamespace(String, String)}
	 *             operation on.
	 * @return true if the interceptor has been denied access to the setNamespace operation, false otherwise.
	 */
	boolean setNamespace(RepositoryConnection conn, String prefix, String name);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#removeNamespace(String)}
	 *             operation on.
	 * @return true if the interceptor has been denied access to the removeNamespace operation, false otherwise.
	 */
	boolean removeNamespace(RepositoryConnection conn, String prefix);

	/**
	 * @param conn the RepositoryConnection to perform the {@link RepositoryConnection#clearNamespaces()} operation on.
	 * @return true if the interceptor has been denied access to the clearNamespaces operation, false otherwise.
	 */
	boolean clearNamespaces(RepositoryConnection conn);

	/**
	 * @param conn the RepositoryConnection to perform the query execution operations on.
	 * @return true if the interceptor has been denied access to the query execution operations, false otherwise.
	 */
	boolean execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI,
			Update operation);
}
