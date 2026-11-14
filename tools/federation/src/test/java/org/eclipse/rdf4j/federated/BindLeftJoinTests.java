/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.structures.SubQuery;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class BindLeftJoinTests extends SPARQLBaseTest {

	@Override
	protected void initFedXConfig() {

		fedxRule.withConfiguration(config -> {
			config.withEnableMonitoring(true);
		});
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"true | ?weight > 114 | true",
			"false | ?weight > 114 | true",
			"true | STR(?weight) = \"not-the-weight\" | false",
			"false | STR(?weight) = \"not-the-weight\" | false"
	})
	public void testOptionalFilterCondition(boolean bindLeftJoinOptimizationEnabled, String condition,
			boolean numericWeightMatches) throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));
		fedxRule.setConfig(config -> config.withEnableOptionalAsBindJoin(bindLeftJoinOptimizationEnabled));

		var accepted = Values.iri("urn:accepted");
		var rejected = Values.iri("urn:rejected");
		var missing = Values.iri("urn:missing");
		var kind = Values.iri("urn:kind");
		var drug = Values.iri("urn:Drug");
		var weight = Values.iri("urn:weight");
		var numericWeight = Values.literal(200);

		try (RepositoryConnection conn = getRepository(1).getConnection()) {
			conn.add(accepted, kind, drug);
			conn.add(rejected, kind, drug);
			conn.add(missing, kind, drug);
		}
		try (RepositoryConnection conn = getRepository(2).getConnection()) {
			conn.add(accepted, weight, numericWeight);
			conn.add(rejected, weight, Values.literal("247.1419"));
		}

		String query = """
				SELECT ?drug ?weight WHERE {
				  ?drug <urn:kind> <urn:Drug> .
				  OPTIONAL {
				    ?drug <urn:weight> ?weight .
				    FILTER (%s)
				  }
				}
				ORDER BY ?drug
				""".formatted(condition);
		try (RepositoryConnection conn = fedxRule.getRepository().getConnection();
				TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			var rows = Iterations.asList(result);
			Assertions.assertEquals(3, rows.size());
			Assertions.assertEquals(accepted, rows.get(0).getValue("drug"));
			Assertions.assertEquals(numericWeightMatches ? numericWeight : null, rows.get(0).getValue("weight"));
			Assertions.assertEquals(missing, rows.get(1).getValue("drug"));
			Assertions.assertFalse(rows.get(1).hasBinding("weight"));
			Assertions.assertEquals(rejected, rows.get(2).getValue("drug"));
			Assertions.assertFalse(rows.get(2).hasBinding("weight"), () -> rows.get(2).toString());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void test_leftBindJoin_basic(boolean bindLeftJoinOptimizationEnabled) throws Exception {

		prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl",
						"/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);
		Repository repo3 = getRepository(3);

		Repository fedxRepo = fedxRule.getRepository();

		fedxRule.setConfig(config -> {
			config.withBoundJoinBlockSize(10);
			config.withEnableOptionalAsBindJoin(bindLeftJoinOptimizationEnabled);
		});

		// add some persons
		try (RepositoryConnection conn = repo1.getConnection()) {

			for (int i = 1; i <= 30; i++) {
				var p = Values.iri("http://ex.com/p" + i);
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(p, OWL.SAMEAS, otherP);
			}
		}

		// add names for person 1, 4, 7, ...
		try (RepositoryConnection conn = repo2.getConnection()) {

			for (int i = 1; i <= 30; i += 3) {
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(otherP, FOAF.NAME, Values.literal("Person " + i));
			}
		}

		// add names for person 2, 5, 8, ...
		try (RepositoryConnection conn = repo3.getConnection()) {

			for (int i = 2; i <= 30; i += 3) {
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(otherP, FOAF.NAME, Values.literal("Person " + i));
			}
		}

		try {
			// run query which joins results from multiple repos
			// for a subset of persons there exist names
			try (RepositoryConnection conn = fedxRepo.getConnection()) {
				String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
						"SELECT * WHERE { "
						+ " ?person owl:sameAs ?otherPerson . "
						+ " OPTIONAL { ?otherPerson foaf:name ?name .  } " // # @repo2 and @repo3
						+ "}";

				TupleQuery tupleQuery = conn.prepareTupleQuery(query);
				try (TupleQueryResult tqr = tupleQuery.evaluate()) {
					var bindings = Iterations.asList(tqr);

					Assertions.assertEquals(30, bindings.size());

					for (int i = 1; i <= 30; i++) {
						var p = Values.iri("http://ex.com/p" + i);
						var otherP = Values.iri("http://other.com/p" + i);

						// find the bindingset for the person in the unordered result
						BindingSet bs = bindings.stream()
								.filter(b -> b.getValue("person").equals(p))
								.findFirst()
								.orElseThrow();

						Assertions.assertEquals(otherP, bs.getValue("otherPerson"));
						if (i % 3 == 1 || i % 3 == 2) {
							// names from repo 2 or 3
							Assertions.assertEquals("Person " + i, bs.getValue("name").stringValue());
						} else {
							// no name for others
							Assertions.assertFalse(bs.hasBinding("name"));
						}
					}
				}

			}

			if (bindLeftJoinOptimizationEnabled) {
				assertNumberOfRequests("endpoint1", 3);
				assertNumberOfRequests("endpoint2", 5);
				assertNumberOfRequests("endpoint3", 5);
			} else {
				assertNumberOfRequests("endpoint1", 3);
				assertNumberOfRequests("endpoint2", 32);
				assertNumberOfRequests("endpoint3", 32);
			}

		} finally {
			fedxRepo.shutDown();
		}

	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testBoundLeftJoin_stmt_nonExclusive_boundCheck(boolean bindLeftJoinOptimizationEnabled)
			throws Exception {

		prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl",
						"/tests/basic/data_emptyStore.ttl"));

		// test scenario:
		// 3 repositories, 30 persons, bind join size 10, names distributed in repo 2
		// and repo 3
		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);
		Repository repo3 = getRepository(3);

		Repository fedxRepo = fedxRule.getRepository();

		fedxRule.setConfig(config -> {
			config.withBoundJoinBlockSize(10);
			config.withEnableOptionalAsBindJoin(bindLeftJoinOptimizationEnabled);
		});

		// add some persons
		try (RepositoryConnection conn = repo1.getConnection()) {

			for (int i = 1; i <= 30; i++) {
				var p = Values.iri("http://ex.com/p" + i);
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(p, OWL.SAMEAS, otherP);
			}
		}

		// add "male" for person 1, 4, 7, ...
		try (RepositoryConnection conn = repo2.getConnection()) {

			for (int i = 1; i <= 30; i += 3) {
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(otherP, FOAF.GENDER, Values.literal("male"));
			}
		}

		// add "female" for person 2, 5, 8, ...
		// add "male" for person 30
		try (RepositoryConnection conn = repo3.getConnection()) {

			for (int i = 2; i <= 30; i += 3) {
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(otherP, FOAF.GENDER, Values.literal("female"));
			}

			conn.add(Values.iri("http://other.com/p30"), FOAF.GENDER, Values.literal("male"));
		}

		try {
			// run query which joins results from multiple repos
			// for a subset of persons there exist names
			try (RepositoryConnection conn = fedxRepo.getConnection()) {
				String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
						+ "SELECT * WHERE { "
						+ " ?person owl:sameAs ?otherPerson . "
						+ "  OPTIONAL { "
						+ "    ?otherPerson foaf:gender \"male\" . " // # @repo2 and @repo3
						+ " } "
						+ "}";

				TupleQuery tupleQuery = conn.prepareTupleQuery(query);
				try (TupleQueryResult tqr = tupleQuery.evaluate()) {
					var bindings = Iterations.asList(tqr);

					Assertions.assertEquals(30, bindings.size());

					for (int i = 1; i <= 30; i++) {
						var p = Values.iri("http://ex.com/p" + i);
						var otherP = Values.iri("http://other.com/p" + i);

						// find the bindingset for the person in the unordered result
						BindingSet bs = bindings.stream()
								.filter(b -> b.getValue("person").equals(p))
								.findFirst()
								.orElseThrow();

						Assertions.assertEquals(otherP, bs.getValue("otherPerson"));
						Assertions.assertEquals(Set.of("person", "otherPerson"), bs.getBindingNames());
					}
				}

			}

			if (bindLeftJoinOptimizationEnabled) {
				assertNumberOfRequests("endpoint1", 3);
				assertNumberOfRequests("endpoint2", 5);
				assertNumberOfRequests("endpoint3", 5);
			} else {
				assertNumberOfRequests("endpoint1", 3);
				// Note: with the current implementation we cannot
				// make exact assertions for endpoint 2 and 3
				// this is because due to the check statement
				// not all requests are required
			}

		} finally {
			fedxRepo.shutDown();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void test_leftBindJoin_emptyOptional(boolean bindLeftJoinOptimizationEnabled) throws Exception {

		prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl",
						"/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);
		Repository repo3 = getRepository(3);

		Repository fedxRepo = fedxRule.getRepository();

		fedxRule.setConfig(config -> {
			config.withBoundJoinBlockSize(10);
			config.withEnableOptionalAsBindJoin(bindLeftJoinOptimizationEnabled);
		});

		// add some persons
		try (RepositoryConnection conn = repo1.getConnection()) {

			for (int i = 1; i <= 30; i++) {
				var p = Values.iri("http://ex.com/p" + i);
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(p, OWL.SAMEAS, otherP);
			}
		}

		// add names for person 1, 4, 7, ...
		try (RepositoryConnection conn = repo2.getConnection()) {

			for (int i = 1; i <= 30; i += 3) {
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(otherP, FOAF.NAME, Values.literal("Person " + i));
			}
		}

		// add names for person 2, 5, 8, ...
		try (RepositoryConnection conn = repo3.getConnection()) {

			for (int i = 2; i <= 30; i += 3) {
				var otherP = Values.iri("http://other.com/p" + i);
				conn.add(otherP, FOAF.NAME, Values.literal("Person " + i));
			}
		}

		try {
			// run query which joins results from multiple repos
			// for a subset of persons there exist names
			// the age does not exist for any person
			try (RepositoryConnection conn = fedxRepo.getConnection()) {
				String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
						"SELECT * WHERE { "
						+ " ?person owl:sameAs ?otherPerson . "
						+ " OPTIONAL { ?otherPerson foaf:name ?name .  } " // # @repo2 and @repo3
						+ " OPTIONAL { ?otherPerson foaf:age ?age . } " // # does not exist
						+ "}";

				TupleQuery tupleQuery = conn.prepareTupleQuery(query);
				try (TupleQueryResult tqr = tupleQuery.evaluate()) {
					var bindings = Iterations.asList(tqr);

					Assertions.assertEquals(30, bindings.size());

					for (int i = 1; i <= 30; i++) {
						var p = Values.iri("http://ex.com/p" + i);
						var otherP = Values.iri("http://other.com/p" + i);

						// find the bindingset for the person in the unordered result
						BindingSet bs = bindings.stream()
								.filter(b -> b.getValue("person").equals(p))
								.findFirst()
								.orElseThrow();

						Assertions.assertEquals(otherP, bs.getValue("otherPerson"));
						if (i % 3 == 1 || i % 3 == 2) {
							// names from repo 2 or 3
							Assertions.assertEquals("Person " + i, bs.getValue("name").stringValue());
						} else {
							// no name for others
							Assertions.assertFalse(bs.hasBinding("name"));
						}

						Assertions.assertEquals(otherP, bs.getValue("otherPerson"));
						Assertions.assertFalse(bs.hasBinding("age"));
					}
				}
			}

		} finally {
			fedxRepo.shutDown();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void test_leftBindJoin_emptyLeftArgumentAsExclusiveGroup(boolean bindLeftJoinOptimizationEnabled)
			throws Exception {

		var endpoints = prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		Repository fedxRepo = fedxRule.getRepository();

		fedxRule.setConfig(config -> {
			config.withBoundJoinBlockSize(10);
			config.withEnableOptionalAsBindJoin(bindLeftJoinOptimizationEnabled);
		});

		// add a person
		try (RepositoryConnection conn = repo1.getConnection()) {
			var p = Values.iri("http://ex.com/p1");
			var otherP = Values.iri("http://other.com/p1");
			conn.add(p, OWL.SAMEAS, otherP);
		}

		// add name for person 1
		try (RepositoryConnection conn = repo2.getConnection()) {
			var otherP = Values.iri("http://other.com/p1");
			conn.add(otherP, FOAF.NAME, Values.literal("Person 1"));
		}

		// mark that repo2 for some reason has foaf:age statements (e.g. old cache entry)
		Endpoint repo2Endpoint = endpoints.get(1);
		federationContext().getSourceSelectionCache()
				.updateInformation(new SubQuery(null, FOAF.AGE, null), repo2Endpoint, true);

		try {
			// run query which joins results from multiple repos
			// the age does not exist for any person
			try (RepositoryConnection conn = fedxRepo.getConnection()) {
				String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
						"SELECT * WHERE { "
						+ " ?person owl:sameAs ?otherPerson . "
						+ " OPTIONAL { ?otherPerson foaf:age ?age .  } " // age does not exist, however is marked as
																			// ExclusiveStatement
						+ "}";

				TupleQuery tupleQuery = conn.prepareTupleQuery(query);
				try (TupleQueryResult tqr = tupleQuery.evaluate()) {
					var bindings = Iterations.asList(tqr);

					Assertions.assertEquals(1, bindings.size());

					for (int i = 1; i <= 1; i++) {
						var p = Values.iri("http://ex.com/p" + i);
						var otherP = Values.iri("http://other.com/p" + i);

						// find the bindingset for the person in the unordered result
						BindingSet bs = bindings.stream()
								.filter(b -> b.getValue("person").equals(p))
								.findFirst()
								.orElseThrow();

						Assertions.assertEquals(otherP, bs.getValue("otherPerson"));

						Assertions.assertEquals(otherP, bs.getValue("otherPerson"));
						Assertions.assertFalse(bs.hasBinding("age"));
					}
				}
			}

		} finally {
			fedxRepo.shutDown();
		}
	}
}
