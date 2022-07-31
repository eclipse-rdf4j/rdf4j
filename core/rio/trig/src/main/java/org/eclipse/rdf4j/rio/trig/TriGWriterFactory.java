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
package org.eclipse.rdf4j.rio.trig;

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for TriG parsers.
 *
 * @author Arjohn Kampman
 */
public class TriGWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#TRIG}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIG;
	}

	/**
	 * Returns a new instance of {@link TriGWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new TriGWriter(out);
	}

	/**
	 * Returns a new instance of {@link TriGWriter}.
	 *
	 * @throws URISyntaxException
	 */
	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException {
		return new TriGWriter(out, new ParsedIRI(baseURI));
	}

	/**
	 * Returns a new instance of {@link TriGWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new TriGWriter(writer);
	}

	/**
	 * Returns a new instance of {@link TriGWriter}.
	 *
	 * @throws URISyntaxException
	 */
	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException {
		return new TriGWriter(writer, new ParsedIRI(baseURI));
	}
}
