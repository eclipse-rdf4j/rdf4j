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
package org.eclipse.rdf4j.federated.optimizer;

import java.util.Arrays;

import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.junit.jupiter.api.Test;

public class OptimizerTest extends SPARQLBaseTest {

	@Test
	public void testLocalVars_Optional1() throws Exception {

		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));

		evaluateQueryPlan("/tests/basic/query_optional01.rq", "/tests/optimizer/queryPlan_Optional1");
	}

	@Test
	public void testLocalVars_Optional2() throws Exception {

		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));

		evaluateQueryPlan("/tests/basic/query_optional02.rq", "/tests/optimizer/queryPlan_Optional2");
	}

	@Test
	public void testOptimizer_QueryWithComment() throws Exception {

		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));

		evaluateQueryPlan("/tests/basic/query01.rq", "/tests/optimizer/queryPlan_query01");
	}
}
