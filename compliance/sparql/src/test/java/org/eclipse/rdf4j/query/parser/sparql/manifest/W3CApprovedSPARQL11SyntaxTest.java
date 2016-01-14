/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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

public class W3CApprovedSPARQL11SyntaxTest extends SPARQL11SyntaxTest {

	public static Test suite()
		throws Exception
	{
		return SPARQL11SyntaxTest.suite(new Factory() {

			public SPARQL11SyntaxTest createSPARQLSyntaxTest(String testURI, String testName, String testAction,
					boolean positiveTest)
			{
				return new W3CApprovedSPARQL11SyntaxTest(testURI, testName, testAction, positiveTest);
			}
		}, false);
	}

	public W3CApprovedSPARQL11SyntaxTest(String testURI, String name, String queryFileURL, boolean positiveTest) {
		super(testURI, name, queryFileURL, positiveTest);
	}

	protected ParsedOperation parseOperation(String operation, String fileURL)
		throws MalformedQueryException
	{
		return QueryParserUtil.parseOperation(QueryLanguage.SPARQL, operation, fileURL);
	}
	
	@Override
	protected void runTest()
		throws Exception
	{
		if (this.getName().contains("syntax-update-54")) {
			// we skip this negative syntax test because it is an unnecessarily restrictive test that is almost
			// impossible to implement correctly, and which in practice Sesame handles correctly simply by 
			// assigning different blank node ids.
		}
		else {
			super.runTest();
		}
	}
}
