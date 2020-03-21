/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.structures.FedXDataset;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BasicTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query01.rq", "/tests/basic/query01.srx", false);
	}

	@Test
	public void test2() throws Exception {
		/* test a basic Construct query retrieving all triples */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query02.rq", "/tests/basic/query02.ttl", false);
	}

	@Test
	public void testBooleanTrueSingleSource() throws Exception {
		/* test a basic boolean query (result true) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query03.rq", "/tests/basic/query03.srx", false);
	}

	@Test
	public void testBooleanTrueMultipleSource() throws Exception {
		/* test a basic boolean query (result true) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query03a.rq", "/tests/basic/query03.srx", false);
	}

	@Test
	public void testBooleanFalse() throws Exception {
		/* test a basic boolean query (result false) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query04.rq", "/tests/basic/query04.srx", false);
	}

	@Test
	public void testSingleSourceSelect() throws Exception {
		/* test a single source select query */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_singleSource01.rq", "/tests/basic/query_singleSource01.srx", false);
	}

	@Test
	public void testSingleSourceConstruct() throws Exception {
		/* test a single source construct */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_singleSource02.rq", "/tests/basic/query_singleSource02.ttl", false);
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
		execute("/tests/basic/query_values.rq", "/tests/basic/query_values.srx", false);
	}

	@Test
	public void testBindClause() throws Exception {

		/* test query with bind clause */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_bind.rq", "/tests/basic/query_bind.srx", false);
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
			TupleQueryResult result = tq.evaluate();

			TupleQueryResult expected = tupleQueryResultBuilder(Arrays.asList("person"))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_1")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_2")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_3")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_4")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_5")))
					.add(Arrays.asList(vf.createIRI(ns2, "Person_6")))
					.add(Arrays.asList(vf.createIRI(ns2, "Person_7")))
					.add(Arrays.asList(vf.createIRI(ns2, "Person_8")))
					.add(Arrays.asList(vf.createIRI(ns2, "Person_9")))
					.add(Arrays.asList(vf.createIRI(ns2, "Person_10")))
					.build();

			compareTupleQueryResults(result, expected, false);

			// evaluate against ep 1 and ep 3 only
			FedXDataset fedxDataset = new FedXDataset(tq.getDataset());
			fedxDataset.addEndpoint(endpoints.get(0).getId());
			fedxDataset.addEndpoint(endpoints.get(2).getId());
			tq.setDataset(fedxDataset);
			result = tq.evaluate();

			expected = tupleQueryResultBuilder(Arrays.asList("person"))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_1")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_2")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_3")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_4")))
					.add(Arrays.asList(vf.createIRI(ns1, "Person_5")))
					.build();

			compareTupleQueryResults(result, expected, false);
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

		TupleQueryResult actual = query.evaluate();

		TupleQueryResult expected = tupleQueryResultBuilder(Arrays.asList("name"))
				.add(Arrays.asList(vf.createLiteral("Person1")))
				.build();

		compareTupleQueryResults(actual, expected, false);
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
}
