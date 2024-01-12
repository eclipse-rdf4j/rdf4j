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
 * A test suite that runs the SPARQL 1.2 community group's query tests.
 *
 * @author Jeen Broekstra
 * @see <a href="https://github.com/w3c/sparql-12/">sparql 1.2</a>
 */
public abstract class SPARQL12QueryComplianceTest extends SPARQLQueryComplianceTest {

	private static final String[] defaultIgnoredTests = {};

	private static final List<String> excludedSubdirs = List.of();

	public SPARQL12QueryComplianceTest() {
		super(excludedSubdirs);
		for (String ig : defaultIgnoredTests) {
			addIgnoredTest(ig);
		}
	}

	@TestFactory
	public Collection<DynamicTest> tests() {
		return getTestData("testcases-sparql-1.2/manifest-all.ttl");
	}
}
