package com.fluidops.fedx;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class SPARQL1_1Tests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {
		
		/* test select query with sum */
		prepareTest(Arrays.asList("/tests/sparql1_1/data01endpoint1.ttl", "/tests/sparql1_1/data01endpoint2.ttl"));
		execute("/tests/sparql1_1/query01.rq", "/tests/sparql1_1/query01.srx", false);			
	}
	
	
	@Test
	public void test2() throws Exception {
		
		/* test select query with concat */
		prepareTest(Arrays.asList("/tests/sparql1_1/data01endpoint1.ttl", "/tests/sparql1_1/data01endpoint2.ttl"));
		execute("/tests/sparql1_1/query02.rq", "/tests/sparql1_1/query02.srx", false);			
	}
}
