/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

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
