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
package org.eclipse.rdf4j.repository.sparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPMemServer;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class SPARQLStoreConnectionTest extends RepositoryConnectionTest {

	private static HTTPMemServer server;

	public SPARQLStoreConnectionTest(IsolationLevel level) {
		super(level);
	}

	@BeforeClass
	public static void startServer() throws Exception {
		server = new HTTPMemServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			server = null;
			throw e;
		}

	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
		server = null;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		// overwrite bnode test values as SPARQL endpoints do not generally work
		// well with bnodes
		bob = testRepository.getValueFactory().createIRI("urn:x-local:bob");
		alice = testRepository.getValueFactory().createIRI("urn:x-local:alice");
		alexander = testRepository.getValueFactory().createIRI("urn:x-local:alexander");
	}

	@Override
	protected Repository createRepository() {
		return new SPARQLRepository(HTTPMemServer.REPOSITORY_URL,
				Protocol.getStatementsLocation(HTTPMemServer.REPOSITORY_URL));
	}

	@Override
	@Ignore
	public void testDuplicateFilter() throws Exception {
		System.err.println("temporarily disabled testDuplicateFilter() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on SPARQL update operation handled as part of txn")
	public void testAddDelete() throws RDF4JException {
		System.err.println("temporarily disabled testAddDelete() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on SPARQL update operation handled as part of txn")
	public void testAddRemoveInsert() throws RDF4JException {
		System.err.println("temporarily disabled testAddRemoveInsert() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on pending updates being visible in own connection")
	public void testSizeRollback() throws Exception {
		System.err.println("temporarily disabled testSizeRollback() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on pending updates being visible in own connection")
	public void testAutoCommit() throws Exception {
		System.err.println("temporarily disabled testAutoCommit() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on pending updates being visible in own connection")
	public void testRollback() throws Exception {
		System.err.println("temporarily disabled testRollback() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on pending updates being visible in own connection")
	public void testEmptyRollback() throws Exception {
		System.err.println("temporarily disabled testEmptyRollback() for SPARQLRepository");
	}

	@Override
	@Ignore("relies on pending updates being visible in own connection")
	public void testEmptyCommit() throws Exception {
		System.err.println("temporarily disabled testEmptyCommit() for SPARQLRepository");
	}

	@Override
	@Ignore
	public void testSizeCommit() throws Exception {
		System.err.println("temporarily disabled testSizeCommit() for SPARQLRepository");
	}

	@Override
	@Ignore
	public void testGetStatementsInMultipleContexts() throws Exception {
		System.err.println(
				"temporarily disabled testGetStatementsInMultipleContexts() for SPARQLRepository: implementation of statement context using SPARQL not yet complete");
		// TODO see SES-1776
	}

	@Test
	public void testGetStatementsContextHandling() throws Exception {
		// enable quad mode
		enableQuadModeOnConnection((SPARQLConnection) testCon);

		testCon.clear();

		testCon.begin();
		testCon.add(alice, name, nameAlice, context1);
		testCon.add(bob, name, nameBob);
		testCon.commit();

		List<Statement> res;

		// test 1: alice statement should have context 1
		res = Iterations.asList(testCon.getStatements(alice, null, null, false));
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(context1, res.iterator().next().getContext());

		// test 2: bob statement should have default named graph
		res = Iterations.asList(testCon.getStatements(bob, null, null, false));
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(null, res.iterator().next().getContext());

		// test 3: bound statement should fetch context
		res = Iterations.asList(testCon.getStatements(alice, name, nameAlice, false));
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(context1, res.iterator().next().getContext());

	}

	/**
	 * Enable the quadMode on the given connection. This is done via reflection here as the test setup already creates
	 * the repository and connection and we do not have a chance to set the mode easily inside the test (as quadMode is
	 * an immutable field of the connection). Note: this is only done such that we can reuse the test infrastructure of
	 * the base class.
	 */
	private void enableQuadModeOnConnection(SPARQLConnection con) throws Exception {
		Field quadModeField = SPARQLConnection.class.getDeclaredField("quadMode");
		quadModeField.setAccessible(true);
		quadModeField.set(con, true);
	}

	@Override
	@Ignore
	public void testGetStatementsInSingleContext() throws Exception {
		System.err.println(
				"temporarily disabled testGetStatementsInSingleContext() for SPARQLRepository: implementation of statement context using SPARQL not yet complete");
		// TODO see SES-1776
	}

	@Test
	@Override
	@Ignore("can not execute test because required data add results in illegal SPARQL syntax")
	public void testGetStatementsMalformedLanguageLiteral() throws Exception {
		System.err.println("temporarily disabled testGetStatementsMalformedLanguageLiteral() for SPARQLRepository");
	}

	@Override
	public void testPreparedTupleQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append(" SELECT ?name ?mbox");
		queryBuilder.append(" WHERE { [] foaf:name ?name;");
		queryBuilder.append("            foaf:mbox ?mbox. }");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding("name", nameBob);

		try (TupleQueryResult result = query.evaluate()) {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("name"));
				assertTrue(solution.hasBinding("mbox"));

				Value nameResult = solution.getValue("name");
				Value mboxResult = solution.getValue("mbox");

				assertEquals("unexpected value for name: " + nameResult, nameBob, nameResult);
				assertEquals("unexpected value for mbox: " + mboxResult, mboxBob, mboxResult);
			}
		}
	}

	@Override
	@Ignore
	public void testGetNamespaces() throws Exception {
		System.err.println("disabled testGetNamespaces() as namespace retrieval is not supported by SPARQL");
	}

	@Override
	@Ignore
	public void testGetNamespace() throws Exception {
		System.err.println("disabled testGetNamespace() as namespace retrieval is not supported by SPARQL");
	}

	@Override
	@Ignore
	public void testTransactionIsolation() throws Exception {
		System.err.println("temporarily disabled testTransactionIsolation() for SPARQLRepository");
	}

	@Override
	public void testPreparedTupleQueryUnicode() throws Exception {
		testCon.add(alexander, name, Александър);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE {?person foaf:name ?name . }");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding("name", Александър);

		try (TupleQueryResult result = query.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("person"));
				assertEquals(alexander, solution.getValue("person"));
			}
		}
	}

	@Override
	public void testSimpleGraphQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" CONSTRUCT ");
		queryBuilder.append(" WHERE { [] foaf:name ?name; ");
		queryBuilder.append("            foaf:mbox ?mbox. }");

		try (GraphQueryResult result = testCon.prepareGraphQuery(QueryLanguage.SPARQL, queryBuilder.toString())
				.evaluate()) {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				if (name.equals(st.getPredicate())) {
					assertTrue(nameAlice.equals(st.getObject()) || nameBob.equals(st.getObject()));
				} else {
					assertTrue(mbox.equals(st.getPredicate()));
					assertTrue(mboxAlice.equals(st.getObject()) || mboxBob.equals(st.getObject()));
				}
			}
		}
	}

	@Override
	public void testPreparedGraphQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append(" CONSTRUCT ");
		queryBuilder.append(" WHERE { [] foaf:name ?name ;");
		queryBuilder.append("            foaf:mbox ?mbox . ");
		queryBuilder.append(" } ");

		GraphQuery query = testCon.prepareGraphQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding("name", nameBob);

		try (GraphQueryResult result = query.evaluate()) {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				assertTrue(name.equals(st.getPredicate()) || mbox.equals(st.getPredicate()));
				if (name.equals(st.getPredicate())) {
					assertTrue("unexpected value for name: " + st.getObject(), nameBob.equals(st.getObject()));
				} else {
					assertTrue(mbox.equals(st.getPredicate()));
					assertTrue("unexpected value for mbox: " + st.getObject(), mboxBob.equals(st.getObject()));
				}

			}
		}
	}

	@Override
	public void testSimpleTupleQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append(" SELECT ?name ?mbox");
		queryBuilder.append(" WHERE { [] foaf:name ?name ;");
		queryBuilder.append("            foaf:mbox ?mbox . ");
		queryBuilder.append(" } ");
		try (TupleQueryResult result = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString())
				.evaluate()) {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("name"));
				assertTrue(solution.hasBinding("mbox"));

				Value nameResult = solution.getValue("name");
				Value mboxResult = solution.getValue("mbox");

				assertTrue((nameAlice.equals(nameResult) || nameBob.equals(nameResult)));
				assertTrue((mboxAlice.equals(mboxResult) || mboxBob.equals(mboxResult)));
			}
		}
	}

	@Override
	public void testSimpleTupleQueryUnicode() throws Exception {
		testCon.add(alexander, name, Александър);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name \"").append(Александър.getLabel()).append("\" . } ");

		try (TupleQueryResult result = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString())
				.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("person"));
				assertEquals(alexander, solution.getValue("person"));
			}
		}
	}

	@Override
	@Ignore
	public void testBNodeSerialization() throws Exception {
		System.err.println("temporarily disabled testBNodeSerialization() for SPARQLRepository");
	}

	@Test
	public void testUpdateExecution() throws Exception {

		IRI foobar = vf.createIRI("foo:bar");

		String sparql = "INSERT DATA { <foo:bar> <foo:bar> <foo:bar> . } ";

		Update update = testCon.prepareUpdate(QueryLanguage.SPARQL, sparql);

		update.execute();

		assertTrue(testCon.hasStatement(foobar, foobar, foobar, true));

		testCon.clear();

		assertFalse(testCon.hasStatement(foobar, foobar, foobar, true));

		testCon.begin();
		update.execute();
		testCon.commit();

		assertTrue(testCon.hasStatement(foobar, foobar, foobar, true));

	}

	@Test
	@Ignore("relies on pending updates being visible in own connection")
	@Override
	public void testRemoveStatementsFromContextSingleTransaction() throws Exception {
		super.testRemoveStatementsFromContextSingleTransaction();
	}

	@Test
	@Ignore("relies on pending updates being visible in own connection")
	@Override
	public void testClearStatementsFromContextSingleTransaction() throws Exception {
		super.testClearStatementsFromContextSingleTransaction();
	}
}
