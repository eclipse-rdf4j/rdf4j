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
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a simple path for exactly one predicate. Currently there is no support for complex paths.
 *
 * @author Heshan Jayasinghe
 */
public class SimplePath extends Path {

	private final IRI path;

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
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {

		return
			addedStatements.hasStatement(null, path, null, false) ||
			removedStatements.hasStatement(null, path, null, false);
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable) {

		return subjectVariable+" <" + path + "> "+objectVariable+" . \n";

	}

	public IRI getPath() {
		return path;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimplePath that = (SimplePath) o;
		return Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}
}
