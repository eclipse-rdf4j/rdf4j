/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtlestar;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.turtle.ArrangedWriter;

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

/**
 * An {@link RDFWriterFactory} for Turtle* writers.
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
		return new ArrangedWriter(new TurtleStarWriter(out));
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException {
		return new ArrangedWriter(new TurtleStarWriter(out, new ParsedIRI(baseURI)));
	}

	/**
	 * Returns a new instance of {@link TurtleStarWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new ArrangedWriter(new TurtleStarWriter(writer));
	}

	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException {
		return new ArrangedWriter(new TurtleStarWriter(writer, new ParsedIRI(baseURI)));
	}
}
