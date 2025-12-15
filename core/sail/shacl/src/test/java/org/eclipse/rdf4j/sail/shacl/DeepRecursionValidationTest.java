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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclShapeParsingException;
import org.junit.jupiter.api.Test;

class DeepRecursionValidationTest {

	@Test
	void recursiveShapesCanBeRemovedAfterFailedValidation() throws Exception {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setShapesGraphs(Set.of(RDF4J.SHACL_SHAPE_GRAPH));
		SailRepository shaclRepository = new SailRepository(shaclSail);
		shaclRepository.init();

		URL shapes = Objects.requireNonNull(
				getClass().getClassLoader().getResource("recursion/deep/shacl.trig"));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shapes, shapes.toString(), RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDF4J.SHACL_SHAPE_GRAPH, RDF4J.SHACL_SHAPE_GRAPH, RDF4J.SHACL_SHAPE_GRAPH);

			assertThrows(ShaclShapeParsingException.class, connection::commit);
			connection.rollback();
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.clear(RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			assertFalse(connection.hasStatement(null, null, null, false, RDF4J.SHACL_SHAPE_GRAPH));
		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	void recursiveShapesCanBeRemovedAfterFailedValidation2() throws Exception {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setShapesGraphs(Set.of(RDF4J.NIL));

		SailRepository shaclRepository = new SailRepository(shaclSail);
		shaclRepository.init();

		URL shapes = Objects.requireNonNull(
				getClass().getClassLoader().getResource("recursion/deep/shacl.trig"));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(shapes, shapes.toString(), RDFFormat.TRIG);
			connection.commit();
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(Values.bnode(), RDFS.LABEL, Values.literal("This will fail"));

			assertThrows(ShaclShapeParsingException.class, connection::commit);
			connection.rollback();
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.clear(new Resource[1]);
			connection.commit();

			assertFalse(connection.hasStatement(null, null, null, false));
		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	void deepRecursionAcrossAllConstraintTypesValidates() throws Exception {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository(
				"recursion/deep/shacl.trig");

		ShaclSail shaclSail = (ShaclSail) shaclRepository.getSail();
		shaclSail.setShapesGraphs(Set.of(RDF4J.NIL));

		URL data = Objects.requireNonNull(
				getClass().getClassLoader().getResource("recursion/deep/data.trig"));

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

			assertNotNull(exception.getId());

		} finally {
			shaclRepository.shutDown();
		}
	}
}
