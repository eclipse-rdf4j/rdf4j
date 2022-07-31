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
package org.eclipse.rdf4j.common.text;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.junit.Test;

/**
 * @author Bart Hanssens
 */
public class StringUtilTest {
	private final String str = "http://example.com/?q=a&b='+ °àµ~<\"'\u00AA\u0AAA\uAAAA" +
			new String(Character.toChars(0x1AAAA));

	@Test
	public void testEncodeParsedASCII() throws IOException {
		// can't use toASCIIString, since it only does percent-encoding
		// String parsed = ParsedIRI.create(str).toASCIIString();
		String parsed = "http://example.com/?q=a&b='+%20\\u00B0\\u00E0\\u00B5~%3C%22'\\u00AA\\u0AAA\\uAAAA\\U0001AAAA";
		StringBuffer out = new StringBuffer();
		StringUtil.simpleEscapeIRI(str, out, true);
		String encoded = out.toString();
		assertEquals("Encoded does not match parsed", encoded, parsed);
	}

	@Test
	public void testEncodeParsed() throws IOException {
		String parsed = ParsedIRI.create(str).toString();

		StringBuffer out = new StringBuffer();
		StringUtil.simpleEscapeIRI(str, out, false);
		String encoded = out.toString();

		assertEquals("Encoded does not match parsed", encoded, parsed);
	}
}
