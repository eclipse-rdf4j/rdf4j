/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.Dataset;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class OldSPARQL11QueryComplianceTest extends SPARQLQueryComplianceTest {

	private static final String[] defaultIgnoredTests = {

	};

	private static final List<String> excludedSubdirs = List.of(
		//			"aggregates",
//			"bindings",
//			"bsbm",
//			"builtin",
//			"expressions",
//			"negation",
//			"property-paths",
//			"subquery"
	);

	public OldSPARQL11QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL,
			Dataset dataset, boolean ordered, boolean laxCardinality) {
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
				SPARQLQueryComplianceTest.class.getClassLoader()
						.getResource("testcases-sparql-1.1/manifest-evaluation.ttl")
						.toExternalForm());
		while (!manifests.isEmpty()) {
			List<String> excluded = Stream.concat(Stream.of("service"), excludedSubdirs.stream())
					.collect(Collectors.toList());
			String pop = manifests.pop();
			SPARQLQueryTestManifest manifest = new SPARQLQueryTestManifest(pop, excluded, false);
			tests.addAll(manifest.getTests());
			manifests.addAll(manifest.getSubManifests());
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

}
