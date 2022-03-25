/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LimitedSizeNativeStoreConnectionTest extends RepositoryConnectionTest {
	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	public LimitedSizeNativeStoreConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository() throws IOException {
		return new SailRepository(new LimitedSizeNativeStore(tmpDir.newFolder(), "spoc"));
	}

	@Test
	public void testSES715() throws Exception {
		// load 1000 triples in two different contexts
		testCon.begin();
		ValueFactory vf = testCon.getValueFactory();
		IRI context1 = vf.createIRI("http://my.context.1");
		IRI context2 = vf.createIRI("http://my.context.2");
		IRI context3 = vf.createIRI("http://my.context.3");
		IRI predicate = vf.createIRI("http://my.predicate");
		IRI object = vf.createIRI("http://my.object");

		for (int j = 0; j < 1000; j++) {
			testCon.add(vf.createIRI("http://my.subject" + j), predicate, object, context1);
			testCon.add(vf.createIRI("http://my.subject" + j), predicate, object, context2);
			testCon.add(vf.createIRI("http://my.subject" + j), predicate, object, context3);
		}
		assertEquals(1000, Iterations.asList(testCon.getStatements(null, null, null, false, context1)).size());
		assertEquals(1000, Iterations.asList(testCon.getStatements(null, null, null, false, context2)).size());

		// remove all triples from context 1
		testCon.clear(context1);
		assertEquals(0, Iterations.asList(testCon.getStatements(null, null, null, false, context1)).size());
		assertEquals(1000, Iterations.asList(testCon.getStatements(null, null, null, false, context2)).size());
		assertEquals(2000,
				Iterations.asList(testCon.getStatements(null, null, null, false, context2, context3)).size());
		testCon.commit();

		// check context content using fresh connection
		assertEquals(0, Iterations.asList(testCon2.getStatements(null, null, null, false, context1)).size());
		assertEquals(1000, Iterations.asList(testCon2.getStatements(null, null, null, false, context2)).size());

		testCon2.close();
	}

	@Test
	public void testLimit() throws Exception {
		((LimitedSizeNativeStoreConnection) ((SailRepositoryConnection) testCon).getSailConnection())
				.setMaxCollectionsSize(2);
		testCon.begin();
		ValueFactory vf = testCon.getValueFactory();
		IRI context1 = vf.createIRI("http://my.context.1");
		IRI predicate = vf.createIRI("http://my.predicate");
		IRI object = vf.createIRI("http://my.object");

		for (int j = 0; j < 100; j++) {
			testCon.add(vf.createIRI("http://my.subject" + j), predicate, object, context1);
		}
		testCon.commit();
		String queryString = "SELECT DISTINCT ?s WHERE {?s ?p ?o}";
		TupleQuery q = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		QueryEvaluationException shouldThrow = runQuery(q);
		assertNotNull(shouldThrow);

		// There is just one object therefore we should not throw a new
		// exception
		queryString = "SELECT DISTINCT ?o WHERE {?s ?p ?o}";
		q = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		shouldThrow = runQuery(q);
		assertNull(shouldThrow);
	}

	protected QueryEvaluationException runQuery(TupleQuery q) {
		QueryEvaluationException shouldThrow = null;
		try (TupleQueryResult r = q.evaluate()) {
			assertTrue(r.hasNext());
			while (r.hasNext()) {
				assertNotNull(r.next());
			}
		} catch (QueryEvaluationException e) {
			shouldThrow = e;
		}
		return shouldThrow;
	}
}
