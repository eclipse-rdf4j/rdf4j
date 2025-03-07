/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.structures.FedXDataset;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class BasicTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query01.rq", "/tests/basic/query01.srx", false, true);
	}

	@Test
	public void test2() throws Exception {
		/* test a basic Construct query retrieving all triples */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query02.rq", "/tests/basic/query02.ttl", false, true);
	}

	@Test
	public void testBooleanTrueSingleSource() throws Exception {
		/* test a basic boolean query (result true) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query03.rq", "/tests/basic/query03.srx", false, true);
	}

	@Test
	public void testBooleanTrueMultipleSource() throws Exception {
		/* test a basic boolean query (result true) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query03a.rq", "/tests/basic/query03.srx", false, true);
	}

	@Test
	public void testBooleanFalse() throws Exception {
		/* test a basic boolean query (result false) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query04.rq", "/tests/basic/query04.srx", false, true);
	}

	@Test
	public void testSingleSourceSelect() throws Exception {
		/* test a single source select query */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_singleSource01.rq", "/tests/basic/query_singleSource01.srx", false, true);
	}

	@Test
	public void testSingleSourceConstruct() throws Exception {
		/* test a single source construct */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_singleSource02.rq", "/tests/basic/query_singleSource02.ttl", false, true);
	}

	@Test
	public void testGetStatements() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		Set<Statement> res = getStatements(null, null, null);
		compareGraphs(res, readExpectedGraphQueryResult("/tests/basic/query02.ttl"));
	}

	@Test
	public void testValuesClause() throws Exception {
		/* test query with values clause */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_values.rq", "/tests/basic/query_values.srx", false, true);
	}

	@Test
	public void testQuotes() throws Exception {
		/* test query with new line in literal */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint3.ttl"));
		execute("/tests/basic/query_quotes.rq", "/tests/basic/query_quotes.srx", false, true);
	}

	@Test
	public void testQuotesDatatype() throws Exception {
		/* test query with new line in triple quotes and datatype */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint3.ttl"));
		execute("/tests/basic/query_quotes_datatype.rq", "/tests/basic/query_quotes_datatype.srx", false, true);
	}

	@Test
	public void testLanguageTag() throws Exception {
		/* test query with a language tag */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint4.ttl"));
		execute("/tests/basic/query_lang.rq", "/tests/basic/query_lang.srx", false, true);
	}

	@Test
	public void testBindClause() throws Exception {

		/* test query with bind clause */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_bind.rq", "/tests/basic/query_bind.srx", false, true);
	}

	@Test
	public void testRepositoryConnectionApi() throws Exception {

		prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		IRI bob = Values.iri("http://example.org/bob");
		IRI alice = Values.iri("http://example.org/alice");
		IRI graph1 = Values.iri("http://example.org/graph1");
		IRI graph2 = Values.iri("http://example.org/graph2");

		try (RepositoryConnection conn = repo1.getConnection()) {
			conn.add(bob, RDF.TYPE, FOAF.PERSON, graph1);
			conn.add(bob, FOAF.NAME, Values.literal("Bob"), graph1);
		}

		try (RepositoryConnection conn = repo2.getConnection()) {
			conn.add(alice, RDF.TYPE, FOAF.PERSON, graph2);
			conn.add(alice, FOAF.NAME, Values.literal("Alice"), graph2);
		}

		var fedxRepo = fedxRule.getRepository();

		try (var conn = fedxRepo.getConnection()) {

			// hasStatement which exist
			Assertions.assertTrue(conn.hasStatement(bob, RDF.TYPE, FOAF.PERSON, false));
			Assertions.assertTrue(conn.hasStatement(bob, RDF.TYPE, FOAF.PERSON, false, graph1));
			Assertions.assertTrue(conn.hasStatement(null, RDF.TYPE, FOAF.PERSON, false));
			Assertions.assertTrue(conn.hasStatement(null, RDF.TYPE, FOAF.PERSON, false, graph1));
			Assertions.assertTrue(conn.hasStatement(null, RDF.TYPE, null, false));
			Assertions.assertTrue(conn.hasStatement(null, RDF.TYPE, null, false, graph1));
			Assertions.assertTrue(conn.hasStatement(null, RDF.TYPE, null, false, graph2));
			Assertions.assertTrue(conn.hasStatement(null, null, null, false));
			Assertions.assertTrue(conn.hasStatement(null, null, null, false, graph1));

			// hasStatement which do not exist
			Assertions.assertFalse(conn.hasStatement(bob, RDF.TYPE, FOAF.ORGANIZATION, false));
			Assertions.assertFalse(conn.hasStatement(bob, RDF.TYPE, FOAF.PERSON, false, graph2));

			// getStatements
			Assertions.assertEquals(Set.of(bob, alice),
					QueryResults.asModel(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, false)).subjects());
			Assertions.assertEquals(Set.of(bob),
					QueryResults.asModel(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, false, graph1)).subjects());
			Assertions.assertEquals(Set.of(bob, alice),
					QueryResults.asModel(conn.getStatements(null, null, null, false)).subjects());
			Assertions.assertEquals(Set.of(bob),
					QueryResults.asModel(conn.getStatements(null, null, null, false, graph1)).subjects());
		}
	}

	@Test
	public void testFederationSubSetQuery() throws Exception {
		String ns1 = "http://namespace1.org/";
		String ns2 = "http://namespace2.org/";
		List<Endpoint> endpoints = prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl",
				"/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?person WHERE { ?person a <http://xmlns.com/foaf/0.1/Person> }");

			try (TupleQueryResult result = tq.evaluate()) {

				try (TupleQueryResult expected = tupleQueryResultBuilder(List.of("person"))
						.add(List.of(vf.createIRI(ns1, "Person_1")))
						.add(List.of(vf.createIRI(ns1, "Person_2")))
						.add(List.of(vf.createIRI(ns1, "Person_3")))
						.add(List.of(vf.createIRI(ns1, "Person_4")))
						.add(List.of(vf.createIRI(ns1, "Person_5")))
						.add(List.of(vf.createIRI(ns2, "Person_6")))
						.add(List.of(vf.createIRI(ns2, "Person_7")))
						.add(List.of(vf.createIRI(ns2, "Person_8")))
						.add(List.of(vf.createIRI(ns2, "Person_9")))
						.add(List.of(vf.createIRI(ns2, "Person_10")))
						.build()) {

					compareTupleQueryResults(result, expected, false);
				}
			}

			// evaluate against ep 1 and ep 3 only
			FedXDataset fedxDataset = new FedXDataset(tq.getDataset());
			fedxDataset.addEndpoint(endpoints.get(0).getId());
			fedxDataset.addEndpoint(endpoints.get(2).getId());
			tq.setDataset(fedxDataset);
			try (TupleQueryResult result = tq.evaluate()) {

				try (TupleQueryResult expected = tupleQueryResultBuilder(List.of("person"))
						.add(List.of(vf.createIRI(ns1, "Person_1")))
						.add(List.of(vf.createIRI(ns1, "Person_2")))
						.add(List.of(vf.createIRI(ns1, "Person_3")))
						.add(List.of(vf.createIRI(ns1, "Person_4")))
						.add(List.of(vf.createIRI(ns1, "Person_5")))
						.build()) {

					compareTupleQueryResults(result, expected, false);
				}
			}
		}

	}

	@Test
	public void testFederationSubSetQueryWithDataset() throws Exception {
		String ns1 = "http://namespace1.org/";
		String ns2 = "http://namespace2.org/";
		prepareTest(Arrays.asList("/tests/data/data1.trig", "/tests/data/data2.ttl"));
		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?person WHERE { ?person a <http://xmlns.com/foaf/0.1/Person> }");

			try (TupleQueryResult result = tq.evaluate()) {

				try (TupleQueryResult expected = tupleQueryResultBuilder(List.of("person"))
						.add(List.of(vf.createIRI(ns1, "Person_1")))
						.add(List.of(vf.createIRI(ns1, "Person_2")))
						.add(List.of(vf.createIRI(ns1, "Person_3")))
						.add(List.of(vf.createIRI(ns1, "Person_4")))
						.add(List.of(vf.createIRI(ns1, "Person_5")))
						.add(List.of(vf.createIRI(ns2, "Person_6")))
						.add(List.of(vf.createIRI(ns2, "Person_7")))
						.add(List.of(vf.createIRI(ns2, "Person_8")))
						.add(List.of(vf.createIRI(ns2, "Person_9")))
						.add(List.of(vf.createIRI(ns2, "Person_10")))
						.build()) {

					compareTupleQueryResults(result, expected, false);
				}
			}

			// evaluate against ep 1 and ep 3 only
			SimpleDataset fedxDataset = new SimpleDataset();
			fedxDataset.addDefaultGraph(vf.createIRI(ns1, "PG1"));
			tq.setDataset(fedxDataset);
			try (TupleQueryResult result = tq.evaluate()) {

				try (TupleQueryResult expected = tupleQueryResultBuilder(List.of("person"))
						.add(List.of(vf.createIRI(ns1, "Person_1")))
						.build()) {

					compareTupleQueryResults(result, expected, false);
				}
			}
		}

	}

	@Test
	public void testQueryBinding() throws Exception {

		final QueryManager qm = federationContext().getQueryManager();

		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		String queryString = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\n" +
				"SELECT ?name WHERE {\r\n" +
				" ?person a foaf:Person .\r\n" +
				" ?person foaf:name ?name .\r\n" +
				"}";
		TupleQuery query = qm.prepareTupleQuery(queryString);
		query.setBinding("person", vf.createIRI("http://namespace1.org/", "Person_1"));

		try (TupleQueryResult actual = query.evaluate();

				TupleQueryResult expected = tupleQueryResultBuilder(List.of("name"))
						.add(List.of(vf.createLiteral("Person1")))
						.build()) {

			compareTupleQueryResults(actual, expected, false);
		}
	}

	@Test
	public void testQueryWithLimit() throws Exception {

		final QueryManager qm = federationContext().getQueryManager();

		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		String queryString = readQueryString("/tests/basic/query_limit01.rq");

		evaluateQueryPlan("/tests/basic/query_limit01.rq", "/tests/basic/query_limit01.qp");

		TupleQuery query = qm.prepareTupleQuery(queryString);

		try (TupleQueryResult actual = query.evaluate()) {
			if (actual.hasNext()) {
				@SuppressWarnings("unused")
				BindingSet firstResult = actual.next();
			}
			if (actual.hasNext()) {
				throw new Exception("Expected single result due to LIMIT 1");
			}
		}
	}

	@Test
	public void testBeginTransaction() throws Exception {

		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		Assertions.assertEquals(
				1, Repositories
						.tupleQueryNoTransaction(fedxRule.repository, "SELECT ?person WHERE { ?person ?p 'Alan' }",
								it -> QueryResults.asList(it))
						.size());

		Assertions.assertEquals(1,
				Repositories.tupleQuery(fedxRule.repository, "SELECT ?person WHERE { ?person ?p 'Alan' }",
						it -> QueryResults.asList(it)).size());
	}

	@Test
	public void testSingleSource_SetBinding() throws Exception {

		/* test a single source select query where we set a binding */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// SELECT query
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?person WHERE { ?person <http://xmlns.com/foaf/0.1/name> ?name }");
			tq.setBinding("name", l("Alan"));
			TupleQueryResult tqr = tq.evaluate();
			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "person", Sets.newHashSet(iri("http://example.org/", "a")));

			// CONSTRUCT query
			GraphQuery gq = conn.prepareGraphQuery(
					"CONSTRUCT { ?person <http://xmlns.com/foaf/0.1/name> ?name } WHERE { ?person <http://xmlns.com/foaf/0.1/name> ?name }");
			gq.setBinding("name", l("Alan"));
			GraphQueryResult gqr = gq.evaluate();
			List<Statement> stmts = Iterations.asList(gqr);
			Assertions.assertEquals(1, stmts.size());
			Assertions.assertEquals(iri("http://example.org/", "a"), stmts.get(0).getSubject());

			// BOOLEAN query
			BooleanQuery bq = conn.prepareBooleanQuery("ASK { ?person <http://xmlns.com/foaf/0.1/name> ?name }");
			bq.setBinding("name", l("non-existing-name"));
			Assertions.assertEquals(false, bq.evaluate());

		}

	}

	@Test
	public void testPassThroughHandler_MultiSourceQuery() throws Exception {

		/* test query with custom RDF handler */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// SELECT query
			TupleQuery tq = conn
					.prepareTupleQuery(
							"SELECT ?person ?interest WHERE { ?person <http://xmlns.com/foaf/0.1/name> ?name ; <http://xmlns.com/foaf/0.1/interest> ?interest }");
			tq.setBinding("name", l("Alan"));

			AtomicBoolean started = new AtomicBoolean(false);
			AtomicInteger numberOfResults = new AtomicInteger(0);

			tq.evaluate(new AbstractTupleQueryResultHandler() {

				@Override
				public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
					if (started.get()) {
						throw new IllegalStateException("Must not start query result twice.");
					}
					started.set(true);

					/*
					 * Expected trace looks like this java.lang.Exception at
					 * org.eclipse.rdf4j.federated.BasicTests$1.startQueryResult(BasicTests.java:276) at
					 * org.eclipse.rdf4j.query.QueryResults.report(QueryResults.java:263) at
					 * org.eclipse.rdf4j.federated.structures.FedXTupleQuery.evaluate(FedXTupleQuery.java:69)
					 */
					Assertions.assertEquals(QueryResults.class.getName(),
							new Exception().getStackTrace()[1].getClassName());

				}

				@Override
				public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
					Assertions.assertEquals(bs.getValue("person"), iri("http://example.org/", "a"));
					Assertions.assertEquals(bs.getValue("interest").stringValue(), "SPARQL 1.1 Basic Federated Query");
					numberOfResults.incrementAndGet();
				}
			});

			Assertions.assertTrue(started.get());
			Assertions.assertEquals(1, numberOfResults.get());
		}
	}

	@Test
	public void testPassThroughHandler_SingleSourceQuery() throws Exception {

		/* test query with custom RDF handler */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// SELECT query
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?person WHERE { ?person <http://xmlns.com/foaf/0.1/name> ?name . }");
			tq.setBinding("name", l("Alan"));

			AtomicBoolean started = new AtomicBoolean(false);
			AtomicInteger numberOfResults = new AtomicInteger(0);

			tq.evaluate(new AbstractTupleQueryResultHandler() {

				@Override
				public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
					if (started.get()) {
						throw new IllegalStateException("Must not start query result twice.");
					}
					started.set(true);

					/*
					 * Expected trace is expected to come from some original repository (e.g. SPARQL) => we explicitly
					 * do not expect QueryResults#report to be the second element (compare test
					 * testPassThroughHandler_MultiSourceQuery)
					 */
					Assertions.assertNotEquals(QueryResults.class, new Exception().getStackTrace()[1].getClass());

				}

				@Override
				public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
					Assertions.assertEquals(bs.getValue("person"), iri("http://example.org/", "a"));
					numberOfResults.incrementAndGet();
				}
			});

			Assertions.assertTrue(started.get());
			Assertions.assertEquals(1, numberOfResults.get());
		}
	}

	@Test
	public void testPassThroughHandler_EmptyResult() throws Exception {

		/* test query with custom RDF handler */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// SELECT query
			TupleQuery tq = conn
					.prepareTupleQuery(
							"SELECT ?person ?interest WHERE { ?person <http://xmlns.com/foaf/0.1/name> ?name ; <http://xmlns.com/foaf/0.1/interest> ?interest }");
			tq.setBinding("name", l("NotExist"));

			AtomicBoolean started = new AtomicBoolean(false);

			tq.evaluate(new AbstractTupleQueryResultHandler() {

				@Override
				public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
					if (started.get()) {
						throw new IllegalStateException("Must not start query result twice.");
					}
					started.set(true);

					Assertions.assertEquals(Lists.newArrayList("person", "interest"), bindingNames);
				}

				@Override
				public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
					throw new IllegalStateException("Expected empty result");
				}
			});

			Assertions.assertTrue(started.get());
		}
	}

	@Test
	public void testPassThroughHandler_emptySingleSourceQuery() throws Exception {

		/* test query with custom RDF handler */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// SELECT query
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?person WHERE { ?person <http://xmlns.com/foaf/0.1/name> ?name . }");
			tq.setBinding("name", l("notExist"));

			AtomicBoolean started = new AtomicBoolean(false);
			AtomicInteger numberOfResults = new AtomicInteger(0);

			tq.evaluate(new AbstractTupleQueryResultHandler() {

				@Override
				public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
					if (started.get()) {
						throw new IllegalStateException("Must not start query result twice.");
					}
					started.set(true);

					/*
					 * Expected trace is expected to come from some original repository (e.g. SPARQL) => we explicitly
					 * do not expect QueryResults#report to be the second element (compare test
					 * testPassThroughHandler_MultiSourceQuery)
					 */
					Assertions.assertNotEquals(QueryResults.class, new Exception().getStackTrace()[1].getClass());

				}

				@Override
				public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
					throw new IllegalStateException("Expected empty result");
				}
			});

			Assertions.assertTrue(started.get());
			Assertions.assertEquals(0, numberOfResults.get());
		}
	}

	@Test
	public void testDescribe_SingleResource() throws Exception {

		/* test DESCRIBE query for a single resource (data in two members) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_describe1.rq", "/tests/basic/query_describe1.ttl", false, true);
	}

	@Test
	public void testDescribe_MultipleResources() throws Exception {

		/* test DESCRIBE query for multiple resources (data in two members) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_describe2.rq", "/tests/basic/query_describe2.ttl", false, true);
	}

	@Test
	public void testDescribe_SingleSource() throws Exception {

		/* test DESCRIBE query for a single resource (one federation member to simulate single source) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl"));
		execute("/tests/basic/query_describe1.rq", "/tests/basic/query_describe1_singleSource.ttl", false, true);
	}

	@Test
	public void test_EscapingQuotedLiteral() throws Exception {

		IRI publication = Values.iri("http://example.org/mypublication");
		Literal quotedTitle = Values.literal("'A publication with quoted (') title'");

		/* add two members */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		// add some additional quoted literal
		try (RepositoryConnection conn1 = getRepository(1).getConnection()) {
			conn1.add(publication, DCTERMS.TITLE, quotedTitle);
		}

		// add data to second endpoint to do a join with the literal
		try (RepositoryConnection conn2 = getRepository(2).getConnection()) {
			conn2.add(publication, RDFS.COMMENT, quotedTitle);
		}

		try (RepositoryConnection conn = fedxRule.getRepository().getConnection()) {

			// SELECT query (with an artificial join on ?title to simulate the escaping issue
			TupleQuery tq = conn
					.prepareTupleQuery(
							"SELECT * WHERE { ?publication <http://purl.org/dc/terms/title> ?title ; rdfs:comment ?title }");

			try (TupleQueryResult tqr = tq.evaluate()) {
				while (tqr.hasNext()) {

					BindingSet bs = tqr.next();
					Assertions.assertEquals(publication, bs.getValue("publication"));
					Assertions.assertEquals(quotedTitle, bs.getValue("title"));
					Assertions.assertFalse(tqr.hasNext(), "Result is expected to have a single result");
				}
			}
		}

	}

	@Test
	public void test_reduceFederation() throws Exception {

		List<Endpoint> endpoints = prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		String repo1Id = endpoints.get(0).getId();

		IRI graph1 = Values.iri("http://example.org/graph1");
		IRI graph2 = Values.iri("http://example.org/graph2");

		try (RepositoryConnection con = repo1.getConnection()) {
			con.add(Values.iri("http://example.org/repo1/p1"), RDF.TYPE, FOAF.PERSON, graph1);
			con.add(Values.iri("http://example.org/repo2/p2"), RDF.TYPE, FOAF.PERSON, graph2);
		}

		try (RepositoryConnection con = repo2.getConnection()) {
			con.add(Values.iri("http://example.org/repo2/p3"), RDF.TYPE, FOAF.PERSON, graph1);
		}

		Repository fedxRepo = fedxRule.getRepository();

		// 1: regular federation
		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
							+ "SELECT * WHERE { "
							+ "   ?subClass a foaf:Person. "
							+ " } "
			);

			// expect from both repos
			Assertions.assertEquals(3, QueryResults.asSet(tupleQuery.evaluate()).size());

			// now we scope it additional to graph1
			tupleQuery = con.prepareTupleQuery(
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
							+ "SELECT * FROM <http://example.org/graph1> WHERE { "
							+ "   ?subClass a foaf:Person. "
							+ " } ");

			// expect results defined in graph1 (1 in repo1, 2 from repo2)
			Assertions.assertEquals(2, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

		// 2: reduce to federation member 1 id
		// 2a: additionall restrict to named graph
		FedXDataset fedXDataset = new FedXDataset(new SimpleDataset());
		fedXDataset.addEndpoint(repo1Id);

		try (RepositoryConnection con = fedxRepo.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
							+ "SELECT * WHERE { "
							+ "   ?subClass a foaf:Person. "
							+ " } "
			);
			tupleQuery.setDataset(fedXDataset);

			// expect result from repo 1
			Assertions.assertEquals(2, QueryResults.asSet(tupleQuery.evaluate()).size());

			// now we scope it additional to graph1
			tupleQuery = con.prepareTupleQuery(
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
							+ "SELECT * FROM <http://example.org/graph1> WHERE { "
							+ "   ?subClass a foaf:Person. "
							+ " } ");
			tupleQuery.setDataset(fedXDataset);

			// expect result from graph1 from repo1
			Assertions.assertEquals(1, QueryResults.asSet(tupleQuery.evaluate()).size());
		}

	}
}
