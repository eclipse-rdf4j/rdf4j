/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

	public void close(RepositoryConnection conn) {
	}

	@Deprecated
	public void setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
	}

	public void begin(RepositoryConnection conn) {
	}

	public void commit(RepositoryConnection conn) {
	}

	public void rollback(RepositoryConnection conn) {
	}

	public void add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts)
	{
	}

	public void remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts)
	{
	}

	public void clear(RepositoryConnection conn, Resource... contexts) {
	}

	public void setNamespace(RepositoryConnection conn, String prefix, String name) {
	}

	public void removeNamespace(RepositoryConnection conn, String prefix) {
	}

	public void clearNamespaces(RepositoryConnection conn) {
	}

	public void execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI,
			Update operation)
	{
	}
}
