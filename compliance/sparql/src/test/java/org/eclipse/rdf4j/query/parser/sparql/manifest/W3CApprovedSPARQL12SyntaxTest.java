/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL12SyntaxComplianceTest;

public class W3CApprovedSPARQL12SyntaxTest extends SPARQL12SyntaxComplianceTest {

	public W3CApprovedSPARQL12SyntaxTest() {
		setTestsSource("testcases-sparql-1.2-w3c/manifest.ttl");
	}

	@Override
	protected ParsedOperation parseOperation(String operation, String fileURL) throws MalformedQueryException {
		QueryParser parser = QueryParserUtil.createParser(QueryLanguage.SPARQL);

		if (fileURL.endsWith(".ru")) {
			return parser.parseUpdate(operation, fileURL);
		} else {
			return parser.parseQuery(operation, fileURL);
		}
	}

}
