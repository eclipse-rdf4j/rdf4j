/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * A test suite that runs the W3C Approved SPARQL 1.1 query tests.
 *
 * @author Jeen Broekstra
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs tests</a>
 */
public abstract class SPARQL11QueryComplianceTest extends SPARQLQueryComplianceTest {

	public SPARQL11QueryComplianceTest() {
		super(List.of("service"));
		for (String ig : defaultIgnoredTests) {
			addIgnoredTest(ig);
		}
	}

	private static final String[] defaultIgnoredTests = {
			// test case incompatible with RDF 1.1 - see
			// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
			"STRDT() TypeErrors",
			// test case incompatible with RDF 1.1 - see
			// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
			"STRLANG() TypeErrors",
			// known issue: SES-937
			"sq03 - Subquery within graph pattern, graph variable is not bound" };

	@TestFactory
	public Collection<DynamicTest> tests() {
		return getTestData("testcases-sparql-1.1-w3c/manifest-all.ttl");
	}
}
