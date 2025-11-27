/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclShapeParsingException;
import org.junit.jupiter.api.Test;

class LogicalRecursionValidationTest {

	@Test
	void recursionInNotIsDetected() throws Exception {
		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("recursion/logical/not/shacl.trig");

		ShaclSail shaclSail = (ShaclSail) shaclRepository.getSail();
		shaclSail.setShapesGraphs(Set.of(RDF4J.NIL));

		URL data = Objects.requireNonNull(
				getClass().getClassLoader().getResource("recursion/logical/not/data.trig"));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(data, data.toString(), RDFFormat.TRIG);

			assertThrows(ShaclShapeParsingException.class, () -> {
				try {
					connection.commit();
				} catch (Exception e) {
					connection.rollback();
					throw e;
				}
			});
		}
	}

	@Test
	void recursionInOrIsDetected() throws Exception {
		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("recursion/logical/or/shacl.trig");

		ShaclSail shaclSail = (ShaclSail) shaclRepository.getSail();
		shaclSail.setShapesGraphs(Set.of(RDF4J.NIL));

		URL data = Objects.requireNonNull(
				getClass().getClassLoader().getResource("recursion/logical/or/data.trig"));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(data, data.toString(), RDFFormat.TRIG);

			assertThrows(ShaclShapeParsingException.class, () -> {
				try {
					connection.commit();
				} catch (Exception e) {
					connection.rollback();
					throw e;
				}
			});
		}
	}

	@Test
	void recursionInNodeIsDetected() throws Exception {
		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("recursion/node/selfNode/shacl.trig");

		ShaclSail shaclSail = (ShaclSail) shaclRepository.getSail();
		shaclSail.setShapesGraphs(Set.of(RDF4J.NIL));

		URL data = Objects.requireNonNull(
				getClass().getClassLoader().getResource("recursion/node/selfNode/data.trig"));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(data, data.toString(), RDFFormat.TRIG);

			assertThrows(ShaclShapeParsingException.class, () -> {
				try {
					connection.commit();
				} catch (Exception e) {
					connection.rollback();
					throw e;
				}
			});
		}
	}
}
