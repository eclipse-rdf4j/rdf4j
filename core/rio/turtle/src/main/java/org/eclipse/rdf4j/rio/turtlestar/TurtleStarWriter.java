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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

/**
 * An extension of {@link TurtleWriter} that writes RDF-star documents in the Turtle-star format by including the
 * RDF-star triples.
 *
 * @author Pavel Mihaylov
 */
public class TurtleStarWriter extends TurtleWriter {
	/**
	 * Creates a new TurtleStarWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the TurtleStar document to.
	 */
	public TurtleStarWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new TurtleStarWriter that will write to the supplied OutputStream using the supplied base IRI.
	 *
	 * @param out     The OutputStream to write the TurtleStar document to.
	 * @param baseIRI The base IRI to use.
	 */
	public TurtleStarWriter(OutputStream out, ParsedIRI baseIRI) {
		super(out, baseIRI);
	}

	/**
	 * Creates a new TurtleStarWriter that will write to the supplied Writer.
	 *
	 * @param writer The Writer to write the TurtleStar document to.
	 */
	public TurtleStarWriter(Writer writer) {
		super(writer);
	}

	/**
	 * Creates a new TurtleStarWriter that will write to the supplied Writer using the supplied base IRI.
	 *
	 * @param writer  The Writer to write the Turtle document to.
	 * @param baseIRI The base IRI to use.
	 */
	public TurtleStarWriter(Writer writer, ParsedIRI baseIRI) {
		super(writer, baseIRI);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLESTAR;
	}

	@Override
	public boolean acceptsFileFormat(FileFormat format) {
		// since Turtle-star is a superset of regular Turtle, this Sink also accepts regular Turtle
		// serialization
		return super.acceptsFileFormat(format) || RDFFormat.TURTLE.equals(format);
	}

	@Override
	protected void writeTriple(Triple triple, boolean canShorten) throws IOException {
		writeTripleRDFStar(triple, canShorten);
	}
}
