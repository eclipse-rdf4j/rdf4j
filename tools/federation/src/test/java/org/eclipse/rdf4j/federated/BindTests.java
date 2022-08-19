/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class BindTests extends SPARQLBaseTest {

	@BeforeEach
	public void prepareData() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
	}

	@Test
	public void testSimple() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { BIND(20 AS ?age) . ?person foaf:age ?age }");
		assertContainsAll(res, "person", Sets.newHashSet(iri("http://namespace1.org/", "Person_1")));
	}

	@Test
	public void testConcat() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { <http://namespace1.org/Person_1> foaf:age ?age . BIND(CONCAT('age: ', str(?age)) AS ?outAge) }");

		assertContainsAll(res, "outAge", Sets.newHashSet(l("age: 20")));
	}

	@Test
	public void testRebind() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { <http://namespace1.org/Person_1> foaf:age ?age . BIND(str(?age) AS ?outAge) }");

		assertContainsAll(res, "outAge", Sets.newHashSet(l("20")));
	}

	@Test
	public void testMultiBind() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { BIND(20 AS ?age) . <http://namespace1.org/Person_1> foaf:age ?age . BIND(str(?age) AS ?outAge) }");

		assertContainsAll(res, "outAge", Sets.newHashSet(l("20")));
	}

	protected List<BindingSet> runQuery(String query) {
		String prefixes = "PREFIX : <http://example.org/> \n" +
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";
		query = prefixes + query;
		return Repositories.tupleQueryNoTransaction(this.fedxRule.repository, query, it -> QueryResults.asList(it));
	}

}
