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

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class OptionalTests extends SPARQLBaseTest {

	@BeforeEach
	public void before() {
		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("owl", OWL.NAMESPACE);
		qm.addPrefixDeclaration("rdfs", RDFS.NAMESPACE);
		qm.addPrefixDeclaration("foaf", FOAF.NAMESPACE);
	}

	@Test
	public void test1() throws Exception {

		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));
		execute("/tests/basic/query_optional01.rq", "/tests/basic/query_optional01.srx", false, true);
	}

	@Test
	public void test2() throws Exception {

		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));
		execute("/tests/basic/query_optional02.rq", "/tests/basic/query_optional02.srx", false, true);
	}

	@Test
	public void test3() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data4.ttl"));

		String query = "SELECT * WHERE{ " +
				"	?person a foaf:Person . " +
				"	OPTIONAL { ?person foaf:name ?name } ." +
				"	OPTIONAL { ?person foaf:age ?age } . " +
				"	?author owl:sameAs ?person ." +
				"}";

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "name",
					Sets.newHashSet(l("Person2"), l("Person5")));
			Assertions.assertTrue(res.stream()
					.anyMatch(b -> b.getValue("name")
							.equals(l("Person2")) && b.getValue("age").stringValue().equals("27")));
			Assertions.assertTrue(res.stream()
					.anyMatch(b -> b.getValue("name")
							.equals(l("Person5")) && b.getValue("age") == null));
		}
	}

}
