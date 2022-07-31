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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.trig.TriGWriter;

/**
 * An extension of {@link TriGWriter} that writes RDF-star documents in the TriG-star format by including the RDF-star
 * triples.
 *
 * @author Pavel Mihaylov
 */
public class TriGStarWriter extends TriGWriter {
	/**
	 * Creates a new TriGStarWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the TriG-star document to.
	 */
	public TriGStarWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new TriGStarWriter that will write to the supplied OutputStream using the supplied base IRI.
	 *
	 * @param out     The OutputStream to write the TriG-star document to.
	 * @param baseIRI The base IRI to use.
	 */
	public TriGStarWriter(OutputStream out, ParsedIRI baseIRI) {
		super(out, baseIRI);
	}

	/**
	 * Creates a new TriGStarWriter that will write to the supplied Writer.
	 *
	 * @param writer The Writer to write the TriG-star document to.
	 */
	public TriGStarWriter(Writer writer) {
		super(writer);
	}

	/**
	 * Creates a new TriGStarWriter that will write to the supplied Writer using the supplied base IRI.
	 *
	 * @param writer  The Writer to write the TriG-star document to.
	 * @param baseIRI The base IRI to use.
	 */
	public TriGStarWriter(Writer writer, ParsedIRI baseIRI) {
		super(writer, baseIRI);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIGSTAR;
	}

	@Override
	public boolean acceptsFileFormat(FileFormat format) {
		// since TriG-star is a superset of regular TriG, this Sink also accepts regular TriG
		// serialization
		return super.acceptsFileFormat(format) || RDFFormat.TRIG.equals(format);
	}

	@Override
	protected void writeTriple(Triple triple, boolean canShorten) throws IOException {
		writeTripleRDFStar(triple, canShorten);
	}
}
