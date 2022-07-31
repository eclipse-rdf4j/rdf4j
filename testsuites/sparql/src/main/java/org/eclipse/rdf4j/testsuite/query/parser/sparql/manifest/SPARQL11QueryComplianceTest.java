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

import java.net.URL;
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
 * A test suite that runs the W3C Approved SPARQL 1.1 query tests.
 *
 * @author Jeen Broekstra
 *
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs tests</a>
 */
@RunWith(Parameterized.class)
public abstract class SPARQL11QueryComplianceTest extends SPARQLQueryComplianceTest {

	private static final String[] defaultIgnoredTests = {
			// test case incompatible with RDF 1.1 - see
			// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
			"STRDT() TypeErrors",
			// test case incompatible with RDF 1.1 - see
			// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
			"STRLANG() TypeErrors",
			// known issue: SES-937
			"sq03 - Subquery within graph pattern, graph variable is not bound"
	};

	private static final List<String> excludedSubdirs = List.of("service");

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(getTestData());
	}

	protected static Object[][] getTestData() {
		List<Object[]> tests = new ArrayList<>();

		Deque<String> manifests = new ArrayDeque<>();
		manifests.add(getManifestURL().toExternalForm());
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

	protected static URL getManifestURL() {
		return SPARQL11QueryComplianceTest.class.getClassLoader()
				.getResource("testcases-sparql-1.1-w3c/manifest-all.ttl");
	}

	public SPARQL11QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered, boolean laxCardinality) {
		super(displayName, testURI, name, queryFileURL, resultFileURL, dataset, ordered, laxCardinality);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		for (String defaultIgnoredTest : defaultIgnoredTests) {
			addIgnoredTest(defaultIgnoredTest);
		}
	}

}
