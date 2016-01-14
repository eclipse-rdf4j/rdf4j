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
 * Listener interface for connection modification.
 * 
 * @author James Leigh
 */
public interface RepositoryConnectionListener {

	public abstract void close(RepositoryConnection conn);

	/**
	 * @deprecated since release 2.7.0. Use {@link #begin(RepositoryConnection)} instead.
	 *  
	 * @param conn
	 * @param autoCommit
	 */
	@Deprecated
	public abstract void setAutoCommit(RepositoryConnection conn, boolean autoCommit);

	public abstract void begin(RepositoryConnection conn);
	
	public abstract void commit(RepositoryConnection conn);

	public abstract void rollback(RepositoryConnection conn);

	public abstract void add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	public abstract void remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
			Resource... contexts);

	public abstract void clear(RepositoryConnection conn, Resource... contexts);

	public abstract void setNamespace(RepositoryConnection conn, String prefix, String name);

	public abstract void removeNamespace(RepositoryConnection conn, String prefix);

	public abstract void clearNamespaces(RepositoryConnection conn);

	public abstract void execute(RepositoryConnection conn, QueryLanguage ql, String update,
			String baseURI, Update operation);
}
