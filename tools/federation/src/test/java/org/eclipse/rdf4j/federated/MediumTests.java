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

public class MediumTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {

		/* test select query retrieving all persons (2 endpoints) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query01.rq", "/tests/medium/query01.srx", false, true);
	}

	@Test
	public void test2() throws Exception {

		/* test select query retrieving all projects (1 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query02.rq", "/tests/medium/query02.srx", false, true);
	}

	@Test
	public void test3() throws Exception {

		/* test select query retrieving all projects (3 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query03.rq", "/tests/medium/query03.srx", false, true);
	}

	@Test
	public void test4() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query04.rq", "/tests/medium/query04.srx", false, true);
	}

	@Test
	public void test5() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query05.rq", "/tests/medium/query05.srx", false, true);
	}

	@Test
	public void test6() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query06.rq", "/tests/medium/query06.srx", false, true);
	}

	@Test
	public void test7() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query07.rq", "/tests/medium/query07.srx", false, true);
	}

	@Test
	public void test8() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query08.rq", "/tests/medium/query08.srx", false, true);
	}

	@Test
	public void test9() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query09.rq", "/tests/medium/query09.srx", false, true);
	}

	@Test
	public void test10() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query10.rq", "/tests/medium/query10.srx", false, true);
	}

	@Test
	public void test11() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query11.rq", "/tests/medium/query11.srx", false, true);
	}

	@Test
	public void test12() throws Exception {

		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query12.rq", "/tests/medium/query12.srx", false, true);
	}

}
