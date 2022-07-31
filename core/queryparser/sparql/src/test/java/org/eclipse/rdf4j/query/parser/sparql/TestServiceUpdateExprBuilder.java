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
package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.query.algebra.Service;
import org.junit.Test;

public class TestServiceUpdateExprBuilder {

	/**
	 * The test reproduces a {@link NullPointerException} that is thrown when parsing an update with several update
	 * expressions and one of these contain {@link Service} operator The NPE is thrown because the sourceString is set
	 * to the outermost operation container
	 */
	@Test
	public void testServiceWithMultipleUpdateExpr() {
		SPARQLParser parser = new SPARQLParser();
		String updateStr = "PREFIX family: <http://examples.ontotext.com/family#>\n" +
				"DROP ALL ;\n" +
				"INSERT {\n" +
				"    family:Alice family:knows family:Bob .\n" +
				"}\n" +
				"WHERE {\n" +
				"    SERVICE <repository:1> {\n" +
				"        family:Alice family:knows family:Bob .\n" +
				"    }\n" +
				"}";
		// should not throw NPE, but prior to 3.1.3 it does
		parser.parseUpdate(updateStr, null);
	}
}
