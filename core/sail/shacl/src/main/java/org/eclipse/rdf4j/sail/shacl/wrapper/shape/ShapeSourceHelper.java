/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

class ShapeSourceHelper {

	static Value getFirst(SailConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var iteration = connection.getStatements(subject, RDF.FIRST, null, true, context)) {
			return getObject(iteration);
		}
	}

	static Value getFirst(RepositoryConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var iteration = connection.getStatements(subject, RDF.FIRST, null, true, context)) {
			return getObject(iteration);
		}
	}

	static Resource getRdfRest(RepositoryConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var iteration = connection.getStatements(subject, RDF.REST, null, true, context)) {
			return (Resource) getObject(iteration);
		}
	}

	static Resource getRdfRest(SailConnection connection, Resource subject, Resource[] context) {
		assert context != null;
		try (var iteration = connection.getStatements(subject, RDF.REST, null, true, context)) {
			return (Resource) getObject(iteration);
		}
	}

//	private static Value getObject(Stream<? extends Statement> stream) {
//		return stream
//				.map(Statement::getObject)
//				.findAny()
//				.orElse(null);
//	}

	private static Value getObject(CloseableIteration<? extends Statement, SailException> iteration) {
		if (iteration.hasNext()) {
			return iteration.next().getObject();
		}
		return null;
	}

	private static Value getObject(RepositoryResult<Statement> iteration) {
		if (iteration.hasNext()) {
			return iteration.next().getObject();
		}
		return null;
	}

}
