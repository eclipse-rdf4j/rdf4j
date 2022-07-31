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

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import junit.framework.TestCase;

/**
 * @author James Leigh
 */
public class TurtleMimeTypeTest extends TestCase {

	public void testTextTurtle() {
		assertEquals(RDFFormat.TURTLE,
				Rio.getParserFormatForMIMEType("text/turtle").orElseThrow(Rio.unsupportedFormat(RDFFormat.TURTLE)));
	}

	public void testTextTurtleUtf8() {
		assertEquals(RDFFormat.TURTLE, Rio.getParserFormatForMIMEType("text/turtle;charset=UTF-8")
				.orElseThrow(Rio.unsupportedFormat(RDFFormat.TURTLE)));
	}

	public void testApplicationXTurtle() {
		assertEquals(RDFFormat.TURTLE, Rio.getParserFormatForMIMEType("application/x-turtle")
				.orElseThrow(Rio.unsupportedFormat(RDFFormat.TURTLE)));
	}

}
