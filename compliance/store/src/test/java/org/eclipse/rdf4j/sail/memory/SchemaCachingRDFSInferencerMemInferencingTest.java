/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.InferencingTest;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class SchemaCachingRDFSInferencerMemInferencingTest extends InferencingTest {

	@Override
	protected Sail createSail() {
		Sail sailStack = new SchemaCachingRDFSInferencer(new MemoryStore(), true);
		return sailStack;
	}

	@Test
	public void testBlankNodePredicateInference() {
		Repository sailRepository = new SailRepository(createSail());
		sailRepository.initialize();
		ValueFactory vf = sailRepository.getValueFactory();

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			BNode bNode = vf.createBNode();
			connection.add(vf.createStatement(vf.createIRI("http://a"), RDFS.SUBPROPERTYOF, bNode)); // 1
			connection.add(vf.createStatement(bNode, RDFS.DOMAIN, vf.createIRI("http://c"))); // 2
			connection.add(vf.createStatement(vf.createIRI("http://d"), vf.createIRI("http://a"),
					vf.createIRI("http://e"))); // 3
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			boolean correctInference = connection.hasStatement(vf.createIRI("http://d"), RDF.TYPE,
					vf.createIRI("http://c"), true);
			assertTrue("d should be type c, because 3 and 1 entail 'd _:bNode e' with 2 entail 'd type c'",
					correctInference);
		}

	}

	@Test
	public void testRollback()
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		Repository sailRepository = new SailRepository(createSail());
		sailRepository.initialize();
		ValueFactory vf = sailRepository.getValueFactory();

		IRI A = vf.createIRI("http://A");
		IRI aInstance = vf.createIRI("http://aInstance");

		IRI B = vf.createIRI("http://B");
		IRI C = vf.createIRI("http://C");

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(vf.createStatement(A, RDFS.SUBCLASSOF, C));
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createStatement(A, RDFS.SUBCLASSOF, B));
			connection.size(); // forces flushUpdate() to be called
			connection.rollback();
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(vf.createStatement(aInstance, RDF.TYPE, A));
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createStatement(vf.createBNode(), RDF.TYPE, A));
			connection.rollback();
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(vf.createStatement(vf.createBNode(), RDF.TYPE, A));
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {

			boolean incorrectInference = connection.hasStatement(aInstance, RDF.TYPE, B, true);
			assertFalse("Previous rollback() should have have cleared the cache for A subClassOf B. ",
					incorrectInference);

			boolean correctInference = connection.hasStatement(aInstance, RDF.TYPE, C, true);
			assertTrue("aInstance should be instance of C because A subClassOfC was added earlier.",
					correctInference);
		}
	}

	@Test
	public void testFastInstantiate()
		throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		Sail sail = createSail();
		Repository sailRepository = new SailRepository(sail);
		sailRepository.initialize();
		ValueFactory vf = sailRepository.getValueFactory();

		IRI A = vf.createIRI("http://A");
		IRI aInstance = vf.createIRI("http://aInstance");

		IRI B = vf.createIRI("http://B");
		IRI C = vf.createIRI("http://C");

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(vf.createStatement(A, RDFS.SUBCLASSOF, C));
		}

		SailRepository sailRepository1 = new SailRepository(
				SchemaCachingRDFSInferencer.fastInstantiateFrom(
						(SchemaCachingRDFSInferencer)sail, new MemoryStore()));
		sailRepository1.initialize();

		try (RepositoryConnection connection = sailRepository1.getConnection()) {
			connection.add(vf.createStatement(aInstance, RDF.TYPE, A));
		}

		try (RepositoryConnection connection = sailRepository1.getConnection()) {
			boolean correctInference = connection.hasStatement(aInstance, RDF.TYPE, C, true);
			assertTrue(
					"aInstance should be instance of C because A subClassOfC was added to the sail used by fastInstantiateFrom.",
					correctInference);
		}
	}

}
