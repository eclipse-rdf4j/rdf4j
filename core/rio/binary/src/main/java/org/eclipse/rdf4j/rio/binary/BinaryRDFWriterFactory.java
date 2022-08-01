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
package org.eclipse.rdf4j.rio.binary;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for Binary RDF writers.
 *
 * @author Arjohn Kampman
 */
public class BinaryRDFWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#BINARY}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.BINARY;
	}

	/**
	 * Returns a new instance of {@link BinaryRDFWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new BinaryRDFWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) {
		return new BinaryRDFWriter(out);
	}

	/**
	 * throws UnsupportedOperationException
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		throw new UnsupportedOperationException();
	}

	/**
	 * throws UnsupportedOperationException
	 */
	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) {
		throw new UnsupportedOperationException();
	}
}
