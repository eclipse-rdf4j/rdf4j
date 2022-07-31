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
package org.eclipse.rdf4j.rio.nquads;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for {@link RDFFormat#NQUADS N-Quads} writers.
 *
 * @author Peter Ansell
 */
public class NQuadsWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#NQUADS}.
	 */
	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NQUADS;
	}

	/**
	 * Returns a new instance of {@link NQuadsWriter}.
	 */
	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new NQuadsWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) {
		return getWriter(out);
	}

	/**
	 * Returns a new instance of {@link NQuadsWriter}.
	 */
	@Override
	public RDFWriter getWriter(Writer writer) {
		return new NQuadsWriter(writer);
	}

	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) {
		return getWriter(writer);
	}
}
