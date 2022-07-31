/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ndjsonld;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

public class NDJSONLDWriterFactory implements RDFWriterFactory {

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NDJSONLD;
	}

	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new NDJSONLDWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String baseURI) {
		return new NDJSONLDWriter(out, baseURI);
	}

	@Override
	public RDFWriter getWriter(Writer writer) {
		return new NDJSONLDWriter(writer);
	}

	@Override
	public RDFWriter getWriter(Writer writer, String baseURI) {
		return new NDJSONLDWriter(writer, baseURI);
	}
}
