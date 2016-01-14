/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import junit.framework.Test;

import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.TurtleParserSettings;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParserTestCase;

/**
 * JUnit test for the Turtle parser that uses the tests that are available <a
 * href="https://dvcs.w3.org/hg/rdf/file/09a9da374a9f/rdf-turtle/">online</a>.
 */
public class TurtleParserTest extends TurtleParserTestCase {

	public static Test suite()
		throws Exception
	{
		return new TurtleParserTest().createTestSuite();
	}

	@Override
	protected RDFParser createTurtleParser() {
		RDFParser result = new TurtleParser();
		return result;
	}

	@Override
	protected RDFParser createNTriplesParser() {
		return new NTriplesParser();
	}
}
