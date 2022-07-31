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

public class SubSelectTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {

		/* test select query retrieving all persons (2 endpoints) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data4.ttl"));
		execute("/tests/subselects/query01.rq", "/tests/subselects/query01.srx", false, true);
	}
}
