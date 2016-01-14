/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.nquads.NQuadsParser;
import org.eclipse.rdf4j.rio.trig.TriGParser;
import org.eclipse.rdf4j.rio.trig.TriGParserTestCase;

/**
 * JUnit test for the TriG parser.
 */
public class TriGParserTest extends TriGParserTestCase {

	public static junit.framework.Test suite()
		throws Exception
	{
		return new TriGParserTest().createTestSuite();
	}

	@Override
	protected RDFParser createTriGParser() {
		return new TriGParser();
	}

	@Override
	protected RDFParser createNQuadsParser() {
		return new NQuadsParser();
	}
}
