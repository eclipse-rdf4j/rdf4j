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

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LimitTests extends SPARQLBaseTest {

	@Test
	public void testLimitPushing_Select_SingleStatement() throws Exception {

		// datsets contain both instances of foaf:Person
		prepareTest(
				Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));

		Repository fedxRepo = fedxRule.getRepository();

		try (RepositoryConnection conn = fedxRepo.getConnection()) {

			String query = "SELECT * WHERE { ?person a <" + FOAF.PERSON.stringValue() + "> } LIMIT 2";
			TupleQuery tq = conn.prepareTupleQuery(query);
			Assertions.assertEquals(2, QueryResults.asList(tq.evaluate()).size());

			// check that the query plan contains information about limit
			String queryPlan = fedxRule.getFederationContext().getQueryManager().getQueryPlan(query);
			Assertions.assertTrue(queryPlan.contains("Upper Limit: 2"));
		}
	}

	@Test
	public void testLimitPushing_Ask_SingleStatement() throws Exception {

		// datsets contain both instances of foaf:Person
		prepareTest(
				Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));

		Repository fedxRepo = fedxRule.getRepository();

		try (RepositoryConnection conn = fedxRepo.getConnection()) {

			String query = "ASK { ?person a <" + FOAF.PERSON.stringValue() + "> }";
			Assertions.assertTrue(conn.prepareBooleanQuery(query).evaluate());

			// check that the query plan contains information about limit
			String queryPlan = fedxRule.getFederationContext().getQueryManager().getQueryPlan(query);
			Assertions.assertTrue(queryPlan.contains("Upper Limit: 1"));

			// also run a query with no backing data
			query = "ASK { ?organization a <" + FOAF.ORGANIZATION.stringValue() + "> }";
			Assertions.assertFalse(conn.prepareBooleanQuery(query).evaluate());
		}
	}
}
