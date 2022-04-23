/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * A test suite that runs the W3C Approved SPARQL 1.1 Syntax tests.
 *
 * @author Jeen Broekstra
 *
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs tests</a>
 */
@RunWith(Parameterized.class)
public abstract class SPARQL11SyntaxComplianceTest extends SPARQLSyntaxComplianceTest {

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(getTestData());
	}

	public SPARQL11SyntaxComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			boolean positiveTest) {
		super(displayName, testURI, name, queryFileURL, positiveTest);
	}

	private static Object[][] getTestData() {

		List<Object[]> tests = new ArrayList<>();

		Deque<String> manifests = new ArrayDeque<>();
		manifests.add(
				SPARQL11SyntaxComplianceTest.class.getClassLoader()
						.getResource("testcases-sparql-1.1-w3c/manifest-all.ttl")
						.toExternalForm());
		while (!manifests.isEmpty()) {
			String pop = manifests.pop();
			SPARQLSyntaxManifest manifest = new SPARQLSyntaxManifest(pop);
			tests.addAll(manifest.tests);
			manifests.addAll(manifest.subManifests);
		}

		Object[][] result = new Object[tests.size()][6];
		tests.toArray(result);

		return result;
	}

	@Override
	protected abstract ParsedOperation parseOperation(String operation, String fileURL) throws MalformedQueryException;

	@Override
	protected Repository getDataRepository() {
		return null; // not needed in syntax tests
	}

}
