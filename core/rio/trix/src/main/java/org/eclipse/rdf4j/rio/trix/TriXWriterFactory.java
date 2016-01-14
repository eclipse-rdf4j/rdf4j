/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trix;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for TriX writers.
 * 
 * @author Arjohn Kampman
 */
public class TriXWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#TRIX}.
	 */
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIX;
	}

	/**
	 * Returns a new instance of {@link TriXWriter}.
	 */
	public RDFWriter getWriter(OutputStream out) {
		return new TriXWriter(out);
	}

	/**
	 * Returns a new instance of {@link TriXWriter}.
	 */
	public RDFWriter getWriter(Writer writer) {
		return new TriXWriter(writer);
	}
}
