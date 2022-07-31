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

public class QueryPlanTest extends SPARQLBaseTest {

	@Test
	public void testQueryPlan_q03() throws Exception {
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		evaluateQueryPlan("/tests/medium/query03.rq", "/tests/medium/query03.qp");
	}

	@Test
	public void testQueryPlan_joinOrderBind() throws Exception {
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		evaluateQueryPlan("/tests/optimizer/queryPlan_bind.rq", "/tests/optimizer/queryPlan_bind.qp");
	}

}
