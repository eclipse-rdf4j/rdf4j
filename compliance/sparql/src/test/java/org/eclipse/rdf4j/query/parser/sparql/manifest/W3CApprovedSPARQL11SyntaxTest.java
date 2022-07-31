/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11SyntaxComplianceTest;

public class W3CApprovedSPARQL11SyntaxTest extends SPARQL11SyntaxComplianceTest {

	/**
	 * @param displayName
	 * @param testURI
	 * @param name
	 * @param queryFileURL
	 * @param resultFileURL
	 * @param positiveTest
	 */
	public W3CApprovedSPARQL11SyntaxTest(String displayName, String testURI, String name, String queryFileURL,
			boolean positiveTest) {
		super(displayName, testURI, name, queryFileURL, positiveTest);
	}

	@Override
	protected ParsedOperation parseOperation(String operation, String fileURL) throws MalformedQueryException {
		return QueryParserUtil.parseOperation(QueryLanguage.SPARQL, operation, fileURL);
	}

}
