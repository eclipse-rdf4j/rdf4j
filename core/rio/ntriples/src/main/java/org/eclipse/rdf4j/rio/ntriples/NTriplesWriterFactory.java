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
package org.eclipse.rdf4j.rio.ntriples;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for N-Triples writers.
 *
 * @author Arjohn Kampman
 */
public class NTriplesWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#NTRIPLES}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NTRIPLES;
	}

	/**
	 * Returns a new instance of {@link NTriplesWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new NTriplesWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) {
		return getWriter(out);
	}

	/**
	 * Returns a new instance of {@link NTriplesWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new NTriplesWriter(writer);
	}

	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) {
		return getWriter(writer);
	}
}
