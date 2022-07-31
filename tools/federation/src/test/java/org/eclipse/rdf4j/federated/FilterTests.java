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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class FilterTests extends SPARQLBaseTest {

	@Test
	public void testSimpleFilter() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		evaluateQueryPlan("/tests/filter/query01.rq", "/tests/filter/query01.qp");
		execute("/tests/filter/query01.rq", "/tests/filter/query01.srx", false, true);
	}

	@Test
	public void testSimpleFilter_ExclusiveStatement() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		evaluateQueryPlan("/tests/filter/query01a.rq", "/tests/filter/query01a.qp");
		execute("/tests/filter/query01a.rq", "/tests/filter/query01a.srx", false, true);
	}

	@Test
	public void testOrFilter() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query02.rq", "/tests/filter/query02.srx", false, true);
	}

	@Test
	public void testAndFilter() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query03.rq", "/tests/filter/query03.srx", false, true);
	}

	@Test
	public void testAndFilter2() throws Exception {
		/* test insertion of resource filter into query */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query04.rq", "/tests/filter/query04.srx", false, true);
	}

	@Test
	public void testAndFilter3() throws Exception {
		/* test range filter with integers */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query05.rq", "/tests/filter/query05.srx", false, true);
	}

	@Test
	public void testFilterPushing() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data4.ttl"));
		execute("/tests/filter/query06.rq", "/tests/filter/query06.srx", false, true);
	}

	@Test
	public void testSimpleFilterExclusiveGroup() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));

		String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX ns1: <http://namespace1.org/> "
				+ "SELECT * WHERE { "
				+ "?person a foaf:Person ."
				+ "?person foaf:name \"Person1\" .\n"
				+ "?person foaf:age 20 . "
				+ "FILTER (?person=ns1:Person_1)"
				+ "}";

		QueryManager queryManager = federationContext().getQueryManager();
		TupleQuery prepareTupleQuery = queryManager.prepareTupleQuery(query);
		try (TupleQueryResult tqr = prepareTupleQuery.evaluate()) {

			List<BindingSet> res = Iterations.asList(tqr);
			assertContainsAll(res, "person",
					Sets.newHashSet(iri("http://namespace1.org/", "Person_1")));
		}
	}

	@Test
	public void testFilter_ExclusiveGroup_Regex() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data3.ttl"));

		execute("/tests/filter/query07.rq", "/tests/filter/query07.srx", false, true);
		evaluateQueryPlan("/tests/filter/query07.rq", "/tests/filter/query07.qp");
	}
}
