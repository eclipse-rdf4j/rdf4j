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

public class SPARQL1_1Tests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {

		/* test select query with sum */
		prepareTest(Arrays.asList("/tests/sparql1_1/data01endpoint1.ttl", "/tests/sparql1_1/data01endpoint2.ttl"));
		execute("/tests/sparql1_1/query01.rq", "/tests/sparql1_1/query01.srx", false, true);
	}

	@Test
	public void test2() throws Exception {

		/* test select query with concat */
		prepareTest(Arrays.asList("/tests/sparql1_1/data01endpoint1.ttl", "/tests/sparql1_1/data01endpoint2.ttl"));
		execute("/tests/sparql1_1/query02.rq", "/tests/sparql1_1/query02.srx", false, true);
	}
}
