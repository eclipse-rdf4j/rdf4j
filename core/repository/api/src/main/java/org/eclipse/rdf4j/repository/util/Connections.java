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
package org.eclipse.rdf4j.repository.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.GetStatementOptional;
import org.eclipse.rdf4j.model.util.RDFCollections;
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
	 * Retrieve all {@link Statement}s that together form the RDF Collection starting with the supplied start resource
	 * and send them to the supplied {@link Consumer}.
	 *
	 * @param conn               the {@link RepositoryConnection} to use for statement retrieval.
	 * @param head               the start resource of the RDF Collection. May not be {@code null}.
	 * @param collectionConsumer a {@link Consumer} function to which all retrieved statements will be reported. May not
	 *                           be {@code null}.
	 * @param contexts           the context(s) from which to read the RDF Collection. This argument is an optional
	 *                           vararg and can be left out.
	 * @throws RepositoryException if an error occurred while reading the collection statements, for example if a cycle
	 *                             is detected in the RDF collection, or some other anomaly which makes it
	 *                             non-wellformed.
	 * @see RDFCollections
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>.
	 */
	public static void consumeRDFCollection(RepositoryConnection conn, Resource head,
			Consumer<Statement> collectionConsumer, Resource... contexts) throws RepositoryException {
		GetStatementOptional statementSupplier = (s, p, o, c) -> getStatement(conn, s, p, o, c);
		Function<String, Supplier<RepositoryException>> exceptionSupplier = Repositories::repositoryException;
		RDFCollections.extract(statementSupplier, head, collectionConsumer, exceptionSupplier, contexts);
	}

	/**
	 * Retrieve all {@link Statement}s that together form the RDF Collection starting with the supplied starting
	 * resource.
	 *
	 * @param conn                the {@link RepositoryConnection} to use for statement retrieval.
	 * @param head                the start resource of the RDF Collection. May not be {@code null}.
	 * @param statementCollection a {@link Collection} of {@link Statement}s (for example, a {@link Model}) to which all
	 *                            retrieved statements will be reported. May not be {@code null}.
	 * @param contexts            the context(s) from which to read the RDF Collection. This argument is an optional
	 *                            vararg and can be left out.
	 * @return the input statement collection, with the statements forming the retrieved RDF Collection added.
	 * @throws RepositoryException if an error occurred while reading the collection statements, for example if a cycle
	 *                             is detected in the RDF collection, or some other anomaly which makes it
	 *                             non-wellformed.
	 * @see RDFCollections
	 * @see <a href="http://www.w3.org/TR/rdf-schema/#ch_collectionvocab">RDF Schema 1.1 section on Collection
	 *      vocabulary</a>.
	 */
	public static <C extends Collection<Statement>> C getRDFCollection(RepositoryConnection conn, Resource head,
			C statementCollection, Resource... contexts) throws RepositoryException {
		Objects.requireNonNull(statementCollection, "statementCollection may not be null");
		consumeRDFCollection(conn, head, st -> statementCollection.add(st), contexts);
		return statementCollection;
	}

	/**
	 * Retrieve a single {@link Statement} matching with the supplied subject, predicate, object and context(s) from the
	 * given {@link RepositoryConnection}. If more than one Statement matches, any one Statement is selected and
	 * returned.
	 *
	 * @param conn      the {@link RepositoryConnection} from which to retrieve the statement.
	 * @param subject   the subject to which the statement should match. May be {@code null}.
	 * @param predicate the predicate to which the statement should match. May be {@code null}.
	 * @param object    the object to which the statement should match. May be {@code null} .
	 * @param contexts  the context(s) from which to read the Statement. This argument is an optional vararg and can be
	 *                  left out.
	 * @return an {@link Optional} of {@link Statement}. If no matching Statement was found, {@link Optional#empty()} is
	 *         returned.
	 * @throws RepositoryException
	 */
	public static Optional<Statement> getStatement(RepositoryConnection conn, Resource subject, IRI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		try (RepositoryResult<Statement> stmts = conn.getStatements(subject, predicate, object, contexts)) {
			Statement st = stmts.hasNext() ? stmts.next() : null;
			return Optional.ofNullable(st);
		}
	}

}
