/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for RDF/XML writers.
 *
 * @author Arjohn Kampman
 */
public class RDFXMLWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#RDFXML}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFXML;
	}

	/**
	 * Returns a new instance of {@link RDFXMLWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new RDFXMLWriter(out);
	}

	/**
	 * Returns a new instance of {@link RDFXMLWriter}.
	 *
	 * @throws URISyntaxException
	 */
	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException {
		return new RDFXMLWriter(out, new ParsedIRI(baseURI));
	}

	/**
	 * Returns a new instance of {@link RDFXMLWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new RDFXMLWriter(writer);
	}

	/**
	 * Returns a new instance of {@link RDFXMLWriter}.
	 *
	 * @throws URISyntaxException
	 */
	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException {
		return new RDFXMLWriter(writer, new ParsedIRI(baseURI));
	}
}
