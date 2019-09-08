package com.fluidops.fedx;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class QueryPlanTest extends SPARQLBaseTest {

	
	@Test
	public void testQueryPlan_q03() throws Exception {
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl", "/tests/medium/data4.ttl"));
		evaluateQueryPlan("/tests/medium/query03.rq", "/tests/medium/query03.qp");
	}
}
