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
package org.eclipse.rdf4j.rio.turtlestar;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

/**
 * @author Pavel Mihaylov
 */
public class TurtleStarMimeTypeRDFFormatTest {
	private final RDFFormat expectedRDFFormat = RDFFormat.TURTLESTAR;

	@Test
	public void testApplicationXTurtleStarUtf8() {
		assertEquals(expectedRDFFormat, Rio.getParserFormatForMIMEType("application/x-turtlestar;charset=UTF-8")
				.orElseThrow(Rio.unsupportedFormat(expectedRDFFormat)));
	}

	@Test
	public void testApplicationXTurtleStar() {
		assertEquals(expectedRDFFormat, Rio.getParserFormatForMIMEType("application/x-turtlestar")
				.orElseThrow(Rio.unsupportedFormat(expectedRDFFormat)));
	}

	@Test
	public void testRDFFormatParser() {
		assertEquals(expectedRDFFormat, new TurtleStarParser().getRDFFormat());
	}

	@Test
	public void testRDFFormatWriter() throws IOException {
		try (Writer w = new StringWriter()) {
			assertEquals(expectedRDFFormat, new TurtleStarWriter(w).getRDFFormat());
		}
	}

	@Test
	public void testRDFFormatParserFactory() {
		assertEquals(expectedRDFFormat, new TurtleStarParserFactory().getRDFFormat());
	}

	@Test
	public void testRDFFormatWriterFactory() {
		assertEquals(expectedRDFFormat, new TurtleStarWriterFactory().getRDFFormat());
	}
}
