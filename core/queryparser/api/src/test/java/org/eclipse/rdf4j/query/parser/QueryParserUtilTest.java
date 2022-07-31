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
package org.eclipse.rdf4j.query.parser;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andreas Schwarte
 */
public class QueryParserUtilTest {

	@Test
	public void testGetRestOfQueryString() throws Exception {

		String queryString = "# this is a comment\n" + "PREFIX : <http://example.com/base/>\n"
				+ "# one more comment\r\n" + "SELECT * WHERE { ?s ?p ?o }";

		String restQuery = QueryParserUtil.removeSPARQLQueryProlog(queryString);
		Assert.assertEquals("SELECT * WHERE { ?s ?p ?o }", restQuery);
	}
}
