/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class ExtendedFeaturesetTest {

	SimpleValueFactory vf = SimpleValueFactory.getInstance();
	IRI ex_knows = vf.createIRI("http://example.com/ns#knows");
	IRI ex_Person = vf.createIRI("http://example.com/ns#Person");

	@AfterClass
	public static void afterClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void testDashIsDisabledByDefault() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("test-cases/class/allSubjects/shacl.ttl",
				false);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createBNode(), ex_knows, vf.createBNode());
			connection.commit();
		}

	}

	@Test(expected = ShaclSailValidationException.class)
	public void testThatDashCanBeEnabled() throws Throwable {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("test-cases/class/allSubjects/shacl.ttl",
				false);
		((ShaclSail) shaclRepository.getSail()).setDashDataShapes(true);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createBNode(), ex_knows, vf.createBNode());
			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}
		}

	}

	@Test
	public void testTargetShapeIsDisabledByDefault() throws Exception {

		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("test-cases/class/simpleTargetShape/shacl.ttl", false);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.add(bNode, RDF.TYPE, ex_Person);
			connection.add(bNode, ex_knows, vf.createBNode());
			connection.commit();
		}

	}

	@Test(expected = ShaclSailValidationException.class)
	public void testThatTargetShapesCanBeEnabled() throws Throwable {

		SailRepository shaclRepository = Utils
				.getInitializedShaclRepository("test-cases/class/simpleTargetShape/shacl.ttl", false);

		((ShaclSail) shaclRepository.getSail()).setDashDataShapes(true);
		((ShaclSail) shaclRepository.getSail()).setEclipseRdf4jShaclExtensions(true);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			BNode bNode = vf.createBNode();
			connection.add(bNode, RDF.TYPE, ex_Person);
			connection.add(bNode, ex_knows, vf.createBNode());
			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}
		}

	}

}
