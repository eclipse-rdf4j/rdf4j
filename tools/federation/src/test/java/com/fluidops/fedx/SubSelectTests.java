package com.fluidops.fedx;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class SubSelectTests extends SPARQLBaseTest {

	
	@Test
	public void test1()  throws Exception {
		
		/* test select query retrieving all persons (2 endpoints) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data4.ttl"));
		execute("/tests/subselects/query01.rq", "/tests/subselects/query01.srx", false);			
	}
}
