/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtlestar;

import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

import java.io.OutputStream;

/**
 * @author Pavel Mihaylov
 */
public class TurtleStarHandlingTest extends AbstractParserHandlingTest {
	@Override
	protected RDFParser getParser() {
		return new TurtleStarParser();
	}

	@Override
	protected RDFWriter createWriter(OutputStream output) {
		return new TurtleStarWriter(output);
	}
}
