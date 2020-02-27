/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

import java.io.OutputStream;

/**
 * Unit tests for {@link JSONLDParser} related to handling of datatypes and languages.
 * 
 * @author Peter Ansell
 */
public class JSONLDParserHandlerTest extends AbstractParserHandlingTest {
	@Override
	protected RDFParser getParser() {
		return new JSONLDParser();
	}

	@Override
	protected RDFWriter createWriter(OutputStream output) {
		return new JSONLDWriter(output);
	}
}
