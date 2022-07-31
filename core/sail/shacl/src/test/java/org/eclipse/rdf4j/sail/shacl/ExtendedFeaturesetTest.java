/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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

import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class ExtendedFeaturesetTest {

	SimpleValueFactory vf = SimpleValueFactory.getInstance();
	IRI ex_knows = vf.createIRI("http://example.com/ns#knows");
	IRI ex_Person = vf.createIRI("http://example.com/ns#Person");

	@Test
	public void testDashIsDisabledByDefault() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("test-cases/class/allSubjects/shacl.trig"
		);
		((ShaclSail) shaclRepository.getSail()).setShapesGraphs(
				Set.of(RDF4J.SHACL_SHAPE_GRAPH, RDF4J.NIL, Values.iri("http://example.com/ns#shapesGraph1")));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createBNode(), ex_knows, vf.createBNode());
			connection.commit();
		}
		shaclRepository.shutDown();

	}

	@Test
	public void testThatDashCanBeEnabled() throws Throwable {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("test-cases/class/allSubjects/shacl.trig");
		((ShaclSail) shaclRepository.getSail()).setDashDataShapes(true);
		((ShaclSail) shaclRepository.getSail()).setShapesGraphs(
				Set.of(RDF4J.SHACL_SHAPE_GRAPH, RDF4J.NIL, Values.iri("http://example.com/ns#shapesGraph1")));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createBNode(), ex_knows, vf.createBNode());

			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		}
		shaclRepository.shutDown();

	}

	@Test
	public void testTargetShapeIsDisabledByDefault() throws Exception {

		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("test-cases/class/simpleTargetShape/shacl.trig");
		((ShaclSail) shaclRepository.getSail()).setShapesGraphs(
				Set.of(RDF4J.SHACL_SHAPE_GRAPH, RDF4J.NIL, Values.iri("http://example.com/ns#shapesGraph1")));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.add(bNode, RDF.TYPE, ex_Person);
			connection.add(bNode, ex_knows, vf.createBNode());
			connection.commit();
		}

		shaclRepository.shutDown();

	}

	@Test
	public void testThatTargetShapesCanBeEnabled() throws Throwable {

		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("test-cases/class/simpleTargetShape/shacl.trig");

		((ShaclSail) shaclRepository.getSail()).setDashDataShapes(true);
		((ShaclSail) shaclRepository.getSail()).setEclipseRdf4jShaclExtensions(true);
		((ShaclSail) shaclRepository.getSail()).setShapesGraphs(
				Set.of(RDF4J.SHACL_SHAPE_GRAPH, RDF4J.NIL, Values.iri("http://example.com/ns#shapesGraph1")));

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.add(bNode, RDF.TYPE, ex_Person);
			connection.add(bNode, ex_knows, vf.createBNode());

			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		}
		shaclRepository.shutDown();

	}

}
