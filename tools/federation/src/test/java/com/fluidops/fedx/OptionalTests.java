package com.fluidops.fedx;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class OptionalTests extends SPARQLBaseTest {


	@Test
	public void test1() throws Exception {
		
		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));
		execute("/tests/basic/query_optional01.rq", "/tests/basic/query_optional01.srx", false);
	}
	
	
	@Test
	public void test2() throws Exception {
		
		prepareTest(Arrays.asList("/tests/data/optional1.ttl", "/tests/data/optional2.ttl"));
		execute("/tests/basic/query_optional02.rq", "/tests/basic/query_optional02.srx", false);			
	}	

}
