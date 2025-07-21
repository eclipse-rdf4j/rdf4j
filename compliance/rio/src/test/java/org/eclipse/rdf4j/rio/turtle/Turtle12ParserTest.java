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
package org.eclipse.rdf4j.rio.turtle;

import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.testsuite.rio.turtle.Turtle12ParserTestCase;

import junit.framework.Test;

/**
 * JUnit test for the Turtle parser that uses the tests that are available
 * <a href="">online</a>.
 */
public class Turtle12ParserTest extends Turtle12ParserTestCase {
    public static Test suite() throws Exception {
        return new Turtle12ParserTest().createTestSuite();
    }

    @Override
    protected RDFParser createTurtleParser() {
        RDFParser result = new TurtleParser();
        result.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
        return result;
    }

    @Override
    protected RDFParser createNTriplesParser() {
        return new NTriplesParser();
    }
}
