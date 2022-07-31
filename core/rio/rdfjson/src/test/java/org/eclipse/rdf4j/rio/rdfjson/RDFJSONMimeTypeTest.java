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
package org.eclipse.rdf4j.rio.rdfjson;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

/**
 * @author Peter Ansell
 */
public class RDFJSONMimeTypeTest {

	@Test
	public void testApplicationRDFJSON() {
		assertEquals(RDFFormat.RDFJSON, Rio.getParserFormatForMIMEType("application/rdf+json")
				.orElseThrow(Rio.unsupportedFormat(RDFFormat.RDFJSON)));
	}

	@Test
	public void testApplicationRDFJSONUtf8() {
		assertEquals(RDFFormat.RDFJSON, Rio.getParserFormatForMIMEType("application/rdf+json;charset=UTF-8")
				.orElseThrow(Rio.unsupportedFormat(RDFFormat.RDFJSON)));
	}

}
