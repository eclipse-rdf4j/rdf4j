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

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for TriG-star writers.
 *
 * @author Pavel Mihaylov
 */
public class TriGStarWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#TRIGSTAR}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIGSTAR;
	}

	/**
	 * Returns a new instance of {@link TriGStarWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new TriGStarWriter(out);
	}

	/**
	 * Returns a new instance of {@link TriGStarWriter}.
	 *
	 * @throws URISyntaxException
	 */
	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException {
		return new TriGStarWriter(out, new ParsedIRI(baseURI));
	}

	/**
	 * Returns a new instance of {@link TriGStarWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new TriGStarWriter(writer);
	}

	/**
	 * Returns a new instance of {@link TriGStarWriter}.
	 *
	 * @throws URISyntaxException
	 */
	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException {
		return new TriGStarWriter(writer, new ParsedIRI(baseURI));
	}
}
