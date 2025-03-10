/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ndjsonld;

import java.io.OutputStream;

import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

public class NDJSONLDParserHandlerTest extends AbstractParserHandlingTest {

	@Override
	protected RDFParser getParser() {
		return new NDJSONLDParser();
	}

	@Override
	protected RDFWriter createWriter(OutputStream output) {
		return new NDJSONLDWriter(output);
	}

	@Override
	public final void testUnknownLanguageWithMessageWithFailCase1() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageNoMessageNoFailCase1() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageNoMessageNoFailCase2() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageNoMessageNoFailCase3() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageNoMessageNoFailCase4() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageNoMessageNoFailCase5() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageWithMessageNoFailCase1() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageWithMessageNoFailCase2() throws Exception {
		// Not support by JSON-LD
	}

	@Override
	public void testUnknownLanguageWithMessageNoFailCase3() throws Exception {
		// Not support by JSON-LD
	}

}
