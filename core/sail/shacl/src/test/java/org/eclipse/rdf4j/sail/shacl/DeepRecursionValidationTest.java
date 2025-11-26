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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclShapeParsingException;
import org.junit.jupiter.api.Test;

class DeepRecursionValidationTest {

	private static final IRI CHAIN_SHAPE_12 = Values.iri("http://example.com/ns#ChainShape12");

	@Test
	void deepRecursionAcrossAllConstraintTypesValidates() throws Exception {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository(
				"test-cases/recursion/deep/shacl.trig");

		ShaclSail shaclSail = (ShaclSail) shaclRepository.getSail();
		shaclSail.setShapesGraphs(Set.of(RDF4J.NIL));

		URL data = Objects.requireNonNull(
				getClass().getClassLoader().getResource("test-cases/recursion/deep/data.trig"));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(data, data.toString(), RDFFormat.TRIG);

			ShaclShapeParsingException exception = assertThrows(ShaclShapeParsingException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					connection.rollback();
					throw e.getCause();
				}
			});

			System.out.println(exception.getMessage());
			System.out.println(exception.getCause());

			assertFalse(exception.getCause() instanceof StackOverflowError);

		} finally {
			shaclRepository.shutDown();
		}
	}
}
