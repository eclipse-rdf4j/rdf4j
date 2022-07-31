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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.eclipse.rdf4j.query.Dataset;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * A test suite that runs the W3C Approved SPARQL 1.0 query tests.
 *
 * @author Jeen Broekstra
 *
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs test</a>
 */
@RunWith(Parameterized.class)
public abstract class SPARQL10QueryComplianceTest extends SPARQLQueryComplianceTest {

	private static final String[] defaultIgnoredTests = {
			// incompatible with SPARQL 1.1 - syntax for decimals was modified
			"Basic - Term 6",
			// incompatible with SPARQL 1.1 - syntax for decimals was modified
			"Basic - Term 7",
			// Test is incorrect: assumes timezoned date is comparable with non-timezoned
			"date-2",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are identical
			"Strings: Distinct",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are identical
			"All: Distinct",
			// Incompatible with SPARQL 1.1 - string-typed literals and plain literals are identical
			"SELECT REDUCED ?x with strings"
	};

	private static final List<String> excludedSubdirs = List.of("service");

	/**
	 * @param displayName
	 * @param testURI
	 * @param name
	 * @param queryFileURL
	 * @param resultFileURL
	 * @param dataset
	 * @param ordered
	 */
	public SPARQL10QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered, boolean laxCardinality) {
		super(displayName, testURI, name, queryFileURL, resultFileURL, dataset, ordered, laxCardinality);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(getTestData());
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		for (String defaultIgnoredTest : defaultIgnoredTests) {
			addIgnoredTest(defaultIgnoredTest);
		}
	}

	private static Object[][] getTestData() {

		List<Object[]> tests = new ArrayList<>();

		Deque<String> manifests = new ArrayDeque<>();
		manifests.add(
				SPARQL10QueryComplianceTest.class.getClassLoader()
						.getResource("testcases-sparql-1.0-w3c/data-r2/manifest-evaluation.ttl")
						.toExternalForm());
		while (!manifests.isEmpty()) {
			String pop = manifests.pop();
			SPARQLQueryTestManifest manifest = new SPARQLQueryTestManifest(pop, excludedSubdirs);
			tests.addAll(manifest.getTests());
			manifests.addAll(manifest.getSubManifests());
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

}
