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
 * Listener interface for connection modification.
 *
 * @author James Leigh
 */
public interface RepositoryConnectionListener {

	void close(RepositoryConnection conn);

	/**
	 * @deprecated since 2.0. Use {@link #begin(RepositoryConnection)} instead.
	 * @param conn
	 * @param autoCommit
	 */
	@Deprecated
	void setAutoCommit(RepositoryConnection conn, boolean autoCommit);

	void begin(RepositoryConnection conn);

	void commit(RepositoryConnection conn);

	void rollback(RepositoryConnection conn);

	void add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	void remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	void clear(RepositoryConnection conn, Resource... contexts);

	void setNamespace(RepositoryConnection conn, String prefix, String name);

	void removeNamespace(RepositoryConnection conn, String prefix);

	void clearNamespaces(RepositoryConnection conn);

	void execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI,
			Update operation);
}
