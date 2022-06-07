/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;

class ShapeSourceHelper {

	static Value getFirst(SailConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var stream = connection.getStatements(subject, RDF.FIRST, null, true, context).stream()) {
			return getObject(stream);
		}
	}

	static Value getFirst(RepositoryConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var stream = connection.getStatements(subject, RDF.FIRST, null, true, context).stream()) {
			return getObject(stream);
		}
	}

	static Resource getRdfRest(RepositoryConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var stream = connection.getStatements(subject, RDF.REST, null, true, context).stream()) {
			return (Resource) getObject(stream);
		}
	}

	static Resource getRdfRest(SailConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var stream = connection.getStatements(subject, RDF.REST, null, true, context).stream()) {
			return (Resource) getObject(stream);
		}
	}

	private static Value getObject(Stream<? extends Statement> stream) {
		return stream
				.map(Statement::getObject)
				.findAny()
				.orElse(null);
	}

}
