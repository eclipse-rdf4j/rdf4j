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

import org.junit.jupiter.api.Test;

public class DistinctTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {
		prepareTest(Arrays.asList("/tests/data/distinctTest.ttl", "/tests/data/distinctTest1.ttl"));
		execute("/tests/basic/query_distinct01.rq", "/tests/basic/query_distinct01.srx", false, true);
	}

	@Test
	public void test2() throws Exception {
		/* test more complex structure for distinct */
		prepareTest(Arrays.asList("/tests/data/distinctTest02a.ttl", "/tests/data/distinctTest02b.ttl"));
		execute("/tests/basic/query_distinct02.rq", "/tests/basic/query_distinct02.srx", false, true);
	}

	@Test
	public void test3() throws Exception {
		/* test for subquery with no projection variables */
		prepareTest(Arrays.asList("/tests/data/distinctTest03a.ttl", "/tests/data/distinctTest03b.ttl"));
		execute("/tests/basic/query_distinct03.rq", "/tests/basic/query_distinct03.srx", false, true);
	}

	@Test
	public void test4() throws Exception {
		/* test for subquery with Filter */
		prepareTest(Arrays.asList("/tests/data/distinctTest03a.ttl", "/tests/data/distinctTest03b.ttl"));
		execute("/tests/basic/query_distinct04.rq", "/tests/basic/query_distinct04.srx", false, true);
	}

	@Test
	public void test5() throws Exception {
		/* test for fetching full data: no distinct in subqueries (fixed in FedX 3.0) */
		prepareTest(Arrays.asList("/tests/data/distinctTest04a.ttl", "/tests/data/distinctTest04b.ttl"));
		execute("/tests/basic/query_distinct05.rq", "/tests/basic/query_distinct05.srx", false, true);
	}

	@Test
	public void test5a() throws Exception {
		/* test for fetching full data: no distinct in subqueries, with DISTINCT (fixed in FedX 3.0) */
		prepareTest(Arrays.asList("/tests/data/distinctTest04a.ttl", "/tests/data/distinctTest04b.ttl"));
		execute("/tests/basic/query_distinct05a.rq", "/tests/basic/query_distinct05a.srx", false, true);
	}
}
