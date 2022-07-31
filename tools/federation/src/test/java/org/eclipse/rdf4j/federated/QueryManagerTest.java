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

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Various tests for the junit framework
 *
 * @author as
 *
 */
public class QueryManagerTest {

	@Test
	public void testPrefix() {
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
		Assertions.assertTrue(QueryManager.prefixCheck.matcher(queryString).matches());
	}

	@Test
	public void testPrefix2() {
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\nPREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\nSELECT ?person ?name ?publication WHERE {\r\n ?person rdf:type foaf:Person .\r\n ?person foaf:name ?name .\r\n}";
		Assertions.assertTrue(QueryManager.prefixCheck.matcher(queryString).matches());
	}

	@Test
	public void testPrefix3() {
		/* find query prefixes */
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
		Set<String> prefixes = QueryManager.findQueryPrefixes(queryString);
		Assertions.assertTrue(prefixes.size() == 1);
		Assertions.assertTrue(prefixes.contains("rdf"));
	}

	@Test
	public void testPrefix4() {
		/* find query prefixes */
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/>";
		Set<String> prefixes = QueryManager.findQueryPrefixes(queryString);
		Assertions.assertTrue(prefixes.size() == 2);
		Assertions.assertTrue(prefixes.contains("rdf"));
		Assertions.assertTrue(prefixes.contains("foaf"));
	}

	@Test
	public void testPrefix5() {
		/* find query prefixes */
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\nPREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\nSELECT ?person ?name ?publication WHERE {\r\n ?person rdf:type foaf:Person .\r\n ?person foaf:name ?name .\r\n}";
		Set<String> prefixes = QueryManager.findQueryPrefixes(queryString);
		Assertions.assertTrue(prefixes.size() == 2);
		Assertions.assertTrue(prefixes.contains("rdf"));
		Assertions.assertTrue(prefixes.contains("foaf"));
	}

}
