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
import org.eclipse.rdf4j.repository.event.RepositoryConnectionInterceptor;

/**
 * @author Herko ter Horst
 */
public class RepositoryConnectionInterceptorAdapter implements RepositoryConnectionInterceptor {

	@Override
	public boolean add(RepositoryConnection conn, Resource subject, IRI predicate, Value object, Resource... contexts) {
		return false;
	}

	@Override
	public boolean begin(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean clear(RepositoryConnection conn, Resource... contexts) {
		return false;
	}

	@Override
	public boolean clearNamespaces(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean close(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean commit(RepositoryConnection conn) {
		return false;
	}

	@Override
	public boolean remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts) {
		return false;
	}

	@Override
	public boolean removeNamespace(RepositoryConnection conn, String prefix) {
		return false;
	}

	@Override
	public boolean rollback(RepositoryConnection conn) {
		return false;
	}

	@Deprecated
	@Override
	public boolean setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
		return false;
	}

	@Override
	public boolean setNamespace(RepositoryConnection conn, String prefix, String name) {
		return false;
	}

	@Override
	public boolean execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI,
			Update operation) {
		return false;
	}

}
