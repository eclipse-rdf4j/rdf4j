/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for HDT writers.
 * 
 * @author Bart Hanssens
 */
public class HDTWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#HDT}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.HDT;
	}

	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new HDTWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public RDFWriter getWriter(Writer writer) {
		throw new UnsupportedOperationException("HDT is binary, text writer not supported.");
	}

	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException {
		throw new UnsupportedOperationException("HDT is binary, text writer not supported.");
	}
}
