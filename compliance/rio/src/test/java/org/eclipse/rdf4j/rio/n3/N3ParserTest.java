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
package org.eclipse.rdf4j.rio.n3;

import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.testsuite.rio.n3.N3ParserTestCase;
import org.junit.Ignore;

import junit.framework.Test;

/**
 * JUnit test for the N3 parser that uses the tests that are available
 * <a href="http://www.w3.org/2000/10/swap/test/n3parser.tests">online</a>.
 */
@Ignore("FIXME: This test is badly broken")
public class N3ParserTest extends N3ParserTestCase {

	public static Test suite() throws Exception {
		return new N3ParserTest().createTestSuite();
	}

	@Override
	protected RDFParser createRDFParser() {
		return new TurtleParser();
	}
}
