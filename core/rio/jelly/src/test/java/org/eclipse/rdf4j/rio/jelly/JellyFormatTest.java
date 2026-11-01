/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jelly;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import eu.neverblink.jelly.convert.rdf4j.rio.JellyFormat;

/**
 * @author Piotr Sowiński
 */
public class JellyFormatTest {

	@Test
	public void testJellyFormatDefinition() {
		// jelly-rdf4j also defines JellyFormat.JELLY, which should be identical to RDFFormat.JELLY
		assertEquals(RDFFormat.JELLY, JellyFormat.JELLY);
		assertEquals(RDFFormat.JELLY.supportsContexts(), JellyFormat.JELLY.supportsContexts());
		assertEquals(RDFFormat.JELLY.supportsTripleTerms(), JellyFormat.JELLY.supportsTripleTerms());
		assertEquals(RDFFormat.JELLY.supportsNamespaces(), JellyFormat.JELLY.supportsNamespaces());
		assertEquals(RDFFormat.JELLY.getFileExtensions(), JellyFormat.JELLY.getFileExtensions());
		assertEquals(RDFFormat.JELLY.getDefaultFileExtension(), JellyFormat.JELLY.getDefaultFileExtension());
		assertEquals(RDFFormat.JELLY.getMIMETypes(), JellyFormat.JELLY.getMIMETypes());
		assertEquals(RDFFormat.JELLY.getDefaultMIMEType(), JellyFormat.JELLY.getDefaultMIMEType());
	}
}
