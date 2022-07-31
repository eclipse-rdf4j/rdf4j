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

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for Turtle-star writers.
 *
 * @author Pavel Mihaylov
 */
public class TurtleStarWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#TURTLESTAR}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLESTAR;
	}

	/**
	 * Returns a new instance of {@link TurtleStarWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new TurtleStarWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException {
		return new TurtleStarWriter(out, new ParsedIRI(baseURI));
	}

	/**
	 * Returns a new instance of {@link TurtleStarWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new TurtleStarWriter(writer);
	}

	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException {
		return new TurtleStarWriter(writer, new ParsedIRI(baseURI));
	}
}
