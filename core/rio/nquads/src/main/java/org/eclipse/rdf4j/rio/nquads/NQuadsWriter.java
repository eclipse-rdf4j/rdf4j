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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;

/**
 * RDFWriter implementation for the {@link org.eclipse.rdf4j.rio.RDFFormat#NQUADS N-Quads} RDF format.
 *
 * @author Joshua Shinavier
 */
public class NQuadsWriter extends NTriplesWriter {

	public NQuadsWriter(OutputStream outputStream) {
		super(outputStream);
	}

	public NQuadsWriter(Writer writer) {
		super(writer);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NQUADS;
	}

	@Override
	public void consumeStatement(Statement st) throws RDFHandlerException {
		try {
			// SUBJECT
			writeValue(st.getSubject());
			writer.write(" ");

			// PREDICATE
			writeValue(st.getPredicate());
			writer.write(" ");

			// OBJECT
			writeValue(st.getObject());

			if (null != st.getContext()) {
				writer.write(" ");
				writeValue(st.getContext());
			}

			writer.write(" .\n");
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}
}
