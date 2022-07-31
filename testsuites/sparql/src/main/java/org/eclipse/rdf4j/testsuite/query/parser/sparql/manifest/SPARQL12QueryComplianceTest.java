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
 * A test suite that runs the SPARQL 1.2 community group's query tests.
 *
 * @author Jeen Broekstra
 *
 * @see <a href="https://github.com/w3c/sparql-12/">sparql 1.2</a>
 */
@RunWith(Parameterized.class)
public abstract class SPARQL12QueryComplianceTest extends SPARQLQueryComplianceTest {

	private static final String[] defaultIgnoredTests = {};

	private static final List<String> excludedSubdirs = List.of();

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
			SPARQLQueryTestManifest manifest = new SPARQLQueryTestManifest(pop, excludedSubdirs, false);
			tests.addAll(manifest.getTests());
			manifests.addAll(manifest.getSubManifests());
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

	protected static URL getManifestURL() {
		return SPARQL12QueryComplianceTest.class.getClassLoader().getResource("testcases-sparql-1.2/manifest-all.ttl");
	}

	public SPARQL12QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
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
