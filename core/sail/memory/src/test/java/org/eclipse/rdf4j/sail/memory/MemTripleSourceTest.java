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
package org.eclipse.rdf4j.sail.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Test for {@link TripleSource}
 *
 * @author Peter Ansell
 */
public class MemTripleSourceTest {

	private static final Logger logger = LoggerFactory.getLogger(MemTripleSourceTest.class);

	private MemoryStore store;

	protected static final String EX_NS = "http://example.org/";

	private IRI bob;

	private IRI alice;

	private IRI mary;

	private ValueFactory f;

	private SailDataset snapshot;

	private SailSource source;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		store = new MemoryStore();
		store.init();
		f = store.getValueFactory();

		bob = f.createIRI(EX_NS, "bob");
		alice = f.createIRI(EX_NS, "alice");
		mary = f.createIRI(EX_NS, "mary");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		if (snapshot != null) {
			snapshot.close();
		}
		if (source != null) {
			source.close();
		}
		store.shutDown();
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsAllNull() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextAllNull() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsAllNull() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(16, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsOnePredicate() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextOnePredicate() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsOnePredicate() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsOnePredicateOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextOnePredicateOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsOnePredicateOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsOnePredicateTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextOnePredicateTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsOnePredicateTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateOwlThingTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, OWL.THING, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateOwlThingTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, OWL.THING, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(1, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateOwlThingTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, OWL.THING, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(2, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateOwlClassTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateOwlClassTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateOwlClassTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateOwlClassNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateOwlClassNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(4, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateOwlClassNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateExClassNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"))) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(3, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateExClassNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"))) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(3, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateExClassNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"))) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(6, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateExClassOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateExClassOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(3, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateExClassOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(3, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateExClassTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateExClassTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(3, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateExClassTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(6, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsExClassPredicateTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source
				.getStatements(f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(0, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextExClassPredicateTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source
				.getStatements(f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(1, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsExClassPredicateTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source
				.getStatements(f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(2, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsExClassPredicateNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source
				.getStatements(f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(1, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextExClassPredicateNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source
				.getStatements(f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(1, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsExClassPredicateNoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source
				.getStatements(f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(2, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsAllNull() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(24, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsOneContext() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null, this.alice)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(8, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsTwoContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null, this.alice, this.bob)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(16, list.size());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.sail.memory.MemTripleSource#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsThreeContexts() throws Exception {
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		try (CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null, this.alice, this.bob, this.mary)) {
			List<Statement> list = Iterations.asList(statements);

			Assertions.assertEquals(24, list.size());
		}
	}

	protected void loadTestData(String dataFile, Resource... contexts)
			throws RDFParseException, IOException, SailException {
		logger.debug("loading dataset {}", dataFile);
		try (InputStream dataset = this.getClass().getResourceAsStream(dataFile)) {
			try (SailConnection con = store.getConnection()) {
				con.begin();
				for (Statement nextStatement : Rio.parse(dataset, "", RDFFormat.TURTLE, contexts)) {
					con.addStatement(nextStatement.getSubject(), nextStatement.getPredicate(),
							nextStatement.getObject(),
							nextStatement.getContext());
				}
				con.commit();
			}
		}
		logger.debug("dataset loaded.");
	}

	/**
	 * Helper method to avoid writing this constructor multiple times. It needs to be created after statements are added
	 * and committed.
	 *
	 * @return
	 * @throws SailException
	 */
	private TripleSource getTripleSourceCommitted() throws SailException {
		IsolationLevel level = store.getDefaultIsolationLevel();
		source = store.getSailStore().getExplicitSailSource().fork();
		snapshot = source.dataset(level);
		final ValueFactory vf = store.getValueFactory();
		return new TripleSource() {

			@Override
			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
					IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
				try {
					return new ExceptionConvertingIteration<Statement, QueryEvaluationException>(
							snapshot.getStatements(subj, pred, obj, contexts)) {

						@Override
						protected QueryEvaluationException convert(Exception e) {
							return new QueryEvaluationException(e);
						}
					};
				} catch (SailException e) {
					throw new QueryEvaluationException(e);
				}
			}

			@Override
			public ValueFactory getValueFactory() {
				return vf;
			}
		};
	}

}
