/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a simple path for exactly one predicate. Currently there is no support for complex paths.
 *
 * @author Heshan Jayasinghe
 */
public class SimplePath extends Path {

	private IRI path;

	SimplePath(Resource id, SailRepositoryConnection connection) {
		super(id);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.PATH, null, true))) {
			path = stream.map(Statement::getObject).map(v -> (IRI) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:path on " + id));
		}

	}

	@Override
	public String toString() {
		return "Path{" + "path=" + path + '}';
	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		boolean requiresEvalutation;
		try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
			requiresEvalutation = addedStatementsConnection.hasStatement(null, path, null, false);
		}

		try (RepositoryConnection removedStatementsConnection = removedStatements.getConnection()) {
			requiresEvalutation |= removedStatementsConnection.hasStatement(null, path, null, false);
		}

		return requiresEvalutation;
	}

	@Override
	public String getQuery() {

		return "?a <" + path + "> ?c. ";

	}
}
