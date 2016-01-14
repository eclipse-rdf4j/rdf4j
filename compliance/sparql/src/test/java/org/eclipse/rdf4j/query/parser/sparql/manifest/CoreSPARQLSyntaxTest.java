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
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLSyntaxTest;

public class CoreSPARQLSyntaxTest extends SPARQLSyntaxTest {

	public static Test suite()
		throws Exception
	{
		return SPARQLSyntaxTest.suite(new Factory() {

			public SPARQLSyntaxTest createSPARQLSyntaxTest(String testURI, String testName, String testAction,
					boolean positiveTest)
			{
				return new CoreSPARQLSyntaxTest(testURI, testName, testAction, positiveTest);
			}
		});
	}

	public CoreSPARQLSyntaxTest(String testURI, String name, String queryFileURL, boolean positiveTest) {
		super(testURI, name, queryFileURL, positiveTest);
	}

	protected void parseQuery(String query, String queryFileURL)
		throws MalformedQueryException
	{
		QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, queryFileURL);
	}
}
