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
 * A test suite that runs the W3C Approved SPARQL 1.0 query tests.
 *
 * @author Jeen Broekstra
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs test</a>
 */
public abstract class SPARQL10QueryComplianceTest extends SPARQLQueryComplianceTest {

	private static final String[] defaultIgnoredTests = {
			// incompatible with SPARQL 1.1 - syntax for decimals was modified
			"Basic - Term 6",
			// incompatible with SPARQL 1.1 - syntax for decimals was modified
			"Basic - Term 7",
			// Test is incorrect: assumes timezoned date is comparable with non-timezoned
			"date-2",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are
			// identical
			"Strings: Distinct",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are
			// identical
			"All: Distinct",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are
			// identical
			"SELECT REDUCED ?x with strings"
	};

	public SPARQL10QueryComplianceTest() {
		super(List.of("service"));
		for (String defaultIgnoredTest : defaultIgnoredTests) {
			addIgnoredTest(defaultIgnoredTest);
		}
	}

	@TestFactory
	public Collection<DynamicTest> tests() {
		return getTestData("testcases-sparql-1.0-w3c/data-r2/manifest-evaluation.ttl");
	}
}
