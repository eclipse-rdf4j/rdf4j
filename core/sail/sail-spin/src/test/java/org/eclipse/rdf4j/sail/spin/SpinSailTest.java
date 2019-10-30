/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpinSailTest {

	private static final String BASE_DIR = "/testcases/";

	private Repository repo;

	private RepositoryConnection conn;

	@Before
	public void setup() throws RepositoryException {
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		SchemaCachingRDFSInferencer rdfsInferencer = new SchemaCachingRDFSInferencer(deduper, false);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		repo = new SailRepository(spinSail);
		repo.initialize();
		conn = repo.getConnection();
	}

	@After
	public void tearDown() throws RepositoryException {
		if (conn != null) {
			conn.close();
		}
		if (repo != null) {
			repo.shutDown();
		}
	}

	@Test
	public void testAskConstraint() throws Exception {
		assertThatThrownBy(() -> loadStatements("testAskConstraint.ttl"))
				.hasCauseInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("Test constraint");
	}

	@Test
	public void testTemplateConstraint() throws Exception {
		assertThatThrownBy(() -> loadStatements("testTemplateConstraint.ttl"))
				.hasCauseInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("Invalid number of values: 0");
	}

	@Test
	public void testConstructRule() throws Exception {
		loadStatements("testConstructRule.ttl");
		assertStatements("testConstructRule-expected.ttl");
	}

	@Test
	public void testUpdateTemplateRule() throws Exception {
		loadStatements("testUpdateTemplateRule.ttl");
		assertStatements("testUpdateTemplateRule-expected.ttl");
	}

	@Test
	public void testConstructor() throws Exception {
		loadStatements("testConstructor.ttl");
		assertStatements("testConstructor-expected.ttl");
	}

	@Test
	public void testAskFunctionConstraint() throws Exception {
		loadStatements("testAskFunctionConstraint.ttl");
	}

	@Test
	public void testEvalFunctionConstraint() throws Exception {
		loadStatements("testEvalFunctionConstraint.ttl");
	}

	@Test
	public void testConstructProperty() throws Exception {
		loadStatements("testConstructProperty.ttl");
		assertStatements("testConstructProperty-expected.ttl");
	}

	@Test
	public void testSelectProperty() throws Exception {
		loadStatements("testSelectProperty.ttl");
		assertStatements("testSelectProperty-expected.ttl");
	}

	@Test
	public void testMagicPropertyRule() throws Exception {
		loadStatements("testMagicPropertyRule.ttl");
		assertStatements("testMagicPropertyRule-expected.ttl");
	}

	@Test
	public void testMagicPropertyConstraint() throws Exception {
		loadStatements("testMagicPropertyConstraint.ttl");
	}

	@Test
	public void testMagicPropertyFunction() throws Exception {
		loadStatements("testMagicPropertyFunction.ttl");
		assertStatements("testMagicPropertyFunction-expected.ttl");
	}

	@Test
	public void testSpinxRule() throws Exception {
		loadStatements("testSpinxRule.ttl");
		assertStatements("testSpinxRule-expected.ttl");
	}

	@Ignore("This test shows how the SpinSail fails on transactional workloads.")
	@Test
	public void testDeepConstructRule() throws Exception {
		loadStatements("testDeepConstructRule.ttl");

		ValueFactory vf = SimpleValueFactory.getInstance();

		String ns = "http://example.org/";

		IRI ex1 = vf.createIRI(ns, "ex1");
		IRI TestClass = vf.createIRI(ns, "TestClass");
		IRI prop = vf.createIRI(ns, "prop");
		IRI ex2 = vf.createIRI(ns, "ex2");
		IRI ex3 = vf.createIRI(ns, "ex3");
		IRI ex4 = vf.createIRI(ns, "ex4");

		conn.begin();
		conn.add(ex1, RDF.TYPE, TestClass);
		conn.add(ex1, prop, ex2);

		// comment out these two lines, so that all statements are added in the same transaction. This will make the
		// test pass.
		conn.commit();
		conn.begin();

		conn.add(ex2, FOAF.KNOWS, ex3);
		conn.add(ex3, FOAF.KNOWS, ex4);
		conn.commit();

		assertStatements("testDeepConstructRule-expected.ttl");

	}

	@Ignore("This test shows that using negation will lead to incorrect inference.")
	@Test
	public void testNegationConstructRule() throws Exception {
		loadStatements("testNegationConstructRule.ttl");

		ValueFactory vf = SimpleValueFactory.getInstance();

		String ns = "http://example.org/";

		IRI sibling = vf.createIRI(ns, "sibling");
		IRI parent1 = vf.createIRI(ns, "parent1");
		IRI parentOf = vf.createIRI(ns, "parentOf");

		// parent1 is already parentOf onlyChild1, so adding the following statement should stop onlyChild1 from being
		// an only child
		conn.add(parent1, parentOf, sibling);

		IRI OnlyChild = vf.createIRI(ns, "OnlyChild");

		try (Stream<Statement> stream = Iterations.stream(conn.getStatements(null, RDF.TYPE, OnlyChild))) {
			long count = stream.peek(System.out::println).count();
			assertEquals(0, count);
		}

	}

	@Test
	public void testTransactions() throws Exception {
		tx((Callable<Void>) () -> {
			loadStatements("testTransactions-rule.ttl");
			return null;
		});
		tx((Callable<Void>) () -> {
			loadStatements("testTransactions-data.ttl");
			return null;
		});
		tx((Callable<Void>) () -> {
			assertStatements("testTransactions-expected.ttl");
			return null;
		});
	}

	private <T> T tx(Callable<T> c) throws Exception {
		T result;
		conn.begin();
		try {
			result = c.call();
			conn.commit();
		} catch (Exception e) {
			conn.rollback();
			throw e;
		}
		return result;
	}

	private void loadStatements(String ttl) throws RepositoryException, RDFParseException, IOException {
		URL url = getClass().getResource(BASE_DIR + ttl);
		try (InputStream in = url.openStream()) {
			conn.add(in, url.toString(), RDFFormat.TURTLE);
		}
	}

	private void assertStatements(String ttl)
			throws RDFParseException, RDFHandlerException, IOException, RepositoryException {
		StatementCollector expected = new StatementCollector();
		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		parser.setRDFHandler(expected);
		URL url = getClass().getResource(BASE_DIR + ttl);
		try (InputStream rdfStream = url.openStream()) {
			parser.parse(rdfStream, url.toString());
		}

		for (Statement stmt : expected.getStatements()) {
			assertTrue("Expected statement: " + stmt, conn.hasStatement(stmt, true));
		}
	}
}
