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
package org.eclipse.rdf4j.rio.trigstar;

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
public class TriGStarMimeTypeRDFFormatTest {
	private final RDFFormat expectedRDFFormat = RDFFormat.TRIGSTAR;

	@Test
	public void testApplicationXTrigStar() {
		assertEquals(expectedRDFFormat, Rio.getParserFormatForMIMEType("application/x-trigstar")
				.orElseThrow(Rio.unsupportedFormat(expectedRDFFormat)));
	}

	@Test
	public void testApplicationXTrigStarUtf8() {
		assertEquals(RDFFormat.TRIGSTAR, Rio.getParserFormatForMIMEType("application/x-trigstar;charset=UTF-8")
				.orElseThrow(Rio.unsupportedFormat(expectedRDFFormat)));
	}

	@Test
	public void testRDFFormatParser() {
		assertEquals(expectedRDFFormat, new TriGStarParser().getRDFFormat());
	}

	@Test
	public void testRDFFormatWriter() throws IOException {
		try (Writer w = new StringWriter()) {
			assertEquals(expectedRDFFormat, new TriGStarWriter(w).getRDFFormat());
		}
	}

	@Test
	public void testRDFFormatParserFactory() {
		assertEquals(expectedRDFFormat, new TriGStarParserFactory().getRDFFormat());
	}

	@Test
	public void testRDFFormatWriterFactory() {
		assertEquals(expectedRDFFormat, new TriGStarWriterFactory().getRDFFormat());
	}
}
