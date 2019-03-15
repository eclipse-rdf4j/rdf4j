/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import junit.framework.Test;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11SyntaxTest;

/**
 * Checks conformance of SPARQL query parsing against the W3C-approved SPARQL 1.0 test cases
 * 
 * @author Jeen Broekstra
 */
public class W3CApprovedSPARQL10SyntaxTest extends SPARQL11SyntaxTest {

	public static Test suite() throws Exception {
		return SPARQL11SyntaxTest.suite(new Factory() {

			public SPARQL11SyntaxTest createSPARQLSyntaxTest(String testURI, String testName, String testAction,
					boolean positiveTest) {
				return new W3CApprovedSPARQL10SyntaxTest(testURI, testName, testAction, positiveTest);
			}
		}, false);
	}

	public W3CApprovedSPARQL10SyntaxTest(String testURI, String name, String queryFileURL, boolean positiveTest) {
		super(testURI, name, queryFileURL, positiveTest);
	}

	protected ParsedOperation parseOperation(String operation, String fileURL) throws MalformedQueryException {
		return QueryParserUtil.parseOperation(QueryLanguage.SPARQL, operation, fileURL);
	}

}
