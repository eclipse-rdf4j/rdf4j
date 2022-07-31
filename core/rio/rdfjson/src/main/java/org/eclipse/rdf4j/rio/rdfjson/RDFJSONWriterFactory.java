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
package org.eclipse.rdf4j.rio.rdfjson;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for RDF/JSON writers.
 *
 * @author Peter Ansell
 */
public class RDFJSONWriterFactory implements RDFWriterFactory {

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFJSON;
	}

	@Override
	public RDFWriter getWriter(final OutputStream out) {
		return new RDFJSONWriter(out, this.getRDFFormat());
	}

	@Override
	public RDFWriter getWriter(final OutputStream out, String baseURI) {
		return getWriter(out);
	}

	@Override
	public RDFWriter getWriter(final Writer writer) {
		return new RDFJSONWriter(writer, this.getRDFFormat());
	}

	@Override
	public RDFWriter getWriter(final Writer writer, String baseURI) {
		return getWriter(writer);
	}

}
