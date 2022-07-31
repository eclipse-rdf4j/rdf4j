/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.sail.InferencingTest;
import org.junit.Test;

public class MemInferencingTest extends InferencingTest {

	@Override
	protected Sail createSail() {
		Sail sailStack = new SchemaCachingRDFSInferencer(new MemoryStore());
		return sailStack;
	}

	@Test
	public void testPersistence() {
		File datadir = Files.newTemporaryFolder();

		SchemaCachingRDFSInferencer sailStack = new SchemaCachingRDFSInferencer(new MemoryStore(datadir), true);
		SailRepository repo = new SailRepository(sailStack);
		ValueFactory vf = repo.getValueFactory();

		IRI s1 = vf.createIRI("foo:s1");
		IRI c2 = vf.createIRI("foo:c2");
		IRI c1 = vf.createIRI("foo:c1");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(s1, RDF.TYPE, c1);
			conn.add(c1, RDFS.SUBCLASSOF, c2);
			conn.commit();
			assertTrue(conn.hasStatement(s1, RDF.TYPE, c2, true));
		}
		repo.shutDown();

		// re-init
//		sailStack = new SchemaCachingRDFSInferencer(new MemoryStore(datadir), true);
//		repo = new SailRepository(sailStack);
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue(conn.hasStatement(s1, RDF.TYPE, c2, true));
		}
	}

	@Test
	public void testBlankNodePredicateInference() {
		Repository sailRepository = new SailRepository(createSail());

		ValueFactory vf = sailRepository.getValueFactory();

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			BNode bNode = vf.createBNode();
			connection.add(vf.createStatement(vf.createIRI("http://a"), RDFS.SUBPROPERTYOF, bNode)); // 1
			connection.add(vf.createStatement(bNode, RDFS.DOMAIN, vf.createIRI("http://c"))); // 2
			connection.add(
					vf.createStatement(vf.createIRI("http://d"), vf.createIRI("http://a"), vf.createIRI("http://e"))); // 3
		}

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			boolean correctInference = connection.hasStatement(vf.createIRI("http://d"), RDF.TYPE,
					vf.createIRI("http://c"), true);
			assertTrue("d should be type c, because 3 and 1 entail 'd _:bNode e' with 2 entail 'd type c'",
					correctInference);
		}

	}

	@Test
	public void testRollback() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Repository sailRepository = new SailRepository(createSail());
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
			assertTrue("aInstance should be instance of C because A subClassOfC was added earlier.", correctInference);
		}
	}

	@Test
	public void testFastInstantiate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Repository sailRepository = new SailRepository(createSail());
		ValueFactory vf = sailRepository.getValueFactory();

		IRI A = vf.createIRI("http://A");
		IRI aInstance = vf.createIRI("http://aInstance");

		IRI B = vf.createIRI("http://B");
		IRI C = vf.createIRI("http://C");

		try (RepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(vf.createStatement(A, RDFS.SUBCLASSOF, C));
		}

		SailRepository sailRepository1 = new SailRepository(SchemaCachingRDFSInferencer.fastInstantiateFrom(
				(SchemaCachingRDFSInferencer) ((SailRepository) sailRepository).getSail(), new MemoryStore()));

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
