/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import junit.framework.Test;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.serql.SeRQLParser;
import org.eclipse.rdf4j.query.parser.serql.SeRQLParserTestCase;

public class SeRQLParserTest extends SeRQLParserTestCase {
	public static Test suite() throws Exception {
		return SeRQLParserTestCase.suite(new Factory() {
			public Test createTest(String name, String queryFile, Value result) {
				return new SeRQLParserTest(name, queryFile, result);
			}
		});
	}

	public SeRQLParserTest(String name, String queryFile, Value result) {
		super(name, queryFile, result);
	}

	@Override
	protected QueryParser createParser() {
		return new SeRQLParser();
	}
}
