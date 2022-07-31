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
package org.eclipse.rdf4j.federated.misc;

import java.util.Arrays;

import org.eclipse.rdf4j.federated.QueryManager;
import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.junit.jupiter.api.Test;

public class PrefixTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {

		/* test select query retrieving all persons (2 endpoints) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("foaf", "http://xmlns.com/foaf/0.1/");
		qm.addPrefixDeclaration("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

		execute("/tests/prefix/query.rq", "/tests/prefix/query.srx", false, true);

		qm.addPrefixDeclaration("foaf", null);
		qm.addPrefixDeclaration("rdf", null);

	}

	@Test
	public void test2() throws Exception {

		/* test select query retrieving all persons, missing prefix, malformed query exception */
		try {
			prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
					"/tests/data/data4.ttl"));
			execute("/tests/prefix/query.rq", "/tests/prefix/query.srx", false, true);
		} catch (MalformedQueryException m) {
			// this exception is expected
			return;
		}
	}

	@Test
	public void test3() throws Exception {

		/* test select query retrieving all persons - duplicated prefix definition (in the query + qm) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		QueryManager qm = federationContext().getQueryManager();
		qm.addPrefixDeclaration("foaf", "http://xmlns.com/foaf/0.1/");

		execute("/tests/prefix/query2.rq", "/tests/prefix/query2.srx", false, true);

		qm.addPrefixDeclaration("foaf", null);
		qm.addPrefixDeclaration("rdf", null);
	}
}
