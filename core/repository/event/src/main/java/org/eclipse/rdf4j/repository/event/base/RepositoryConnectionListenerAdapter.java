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
package org.eclipse.rdf4j.repository.event.base;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionListener;

/**
 * @author Herko ter Horst
 */
public class RepositoryConnectionListenerAdapter implements RepositoryConnectionListener {

	@Override
	public void close(RepositoryConnection conn) {
	}

	@Deprecated
	@Override
	public void setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
	}

	@Override
	public void begin(RepositoryConnection conn) {
	}

	@Override
	public void commit(RepositoryConnection conn) {
	}

	@Override
	public void rollback(RepositoryConnection conn) {
	}

	@Override
	public void add(RepositoryConnection conn, Resource subject, IRI predicate, Value object, Resource... contexts) {
	}

	@Override
	public void remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object, Resource... contexts) {
	}

	@Override
	public void clear(RepositoryConnection conn, Resource... contexts) {
	}

	@Override
	public void setNamespace(RepositoryConnection conn, String prefix, String name) {
	}

	@Override
	public void removeNamespace(RepositoryConnection conn, String prefix) {
	}

	@Override
	public void clearNamespaces(RepositoryConnection conn) {
	}

	@Override
	public void execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI, Update operation) {
	}
}
