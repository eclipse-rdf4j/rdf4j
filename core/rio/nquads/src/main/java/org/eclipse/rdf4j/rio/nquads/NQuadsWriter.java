/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.nquads;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesWriterSettings;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;

import java.io.OutputStream;
import java.io.Writer;
import java.io.IOException;

/**
 * RDFWriter implementation for the {@link org.eclipse.rdf4j.rio.RDFFormat#NQUADS
 * N-Quads} RDF format.
 * 
 * @since 2.7.0
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
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		try {
			// SUBJECT
			NTriplesUtil.append(st.getSubject(), writer);
			writer.write(" ");

			// PREDICATE
			NTriplesUtil.append(st.getPredicate(), writer);
			writer.write(" ");

			// OBJECT
			NTriplesUtil.append(st.getObject(), writer,
					getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL),
					getWriterConfig().get(NTriplesWriterSettings.ESCAPE_UNICODE));

			if (null != st.getContext()) {
				writer.write(" ");
				NTriplesUtil.append(st.getContext(), writer);
			}

			writer.write(" .\n");
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}
}
