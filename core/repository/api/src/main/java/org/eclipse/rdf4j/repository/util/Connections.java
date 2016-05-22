/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Convenience functions for use with {@link RepositoryConnection}s.
 *
 * @author Jeen Broekstra
 */
public class Connections {

	/**
	 * Retrieve a single {@link Statement} matching with the supplied subject, predicate, object and
	 * context(s) from the given {@link RepositoryConnection}. If more than one Statement matches, any one
	 * Statement is selected and returned.
	 * 
	 * @param conn
	 *        the {@link RepositoryConnection} from which to retrieve the statement.
	 * @param subject
	 *        the subject to which the statement should match. May be {@code null}.
	 * @param predicate
	 *        the predicate to which the statement should match. May be {@code null}.
	 * @param object
	 *        the object to which the statement should match. May be {@code null} .
	 * @param contexts
	 *        the context(s) from which to read the Statement. This argument is an optional vararg and can be
	 *        left out.
	 * @return a {@link Statement}. If no matching Statement was found, null is returned.
	 * @throws RepositoryException
	 */
	public static Statement getStatement(RepositoryConnection conn, Resource subject, IRI predicate,
			Value object, Resource... contexts)
		throws RepositoryException
	{
		try (RepositoryResult<Statement> stmts = conn.getStatements(subject, predicate, object, contexts)) {
			Statement st = stmts.hasNext() ? stmts.next() : null;
			return st;
		}
	}

}
